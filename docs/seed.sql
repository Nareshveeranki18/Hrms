-- Insert Roles
INSERT INTO roles (name) VALUES ('SUPER_ADMIN'), ('HR_ADMIN'), ('MANAGER'), ('EMPLOYEE') ON CONFLICT DO NOTHING;

-- Insert Test Users (Password for all is: Admin@123)
INSERT INTO users (first_name, last_name, email, password, role_id) VALUES 
('System', 'Admin', 'admin@company.com', '$2a$10$X/U5z4N6Rz7L0e0v1c5E.u6k0B/3q4s6u8A0c2D4e6F8g0H2I4J6K', 1),
('HR', 'Admin', 'hr@company.com', '$2a$10$X/U5z4N6Rz7L0e0v1c5E.u6k0B/3q4s6u8A0c2D4e6F8g0H2I4J6K', 2);

-- Insert Job Openings
INSERT INTO jobs (title, department, location, status) VALUES 
('Frontend Developer', 'Engineering', 'Bengaluru', 'OPEN');

-- Insert Candidates
INSERT INTO candidates (first_name, last_name, email, phone, status, job_id) VALUES 
('Alice', 'Smith', 'alice@example.com', '9876543210', 'APPLIED', 1),
('Charlie', 'Brown', 'charlie@example.com', '9876543212', 'SCREENING', 1);

-- Insert Documents
INSERT INTO documents (owner_type, owner_name, email, doc_type, file_name, file_type, status) VALUES 
('CANDIDATE', 'Alice Smith', 'alice@example.com', 'ID Proof', 'alice_id.pdf', 'application/pdf', 'PENDING');