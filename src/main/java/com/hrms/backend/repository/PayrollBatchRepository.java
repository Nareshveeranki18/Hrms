package com.hrms.backend.repository;

import com.hrms.backend.entity.PayrollBatch;
import com.hrms.backend.entity.PayrollBatch.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollBatchRepository extends JpaRepository<PayrollBatch, Long> {
    Optional<PayrollBatch> findByPayrollMonth(String payrollMonth);
    boolean existsByPayrollMonthAndStatus(String payrollMonth, BatchStatus status);
}