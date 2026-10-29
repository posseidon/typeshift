# user-account Specification

## Purpose

Provide simple, professional username/password account access for the application: hashed credentials at rest, form-based login, a minimal signup route, and no pre-provisioned users.

## Requirements

### Requirement: Credentials stored hashed at rest
The system SHALL store user account passwords as salted hashes (BCrypt) and SHALL NOT store or log plaintext passwords.

#### Scenario: Signup persists only a hash
- **WHEN** a new account is created with a password
- **THEN** only a BCrypt hash of that password is persisted, and no plaintext password appears in storage or logs

### Requirement: Username/password form login
The application SHALL authenticate users with a username and password via form login, maintaining a session for subsequent requests.

#### Scenario: Valid credentials grant access
- **WHEN** a user submits their correct username and password through the login form
- **THEN** the application establishes an authenticated session

#### Scenario: Invalid credentials are rejected
- **WHEN** a user submits an incorrect username or password
- **THEN** the application refuses authentication and does not establish a session

### Requirement: Minimal signup path
The application SHALL provide a minimal signup route that creates a new account with a unique username and a hashed password.

#### Scenario: New account can be created and used
- **WHEN** a user signs up with an unused username and a password
- **THEN** an account is created and those credentials authenticate successfully on login

#### Scenario: Duplicate usernames are refused
- **WHEN** a signup attempts to create an account with an already-used username
- **THEN** the account is not created and the duplicate is reported

### Requirement: No pre-provisioned credentials
The application SHALL ship with no pre-provisioned user accounts or default credentials in a fresh deployment.

#### Scenario: Fresh deployment has no existing users
- **WHEN** the application boots against a fresh database
- **THEN** no user accounts exist until one is created through the signup route
