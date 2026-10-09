package com.hrms.backend.service;

import com.hrms.backend.entity.Employee;
import com.hrms.backend.entity.Shift;
import com.hrms.backend.repository.EmployeeRepository;
import com.hrms.backend.repository.ShiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShiftService {

    @Autowired
    private ShiftRepository shiftRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;

    public Shift createShift(Shift shift) {
        // Validation: Start time must be before End time
        if (shift.getStartTime() != null && shift.getEndTime() != null) {
            if (shift.getStartTime().isAfter(shift.getEndTime())) {
                throw new IllegalArgumentException("Shift Start Time must be before End Time.");
            }
        }
        return shiftRepository.save(shift);
    }

    public Employee assignShiftToEmployee(Long employeeId, Long shiftId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
                
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found"));
                
        employee.setShift(shift);
        return employeeRepository.save(employee);
    }
}