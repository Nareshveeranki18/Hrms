package com.hrms.backend.repository;

import com.hrms.backend.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}