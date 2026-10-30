# Story 0.3 — Token validation
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 001

As the system, I must reject unauthenticated access to all protected pages and APIs.

Acceptance criteria
- [ ] A filter checks the cookie on every route except login, registration, and static assets.
- [ ] Missing, malformed, expired, or revoked tokens are rejected.
- [ ] API requests with an invalid token receive 401.
- [ ] Page requests with an invalid token redirect to login.
- [ ] A revoked token is one whose jti exists in revoked_token (Story 0.4).
- [ ] Signature verification uses the environment secret. Algorithm is fixed; "none" is rejected.
- [ ] Each rejection is logged with a reason code, without logging the token.
