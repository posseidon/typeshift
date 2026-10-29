# Spec Delta

## Purpose

Provide the server-rendered application shell (Thymeleaf + HTMX) that all TypeShift views — dashboard, stage select, typing area — plug into, with UTF-8 diacritic support and placeholders reserved for the client-side typing engine.

## ADDED Requirements

### Requirement: Shared layout shell with content injection point
The application SHALL render views through a shared layout template that defines a content injection point, so individual views provide only their content fragment while the shell owns the document skeleton.

#### Scenario: View renders inside the shell
- **WHEN** the index view is requested
- **THEN** the rendered document is composed of the shared layout plus the view's content fragment

### Requirement: HTMX library loaded from CDN in the shell
The layout shell SHALL include the HTMX library in the document head, so swapping server-rendered fragments is available to all views from first render.

#### Scenario: Shell references HTMX
- **WHEN** any view is rendered through the layout
- **THEN** the HTMX library is loaded from its CDN in the document head

### Requirement: Index entry view reachable
The application SHALL serve an index view at the site root that renders without error as the entry point into the shell.

#### Scenario: Root request renders index
- **WHEN** an HTTP GET is made to the site root
- **THEN** the server responds successfully with the index view rendered through the layout

### Requirement: Typing engine asset placeholders present
The application SHALL reserve static asset locations for the client-side typing engine — a stylesheet and a JavaScript engine placeholder — loaded by the shell so no per-view wiring is needed later.

#### Scenario: Shell loads engine assets
- **WHEN** any view is rendered through the layout
- **THEN** the engine stylesheet and engine script placeholders are referenced from the document
