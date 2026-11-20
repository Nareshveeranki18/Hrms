package com.hrms.backend.controller;

import com.hrms.backend.entity.Role;
import com.hrms.backend.entity.User;
import com.hrms.backend.repository.RoleRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.security.JwtUtil;
import com.hrms.backend.service.EmailService;
import com.hrms.backend.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // Stores one-time reset tokens with 15-minute expiry (token -> email + expiry)
    private final Map<String, ResetTokenData> resetTokens = new ConcurrentHashMap<>();

    private record ResetTokenData(String email, Instant expiresAt) {}

    public AuthController(JwtUtil jwtUtil, UserRepository userRepository,
                          PasswordEncoder passwordEncoder, TokenBlacklistService tokenBlacklistService,
                          RoleRepository roleRepository, EmailService emailService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenBlacklistService = tokenBlacklistService;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
    }

    @PostMapping("/login")
    public ResponseEntity> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPassword())) {
            User user = userOptional.get();
            String token = jwtUtil.generateToken(user.getEmail());

            response.put("token", token);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("email", user.getEmail());
            userData.put("role", user.getRole().getName());
            userData.put("fullName", user.getFullName());
            response.put("user", userData);

            return ResponseEntity.ok(response);
        } else {
            response.put("status", 401);
            response.put("error", "Unauthorized");
            response.put("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity> signup(@RequestBody Map<String, String> userData) {
        Map<String, String> response = new HashMap<>();

        if (userRepository.findByEmail(userData.get("email")).isPresent()) {
            response.put("message", "Email already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User newUser = new User();
        newUser.setEmail(userData.get("email"));
        newUser.setPassword(passwordEncoder.encode(userData.get("password")));
        newUser.setFullName(userData.get("fullName"));

        String roleName = userData.getOrDefault("role", "EMPLOYEE");
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found in database: " + roleName));
        newUser.setRole(role);

        userRepository.save(newUser);

        response.put("message", "User registered successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity> logout(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklistToken(token);
            response.put("message", "Logged out successfully.");
            return ResponseEntity.ok(response);
        }

        response.put("message", "No Authorization header found");
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Map<String, String> response = new HashMap<>();

        if (email == null || email.isBlank()) {
            response.put("message", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOptional = userRepository.findByEmail(email.trim());
        if (userOptional.isEmpty()) {
            response.put("message", "No account found with email: " + email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String token = UUID.randomUUID().toString();
        resetTokens.put(token, new ResetTokenData(email.trim(), Instant.now().plus(15, ChronoUnit.MINUTES)));

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // Print clearly in backend console for local testing
        log.info("==========================================================");
        log.info("PASSWORD RESET REQUESTED FOR: {}", email.trim());
        log.info("RESET LINK: {}", resetLink);
        log.info("TOKEN (expires in 15 mins): {}", token);
        log.info("==========================================================");

        // Trigger EmailService (uses local mock mode until AWS SES is configured)
        String templateData = "{\"resetLink\":\"" + resetLink + "\"}";
        emailService.sendTemplatedEmail(email.trim(), "PasswordResetTemplate", templateData);

        response.put("message", "Password reset link sent to " + email.trim());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        Map<String, String> response = new HashMap<>();

        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            response.put("message", "Both token and newPassword are required");
            return ResponseEntity.badRequest().body(response);
        }

        ResetTokenData tokenData = resetTokens.get(token);
        if (tokenData == null || Instant.now().isAfter(tokenData.expiresAt())) {
            resetTokens.remove(token);
            response.put("message", "Invalid or expired reset token");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(tokenData.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokens.remove(token);

        response.put("message", "Password has been reset successfully");
        return ResponseEntity.ok(response);
    }
}