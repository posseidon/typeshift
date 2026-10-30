# Story 0.4 — Logout and token revocation
Points: 5 | Tool: opsx:plan + opsx:apply | ADRs: 001

As the owner, I want logout to end my session immediately.

Acceptance criteria
- [ ] Logout inserts the current jti into revoked_token with revoked_at and expires_at (the original token expiry).
- [ ] The cookie is cleared in the response.
- [ ] A daily scheduled job deletes revoked_token rows whose expires_at is in the past.
- [ ] Reusing a logged-out token returns 401 (Story 0.3).
- [ ] Logout of an already-revoked or expired token still returns success and clears the cookie.
