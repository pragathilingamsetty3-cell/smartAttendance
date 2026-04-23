# Smart Attendance System - Backend Architecture Documentation

This document provides a comprehensive, exhaustive overview of the Smart Attendance System backend architecture. It has been built systematically by analyzing the complete source code, configuration files, and architectural patterns of the Spring Boot 3.5.0 environment.

---

## 1. Executive Summary & Tech Stack

The backend is an enterprise-grade, high-concurrency (targeting 50k+ users), zero-trust architecture built on **Spring Boot 3.5.0**. It employs advanced sensor fusion, AI-driven behavioral learning, and distributed caching to manage real-time academic attendance while strictly preventing spoofing and proxy attendance.

**Core Technology Stack:**
*   **Framework:** Java 23 / Spring Boot 3.5.0
*   **Database:** PostgreSQL with PostGIS (for spatial geofencing)
*   **In-Memory Data Grid:** Redis (for session caching, rate limiting, and temporary state)
*   **Event Streaming:** Apache Kafka (for asynchronous processing and event sourcing)
*   **AI Integration:** Spring AI / Google Gemini (for spatial anomaly detection and learning)
*   **Build System:** Maven
*   **Deployment:** Manual USB Deployment (Optimized for Local Servers)
*   **Monitoring:** Micrometer, Prometheus, Spring Boot Actuator

---

## 2. Infrastructure & Configuration Layer

The application utilizes a robust configuration layer (`com.example.smartAttendence.config.*`) designed for production readiness.

### 2.1 Server & Application Configuration (`application.properties`)
*   **Security:** Strict HTTPS enforced via PKCS12 keystore (`smartattendence.p12`) running on port 8443. HTTP/2 is enabled.
*   **Connection Pooling:** HikariCP configured for high throughput.
*   **Monitoring Checkpoints:** Advanced health indicators configured for all critical infrastructure components (DB, Redis, Disk).

### 2.2 Security Architecture (`SecurityConfig.java`, `JwtConfig.java`)
*   **Zero-Trust Model:** All endpoints require authenticated access, except specific `/auth/login` and `/auth/register` endpoints.
*   **Stateless Authentication:** Implements JWT with 2048-bit RSA key pairs (`private.pem`, `public.pem`).
*   **Token Payload:** Enriches JWT with Device Fingerprint, Session ID, IP Address, and Geo-Location to prevent session hijacking.
*   **Hardened Headers:** HSTS (1 year), Content Security Policy (CSP), X-Frame-Options (DENY), and XSS Protection enforced via Spring Security firewalls.
*   **Web Filters:** Custom filters intercept every request to enforce rate limits, validate device fingerprints, and log security audits.

### 2.3 High Performance & Resilience
*   **AdvancedRateLimitConfig.java:** Utilizes Resilience4j for circuit breaking and granular rate limiting across different APIs (Global, Auth, API, Heartbeat).
*   **AdvancedCacheManager.java:** Implements an intelligent, multi-tier Redis caching strategy. Cache TTL varies by category (e.g., sessions expire faster than master data).
*   **HighPerformanceLoadBalancingConfig.java:** Custom load-aware round-robin selection mechanism.
*   **Async Processing (`AsyncConfig.java`):** Employs dedicated thread pools (`attendanceExecutor`, `reportExecutor`) to ensure I/O bound tasks do not block main application threads.

---

## 3. Data Domain & Entity Architecture

The spatial and structural data model is defined in `com.example.smartAttendence.domain.*` and `com.example.smartAttendence.entity.*`.

### 3.1 User & Access Management Models
*   **User / V1User:** Central authentication entity holding core credentials, roles (`SUPER_ADMIN`, `ADMIN`, `FACULTY`, `STUDENT`), device fingerprints, and biometric signatures. Employs soft-delete via `UserStatus`.
*   **StudentProfile:** Holds academic-specific data (GPA, credits, enrollment status, attendance percentage).
*   **DeviceBinding:** Strictly maps a student to a primary device preventing "one student, multiple devices" proxy attendance.
*   **RefreshToken:** Handles secure session renewals.
*   **SecurityAlert:** Logs suspicious activities (e.g., location spoofing logic).

### 3.2 Academic Structural Models
*   **Department & Section:** Represents the academic structural hierarchy.
*   **Room:** Integrated with PostGIS `Polygon` data types to store exact physical room boundaries for geofencing.
*   **Timetable & ClassroomSession:** Core logic for mapping what class occurs where and when. Includes robust break-time management configuration (lunch breaks, short breaks with specific walk-out tolerances).

### 3.3 Advanced Scheduling & Transition Models
*   **AcademicCalendar:** Handles holidays, exam days, and special events overriding standard timetables.
*   **EmergencySessionChange:** Audits sudden shifts in faculty, room, or timing.
*   **RoomChangeTransition:** Manages complex workflows like "15-minute grace periods" during back-to-back classes in different physical buildings.
*   **CRLRAssignment:** Manages dynamic assignment of Class Representatives and Lab Representatives by Faculty.

