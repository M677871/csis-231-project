# CSIS-231 LMS — High-Level README

A clean, production-style Learning Management System built for CSIS-231.  
Backend: Spring Boot (Java 21, JPA, PostgreSQL, JWT, 2FA via OTP email).  
Frontend: JavaFX desktop app (role-aware dashboards: Student / Instructor / Admin).

---

## 🚀 Overview

This project delivers a secure, role-based LMS with:

- **Authentication & 2FA:** Username/password → email OTP → JWT session.
- **Role routing:** Student / Instructor / Admin each see their own dashboard.
- **Courses:** Public catalog & details, instructor course creation (with categories), student enrollment.
- **Quizzes:** Students take quizzes and receive scores; answers are not leaked before submission.
- **Extensible admin:** Category management (API ready), room to expand to user & content governance.

---

## ✨ Features

- **Secure auth flow:** Spring Security 6 + JWT; OTP codes persisted & expiring.
- **RBAC:** `STUDENT`, `INSTRUCTOR`, `ADMIN` enforced in the API with `@PreAuthorize`.
- **Course catalog:** Public read; details include materials, categories, quizzes.
- **Enrollments:** One-click student enrollment (server-side guarded).
- **Instructor tools:** Create course + assign categories.
- **Quizzes:** Fetch questions/options (no `isCorrect`), submit answers → grade & store result.
- **Clean JavaFX client:** Central navigation, API client layer, DTO models, token store.

---

## 🏗 Architecture

### High-Level

```
JavaFX (desktop)
  ├─ Security: TokenStore (JWT), /api/me cache
  ├─ API: AuthApi, MeApi, CourseApi, CategoryApi, QuizApi
  ├─ UI: Login → OTP → DashboardRouter → Role dashboards
  └─ Screens: Catalog, Course Details, Create Course, Quiz

Spring Boot (REST)
  ├─ AuthController (+OtpController): login, OTP verify, forgot/reset
  ├─ MeController: current user profile + role
  ├─ CourseController: list/details, enroll (STUDENT), create (INSTRUCTOR)
  ├─ QuizController: get quiz (STUDENT), submit (STUDENT)
  └─ CategoryController: list (INSTRUCTOR/ADMIN), CRUD (ADMIN)
```

### Tech Stack

- **Backend:** Java 21, Spring Boot 3.5.x, Spring Security 6, Spring Data JPA (Hibernate), PostgreSQL, Jakarta Mail (via Spring Boot Mail), Lombok, JJWT 0.11.5.
- **Frontend:** JavaFX, Java 21, `java.net.http`, Jackson.
- **Build:** Maven.

---

## 📦 Project Structure (simplified)

```
csis231-api/                      # Spring Boot backend
  ├─ com.csis231.api.auth         # AuthService, AuthController, OTP
  ├─ com.csis231.api.security     # JwtAuthenticationFilter, SecurityConfig, JwtUtil
  ├─ com.csis231.api.user         # User entity, MeController, repository
  ├─ com.csis231.api.course       # Course domain, DTOs, CourseController/Service
  ├─ com.csis231.api.category     # CategoryController/Service/Repo
  ├─ com.csis231.api.quiz         # QuizController/Service/DTOs
  └─ resources/application.yml    # DB + mail + JWT config

csis231-client/                   # JavaFX frontend
  ├─ com.app.client               # MainApp, NavigationManager
  ├─ security                     # TokenStore, Session models
  ├─ api                          # ApiClient, AuthApi, MeApi, CourseApi, CategoryApi, QuizApi
  ├─ model                        # DTOs mirroring backend
  ├─ controllers                  # Login, OTP, dashboards, catalog, details, create, quiz
  └─ resources/.../fxml           # FXML screens (+ css)
```

---

## 🔐 Security & 2FA

- **Login:** `POST /api/auth/login`
    - Invalid → `401`.
    - If OTP required → `202` with `{ "otpRequired": true, "purpose": "LOGIN_2FA", "username": "..." }`.
- **OTP Verify:** `POST /api/auth/otp/verify` → `{ token, id, username, firstName, lastName, role }`.
- **Session:** JavaFX stores JWT in `TokenStore`; all protected calls include `Authorization: Bearer <token>`.
- **RBAC:** `@PreAuthorize("hasRole('STUDENT')")` / `INSTRUCTOR` / `ADMIN` on controller methods.
- **JWT Subject:** username; `JpaUserDetailsService` maps role → `ROLE_<ENUM>`.

---

## 🔗 API Contract (Frontend depends on this)

**Auth**
- `POST /api/auth/login` → `AuthResponse` or `202 otpRequired`.
- `POST /api/auth/otp/verify` → `AuthResponse { token, ..., role }`.
- `POST /api/auth/password/forgot` / `password/reset` (optional UI).

