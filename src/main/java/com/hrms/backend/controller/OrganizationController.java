package com.hrms.backend.controller;

import com.hrms.backend.entity.Organization;
import com.hrms.backend.entity.User;
import com.hrms.backend.repository.OrganizationRepository;
import com.hrms.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public OrganizationController(OrganizationRepository organizationRepository, UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrganization(@RequestBody Organization organization) {
        Optional<User> adminUserOpt = userRepository.findByEmail(organization.getContactEmail());
        
        if (adminUserOpt.isEmpty() || adminUserOpt.get().getRole() == null || !"SUPER_ADMIN".equals(adminUserOpt.get().getRole().getName())) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Validation Failed: A valid SUPER_ADMIN user must exist with the provided contact email before creating the organization.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Organization savedOrg = organizationRepository.save(organization);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Organization created successfully");
        response.put("id", savedOrg.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable Long id) {
        return organizationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        return ResponseEntity.ok(organizationRepository.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(@PathVariable Long id, @RequestBody Organization orgDetails) {
        return organizationRepository.findById(id)
                .map(existingOrg -> {
                    existingOrg.setName(orgDetails.getName());
                    existingOrg.setLogoUrl(orgDetails.getLogoUrl());
                    existingOrg.setAddress(orgDetails.getAddress());
                    existingOrg.setContactEmail(orgDetails.getContactEmail());
                    return ResponseEntity.ok(organizationRepository.save(existingOrg));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}