### 3.4 Attendance & Telemetry Models
*   **AttendanceRecord:** The unified snapshot representing final attendance status.
*   **SensorReading:** High-frequency, massive-scale table storing continuous accelerometer, pedometer, and GPS coordinates emitted by student devices.
*   **OfflineAttendanceRecord:** Synchronizes cryptographically signed attendance markers taken when the student device lost network connectivity.

---

## 4. Business Logic & Service Capabilities

Business logic bridges raw data to the API utilizing smart constraints.

### 4.1 Authentication & Onboarding
*   **UnifiedAuthService:** Handles First-Login detection enforcing immediate biometric and device binding before the app can be used. OTP flows and dynamic password generation.
*   **AdminV1Service:** Controls automated bulk generation of users, sections, departments, and "bulk promotion" of students between academic years. Handles precise spatial boundary generation for rooms.
*   **FacultyHallPassService:** The specific workflow allowing faculty to temporarily pause Walk-Out rules for specific students requiring restroom/library access.

### 4.2 Sensor Fusion & Spoofing Detection
*   Based in `SensorFusionService`.
*   Processes `EnhancedHeartbeatPing` payloads containing step counts, 3-axis acceleration (X, Y, Z), device motion state, battery level, screening status, and strict GPS coordinates.
*   Implements mathematically complex spoofing detection algorithms (e.g., determining if device motion matches the reported GPS coordinate shift).

### 4.3 AI & Analytics Optimization
*   `AILearningOptimizer` & `AISpatialMonitoringEngine`.
*   Connects to Google Gemini (via Spring AI) to learn individual student behavioral patterns.
*   Predicts "Walk-out probability".
*   Calculates dynamic heartbeat intervals to save student device battery life (e.g., increasing ping frequency to 10 seconds if walking, dialing back to 120 seconds if stationary and screen off).

---

## 5. API Controller Surface (v1)

The system exposes unified RESTful endpoints via `com.example.smartAttendence.controller.v1.*`.

### 5.1 AuthV1Controller
*   `POST /api/v1/auth/login`: Validates credentials, checks if First-Login setup is required, generates robust zero-trust JWTs and Refresh Tokens.
*   `POST /api/v1/auth/complete-setup`: Binds initial biometric signatures and device IDs.
*   `POST /api/v1/auth/refresh-token`, `POST /api/v1/auth/logout`.

### 5.2 AdminV1Controller
*   *Security:* Restricted to `ADMIN` and `SUPER_ADMIN`.
*   `POST /onboard/*`: Bulk creation endpoints for Students, Faculty, and Admins.
*   `POST /rooms`: Creates rooms with complex spatial boundaries. Includes validation endpoints for intersecting geofences.
*   `PUT /timetables/{id}/lunch-break`: Manages macro scheduling operations.
*   *AI Command:* `POST /ask-ai`: Allows admins to query system statistics via Natural Language to the LLM.

### 5.3 AttendanceV1Controller
*   *Performance:* Highly optimized endpoint receiving continuous telemetry. Wrapped in Resilience4j `@RateLimiter`.
*   `POST /heartbeat-enhanced`: The core engine receiving battery stats, sensor data, and GPS. Returns JSON dynamically instructing the mobile client on its optimal next ping interval.
*   `POST /hall-pass`: Allows students to request verified physical exit from the geofence.

### 5.4 AIAnalyticsController
*   `POST /spatial-analysis/{studentId}/{sessionId}`: AI-driven spatial anomaly detection.
*   `POST /drift-analysis`: AI detects if the movement is legitimate GPS drift inside a building or malicious spoofing software.
*   `POST /walk-out-prediction`: Proactive analysis based on recent sensor vectors.

### 5.5 FacultyV1Controller & CRLRAssignmentController
*   Allows faculty to approve/deny Hall Passes.
*   Enables faculty to elevate specific student privileges (assigning Class Representatives).

### 5.6 BoundaryManagementController
*   Advanced PostGIS utility exposing endpoints to generate complex Polygons (Rectangle, Circle, L-Shape) mathematically, validating overlaps and calculating physical square footage.

---

## 6. Observability & Error Handling

*   **GlobalExceptionHandler.java:** Intercepts all REST responses. Unifies all errors into a strict `{ timestamp, status, code, error, message }` JSON contract. Specifically masks SQL exceptions while clearly alerting on `SECURITY_VIOLATION`.
*   **PerformanceAspect.java:** Spring AOP intercepts cross-cutting concerns to track exact ms speed of repository and business logic execution. Pushes metrics out to Prometheus.
*   **Event Architecture:** Utilizes Spring ApplicationEvents (`WalkOutEvent.java`) to decouple the detection of a walk-out from the heavy operations of alerting parents via email and updating database penalties.

---

### System Verdict
The architecture is exceptionally sound, combining zero-trust principles with high-frequency temporal-spatial telemetry. It seamlessly merges traditional OOP boundaries with modern AI behavioral learning models.
