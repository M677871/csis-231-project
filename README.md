# LearnOnline – CSIS 231 Final Project

> **University of Balamand – Faculty of Arts & Sciences**  
> **CSIS 231 – java technology**  
> Secure online learning platform with **Spring Boot + PostgreSQL + JavaFX 2D/3D**,  
> **JWT + OTP 2FA**, and **role-based dashboards (Admin / Instructor / Student)**.

---

## 1. Project Overview

**LearnOnline** is a modular e-learning platform built as the final project for **CSIS 231 – Advances in Computer Science**.

The system provides:

- A **secure REST backend** (Spring Boot + Spring Security + JWT + OTP 2FA).
- A **desktop JavaFX client** with:
    - Modular **FXML views** and shared styling.
    - **2D dashboards** using JavaFX charts.
    - **3D analytics** using JavaFX 3D (`SubScene`, `Box`, `PerspectiveCamera`).
- A **PostgreSQL** database as the single source of truth for all business logic and persistence.

**Roles & flows**

- **Student**
    - Register, login (with optional OTP).
    - Browse courses, enroll, take quizzes.
    - View **recent quiz performance** and progress in **2D/3D visualizations**.
- **Instructor**
    - Manage their courses and quizzes.
    - View enrollments and quiz statistics.
    - Open **3D analytics** for their courses and see quiz Aggregates.
- **Admin**
    - Global view and management of users, roles, categories, courses, enrollments.
    - Access **data visualizations** (2D + 3D) for any instructor’s courses via a **course selector**.

The project is designed explicitly to match the **CSIS 231 Final Project requirements** (see section 11).

---

## 2. High-Level Architecture

**Monorepo structure:**

```text
csis-231-project/
└── csis_231-login-registration-jwt/
    ├── csis231-api/          # Spring Boot backend (REST API + security)
    └── demo/                 # JavaFX 17 client (FXML + 2D/3D visualizations)
```

### Backend (Spring Boot – `csis231-api`)

- Follows a **layered design**:
    - `controller` – REST endpoints.
    - `service` – business logic, validation, security checks.
    - `repository` – Spring Data JPA repositories.
    - `domain/model` – entities (User, Role, Course, Enrollment, Quiz, OTP, etc.).
    - `exception` – custom exception hierarchy.
    - `common` – shared utilities, `GlobalExceptionHandler`, converters, etc.
- Stateless security with **JWT**.
- Email-based **OTP** for two-step flows (2FA).
- Centralized **error handling** via `@ControllerAdvice`.

### Database (PostgreSQL)

- Stores:
    - Users & Roles
    - Categories & Courses
    - Enrollments
    - Quizzes, Questions, Results
    - OTP tokens
- Uses **Spring Data JPA** for ORM.
- Enforces relationships (e.g. User–Enrollment–Course, Course–Quiz–Result).

### Frontend (JavaFX – `demo`)

- JavaFX 17 application with:
    - **FXML views** under `demo/src/main/resources/com/example/demo`.
    - Controllers under `demo/src/main/java/com/example/demo`.
    - Shared stylesheet: `styles.css` (dark theme, buttons, tabs, cards, 2D/3D viz styles).
- Navigation:
    - `Launcher` + `HelloApplication` provide scene switching.
    - Dedicated controllers for **Admin**, **Instructor**, **Student**, and **Graphics** (2D/3D).

---

## 3. Technology Stack

**Backend**

- Java 17+
- Spring Boot (Web, Security, Validation)
- Spring Data JPA / Hibernate
- Spring Mail / JavaMail (for OTP, if configured)
- Spring Security + **JWT**
- Jakarta Bean Validation (`jakarta.validation`)

**Database**

- PostgreSQL
- Schema managed via JPA mappings and configuration (`application.properties` / `application.yml`).

**Frontend**

- JavaFX 17 (controls, FXML, 2D + 3D)
- JavaFX charts (`BarChart`, `LineChart`)
- JavaFX 3D (`SubScene`, `Group`, `Box`, `PhongMaterial`, `PerspectiveCamera`)
- Shared CSS (`styles.css`) with:
    - Buttons (`primary-button`, `secondary-button`, `ghost-pill`, etc.)
    - Layout helpers (cards, sections, chips, toolbars)
    - Styled `TabPane` and chart surfaces

