# application-runtime Specification

## Purpose

Define the single-runnable-jar runtime: Spring Boot 3 on Java 21 with virtual threads, PostgreSQL persistence owned by Flyway, separate development and production profiles, Azure OpenAI generation, and UTF-8-safe rendering.

## Requirements

### Requirement: Application boots on Java 21 with virtual threads enabled
The application SHALL run as a Spring Boot 3 application on Java 21 with virtual threading enabled.

#### Scenario: Application starts
- **WHEN** the application is launched
- **THEN** the web server starts successfully and accepts HTTP requests

### Requirement: PostgreSQL persistence owned by Flyway
The application SHALL use PostgreSQL with `ddl-auto` set to `validate`, so Flyway owns the schema. Migrations SHALL use standard PostgreSQL SQL, run on startup in both profiles, and a failed migration SHALL stop startup with a clear error.

#### Scenario: Persistence layer initializes
- **WHEN** the application starts with the PostgreSQL datasource configured
- **THEN** Flyway migrates the schema and Hibernate validates against it

#### Scenario: Failed migration stops startup
- **WHEN** a migration fails to apply
- **THEN** application startup aborts with a clear error

### Requirement: Flyway baseline
Flyway SHALL create and manage its schema history table, with V1 as an empty baseline marker and application schema introduced by subsequent migrations.

#### Scenario: Baseline migration applies cleanly
- **WHEN** Flyway runs against a fresh database
- **THEN** the history table is created and V1 applies as an empty marker

### Requirement: Development and production profiles
The development profile SHALL use Docker Compose PostgreSQL, and the production profile SHALL use Supabase PostgreSQL. Endpoints and credentials SHALL be environment-configured, the active profile SHALL appear in the startup log, and the profiles SHALL never share a database.

#### Scenario: Active profile is logged
- **WHEN** the application starts
- **THEN** the startup log names the active profile

#### Scenario: Development and production use distinct databases
- **WHEN** either profile is active
- **THEN** it resolves its own datasource, with no shared database

### Requirement: Docker Compose PostgreSQL for development
The Compose configuration SHALL define one PostgreSQL service with a named volume, credentials from a git-ignored environment file, a localhost-only exposed port, and a health check, with documented start and stop commands.

#### Scenario: Database starts with persistent data
- **WHEN** the Compose database is started and later restarted
- **THEN** it becomes healthy and its data survives across restarts

### Requirement: Supabase production connection
The production connection string SHALL come from an environment variable, use PostgreSQL-compatible features, keep the connection pool small for the free tier, and fail fast with a clear message if the connection cannot be made.

#### Scenario: Missing production connection fails fast
- **WHEN** the production profile starts without a database URL
- **THEN** startup fails with a clear configuration error

### Requirement: Azure OpenAI generation
Every profile SHALL use the Azure OpenAI-specific content-generation service directly. Endpoint, API key, and deployment name SHALL be configurable through `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, and `AZURE_OPENAI_DEPLOYMENT`. Generation SHALL use the Azure OpenAI-compatible `/openai/v1/chat/completions` endpoint; the default deployment SHALL be `Phi-4-mini-instruct`.

### Requirement: AI error handling
Provider transport, serialization, and response errors SHALL be wrapped in `AiProviderException` with the original cause preserved.

#### Scenario: Provider failure is wrapped
- **WHEN** an Azure generation request fails
- **THEN** the failure is raised as `AiProviderException` with its cause attached

### Requirement: Azure connectivity check
The development-only `/api/admin/provider-connectivity` endpoint SHALL probe the configured Azure OpenAI-compatible `/openai/v1/models` endpoint and report provider reachability. It SHALL not be exposed in the public UI or production.

#### Scenario: Development-only connectivity check
- **WHEN** the development connectivity route is called
- **THEN** it reports Azure reachability and is unavailable in production

### Requirement: UTF-8 language support
The application SHALL preserve UTF-8 across rendering and persistence so Hungarian diacritics display correctly.

#### Scenario: Diacritics render correctly
- **WHEN** a rendered view contains Hungarian diacritic characters
- **THEN** the characters display correctly without mojibake

### Requirement: Deployable as a single jar
The build SHALL package frontend templates and assets into the backend runnable jar so the application deploys as one artifact runnable with Java 21.

#### Scenario: Standalone jar boots
- **WHEN** the packaged backend jar starts with the Java 21 runtime and its database
- **THEN** the application boots and serves the application shell
