# user-account Specification

## Purpose

Provide simple, professional username/password account access for the application: hashed credentials at rest, JWT form login, token revocation, a minimal signup route, profile management, and per-user learning-profile initialisation. No pre-provisioned users. (Epic 0.)

## Requirements

### Requirement: Credentials stored hashed at rest
The system SHALL store user account passwords as salted BCrypt hashes (cost factor 12 or higher) and SHALL NOT store or log plaintext passwords.

#### Scenario: Signup persists only a hash
- **WHEN** a new account is created with a password
- **THEN** only a BCrypt hash of that password is persisted, and no plaintext appears in storage or logs

### Requirement: Registration with field validation
The application SHALL provide a signup route that validates a username of 3–30 letters, digits, or underscores and a password of at least 8 characters, with an optional display name, and SHALL report a duplicate username clearly.

#### Scenario: New account is created and can log in
- **WHEN** a user signs up with an unused, valid username and a valid password
- **THEN** the account and its learning profile are created in one transaction and the user is redirected to login

#### Scenario: Duplicate username is refused
- **WHEN** a signup attempts to create an account with an already-used username
- **THEN** the account is not created and a clear validation message is shown

#### Scenario: Invalid fields are refused
- **WHEN** a signup supplies a username outside 3–30 word characters or a password shorter than 8 characters
- **THEN** the account is not created and the specific rule violation is reported

### Requirement: Login issues a signed JWT in an httpOnly cookie
The application SHALL authenticate username and password, and on success issue a signed JWT (subject, token id, issued-at, 30-day expiry) using a signing secret from an environment variable, delivered in an httpOnly cookie with SameSite=Lax. The token SHALL NOT appear in URLs or local storage.

#### Scenario: Valid credentials grant access
- **WHEN** a user submits correct credentials
- **THEN** a signed JWT is issued in an httpOnly cookie and the user is signed in

#### Scenario: One generic failure message
- **WHEN** a user submits an incorrect username or password
- **THEN** a single generic error is shown regardless of which field was wrong, and no session is established

#### Scenario: Brute-force protection blocks repeated failures
- **WHEN** a user fails login five consecutive times
- **THEN** further login attempts for that username are blocked for fifteen minutes

> **Status: PARTIAL** — the `Secure` cookie flag is opt-in via the `COOKIE_SECURE` environment variable and defaults to off even in the prod profile; it is not forced on in prod.

### Requirement: Token validation on every protected route
A filter SHALL validate the cookie on every route except login, signup, logout, and static assets. Missing, malformed, expired, or revoked tokens SHALL be rejected — API requests with 401, page requests with a redirect to login — using a fixed signature algorithm that rejects unsigned ("none") tokens, logging a reason code without the token value.

#### Scenario: Unauthenticated API request is rejected
- **WHEN** an API request carries a missing, expired, malformed, or revoked token
- **THEN** the request is rejected with 401

#### Scenario: Unauthenticated page request redirects
- **WHEN** a page request carries an invalid token
- **THEN** the user is redirected to the login page

#### Scenario: Revoked token is rejected
- **WHEN** a token's jti exists in the revocation list
- **THEN** the request is rejected

### Requirement: Logout revokes the token and clears the cookie
Logout SHALL insert the current token id into a revocation list with its original expiry, clear the cookie, and still return success for an already-revoked or expired token. A scheduled daily job SHALL purge revocation rows whose expiry has passed.

#### Scenario: Logout ends the session immediately
- **WHEN** a signed-in user logs out
- **THEN** the token id is revoked, the cookie is cleared, and reusing the token is subsequently rejected

#### Scenario: Expired revocation rows are purged
- **WHEN** the daily purge job runs
- **THEN** revocation rows whose expiry is in the past are deleted

### Requirement: Profile view and update
The application SHALL show a profile page with username, display name, account creation date, current stage, the read-only keyboard layout (HU_QWERTZ), and English-only interface language. Display name SHALL be editable and password change SHALL require the current password plus a new password of at least 8 characters.

#### Scenario: Display name is updated
- **WHEN** the user saves a non-blank display name
- **THEN** the new display name is persisted

#### Scenario: Password change verifies the current password
- **WHEN** the user submits a correct current password and a valid new password
- **THEN** the password hash is replaced; an incorrect current password is refused

### Requirement: Learning profile initialised with the account
A learning profile SHALL be created in the same transaction as the user, initialising current stage 1, unlocked stages {1}, program start date empty, and total practice minutes 0. If profile creation fails, the user creation SHALL roll back.

#### Scenario: Registration creates the starting profile
- **WHEN** an account is created
- **THEN** a learning profile exists for it with stage 1 unlocked and no start date

#### Scenario: Profile failure rolls back the account
- **WHEN** learning-profile creation fails during signup
- **THEN** the user account is not persisted

### Requirement: No pre-provisioned credentials
The application SHALL ship with no pre-provisioned user accounts or default credentials in a fresh deployment.

#### Scenario: Fresh deployment has no users
- **WHEN** the application boots against a fresh database
- **THEN** no accounts exist until one is created through signup
