# api-contract Specification

## Purpose

Publish the backend's REST services as a machine-generated OpenAPI 3 contract with a browsable Swagger UI, giving automated test coverage and clients a stable, always-in-sync source of truth.

## Requirements

### Requirement: REST services generate a machine-readable OpenAPI 3 spec
The backend REST services SHALL be documented by an OpenAPI 3 specification generated live from the controllers, exposed at a stable, machine-readable endpoint.

#### Scenario: Spec is fetchable and valid
- **WHEN** the application is running
- **THEN** a machine-readable OpenAPI 3 document is served at the documented endpoint and parses as valid OpenAPI 3

### Requirement: OpenAPI spec stays in sync with controllers
The specification SHALL be generated from the running controllers rather than authored separately, so a documented endpoint and its serving controller cannot drift apart.

#### Scenario: Documented endpoint matches implementation
- **WHEN** an endpoint is annotated for OpenAPI documentation
- **THEN** the generated specification reflects that endpoint's path and method without a separate spec file being edited

### Requirement: Browsable Swagger UI
The application SHALL expose a browsable Swagger UI rendering the generated OpenAPI 3 document, so the contract can be inspected and exercised by a human or test tool.

#### Scenario: Swagger UI renders the contract
- **WHEN** a request is made to the Swagger UI path
- **THEN** the UI loads and displays the documented endpoints from the generated specification

### Requirement: Contract anchors automated test coverage
The OpenAPI document SHALL contain at least one documented REST service endpoint so automated test tooling has a concrete contract to consume from the first build onward.

#### Scenario: Tooling can consume a documented endpoint
- **WHEN** an automated consumer reads the OpenAPI document
- **THEN** it discovers at least one documented service endpoint with its path, method, and response
