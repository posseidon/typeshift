# application-runtime Specification

## Purpose

Define the runtime the application runs on: a Spring Boot 3 / Java 21 app with virtual threads, a PostgreSQL persistence layer owned by Flyway, dev/prod Spring profiles pointing at local Compose and Supabase respectively, an AI provider abstraction with Ollama and Azure OpenAI implementations, and UTF-8-safe rendering. Deploys as a single runnable jar. (Epic 0b, replacing the earlier SQLite prototype.)

## Requirements

### Requirement: Application boots on Java 21 with virtual threads enabled
The application SHALL run as a Spring Boot 3 application on Java 21 with virtual threading enabled so request handling uses virtual threads.

#### Scenario: Application starts
- **WHEN** the application is launched
- **THEN** the web server starts successfully and accepts HTTP requests

### Requirement: PostgreSQL persistence owned by Flyway
The application SHALL be wired to PostgreSQL with `ddl-auto` set to `validate`, so Flyway owns the schema. Migrations SHALL use only standard PostgreSQL SQL, run on startup in both profiles, and a failed migration SHALL stop startup with a clear error. The SQLite prototype from Epic 0 is removed (SQLite remains only as a test-scope in-memory datasource).

#### Scenario: Persistence layer initializes
- **WHEN** the application starts with the PostgreSQL datasource configured
- **THEN** Flyway migrates the schema and Hibernate validates against it

#### Scenario: Failed migration stops startup
- **WHEN** a migration fails to apply
- **THEN** application startup aborts with a clear error

### Requirement: Flyway baseline with self-managed history
Flyway SHALL create its own schema history table, with V1 as an empty marker so real schema arrives in later migrations.

#### Scenario: Baseline migration applies cleanly
- **WHEN** Flyway runs against a fresh database
- **THEN** the history table is created and V1 applies as an empty marker

### Requirement: Dev and prod Spring profiles
A dev profile SHALL point at Docker Compose PostgreSQL and Ollama; a prod profile SHALL point at Supabase and Azure OpenAI. Every secret and endpoint SHALL be an environment variable, the active profile SHALL appear in the startup log, and dev and prod SHALL never share a database.

#### Scenario: Active profile is logged
- **WHEN** the application starts
- **THEN** the startup log names the active profile

#### Scenario: Dev and prod use distinct databases
- **WHEN** either profile is active
- **THEN** it resolves its own datasource, with no shared database

### Requirement: Docker Compose PostgreSQL for development
A Compose file SHALL define a single PostgreSQL service with a named volume, credentials from a git-ignored env file, a localhost-only exposed port, and a health check, with documented start/stop commands.

#### Scenario: Database starts with persistent data
- **WHEN** the Compose database is started and later restarted
- **THEN** it becomes healthy and data survives across restarts

### Requirement: Supabase production connection
The production connection string SHALL come from an environment variable, use only plain PostgreSQL features, keep the connection pool small for the free tier, and fail fast with a clear message if the connection cannot be made.

#### Scenario: Missing production connection fails fast
- **WHEN** the prod profile starts without a database URL
- **THEN** startup fails with a clear configuration error

### Requirement: AI provider interface with a shared envelope
A single interface SHALL define the ADR-004 request/response envelope, with no Telex or Vietnamese fields. Model name and endpoint SHALL be read from environment variables, and no provider-specific types SHALL leak outside the implementations.

#### Scenario: Both providers share one interface
- **WHEN** either provider implementation is called
- **THEN** it consumes the same request envelope and returns the same response envelope

### Requirement: AI provider selection and error handling
A profile setting SHALL select exactly one active provider implementation at runtime. Provider errors SHALL surface as a single exception type carrying the full cause.

#### Scenario: Profile selects the provider
- **WHEN** a profile is active
- **THEN** exactly one provider implementation serves generation requests through the shared interface

#### Scenario: Provider failure is wrapped
- **WHEN** a provider call fails
- **THEN** the failure is raised as the single provider exception type with its cause attached

### Requirement: Ollama local setup
Ollama SHALL run as a native install (not a Compose service), the model name SHALL be an environment variable, and a dev-profile startup check SHALL report whether the configured model is available and warn if it is not.

#### Scenario: Missing model warns at startup
- **WHEN** the dev profile starts with an Ollama model that is not installed
- **THEN** a startup warning names the missing model

### Requirement: Azure OpenAI configuration and connectivity check
Azure OpenAI endpoint, API key, and deployment name SHALL be environment variables, with setup notes covering the quota prerequisite. A connectivity check SHALL run on demand from a dev-profile-only route and SHALL live in the health domain.

#### Scenario: Dev-only connectivity check
- **WHEN** the dev-profile connectivity route is called
- **THEN** it reports provider reachability and is not exposed in the public UI or in prod

### Requirement: UTF-8 language support across layers
The application SHALL support UTF-8 encoding across rendering and persistence layers so Hungarian diacritics render correctly.

#### Scenario: Diacritics render correctly
- **WHEN** a rendered view contains Hungarian diacritic characters
- **THEN** the characters display correctly without mojibake

### Requirement: Deployable as a single jar
The build SHALL package the frontend module's templates and assets into the backend's runnable jar so the whole application deploys as one artifact runnable with the Java 21 runtime.

#### Scenario: Standalone jar boots
- **WHEN** the packaged backend jar is started with the Java 21 runtime and no external services beyond its database
- **THEN** the application boots and serves the application shell
