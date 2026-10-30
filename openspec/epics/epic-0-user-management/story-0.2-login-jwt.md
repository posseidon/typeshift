# Story 0.2 — Login and JWT issuance
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 001

As the owner, I want to log in so I can access my progress.

Acceptance criteria
- [ ] Login accepts username and password.
- [ ] Failure returns one generic message regardless of which field was wrong.
- [ ] Success issues a signed JWT with subject, token id (jti), issued-at, and expiry 30 days later.
- [ ] Signing secret comes from an environment variable. It is never committed.
- [ ] JWT is set in an httpOnly cookie. Secure flag on in prod. SameSite set to Lax.
- [ ] Token is never placed in URLs or local storage.
- [ ] Brute-force protection: after 5 consecutive failures, login is blocked for 15 minutes.

Out of scope: refresh tokens (ADR-001 uses a single 30-day token).