**Identity**
- `GET /api/me` (auth) → `{ id, username, firstName, lastName, role }`.

**Catalog & Courses**
- `GET /api/courses` (public) → `CourseSummary[]`.
- `GET /api/courses/{id}` (public) → `CourseDetails` incl. `materials[]`, `quizzes[]`, `categories[]`.
- `POST /api/courses/{id}/enroll` (STUDENT) → `EnrollResponse`.
- `POST /api/courses` (INSTRUCTOR) → `CreateCourseRequest { courseName, description, published, categoryIds[] }` ⇒ `CourseSummary`.

**Categories**
- `GET /api/categories` (INSTRUCTOR/ADMIN) → `CategoryDto[]`.
- `POST/PUT/DELETE /api/categories/**` (ADMIN).

**Quizzes**
- `GET /api/quizzes/{quizId}` (STUDENT) → `QuizToTake` (no `isCorrect` flags).
- `POST /api/quizzes/{quizId}/submit` (STUDENT) → `QuizResultResponse { score, ... }`.

---

## 🗄 Data Model (core)

- **User**: id, username, email, password, firstName, lastName, phone, role (`STUDENT|INSTRUCTOR|ADMIN`), `emailVerified`, `twoFactorEnabled`.
- **OtpCode**: user, code, purpose (`LOGIN_2FA`, `PASSWORD_RESET`), expiresAt, consumedAt.
- **Course**: id, courseName, description, published, instructor (User), categories[], materials[], quizzes[].
- **Category**: id, name.
- **CourseEnrollment**: (unique student, course), status, enrolledAt.
- **Quiz / Question / Answer**: quiz belongs to course; answers do not include `isCorrect` in client payloads; grading occurs server-side.
- **QuizResult**: quiz, student, score, completedAt.

---

## ⚙️ Setup & Configuration

### Prerequisites
- JDK 21+
- Maven 3.9+
- PostgreSQL 14+ (or compatible)
- SMTP settings for outgoing email (OTP).

### Backend Configuration (example `application.yml`)
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/csis231
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update     # for development
    open-in-view: false
  mail:
    host: smtp.example.com
    port: 587
    username: your_smtp_user
    password: your_smtp_password
    properties:
      mail:
        smtp:
          auth: true
          starttls.enable: true

jwt:
  secret: "CHANGE_ME_MIN_256_BITS"
  expirationMinutes: 120
```

> **Note:** For production, use environment variables / Docker secrets and disable `ddl-auto`.

### Running the Backend
```bash
cd csis231-api
mvn clean spring-boot:run
# or
mvn -DskipTests package
java -jar target/csis231-api-*.jar
```

### Running the Frontend
Set the API base URL in the client (e.g., `ApiClient("http://localhost:8080")`), then:
```bash
cd csis231-client
mvn clean javafx:run
# or run MainApp from your IDE
```

---

## 🧭 Frontend UX Flow

1. **Login** → `POST /api/auth/login`
    - If OTP required → navigate to OTP screen.
2. **OTP** → `POST /api/auth/otp/verify` → store JWT.
3. **Route by role:** call `/api/me` → Student/Instructor/Admin dashboard.
4. **Student**
    - Browse catalog (public)
    - View course details
    - Enroll (auth STUDENT)
    - Take quizzes (auth STUDENT)
5. **Instructor**
    - Create course: fetch categories, submit with `categoryIds`
    - Browse catalog (read-only)
6. **Admin**
    - Browse catalog; category management UI can be added progressively.

---

## ✅ Quality & Security Notes

- All protected endpoints require **valid JWT**; role annotations enforced.
- OTP codes are **single-use**, **expire**, and are **persisted**.
- DTOs **never expose** sensitive fields or `isCorrect` quiz flags.
- JavaFX client hides actions based on role **and** backend re-checks permissions.

---

## 🧪 Testing Hints

- **Unit tests:** services for OTP lifecycle, quiz grading, course enrollment uniqueness.
- **Integration tests:** `@SpringBootTest` with Testcontainers (PostgreSQL).
- **API contract tests:** verify response shapes used by the JavaFX client.

---

## 🗺 Roadmap

- Admin UI for categories & user management.
- Instructor UI for quiz creation & materials upload.
- Student “My enrollments” and “My results”.
- E2E tests (REST + JavaFX).
- Docker Compose for API + DB + MailHog (dev).

---

## 🤝 Contributing

1. Branch from `main` → `feature/short-desc`
2. Follow package structure; keep DTOs out of entity packages.
3. Add/adjust unit tests for services you change.
4. Open PR with clear description & screenshots (when UI).

---

## 📄 License

Educational project for CSIS-231. Choose and add a license file if needed (e.g., MIT).
