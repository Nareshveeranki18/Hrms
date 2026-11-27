package com.hrms.backend.service;

import com.hrms.backend.entity.PayrollBatch;
import com.hrms.backend.entity.PayrollBatch.BatchStatus;
import com.hrms.backend.repository.PayrollBatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PayrollBatchService {

    private final PayrollBatchRepository payrollBatchRepository;

    public PayrollBatchService(PayrollBatchRepository payrollBatchRepository) {
        this.payrollBatchRepository = payrollBatchRepository;
    }

    public List<PayrollBatch> getAllBatches() {
        return payrollBatchRepository.findAll();
    }

    public PayrollBatch getBatchByMonth(String payrollMonth) {
        return payrollBatchRepository.findByPayrollMonth(payrollMonth)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No payroll batch found for month: " + payrollMonth));
    }

    public boolean isMonthLocked(String payrollMonth) {
        return payrollMonth != null
                && payrollBatchRepository.existsByPayrollMonthAndStatus(payrollMonth, BatchStatus.LOCKED);
    }

    // Reusable N-19 guard (Varma's BulkPayrollProcessor can also call this directly)
    public void assertMonthNotLocked(String payrollMonth) {
        if (isMonthLocked(payrollMonth)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Payroll batch for " + payrollMonth + " is LOCKED. Salary and attendance cannot be modified.");
        }
    }

    @Transactional
    public PayrollBatch createBatch(String payrollMonth, String createdBy) {
        if (payrollMonth == null || !payrollMonth.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "payrollMonth must be in YYYY-MM format (e.g., 2026-11)");
        }
        if (payrollBatchRepository.findByPayrollMonth(payrollMonth).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Payroll batch already exists for month: " + payrollMonth);
        }

        PayrollBatch batch = new PayrollBatch(payrollMonth, createdBy);
        return payrollBatchRepository.save(batch);
    }

    @Transactional
    public PayrollBatch transitionStatus(Long batchId, BatchStatus targetStatus, String actor) {
        PayrollBatch batch = payrollBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payroll batch not found with id: " + batchId));

        BatchStatus current = batch.getStatus();

        // Once LOCKED, no further changes are allowed (Task N-19)
        if (current == BatchStatus.LOCKED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Payroll batch for " + batch.getPayrollMonth() + " is already LOCKED and cannot be modified.");
        }

        // Enforce valid workflow transitions: DRAFT -> PROCESSING -> APPROVED -> LOCKED
        boolean validTransition = switch (current) {
            case DRAFT -> targetStatus == BatchStatus.PROCESSING;
            case PROCESSING -> targetStatus == BatchStatus.APPROVED || targetStatus == BatchStatus.DRAFT;
            case APPROVED -> targetStatus == BatchStatus.LOCKED || targetStatus == BatchStatus.PROCESSING;
            case LOCKED -> false;
        };

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + current + " to " + targetStatus);
        }

        batch.setStatus(targetStatus);
        String performedBy = (actor == null || actor.isBlank()) ? "SYSTEM_ADMIN" : actor;

        if (targetStatus == BatchStatus.APPROVED) {
            batch.setApprovedBy(performedBy);
            batch.setApprovedAt(LocalDateTime.now());
        } else if (targetStatus == BatchStatus.LOCKED) {
            batch.setLockedBy(performedBy);
            batch.setLockedAt(LocalDateTime.now());
        }

        return payrollBatchRepository.save(batch);
    }
}