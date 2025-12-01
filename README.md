# LearnOnline – CSIS 231 Final Project

LearnOnline is a **modular online learning platform** built for the  
**CSIS 231 – Advances in Computer Science (University of Balamand)** final project.

The system consists of:

- A **Spring Boot** REST API backend (secured with **JWT** and **OTP-based 2FA**).
- A **JavaFX** desktop client with:
    - **2D data visualizations** (charts and dashboards).
    - **3D interactive visualization** built with JavaFX 3D.
- A **PostgreSQL** database used as the single source of truth for all business logic and persistence.

> Authentication uses **JWT** with optional **email OTP (two-step login)**, and all data access goes through **Spring Data JPA (ORM)**—no native SQL.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Core Features](#core-features)
5. [Security & Authentication Flow (JWT + OTP 2FA)](#security--authentication-flow-jwt--otp-2fa)
6. [JavaFX Desktop Client (2D & 3D)](#javafx-desktop-client-2d--3d)
7. [Backend API – Modules & Endpoints](#backend-api--modules--endpoints)
8. [Database & Domain Model](#database--domain-model)
9. [Error Handling, Validation & Security](#error-handling-validation--security)
10. [Project Setup & How to Run](#project-setup--how-to-run)
11. [Testing & API Documentation](#testing--api-documentation)
12. [Folder Structure](#folder-structure)
13. [Future Improvements](#future-improvements)

---

## Project Overview

**LearnOnline** is a desktop-first learning platform where:

- **Students** can register, log in, browse courses, enroll, and track their progress.
- **Instructors** can manage courses (titles, descriptions, categories, content).
- **Admins** can oversee users and the overall platform.

The project was designed to satisfy the **CSIS 231 final project requirements**:

- Spring Boot backend with clear modularity (Controller / Service / Repository / Domain).
- PostgreSQL database with a clean schema and relationships.
- JavaFX front-end with **FXML** and modular controllers.
- At least one **meaningful 3D JavaFX animation/visualization**.
- Advanced **security**: JWT, OTP 2FA, input validation, and centralized error handling.
- Proper **documentation** of endpoints, setup and architecture.

---

## Architecture

LearnOnline follows a **layered, interaction-oriented architecture**:

- **Backend (Spring Boot)**
    - Exposes RESTful JSON APIs for authentication, users, courses, and statistics.
    - Applies business rules in the **service layer**, not in the UI.
    - Uses **Spring Data JPA** for data access, mapped to a PostgreSQL schema.

- **Database (PostgreSQL)**
    - Stores users, roles, courses, categories, enrollments, OTP tokens, and statistics.
    - Enforces relationships (e.g., one user → many enrollments).

- **Frontend (JavaFX Desktop Client)**
    - JavaFX application with **FXML views** and controllers.
    - Communicates with backend via **REST (JSON)**.
    - Provides:
        - **2D dashboards** (charts, statistics).
        - **3D interactive scene** (meaningful visualization of progress/statistics).

High-level data flow:

1. User interacts with JavaFX UI (login, enroll, view dashboard).
2. JavaFX sends REST requests to Spring Boot (with JWT in the Authorization header).
3. Spring Boot validates the token, applies business logic, and queries PostgreSQL.
4. JSON responses are used to update 2D charts and 3D visualizations in JavaFX.

---

## Technology Stack

**Backend (API)**

- Java (17+)
- Spring Boot (Web, Security, Validation)
- Spring Data JPA / Hibernate
- Spring Mail / JavaMail (for email-based OTP, if configured)
- JSON Web Tokens (JWT) for stateless auth
- Jakarta Validation (Bean Validation)

**Database**

- PostgreSQL
- Flyway / Liquibase or `schema.sql` / `data.sql` (if used) for schema initialization

**Frontend (Desktop Client)**

- JavaFX (2D + 3D)
- FXML & CSS for views and styling
- JavaFX charts for **2D visualizations**
- JavaFX 3D (`SubScene`, `PerspectiveCamera`, `PhongMaterial`, etc.) for **3D visualization**
- HTTP client (e.g. `HttpClient` / `RestTemplate` / `WebClient`) for REST calls
- Centralized state management (e.g. `AppState` class) to share auth/user/statistics between views

**Tooling**

- Maven
- IntelliJ IDEA / VS Code
- pgAdmin (for database inspection)

---

## Core Features

### 1. Authentication & User Management

- User registration and login.
- Role-based access (e.g., **STUDENT / INSTRUCTOR / ADMIN**).
- **JWT** token-based authentication.
- **Optional OTP-based 2FA** over email for sensitive login flows.
- Secure password storage (e.g., BCrypt).

### 2. Courses & Enrollment

- Manage courses and categories:
    - Create / update / archive courses (Instructor/Admin).
    - Associate courses with categories and metadata.
- Student features:
    - Browse available courses.
    - Enroll / view enrolled courses.
    - Track progress (e.g., completed lessons / modules).

### 3. 2D Analytics & Dashboards (JavaFX)

- 2D charts fed by real backend data:
    - Example: **progress per course**, **enrollments per category**, **activity over time**.
- Responsive and modular views:
    - Controllers mapped to specific FXML screens.
    - Clear separation between UI, REST client, and domain models.

### 4. 3D Visualization (JavaFX 3D)

- **meaningful 3D scene**, such as:
    - 3D bars representing course completion percentages.
    - 3D objects whose height, color, or position encodes key metrics.
- User interactions:
    - Rotate or zoom the 3D scene with the mouse.
    - Optionally click elements to display details in a side panel.

---

## Security & Authentication Flow (JWT + OTP 2FA)

High-level login flow:

1. **Register**
    - User submits registration details.
    - Backend creates the user, assigns default role(s), hashes password.

2. **Login (Step 1)**
    - User posts credentials.
    - Backend validates credentials.
    - Depending on configuration:
        - Either returns a JWT directly, **or**
        - Triggers **OTP step** (2FA) and sends a short-lived OTP to the user’s email.

3. **OTP Verification (Step 2 – 2FA)**
    - User submits OTP code received by email.
    - Backend verifies OTP (not expired, matches user, not reused).
    - On success, backend issues a **JWT access token** (and optionally a refresh token).

4. **Authenticated Requests**
    - JavaFX client stores the token securely in memory/state.
    - For all protected endpoints, JavaFX sends `Authorization: Bearer <jwt>`.

5. **Logout / Token Invalidation**
    - Client discards tokens locally.
    - Backend can invalidate tokens via a token blacklist / rotation policy (if implemented).

---

## JavaFX Desktop Client (2D & 3D)

The JavaFX client is a modular desktop app that:

- Handles **login, registration and OTP verification**.
- Stores the authenticated user and JWT in a shared **AppState**.
- Renders:
    - **2D analytics views** using JavaFX charts (line, bar, pie…).
    - **3D visualization** using JavaFX 3D components.

Typical UI modules:

- `LoginView` / `RegisterView`
- `OtpVerificationView`
- `DashboardView` (2D charts)
- `CoursesView`
- `3DStatisticsView` (3D scene linked to statistics endpoints)

Each view:

- Has its own FXML file and controller.
- Uses a small service/helper to call the backend and map JSON to Java models.
- Reacts to **AppState** changes (e.g., when new statistics are loaded, refresh 2D and 3D views).

---

## Backend API – Modules & Endpoints

> **Note:** The exact URL paths in your code may differ slightly; this section is a **high-level overview** of the main endpoint groups and responsibilities.

### 1. Authentication & User API

Typical endpoints:

- `POST /api/v1/auth/register`  
  Register a new user.

- `POST /api/v1/auth/login`  
  Authenticate with email/password.
    - If 2FA is enabled, this may trigger OTP sending instead of returning a JWT directly.

- `POST /api/v1/auth/otp/send`  
  Send or resend OTP code to the user’s email.

- `POST /api/v1/auth/otp/verify`  
  Verify OTP code and issue final JWT token.

- `POST /api/v1/auth/refresh`  
  Refresh access token (if refresh tokens are implemented).

- `GET /api/v1/auth/me`  
  Return the current authenticated user’s profile.

Security:

- `permitAll()` for `register`, `login`, `otp` endpoints.
- `authenticated()` for all others (requires valid `Authorization: Bearer <jwt>` header).

### 2. Course & Category API

- `GET /api/v1/courses`  
  List available courses.

- `GET /api/v1/courses/{courseId}`  
  Details of a single course.

- `POST /api/v1/courses`  
  Create a new course (Instructor/Admin only).

- `PUT /api/v1/courses/{courseId}`  
  Update an existing course.

- `DELETE /api/v1/courses/{courseId}`  
  Archive or delete a course (if allowed).

- `GET /api/v1/categories`  
  List categories.

- `POST /api/v1/categories`  
  Create new course category (Admin).

### 3. Enrollment & Progress API

- `POST /api/v1/courses/{courseId}/enroll`  
  Enroll the current user in a course.

- `GET /api/v1/users/{userId}/enrollments`  
  List courses the user is enrolled in.

- `GET /api/v1/users/{userId}/progress`  
  High-level progress summary by course.

### 4. Statistics / Dashboard API (for 2D/3D)

These endpoints provide data to the JavaFX 2D charts and 3D scene (names are indicative):

- `GET /api/v1/statistics/user/{userId}/summary`  
  Overall summary (completed courses, active courses, etc.).

- `GET /api/v1/statistics/user/{userId}/course-progress`  
  Per-course progress values (used in 2D/3D charts).

- `GET /api/v1/statistics/user/{userId}/activity`  
  Activity metrics (e.g., logins per day, lessons completed over time).

---

## Database & Domain Model

High-level entities (names may vary slightly in code):

- `User` – id, name, email, password hash, status.
- `Role` – role name (STUDENT, INSTRUCTOR, ADMIN) and mapping to users.
- `Course` – title, description, category, instructor, status.
- `Category` – category name, description.
- `Enrollment` – user–course relationship and progress fields.
- `OtpToken` – OTP code, expiration time, related user.
- Additional supporting entities (e.g., refresh tokens, audit/log entities).

Relationships:

- One `User` ↔ Many `Enrollment`s.
- One `Course` ↔ Many `Enrollment`s.
- One `Category` ↔ Many `Course`s.

The database is designed so that **business logic lives close to the data**, minimizing duplicated rules on the client.

---

## Error Handling, Validation & Security

### Backend

- Global exception handling with `@ControllerAdvice` to:
    - Return consistent JSON error responses.
    - Map validation errors (400), unauthorized (401), forbidden (403), not found (404), and internal errors (500).

- Input validation using **Jakarta Validation** annotations:
    - `@NotBlank`, `@Email`, `@Size`, etc. on DTOs and entities.
    - Automatically validated on controller endpoints with `@Valid`.

- Security with **Spring Security + JWT**:
    - Stateless security using a filter to validate JWT on each request.
    - Role-based access to certain endpoints.
    - Protection of sensitive resources so they are accessible only to authenticated users.

### Frontend (JavaFX)

- Client-side validation before calling APIs (e.g., empty fields, basic email format).
- Graceful error messages on network or server error (no crashes).
- Unified error handling at the service/helper level:
    - When an error is received, display alerts or inline messages in the UI.

---

## Project Setup & How to Run

### Prerequisites

- Java 17+
- Maven 3+
- PostgreSQL (local or Docker)
- IDE (IntelliJ IDEA recommended)

### 1. Clone the Repository

```bash
git clone https://github.com/M677871/csis-231-project.git
cd csis-231-project/csis_231-login-registration-jwt
```

### 2. Configure PostgreSQL

Create a PostgreSQL database (for example):

```sql
CREATE DATABASE learnonline;
```

Update `application.properties` (or `application.yml`) in the backend module (e.g., `csis231-api`) with your credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/learnonline
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

Configure **JWT** and **mail/OTP** properties if they are externalized (example):

```properties
app.jwt.secret=YOUR_JWT_SECRET
app.jwt.expiration=3600000

spring.mail.host=smtp.your-provider.com
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_EMAIL_PASSWORD
```

### 3. Run the Backend (Spring Boot)

From the backend module directory (e.g., `csis231-api`):

```bash
mvn spring-boot:run
```

The API will start on the configured port (commonly `http://localhost:8080`).

### 4. Run the JavaFX Desktop Client

From the JavaFX module directory (e.g., `demo`):

```bash
mvn javafx:run
```

Or run the main JavaFX application class from your IDE.

---

## Testing & API Documentation

- Use **Postman**, **Insomnia**, or **curl** to test the REST endpoints.
- Typical scenario to test:
    1. Register a new user.
    2. Login and trigger OTP (if enabled).
    3. Verify OTP and obtain JWT.
    4. Call protected endpoints with `Authorization: Bearer <token>` header.
    5. Open the JavaFX client and verify that 2D and 3D dashboards display the expected data.

If **OpenAPI / Swagger (Springdoc)** is enabled in the project, API docs are typically accessible at:

- `http://localhost:8080/swagger-ui.html` or
- `http://localhost:8080/swagger-ui/index.html`

(Adjust according to your configuration.)

---

## Folder Structure

High-level structure (simplified):

```text
csis-231-project/
├── README.md                         # This file
└── csis_231-login-registration-jwt/
    ├── csis231-api/                  # Spring Boot backend
    │   ├── src/main/java/...         # Controllers, services, repositories, domain
    │   ├── src/main/resources/       # application.properties, schema/data SQL
    │   └── pom.xml
    └── demo/                         # JavaFX desktop client
        ├── src/main/java/...         # JavaFX controllers, models, REST client
        ├── src/main/resources/       # FXML, CSS, icons
        └── pom.xml
```

---

## Future Improvements

Some potential improvements and extensions:

- Add a **web frontend** (React/Angular) consuming the same API.
- Implement **WebSockets** for real-time updates (e.g., live progress or chat).
- Extend 3D visualizations with more metrics and transitions.
- Add more advanced analytics (completion funnels, retention analysis, etc.).
- Implement more detailed logging and monitoring.

---

**CSIS 231 – LearnOnline**  
Designed and implemented as a high-level, secure, and modular platform showcasing:

- Spring Boot backend architecture
- PostgreSQL data modeling
- JWT + OTP 2FA security
- JavaFX 2D dashboards & 3D interactive visualization
- Clean layering and documentation for academic evaluation  
