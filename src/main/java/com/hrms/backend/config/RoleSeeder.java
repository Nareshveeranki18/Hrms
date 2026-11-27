package com.hrms.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        String sql = "INSERT INTO roles (name) SELECT 'MANAGER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'MANAGER')";
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.warn("Role seeding check skipped: {}", e.getMessage());
        }
    }
}