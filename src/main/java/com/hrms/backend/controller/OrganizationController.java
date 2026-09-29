package com.hrms.backend.controller;

import com.hrms.backend.dto.OrganizationDTO;
import com.hrms.backend.entity.Organization;
import com.hrms.backend.service.OrganizationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> createOrganization(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam(value = "logo", required = false) MultipartFile logoFile) {
        
        try {
            Organization org = new Organization();
            org.setName(name);
            org.setAddress(address);
            org.setContactEmail(contactEmail);

            if (logoFile != null && !logoFile.isEmpty()) {
                String uploadDir = "uploads/logos/";
                Path uploadPath = Paths.get(uploadDir);
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID().toString() + "_" + logoFile.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(logoFile.getInputStream(), filePath);
                
                org.setLogoPath(uploadDir + fileName);
            }

            organizationService.saveOrganization(org);
            
            // RETURN TINTU'S EXACT REQUESTED JSON
            return ResponseEntity.ok(Collections.singletonMap("message", "Organization saved successfully"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Collections.singletonMap("message", "Failed to create organization: " + e.getMessage())
            );
        }
    }

    @GetMapping
    public ResponseEntity<List<OrganizationDTO>> getAllOrganizations() {
        List<Organization> organizations = organizationService.getAllOrganizations();
        
        List<OrganizationDTO> safeData = organizations.stream()
                .map(org -> new OrganizationDTO(
                        org.getId(),
                        org.getName(),
                        org.getAddress(),
                        org.getContactEmail()
                ))
                .toList();

        return ResponseEntity.ok(safeData);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDTO> updateOrganization(@PathVariable Long id, @RequestBody Organization orgDetails) {
        Organization updatedOrg = organizationService.updateOrganization(id, orgDetails);
        
        OrganizationDTO safeData = new OrganizationDTO(
                updatedOrg.getId(),
                updatedOrg.getName(),
                updatedOrg.getAddress(),
                updatedOrg.getContactEmail()
        );
        return ResponseEntity.ok(safeData);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok("Organization deleted successfully.");
    }
}