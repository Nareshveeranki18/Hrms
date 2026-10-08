package com.hrms.backend.controller;

import com.hrms.backend.entity.Candidate;
import com.hrms.backend.entity.Employee;
import com.hrms.backend.entity.JobOpening;
import com.hrms.backend.entity.Organization;
import com.hrms.backend.repository.CandidateRepository;
import com.hrms.backend.repository.EmployeeRepository;
import com.hrms.backend.repository.JobOpeningRepository;
import com.hrms.backend.repository.OrganizationRepository;
import com.hrms.backend.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruitment/candidates")
@CrossOrigin(originPatterns = "*")
public class CandidateController {

    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private JobOpeningRepository jobOpeningRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private S3Service s3Service;

    @GetMapping
    public ResponseEntity<List<Candidate>> getAllCandidates() {
        return ResponseEntity.ok(candidateRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Candidate> createCandidate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("jobOpeningId") Long jobOpeningId) throws IOException {
        
        JobOpening job = jobOpeningRepository.findById(jobOpeningId)
                .orElseThrow(() -> new RuntimeException("Job Opening not found"));

        // Assuming your S3 service method is uploadFile. If it's uploadResume, change this line.
        String resumeUrl = s3Service.uploadFile(file, "resumes/");

        Candidate candidate = new Candidate();
        candidate.setFullName(fullName);
        candidate.setEmail(email);
        candidate.setPhone(phone);
        candidate.setJobOpening(job);
        candidate.setStatus("APPLIED");
        candidate.setResumeS3Url(resumeUrl);

        return ResponseEntity.ok(candidateRepository.save(candidate));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Candidate> updateCandidateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        
        candidate.setStatus(payload.get("status").toUpperCase());
        return ResponseEntity.ok(candidateRepository.save(candidate));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<Employee> convertCandidateToEmployee(@PathVariable Long id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        // 1. Update Candidate status
        candidate.setStatus("CONVERTED");
        candidateRepository.save(candidate);

        // 2. Fetch default organization
        Organization defaultOrg = organizationRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No organization found to map employee"));

        // 3. Split name
        String fullName = candidate.getFullName();
        String firstName = fullName;
        String lastName = "N/A";
        if (fullName != null && fullName.contains(" ")) {
            int lastSpaceIdx = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, lastSpaceIdx);
            lastName = fullName.substring(lastSpaceIdx + 1);
        }

        // 4. Create Employee Record
        Employee employee = new Employee();
        employee.setEmployeeCode("EMP-" + System.currentTimeMillis());
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(candidate.getEmail());
        employee.setPhone(candidate.getPhone());
        employee.setResumeS3Url(candidate.getResumeS3Url());
        employee.setOrganization(defaultOrg);

        return ResponseEntity.ok(employeeRepository.save(employee));
    }
}