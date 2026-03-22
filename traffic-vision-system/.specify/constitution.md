# Traffic Vision System Constitution

## Core Principles

### I. Architecture-First
- **Backend:** Must follow the Spring Boot layered architecture: `Controller` -> `Service` -> `Repository`.
- **OpenCV Logic:** Computer vision algorithms must be encapsulated in dedicated detector classes within the `com.example.trafficvision.opencv` package.
- **Frontend:** Must use a component-based architecture with React functional components and Hooks.
- **Decoupling:** Detection logic (OpenCV) must be decoupled from API controllers through service layers.

### II. Reusable Modules
- **CV Detectors:** Core algorithms like `MotionDetector` or `TrafficLightDetector` should be stateless and reusable.
- **DTOs:** Data transfer between backend and frontend must use well-defined DTOs in `com.example.trafficvision.dto`.
- **UI Components:** Reusable frontend components (e.g., `VideoPlayer`, `TrafficStats`) must live in `frontend/src/components`.
- **Shared Utilities:** Image processing helpers must be placed in `DebugDrawingUtils` or similar utility classes.

### III. Test-First Development
- **Mandatory Testing:** All new services and CV algorithms must include automated tests.
- **Backend:** Use JUnit 5 and Mockito for unit testing `Service` and `OpenCV` classes.
- **Frontend:** Critical UI logic and hooks must be tested using Jest or Vitest.
- **Regression:** Changes to CV detectors must be validated against known "Golden" video frames to ensure detection accuracy.

### IV. Integration & System Validation
- **Processing Pipeline:** The end-to-end flow (Upload -> Async Processing -> DB Persistence -> Frontend Polling) must be validated.
- **Database Integrity:** Ensure `video_id` foreign key relationships are maintained across `analysis_result` and `traffic_event`.
- **API Contracts:** Frontend API calls in `services/api.js` must strictly match backend `@RestController` endpoints.

### V. Performance & Observability
- **Async Execution:** Use `ExecutorService` for all video processing tasks to avoid blocking the main thread.
- **CV Optimization:** Implement frame skipping or resolution downsizing in `VideoProcessingService` to maintain performance.
- **Frontend Rendering:** Canvas overlay rendering must use `requestAnimationFrame` for 60 FPS performance.
- **Logging:** Use SLF4J/Lombok `@Slf4j` for structured logging of processing milestones and errors.

---

## Technical Constraints

- **Programming Languages:** Java 17 (Backend), TypeScript 4.9+ (Frontend).
- **Frameworks:** Spring Boot 3.2, React 18, Ant Design (antd) 5.x.
- **CV Library:** OpenCV 4.9.0 (Java bindings).
- **Database:** MySQL 8.0+.
- **Dependency Management:** Maven (`pom.xml`) for Backend, npm (`package.json`) for Frontend.
- **Project Structure:** Monorepo with `backend/` and `frontend/` root directories.

---

## Development Workflow

- **Feature Development:** Use `feature/` branch prefix. Implement logic, then tests, then UI.
- **Code Review:** Ensure strict adherence to this constitution, particularly naming conventions and architectural layering.
- **Branching Strategy:** `main` (stable), `develop` (integration), `feature/*` (tasks).
- **Documentation:** Use Javadoc for backend services and TSDoc for frontend components. Maintain `README.md` for setup instructions.
- **Task Tracking:** Follow the task lists defined in `specs/` (e.g., `specs/stop-line-stabilization/tasks.md`).

---

## Governance

This constitution defines the development principles for the project. All generated code, pull requests, and architectural decisions must comply with this constitution.

Amendments must:
1. Document the reason for change
2. Update version number
3. Be reviewed and approved by maintainers

Version: 1.0.0
Ratified: Monday, March 16, 2026
Last Amended: Monday, March 16, 2026
