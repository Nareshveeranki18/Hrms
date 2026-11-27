package com.hrms.backend.controller;

import com.hrms.backend.entity.Employee;
import com.hrms.backend.entity.PayrollBatch;
import com.hrms.backend.entity.PayrollBatch.BatchStatus;
import com.hrms.backend.entity.SalaryStructure;
import com.hrms.backend.repository.EmployeeRepository;
import com.hrms.backend.repository.SalaryStructureRepository;
import com.hrms.backend.service.PayrollBatchService;
import com.hrms.backend.service.SalaryCalculationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payroll")
public class PayrollWorkflowController {

    private final PayrollBatchService payrollBatchService;
    private final SalaryCalculationService salaryCalculationService;
    private final EmployeeRepository employeeRepository;
    private final SalaryStructureRepository salaryStructureRepository;

    public PayrollWorkflowController(PayrollBatchService payrollBatchService,
                                     SalaryCalculationService salaryCalculationService,
                                     EmployeeRepository employeeRepository,
                                     SalaryStructureRepository salaryStructureRepository) {
        this.payrollBatchService = payrollBatchService;
        this.salaryCalculationService = salaryCalculationService;
        this.employeeRepository = employeeRepository;
        this.salaryStructureRepository = salaryStructureRepository;
    }

    // ==================== Task N-18: Payroll Batch APIs ====================

    @GetMapping("/batches")
    public ResponseEntity<?> listBatches() {
        return ResponseEntity.ok(payrollBatchService.getAllBatches());
    }

    @GetMapping("/batches/{month}")
    public ResponseEntity<?> getBatchByMonth(@PathVariable String month) {
        return ResponseEntity.ok(payrollBatchService.getBatchByMonth(month));
    }

    @PostMapping("/batches")
    public ResponseEntity<?> createBatch(@RequestBody Map<String, String> request,
                                         Authentication authentication) {
        String month = request.get("payrollMonth");
        String actor = resolveActor(authentication, request.get("createdBy"));
        PayrollBatch created = payrollBatchService.createBatch(month, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/batches/{batchId}/status")
    public ResponseEntity<?> updateBatchStatus(@PathVariable Long batchId,
                                               @RequestBody Map<String, String> request,
                                               Authentication authentication) {
        String statusStr = request.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "status is required (DRAFT, PROCESSING, APPROVED, LOCKED)"));
        }

        BatchStatus targetStatus;
        try {
            targetStatus = BatchStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid status. Use: DRAFT, PROCESSING, APPROVED, LOCKED"));
        }

        String actor = resolveActor(authentication, request.get("performedBy"));
        PayrollBatch updated = payrollBatchService.transitionStatus(batchId, targetStatus, actor);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/batches/{batchId}/approve")
    public ResponseEntity<?> approveBatch(@PathVariable Long batchId,
                                          @RequestBody(required = false) Map<String, String> request,
                                          Authentication authentication) {
        String fallback = request != null ? request.get("performedBy") : null;
        return ResponseEntity.ok(
                payrollBatchService.transitionStatus(batchId, BatchStatus.APPROVED, resolveActor(authentication, fallback)));
    }

    @PutMapping("/batches/{batchId}/lock")
    public ResponseEntity<?> lockBatch(@PathVariable Long batchId,
                                       @RequestBody(required = false) Map<String, String> request,
                                       Authentication authentication) {
        String fallback = request != null ? request.get("performedBy") : null;
        return ResponseEntity.ok(
                payrollBatchService.transitionStatus(batchId, BatchStatus.LOCKED, resolveActor(authentication, fallback)));
    }

    // ==================== Task N-20: Single Employee Calculation API ====================

    @PostMapping("/calculate-single/{employeeId}")
    public ResponseEntity<?> calculateSingleEmployee(@PathVariable Long employeeId,
                                                     @RequestBody(required = false) Map<String, Object> request) {
        Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Employee not found with id: " + employeeId));
        }

        Employee employee = employeeOpt.get();
        Map<String, Object> body = (request != null) ? request : Map.of();

        int lopDays = toInt(body.get("lopDays"), 0);
        int totalWorkingDays = toInt(body.get("totalWorkingDays"), 30);
        double otherDeductions = toDouble(body.get("otherDeductions"), 0.0);

        String state = body.get("state") != null
                ? body.get("state").toString()
                : (employee.getState() != null ? employee.getState() : "Telangana");

        boolean manualComponentsProvided = body.containsKey("basic")
                || body.containsKey("hra")
                || body.containsKey("allowances");

        Map<String, Object> breakdown;
        String source;

        if (manualComponentsProvided) {
            double basic = toDouble(body.get("basic"), 0.0);
            double hra = toDouble(body.get("hra"), 0.0);
            double allowances = toDouble(body.get("allowances"), 0.0);
            breakdown = salaryCalculationService.calculateSalaryBreakdown(
                    basic, hra, allowances, otherDeductions, lopDays, totalWorkingDays, state);
            source = "MANUAL_OVERRIDE";
        } else {
            SalaryStructure structure = findStructureByDesignation(employee.getDesignation());
            breakdown = salaryCalculationService.calculateFromStructure(
                    structure, lopDays, totalWorkingDays, otherDeductions, state);
            source = (structure != null) ? "SALARY_STRUCTURE" : "NO_STRUCTURE_FOUND_DEFAULT_ZERO";
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId", employee.getId());
        response.put("employeeCode", employee.getEmployeeCode());
        response.put("employeeName", employee.getFullName());
        response.put("designation", employee.getDesignation());
        response.put("payrollMonth", body.getOrDefault("payrollMonth", "PREVIEW"));
        response.put("calculationSource", source);
        response.putAll(breakdown);

        return ResponseEntity.ok(response);
    }

    private SalaryStructure findStructureByDesignation(String designation) {
        if (designation == null || designation.isBlank()) {
            return null;
        }
        return salaryStructureRepository.findAll().stream()
                .filter(s -> s.getDesignation() != null && s.getDesignation().equalsIgnoreCase(designation.trim()))
                .findFirst()
                .orElse(null);
    }

    private String resolveActor(Authentication authentication, String fallback) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return authentication.getName();
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : "SYSTEM_ADMIN";
    }

    private double toDouble(Object value, double defaultVal) {
        if (value == null) return defaultVal;
        if (value instanceof Number num) return num.doubleValue();
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private int toInt(Object value, int defaultVal) {
        if (value == null) return defaultVal;
        if (value instanceof Number num) return num.intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}