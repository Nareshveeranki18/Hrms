package com.hrms.backend.repository;

import com.hrms.backend.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
}