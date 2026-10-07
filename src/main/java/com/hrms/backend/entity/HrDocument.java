package com.hrms.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
public class HrDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileUrl;
    private String fileType;
    private String status = "PENDING";
    private String rejectionReason;

    private Long candidateId;
    private Long employeeId;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}