package com.hrms.backend.config; // Change this to match your actual package name

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleSeeder implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // This SQL safely checks if MANAGER exists, and if not, inserts it.
        String sql = "INSERT INTO roles (name) SELECT 'MANAGER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'MANAGER')";
        
        try {
            jdbcTemplate.execute(sql);
            System.out.println("✅ SUCCESS: MANAGER role is ready in the database!");
        } catch (Exception e) {
            System.out.println("⚠️ Note: Role check finished. " + e.getMessage());
        }
    }
}