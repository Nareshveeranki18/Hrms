package com.hrms.backend.controller;

import com.hrms.backend.entity.Document;
import com.hrms.backend.repository.DocumentRepository;
import com.hrms.backend.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final S3Service s3Service;

    public DocumentController(DocumentRepository documentRepository, S3Service s3Service) {
        this.documentRepository = documentRepository;
        this.s3Service = s3Service;
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        return ResponseEntity.ok(documentRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerName") String ownerName,
            @RequestParam("email") String email,
            @RequestParam("docType") String docType) throws IOException {
        
        String fileUrl = s3Service.uploadFile(file, "documents/");
        
        Document doc = new Document();
        doc.setOwnerType(ownerType);
        doc.setOwnerName(ownerName);
        doc.setEmail(email);
        doc.setDocType(docType);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setFileUrl(fileUrl);
        doc.setStatus("PENDING");
        
        return ResponseEntity.ok(documentRepository.save(doc));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Map<String, String>> getDocumentFileUrl(@PathVariable Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        String presignedUrl = s3Service.generatePresignedUrl(doc.getFileUrl());
        return ResponseEntity.ok(Map.of("url", presignedUrl));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<Document> verifyDocument(@PathVariable Long id) {
        Document doc = documentRepository.findById(id).orElseThrow();
        doc.setStatus("VERIFIED");
        doc.setRemarks(null);
        doc.setReviewedOn(LocalDateTime.now());
        return ResponseEntity.ok(documentRepository.save(doc));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Document> rejectDocument(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Document doc = documentRepository.findById(id).orElseThrow();
        doc.setStatus("REJECTED");
        doc.setRemarks(body.get("reason"));
        doc.setReviewedOn(LocalDateTime.now());
        return ResponseEntity.ok(documentRepository.save(doc));
    }

    @PutMapping("/{id}/request-reupload")
    public ResponseEntity<Document> requestReupload(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Document doc = documentRepository.findById(id).orElseThrow();
        doc.setStatus("REUPLOAD_REQUESTED");
        doc.setRemarks(body.get("note"));
        doc.setReviewedOn(LocalDateTime.now());
        return ResponseEntity.ok(documentRepository.save(doc));
    }
}