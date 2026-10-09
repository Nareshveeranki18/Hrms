package com.hrms.backend.controller;

import com.hrms.backend.entity.Employee;
import com.hrms.backend.service.EmployeeService;
import com.hrms.backend.service.ShiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/employees")
@CrossOrigin(originPatterns = "*") // CORS fixed for authenticated requests
public class EmployeeController {

    private final EmployeeService employeeService;
    private final ShiftService shiftService; // Added ShiftService

    // Updated constructor for dependency injection
    public EmployeeController(EmployeeService employeeService, ShiftService shiftService) {
        this.employeeService = employeeService;
        this.shiftService = shiftService;
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeService.saveEmployee(employee);
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    // NEW ENDPOINT FOR TASK N-8
    @PostMapping("/{id}/assign-shift")
    public ResponseEntity<?> assignShift(@PathVariable Long id, @RequestBody Map<String, Long> payload) {
        Long shiftId = payload.get("shiftId");
        if (shiftId == null) {
            return ResponseEntity.badRequest().body("shiftId is required in the body");
        }
        
        try {
            shiftService.assignShiftToEmployee(id, shiftId);
            return ResponseEntity.ok(Map.of("message", "Shift assigned successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}