package com.hrms.backend.controller;

import com.hrms.backend.dto.CandidateStatusDto;
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

@RestController
@RequestMapping("/api/v1/recruitment")
public class RecruitmentController {

    @Autowired
    private JobOpeningRepository jobOpeningRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private S3Service s3Service;

    @PostMapping("/job-openings")
    public ResponseEntity<JobOpening> createJobOpening(@RequestBody JobOpening jobOpening) {
        return ResponseEntity.ok(jobOpeningRepository.save(jobOpening));
    }

    @GetMapping("/job-openings")
    public ResponseEntity<List<JobOpening>> getOpenJobs() {
        return ResponseEntity.ok(jobOpeningRepository.findAll());
    }

    @PostMapping("/candidates")
    public ResponseEntity<Candidate> createCandidate(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("jobOpeningId") Long jobOpeningId,
            @RequestParam("resume") MultipartFile resume) throws IOException {

        JobOpening job = jobOpeningRepository.findById(jobOpeningId)
                .orElseThrow(() -> new RuntimeException("Job Opening not found"));

        String s3Url = s3Service.uploadResume(resume);

        Candidate candidate = new Candidate();
        candidate.setFullName(fullName);
        candidate.setEmail(email);
        candidate.setPhone(phone);
        candidate.setJobOpening(job);
        candidate.setResumeS3Url(s3Url);
        candidate.setStatus("APPLIED");

        return ResponseEntity.ok(candidateRepository.save(candidate));
    }

    @PutMapping("/candidates/{id}/status")
    public ResponseEntity<Candidate> updateStatus(
            @PathVariable Long id, 
            @RequestBody CandidateStatusDto statusDto) {
            
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
                
        candidate.setStatus(statusDto.getStatus().toUpperCase());
        return ResponseEntity.ok(candidateRepository.save(candidate));
    }

    @GetMapping("/candidates")
    public ResponseEntity<List<Candidate>> getAllCandidates() {
        return ResponseEntity.ok(candidateRepository.findAll());
    }

    @PostMapping("/candidates/{id}/convert")
    public ResponseEntity<Employee> convertCandidateToEmployee(@PathVariable Long id) {
        Candidate candidate = candidateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        // 1. Update Candidate status
        candidate.setStatus("CONVERTED");
        candidateRepository.save(candidate);

        // 2. Fetch default organization (or handle via tenant/org logic)
        Organization defaultOrg = organizationRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No organization found to map employee"));

        // 3. Split name into first and last name fields required by Employee entity
        String fullName = candidate.getFullName();
        String firstName = fullName;
        String lastName = "N/A";
        if (fullName != null && fullName.contains(" ")) {
            int lastSpaceIdx = fullName.lastIndexOf(' ');
            firstName = fullName.substring(0, lastSpaceIdx);
            lastName = fullName.substring(lastSpaceIdx + 1);
        }

        // 4. Create and save the new Employee Record
        Employee employee = new Employee();
        employee.setEmployeeCode("EMP-" + System.currentTimeMillis());
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(candidate.getEmail());
        employee.setPhone(candidate.getPhone());
        employee.setResumeS3Url(candidate.getResumeS3Url());
        employee.setOrganization(defaultOrg);

        Employee savedEmployee = employeeRepository.save(employee);
        return ResponseEntity.ok(savedEmployee);
    }
}