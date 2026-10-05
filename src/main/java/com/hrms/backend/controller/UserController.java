package com.hrms.backend.controller;

import com.hrms.backend.dto.UserCreationDto;
import com.hrms.backend.entity.Role;
import com.hrms.backend.entity.User;
import com.hrms.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getRoles() {
        return ResponseEntity.ok(userService.getAllRoles());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody UserCreationDto dto) {
        try {
            User createdUser = userService.createUserWithRole(dto);
            
            // Return DTO mapping instead of raw entity to prevent LazyInitializationException
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
            response.put("userId", createdUser.getId());
            response.put("email", createdUser.getEmail());
            response.put("fullName", createdUser.getFullName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}