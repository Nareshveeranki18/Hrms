package com.hrms.backend.controller;

import com.hrms.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(@RequestBody Map<String, String> payload) {
        String toAddress = payload.get("email");
        String templateName = payload.get("template");
        String templateData = payload.get("templateData"); 

        String messageId = emailService.sendTemplatedEmail(toAddress, templateName, templateData);
        return ResponseEntity.ok("Email successfully triggered! SES Message ID: " + messageId);
    }
}