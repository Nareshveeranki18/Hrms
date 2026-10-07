BEGIN;

-- 1. Roles
INSERT INTO roles (name)
SELECT v.name
FROM (VALUES ('SUPER_ADMIN'), ('HR_ADMIN'), ('MANAGER'), ('EMPLOYEE')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = v.name);

-- 2. Test users (password: Admin@123)
INSERT INTO users (full_name, email, password, role_id)
SELECT v.full_name, v.email,
       '$2b$10$HjGWWkMJjJiau7vDKrOXdueCmEx1abmsdQx.kG9nwWh9VIcXeDGL.',
       (SELECT r.id FROM roles r WHERE r.name = v.role ORDER BY r.id LIMIT 1)
FROM (VALUES
  ('System Admin',  'admin@company.com',    'SUPER_ADMIN'),
  ('HR Admin',      'hr@company.com',       'HR_ADMIN'),
  ('Test Manager',  'manager@company.com',  'MANAGER'),
  ('Test Employee', 'employee@company.com', 'EMPLOYEE')
) AS v(full_name, email, role)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = v.email);

-- 3. Job openings
INSERT INTO job_openings (title, description, department, location, status, created_at)
SELECT v.title, v.description, v.department, v.location, 'OPEN', now()
FROM (VALUES
  ('Frontend Developer',        'React and Next.js developer',      'Engineering', 'Bengaluru'),
  ('Java Full Stack Developer', 'Spring Boot and React developer',  'Engineering', 'Hyderabad')
) AS v(title, description, department, location)
WHERE NOT EXISTS (SELECT 1 FROM job_openings j WHERE j.title = v.title);

-- 4. Candidates
INSERT INTO candidates (full_name, email, phone, status, job_opening_id, created_at)
SELECT v.full_name, v.email, v.phone, v.status,
       (SELECT j.id FROM job_openings j WHERE j.title = v.job ORDER BY j.id LIMIT 1),
       now()
FROM (VALUES
  ('Alice Smith',    'alice@example.com',   '9876543210', 'APPLIED',   'Frontend Developer'),
  ('Rohan Mehta',    'rohan@example.com',   '9876543214', 'APPLIED',   'Java Full Stack Developer'),
  ('Divya Patel',    'divya@example.com',   '9876543215', 'APPLIED',   'Frontend Developer'),
  ('Charlie Brown',  'charlie@example.com', '9876543212', 'SCREENING', 'Frontend Developer'),
  ('Sneha Reddy',    'sneha@example.com',   '9876543216', 'SCREENING', 'Java Full Stack Developer'),
  ('Arjun Verma',    'arjun@example.com',   '9876543217', 'INTERVIEW', 'Java Full Stack Developer'),
  ('Meera Nair',     'meera@example.com',   '9876543218', 'INTERVIEW', 'Frontend Developer'),
  ('Kiran Kumar',    'kiran@example.com',   '9876543219', 'OFFER',     'Java Full Stack Developer'),
  ('Pooja Joshi',    'pooja@example.com',   '9876543220', 'OFFER',     'Frontend Developer')
) AS v(full_name, email, phone, status, job)
WHERE NOT EXISTS (SELECT 1 FROM candidates c WHERE c.email = v.email);

-- 5. Employees (Fixes dashboard showing 0)
INSERT INTO employees (employee_code, full_name, email, department, designation, date_of_joining, status)
SELECT v.employee_code, v.full_name, v.email, v.department, v.designation, current_date, 'ACTIVE'
FROM (VALUES
  ('EMP001', 'System Admin',  'admin@company.com',    'Management', 'Super Admin'),
  ('EMP002', 'HR Admin',      'hr@company.com',       'HR',         'HR Manager'),
  ('EMP003', 'Test Manager',  'manager@company.com',  'Engineering','Engineering Manager'),
  ('EMP004', 'Test Employee', 'employee@company.com', 'Engineering','Software Developer')
) AS v(employee_code, full_name, email, department, designation)
WHERE NOT EXISTS (SELECT 1 FROM employees e WHERE e.email = v.email);

COMMIT;