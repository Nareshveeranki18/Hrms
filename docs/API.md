# HRMS API Documentation

## 1. General Info
* **Base URL:** `http://localhost:8081` (Port 8081 as per Tomcat logs)
* **Authentication:** Login first, copy the token, and send `Authorization: Bearer <token>` on every subsequent request.
* **Error Format:** All backend errors return a standard JSON format: `{ "message": "Reason for error..." }`

## 2. Test Accounts
All accounts share the same password: `Admin@123`

| Role | Email | Permissions |
| :--- | :--- | :--- |
| **SUPER_ADMIN** | admin@company.com | Can access everything, including user creation. |
| **HR_ADMIN** | hr@company.com | Recruitment, Documents. Cannot create users. |
| **MANAGER** | manager@company.com | Dashboard only. |
| **EMPLOYEE** | employee@company.com | Dashboard only. |

---

## 3. Endpoints

### 3.1 Login
**POST** `/api/v1/auth/login`
**Who:** Anyone (No token needed)
**Request Body:**
```json
{ "email": "admin@company.com", "password": "Admin@123" }