# application-runtime Specification

## Purpose

Define the runtime configuration of the application: a Spring Boot 3 application running on Java 21 with virtual threads and an SQLite-backed persistence layer, booting successfully with UTF-8-safe language support.

## Requirements

### Requirement: Application boots on Java 21 with virtual threads enabled
The application SHALL run as a Spring Boot 3 application on Java 21 with virtual threading enabled so request handling uses virtual threads.

#### Scenario: Application starts
- **WHEN** the application is launched
- **THEN** the web server starts successfully and accepts HTTP requests

### Requirement: Persistence wired to SQLite
The application SHALL be wired to an SQLite database with the SQLite Hibernate dialect enabled, and the schema SHALL be kept up to date at startup for the MVP.

#### Scenario: Persistence layer initializes
- **WHEN** the application starts with the SQLite datasource configured
- **THEN** the persistence layer initializes against the SQLite database and schema management runs at startup

### Requirement: UTF-8 language support across layers
The application SHALL support UTF-8 encoding across rendering and persistence layers so Hungarian and Vietnamese diacritics render correctly.

#### Scenario: Diacritics render correctly
- **WHEN** a rendered view contains Hungarian or Vietnamese diacritic characters
- **THEN** the characters display correctly without mojibake

### Requirement: Deployable as a single jar
The build SHALL package the frontend module's templates and assets into the backend's runnable jar so the whole application deploys as one artifact runnable with the Java 21 runtime (Azure App Service web app, free tier).

#### Scenario: Standalone jar boots
- **WHEN** the packaged backend jar is started with the Java 21 runtime and no external services
- **THEN** the application boots, serves the shell view, and does not depend on any service outside its own persistence
