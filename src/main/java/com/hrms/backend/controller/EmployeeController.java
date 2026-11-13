package com.hrms.backend.controller;

import com.hrms.backend.entity.Employee;
import com.hrms.backend.repository.EmployeeRepository;
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
    private final ShiftService shiftService;
    private final EmployeeRepository employeeRepository;

    // Preserved original dependencies and added EmployeeRepository for ID lookups/updates
    public EmployeeController(EmployeeService employeeService, 
                                ShiftService shiftService, 
                                EmployeeRepository employeeRepository) {
        this.employeeService = employeeService;
        this.shiftService = shiftService;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping
    public Employee createEmployee(@RequestBody Employee employee) {
        return employeeService.saveEmployee(employee);
    }

    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    // 1. GET /api/v1/employees/{id} - Requested by frontend colleague
    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployee(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 2. PUT /api/v1/employees/{id} - Requested by frontend colleague
    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id,
                                                   @RequestBody EmployeeUpdateRequest request) {
        return employeeRepository.findById(id).map(employee -> {
            if (request.getFirstName() != null) employee.setFirstName(request.getFirstName());
            if (request.getLastName() != null) employee.setLastName(request.getLastName());
            if (request.getEmail() != null) employee.setEmail(request.getEmail());
            if (request.getPhone() != null) employee.setPhone(request.getPhone());
            if (request.getDepartment() != null) employee.setDepartment(request.getDepartment());
            if (request.getStatus() != null) employee.setStatus(request.getStatus());

            Employee updatedEmployee = employeeRepository.save(employee);
            return ResponseEntity.ok(updatedEmployee);
        }).orElse(ResponseEntity.notFound().build());
    }

    // EXISTING ENDPOINT FOR TASK N-8
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

    // Static inner DTO class so no extra file is required
    public static class EmployeeUpdateRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String department;
        private String designation;
        private String status;

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}