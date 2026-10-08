package com.hrms.backend.controller;

import com.hrms.backend.entity.JobOpening;
import com.hrms.backend.repository.JobOpeningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recruitment")
@CrossOrigin(originPatterns = "*")
public class RecruitmentController {

    @Autowired
    private JobOpeningRepository jobOpeningRepository;

    @PostMapping("/job-openings")
    public ResponseEntity<JobOpening> createJobOpening(@RequestBody JobOpening jobOpening) {
        return ResponseEntity.ok(jobOpeningRepository.save(jobOpening));
    }

    @GetMapping("/job-openings")
    public ResponseEntity<List<JobOpening>> getOpenJobs() {
        return ResponseEntity.ok(jobOpeningRepository.findAll());
    }
}