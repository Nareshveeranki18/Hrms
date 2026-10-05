package com.hrms.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.hrms.backend.entity.Role;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name); // Add this line!
}