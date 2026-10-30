# Story 0.6 — Learning profile initialisation
Points: 3 | Tool: executing-plans | ADRs: 013

As the system, I must create a learning profile when the owner registers.

Acceptance criteria
- [ ] A learning profile row is created in the same transaction as the user.
- [ ] Initial state: current stage 1, unlocked stages = {1}, program start date empty, total practice minutes 0.
- [ ] Program start date is set on the first sub-exam attempt, not at registration.
- [ ] If profile creation fails, user creation is rolled back.
