package com.hrms.backend.controller;

import com.hrms.backend.entity.Candidate;
import com.hrms.backend.repository.CandidateRepository;
import com.hrms.backend.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruitment/candidates")
@CrossOrigin(origins = "*")
public class CandidateController {

    private final CandidateRepository candidateRepository;
    private final S3Service s3Service;

    public CandidateController(CandidateRepository candidateRepository, S3Service s3Service) {
        this.candidateRepository = candidateRepository;
        this.s3Service = s3Service;
    }

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
        
        String resumeUrl = s3Service.uploadFile(file, "resumes/");

        Candidate candidate = new Candidate();
        candidate.setFullName(fullName);
        candidate.setEmail(email);
        candidate.setPhone(phone);
        // Note: Assumes JobOpening reference lookup is handled or set accordingly
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
        
        candidate.setStatus(payload.get("status"));
        return ResponseEntity.ok(candidateRepository.save(candidate));
    }
}