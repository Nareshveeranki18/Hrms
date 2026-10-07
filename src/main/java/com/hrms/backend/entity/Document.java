package com.hrms.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ownerType; // CANDIDATE or EMPLOYEE
    private String ownerName;
    private String email;
    private String docType;
    private String fileName;
    private String fileType;
    private String fileUrl;
    
    private String status = "PENDING"; 
    private String remarks;
    private LocalDateTime reviewedOn;
    private LocalDateTime uploadedOn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getReviewedOn() { return reviewedOn; }
    public void setReviewedOn(LocalDateTime reviewedOn) { this.reviewedOn = reviewedOn; }
    public LocalDateTime getUploadedOn() { return uploadedOn; }
    public void setUploadedOn(LocalDateTime uploadedOn) { this.uploadedOn = uploadedOn; }
}