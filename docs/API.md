Markdown# HRMS API Documentation

Base URL: `http://localhost:8081/api/v1`

## Authentication
Protected endpoints require an `Authorization: Bearer <token>` header.

### Login
* **URL:** `/auth/login`
* **Method:** `POST`
* **Body:**
  ```json
  {
    "email": "admin@company.com",
    "password": "Admin@123"
  }
Test AccountsRoleEmailPasswordSUPER_ADMINadmin@company.comAdmin@123HR_ADMINhr@company.comAdmin@123MANAGERmanager@company.comAdmin@123EMPLOYEEemployee@company.comAdmin@123Job OpeningsGET /job-openings - Retrieve all job postings.POST /job-openings - Create a new job opening.Candidates (Recruitment Pipeline)GET /candidates - List candidates across Kanban stages.PATCH /candidates/{id}/status - Update candidate status (APPLIED, SCREENING, INTERVIEW, OFFER).DocumentsGET /documents - List all uploaded documents.POST /documents/upload - Upload file (multipart/form-data).PATCH /documents/{id}/verify - Verify document.PATCH /documents/{id}/reject - Reject document with reason payload.EmployeesGET /employees - Retrieve employee directory and metrics.