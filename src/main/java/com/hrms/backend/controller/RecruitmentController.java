package com.hrms.backend.controller;

import com.hrms.backend.dto.CandidateStatusDto;
import com.hrms.backend.entity.Candidate;
import com.hrms.backend.entity.JobOpening;
import com.hrms.backend.repository.CandidateRepository;
import com.hrms.backend.repository.JobOpeningRepository;
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
}