package com.hrms.backend.repository;

import com.hrms.backend.entity.HrDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrDocumentRepository extends JpaRepository<HrDocument, Long> {
    List<HrDocument> findByCandidateId(Long candidateId);
    List<HrDocument> findByEmployeeId(Long employeeId);
}