**Build & Tools**

- Maven (multi-module)
- IntelliJ IDEA
- pgAdmin (for DB management)

---

## 4. Core Features by Role

### Student

- Register / login with **OTP 2FA** support.
- Browse available courses and enroll.
- Take quizzes and see their results.
- View a **“Visualize Progress”** screen:
    - **3D bars** for quiz scores.
    - **2D chart** of recent quiz performance, labeled by quiz name.

### Instructor

- Manage owned courses (create/update/archive).
- Manage quizzes attached to courses.
- View enrollments & quiz statistics.
- Open **“3D Analytics”**:
    - See a **3D course analytics** view.
    - Course-specific 3D bar (enrollments).
    - 2D chart of quiz averages for the selected course.

### Admin

- Manage users, roles, categories, courses, enrollments (from `dashboard.fxml`).
- Open **“Data Visualizations”**:
    - Choose any instructor’s course from a **course dropdown**.
    - See **3D enrollment bar** for that course.
    - See **2D quiz averages** by quiz name for that course.

---

## 5. Security & Authentication (JWT + OTP 2FA)

- **Authentication flow:**
    1. User registers with email/password.
    2. User logs in → backend validates credentials.
    3. Backend optionally generates and sends a **one-time OTP** (2FA) to the user’s email.
    4. User submits OTP → backend verifies code (correct, unexpired, bound to user).
    5. Backend issues a **JWT access token** (and optionally a refresh token).
    6. JavaFX client stores JWT in a dedicated **TokenStore/SessionStore**.
    7. All protected API calls include `Authorization: Bearer <jwt>` header.

- **Authorization:**
    - Role-based API access enforced by **Spring Security**.
    - UI hides or disables buttons that are not allowed for the current role.
        - Example: `3D Analytics` button only reachable for allowed roles.

- **Error Handling:**
    - Custom exception hierarchy:
        - `ApplicationException` (base)
        - `ResourceNotFoundException`
        - `BadRequestException`
        - `UnauthorizedException`, `ForbiddenException`, `ConflictException`, etc.
    - `GlobalExceptionHandler` (using `@ControllerAdvice`) produces a unified `ErrorResponse` with:
        - Timestamp
        - HTTP status
        - Error code
        - Message
        - Optional details for validation errors

---

## 6. 2D & 3D Visualizations

**Location:**  
`demo/src/main/java/com/example/demo/graphics/GraphicsPlaygroundController.java`  
`demo/src/main/resources/com/example/demo/graphics/graphics_playground.fxml`

### View layout

- **Top bar**:
    - “↩ Back” ghost-pill button (returns to appropriate dashboard based on role).
    - Title and meta chips (e.g. selected course, last refresh).
- **TabPane** with two tabs:
    1. **3D Analytics**
        - Dark “viz-card” section with:
            - `SubScene` hosting a 3D scene (`Group`, `Box` bars, `PerspectiveCamera`).
            - Bars represent metrics:
                - For Instructor/Admin: **enrollments per selected course**.
                - Fallback: student quiz scores in 3D.
            - Animated with `RotateTransition` for a high-level 3D feel.
    2. **2D Progress**
        - JavaFX `BarChart<String, Number>` (or `LineChart`) inside a styled card.
        - For students: recent quiz results by quiz **name**.
        - For admin/instructor + selected course: per-quiz **average scores** (by quiz name).

### Course selector (Admin/Instructor)

- Admin & instructor see a **ComboBox** to pick a course:
    - Admin: can select **any instructor’s course** (published/active courses).
    - Instructor: sees **their own courses**.
- When the selected course changes:
    - 3D scene is refreshed with the new **enrollment bar**.
    - 2D chart is refreshed with **quiz averages** for that course.

### Error/empty data handling

- If no data is available for the role/course:
    - Friendly warning chip / message instead of exceptions.
- Unauthorized situations (e.g. wrong role) are handled silently with:
    - Fallbacks where possible, or
    - Clear “not available for this role” notices.

