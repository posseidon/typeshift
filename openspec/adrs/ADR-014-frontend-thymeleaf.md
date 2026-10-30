# ADR-014: Frontend stack
Status: Locked (revised)

**Decision:** Thymeleaf templates rendered by Spring Boot. Vanilla JavaScript for keyboard capture, real-time WPM display, and the miss map. No frontend framework, no build pipeline, no component library.

**Rationale:** Developer has no React or Vite experience. Thymeleaf integrates directly with Spring Boot.

**Consequences:** Pages are server-rendered. JavaScript is progressive enhancement for typing behaviour.
