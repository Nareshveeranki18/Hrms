package com.hrms.backend.controller;

import com.hrms.backend.entity.Shift;
import com.hrms.backend.repository.ShiftRepository;
import com.hrms.backend.repository.UserRepository; // Adjust if your repo is named differently
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/shifts")
@CrossOrigin(originPatterns = "*")
public class ShiftController {

    private final ShiftRepository shiftRepository;
    private final UserRepository userRepository; 

    public ShiftController(ShiftRepository shiftRepository, UserRepository userRepository) {
        this.shiftRepository = shiftRepository;
        this.userRepository = userRepository;
    }

    private static ResponseEntity<Map<String, String>> msg(HttpStatus status, String text) {
        return ResponseEntity.status(status).body(Map.of("message", text));
    }

    // AC: Only SUPER_ADMIN and HR_ADMIN may change shifts
    private boolean canEdit(Authentication auth) {
        if (auth == null) return false;
        return userRepository.findByEmail(auth.getName())
                .map(u -> u.getRole() != null
                        && ("SUPER_ADMIN".equals(u.getRole()) || "HR_ADMIN".equals(u.getRole())))
                .orElse(false);
    }

    // AC: Validation Rules for Shifts and Night Shifts
    private String validate(Shift s) {
        if (s.getName() == null || s.getName().trim().length() < 2 || s.getName().trim().length() > 50)
            return "Shift name must be 2 to 50 characters";
        if (s.getStartTime() == null || s.getEndTime() == null)
            return "Start time and end time are required";
        if (s.getStartTime().equals(s.getEndTime()))
            return "Start time and end time cannot be the same";
            
        Integer brk = s.getBreakDurationMinutes();
        if (brk == null || brk < 0 || brk > 240)
            return "Break must be between 0 and 240 minutes";
            
        long span = Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
        if (span <= 0) span += 24 * 60; // night shift allowance
        if (brk >= span) return "The break must be shorter than the shift";
        
        return null;
    }

    @GetMapping
    public List<Shift> list() {
        return shiftRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Shift body, Authentication auth) {
        if (!canEdit(auth)) return msg(HttpStatus.FORBIDDEN, "Only HR Admin or Super Admin can create shifts");
        
        String problem = validate(body);
        if (problem != null) return msg(HttpStatus.BAD_REQUEST, problem);
        
        body.setId(null);
        body.setName(body.getName().trim());
        if (shiftRepository.existsByNameIgnoreCase(body.getName())) {
            return msg(HttpStatus.CONFLICT, "Shift name already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(shiftRepository.save(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Shift body, Authentication auth) {
        if (!canEdit(auth)) return msg(HttpStatus.FORBIDDEN, "Only HR Admin or Super Admin can edit shifts");
        
        return shiftRepository.findById(id).map(existing -> {
            String problem = validate(body);
            if (problem != null) return msg(HttpStatus.BAD_REQUEST, problem);
            
            String name = body.getName().trim();
            if (shiftRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
                return msg(HttpStatus.CONFLICT, "Shift name already exists");
            }
            
            existing.setName(name);
            existing.setStartTime(body.getStartTime());
            existing.setEndTime(body.getEndTime());
            existing.setBreakDurationMinutes(body.getBreakDurationMinutes());
            existing.setWeeklyOffDays(body.getWeeklyOffDays());
            
            return ResponseEntity.ok((Object) shiftRepository.save(existing));
        }).orElseGet(() -> msg(HttpStatus.NOT_FOUND, "Shift not found"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        if (!canEdit(auth)) return msg(HttpStatus.FORBIDDEN, "Only HR Admin or Super Admin can delete shifts");
        if (!shiftRepository.existsById(id)) return msg(HttpStatus.NOT_FOUND, "Shift not found");
        
        try {
            shiftRepository.deleteById(id);
            return msg(HttpStatus.OK, "Shift deleted");
        } catch (Exception e) {
            return msg(HttpStatus.CONFLICT, "Shift is assigned to employees and cannot be deleted");
        }
    }
}