---

## 7. Project Structure (Detailed)

### Backend – `csis231-api`

```text
csis231-api/
├── src/main/java/com/example/csis231
│   ├── auth/           # Auth controllers/services (login, register, OTP, JWT)
│   ├── user/           # User & Role domain, services, controllers
│   ├── category/       # Categories
│   ├── course/         # Courses & materials
│   ├── enrollment/     # Enrollments
│   ├── quiz/           # Quizzes, questions, results
│   ├── dashboard/      # Dashboard endpoints (student/instructor stats)
│   ├── otp/            # OTP domain (if separate)
│   ├── common/         # Api constants, helpers, GlobalExceptionHandler, DTOs
│   └── exception/      # Custom exception hierarchy
└── src/main/resources
    ├── application.properties (or application.yml)
    └── [schema/data SQL if used]
```

*(Exact package names may differ slightly; the structure above reflects the conceptual layout.)*

### JavaFX Client – `demo`

```text
demo/
├── src/main/java/com/example/demo
│   ├── auth/          # Login, register, OTP, reset controllers & APIs
│   ├── common/        # ApiClient, ApiException, ErrorDialog, AlertUtils, SessionStore, TokenStore
│   ├── course/        # Course catalog, detail, editor, enrollments
│   ├── dashboard/     # Admin dashboard controller
│   ├── instructor/    # InstructorDashboardController
│   ├── student/       # StudentDashboardController
│   ├── quiz/          # Quiz creation & quiz taker
│   ├── graphics/      # GraphicsPlaygroundController (2D/3D visualizations)
│   ├── Launcher.java  # Central navigation helper
│   └── HelloApplication.java # JavaFX app entry point
└── src/main/resources/com/example/demo
    ├── fxml/          # login.fxml, register.fxml, otp.fxml, dashboard.fxml, etc.
    ├── graphics/      # graphics_playground.fxml
    ├── styles.css     # global JavaFX CSS
    └── icons/         # images/icons (if any)
```

---

## 8. Key REST Endpoints (High-Level Overview)

> **Note:** Exact paths may include prefixes (e.g. `/api`, `/api/v1`).  
> Check the controllers for precise mappings; the list below is **conceptual**.

### Auth

- `POST /api/auth/register` – Register new user.
- `POST /api/auth/login` – Login with email/password.
- `POST /api/auth/otp/verify` – Verify OTP for 2FA.
- `POST /api/auth/forgot` – Request password reset (OTP, token, or link).
- `POST /api/auth/reset` – Reset password using token/OTP.
- `GET  /api/auth/me` – Get current user profile (JWT required).

### Dashboards

- `GET /api/student/dashboard` – Student summary:
    - Enrolled courses, recent quizzes, scores, etc.
- `GET /api/instructor/dashboard` – Instructor summary:
    - Courses taught, enrollments, quiz statistics.
- (Admin dashboard data may reuse other endpoints or have dedicated ones.)

### Users & Roles (Admin)

- `GET /api/users` – List users (optionally paginated).
- `GET /api/users/{id}`
- `POST /api/users` / `PUT /api/users/{id}` / `DELETE /api/users/{id}`
- Role management endpoints, if exposed.

### Categories & Courses

- `GET /api/categories` – List categories (with optional pagination).
- `POST /api/categories` – Create category.
- `GET /api/courses` – List courses (with optional pagination).
- `GET /api/courses/{id}`
- `POST /api/courses`
- `PUT /api/courses/{id}`
- `DELETE /api/courses/{id}`
- `GET /api/courses/{id}/enrollments` – Enrollment stats per course.

### Enrollments

- `POST /api/enrollments` or `POST /api/courses/{courseId}/enroll` – Enroll current user.
- `GET  /api/enrollments` or `GET /api/users/{userId}/enrollments` – List enrollments.

### Quizzes

- `GET  /api/quizzes/{id}` – Quiz definition (questions).
- `POST /api/quizzes` – Create quiz.
- `POST /api/quizzes/{id}/questions` – Add questions.
- `POST /api/quizzes/{id}/submit` – Submit answers.
- `GET  /api/quizzes/{id}/results` – Results overview (instructor/admin).
- `GET  /api/quizzes/{id}/my-result` – Current user’s result.

