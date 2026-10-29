# Spec Delta

## Purpose

Enforce the OpenSpec constitution's quality standards by making the build fail when static analysis rules are violated, so no substandard code can enter the codebase unnoticed.

## ADDED Requirements

### Requirement: Maven build resolves and produces packages for all modules
The Maven multi-module build (parent `hu.elte.ntb.typeshift` with modules `typeshift-backend` and `typeshift-frontend`, Java 21) SHALL resolve all declared dependencies and produce an artifact for every module during the standard build lifecycle.

#### Scenario: Clean build succeeds
- **WHEN** the build is run from a clean checkout
- **THEN** dependency resolution and packaging complete without dependency errors or serialization warnings for both modules

### Requirement: Quality gates defined once and enforced in every module
The analysis gates SHALL be configured once in the parent build and SHALL be enforced for each module during its `verify` phase, so no module can bypass them.

#### Scenario: Violation in either module stops the build
- **WHEN** source code in either the backend or the frontend module contains an analysis violation
- **THEN** running `verify` from the parent fails at that module with a report identifying the violation and its location

### Requirement: Checkstyle gate fails the build on violations
The build SHALL run Checkstyle during the `verify` phase against a standard Java style ruleset and SHALL fail the build when any violation is reported.

#### Scenario: Style violation stops the build
- **WHEN** source code contains a Checkstyle violation
- **THEN** the `verify` phase fails with a report identifying the violation and its location

### Requirement: PMD gate fails the build on violations
The build SHALL run PMD during the `verify` phase and SHALL fail the build when any priority-5 violation is reported.

#### Scenario: PMD violation stops the build
- **WHEN** source code triggers a maximum-priority PMD violation
- **THEN** the `verify` phase fails with the violation reported

### Requirement: SpotBugs gate fails the build on findings
The build SHALL run SpotBugs during the `verify` phase at maximum effort with a low threshold and SHALL fail the build when any finding matches the threshold.

#### Scenario: SpotBugs finding stops the build
- **WHEN** compiled bytecode yields a SpotBugs finding at or above the configured threshold
- **THEN** the `verify` phase fails with the finding reported
