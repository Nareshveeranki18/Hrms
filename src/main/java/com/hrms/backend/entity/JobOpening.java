package com.hrms.backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_openings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("title")
    @Column(nullable = false)
    private String title;

    @JsonProperty("description")
    @Column(columnDefinition = "TEXT")
    private String description;

    @JsonProperty("department")
    private String department;

    @JsonProperty("location")
    private String location;

    @JsonProperty("status")
    @Column(nullable = false)
    private String status = "OPEN"; // OPEN, CLOSED, ON_HOLD

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}