---

## 9. Running the Project

### Prerequisites

- **JDK 17+**
- **Maven 3+**
- **PostgreSQL** running locally or via Docker

### 1) Clone the repo

```bash
git clone https://github.com/M677871/csis-231-project.git
cd csis-231-project/csis_231-login-registration-jwt
```

### 2) Configure PostgreSQL

Create a database, e.g.:

```sql
CREATE DATABASE learnonline;
```

In `csis231-api/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/learnonline
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
app.jwt.secret=YOUR_JWT_SECRET
app.jwt.expiration=3600000

# Mail/OTP (if enabled)
spring.mail.host=smtp.your-provider.com
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_PASSWORD
```

### 3) Run the backend (Spring Boot)

From `csis231-api`:

```bash
mvn clean install
mvn spring-boot:run
```

Backend will start on the configured port (typically `http://localhost:8080`).

### 4) Run the JavaFX client

From `demo`:

```bash
mvn clean install
mvn javafx:run
```

OR run `HelloApplication` from your IDE.  
The JavaFX app will handle login, dashboards, and navigation to the 2D/3D analytics playground.

---

## 10. State Management, Error Handling & Validation

### State & Communication

- **SessionStore / TokenStore** hold:
    - Current user
    - JWT token(s)
    - Role info
- All HTTP requests go through a reusable **ApiClient**:
    - Adds `Authorization` header.
    - Deserializes JSON.
    - Wraps errors in `ApiException`.

### Error Handling (Frontend)

- `ApiException` and other exceptions are displayed via:
    - `ErrorDialog.showError(ex)` or
    - `AlertUtils.showError(...)`.
- Asynchronous calls use `CompletableFuture` + `Platform.runLater` to keep UI responsive.

### Validation (Backend)

- DTOs annotated with `@NotBlank`, `@Email`, `@Size`, etc.
- Validation errors are captured by `GlobalExceptionHandler` and returned as structured JSON.

---

## 11. How This Project Meets CSIS 231 Final Requirements

**Spring Boot Backend (25 pts)**

- Clean **layered architecture** (Controller / Service / Repository / Domain).
- Well-structured **PostgreSQL schema** mapped via JPA.
- Business logic encapsulated in services, not controllers.

**High-Level Features & 3D (20 pts)**

- **Advanced 2D/3D data visualization**:
    - 3D analytics using JavaFX 3D, animated and data-driven.
    - 2D charts showing quiz scores and course performance.
    - Course selector for admin/instructor to inspect any instructor’s course.

**JavaFX UI (30 pts)**

- Modular FXML screens for each feature (Auth, Dashboards, Courses, Quizzes, Viz).
- Shared stylesheet (`styles.css`) with reusable style classes.
- Clear role-based dashboards and dedicated 2D/3D visualization playground.

**Error Handling & Security (10 pts)**

- **JWT + Spring Security** for authentication & authorization.
- **OTP/2FA** support for sensitive flows.
- Centralized error handling with `GlobalExceptionHandler`.
- Input validation with `jakarta.validation`.

**Documentation & API Docs (15 pts)**

- This README provides:
    - Setup & run instructions.
    - Architecture overview.
    - Role-based feature summary.
    - High-level API endpoints.
    - Explanation of 2D/3D analytics and security.
- Codebase includes descriptive naming and structured packages, ready for Javadoc / Swagger if needed.

---

## 12. Possible Future Improvements

- Add **Swagger/OpenAPI (springdoc)** for fully interactive API docs.
- Add **WebSockets** to stream live quiz/enrollment updates to the JavaFX client.
- Add more metrics to the visualization chips (engagement, retention, completion rate).
- Extend 3D scene with multiple bars and transitions per metric.
- Implement audit logging and admin activity reports.

---

## 13. Credits

**Author:** Charbel Boutros  
**Course:** CSIS 231 – Advances in Computer Science  
**Institution:** University of Balamand – Faculty of Arts & Sciences
