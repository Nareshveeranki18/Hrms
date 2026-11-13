Markdown
# HRMS API Documentation

**Base URL:** `http://localhost:8080/api/v1`

## Authentication
Protected endpoints require an `Authorization: Bearer <token>` header.

### Login
- **URL:** `/auth/login`
- **Method:** `POST`
- **Body:**
  ```json
  {
    "email": "admin@company.com",
    "password": "Admin@123"
  }
Test Accounts
Role	Email	Password
SUPER_ADMIN	admin@company.com	Admin@123
HR_ADMIN	hr@company.com	Admin@123
MANAGER	manager@company.com	Admin@123
EMPLOYEE	employee@company.com	Admin@123
Users & Roles
GET /roles - List all available roles (SUPER_ADMIN, HR_ADMIN, MANAGER, EMPLOYEE).

POST /users - Create a new user ({ fullName, email, password, roleId }).

Recruitment & Candidates
GET /recruitment/job-openings - Retrieve all job postings.

POST /recruitment/job-openings - Create a new job opening.

GET /recruitment/candidates - List candidates across Kanban stages.

POST /recruitment/candidates - Create a candidate and upload resume to S3.

PUT /recruitment/candidates/{id}/status - Update candidate status (APPLIED, SCREENING, INTERVIEW, OFFER).

POST /recruitment/candidates/{id}/convert - Convert a selected candidate into an Employee Master record.

Documents
GET /documents - List all uploaded documents.

GET /documents/{id}/file - Retrieve the temporary presigned S3 URL to preview a document.

POST /documents - Upload file (multipart/form-data).

PUT /documents/{id}/verify - Verify document.

PUT /documents/{id}/reject - Reject document with reason payload ({ "reason": "..." }).

PUT /documents/{id}/request-reupload - Request document re-upload with note payload ({ "note": "..." }).

Employees
GET /employees - Retrieve employee directory and metrics.

POST /employees - Create a new employee.

POST /employees/{id}/assign-shift - Assign a shift to an employee. Payload: { "shiftId": 1 }.

Shift Management
Base URL: /shifts

Method	Endpoint	Role Required	Description
GET	/shifts	Any logged-in user	List all shifts
POST	/shifts	SUPER_ADMIN, HR_ADMIN	Create a new shift
PUT	/shifts/{id}	SUPER_ADMIN, HR_ADMIN	Update an existing shift
DELETE	/shifts/{id}	SUPER_ADMIN, HR_ADMIN	Delete a shift
JSON Payload (POST / PUT):

JSON
{
  "name": "General Shift",
  "startTime": "09:00",
  "endTime": "18:00",
  "breakDurationMinutes": 60,
  "weeklyOffDays": ["SATURDAY", "SUNDAY"]
}
Validation Rules:

name: 2-50 characters, must be unique.

startTime / endTime: Required, cannot be equal. Night shifts (end time before start time) are fully supported.

breakDurationMinutes: 0-240 minutes, must be less than total shift duration.

Delete Restriction: Cannot delete a shift if it is actively assigned to an employee (returns 409 Conflict).