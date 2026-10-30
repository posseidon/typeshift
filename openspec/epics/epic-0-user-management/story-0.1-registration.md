# Story 0.1 — Registration
Points: 3 | Tool: executing-plans | ADRs: 001, 014

As the owner, I want to create an account so my progress is saved.

Acceptance criteria
- [ ] Form fields: username (3 to 30 characters, letters, digits, underscore), password (minimum 8 characters), display name (optional).
- [ ] Password stored as a BCrypt hash, cost factor 12 or higher. Plaintext never stored or logged.
- [ ] Duplicate username shows a clear validation message.
- [ ] Success creates the user record and triggers Story 0.6.
- [ ] Redirects to login on success.

Out of scope: email verification, password strength meter.
