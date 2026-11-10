# Single-Project Maven Restructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Collapse the two-POM Maven setup into one single Spring Boot project at the repo root.

**Architecture:** Root `pom.xml` becomes the only POM: re-parented directly to `spring-boot-starter-parent`, carrying over the old parent's build-tooling configs and the old backend module's dependencies. Sources move from `typeshift-backend/src/` to root `src/`; the module directory is deleted.

**Tech Stack:** Maven, Spring Boot 3.5.16, Java 21, Checkstyle/PMD/SpotBugs.

**Spec:** `docs/superpowers/specs/2026-11-03-single-project-maven-design.md`

## Global Constraints

- Spring Boot `spring-boot-starter-parent` version `3.5.16` (exact, from current POM).
- Java `21`, `spring-ai.version 1.1.8`, `springdoc-openapi.version 2.8.17` (exact, from current POM).
- No Java code, resources, templates, or `application*.yml` are modified.
- Do **not** commit: the working tree carries the user's uncommitted in-flight work; the restructure stays uncommitted for them to review and commit together.

## Review Focus

- Quality gates must run exactly once at the root, not once per reactor module. Check: `mvn verify` log shows each of Checkstyle/PMD/SpotBugs once.
- Deleted IDE files (`.classpath`, `.factorypath`, `.project`, `.settings`, `*.iml`) must not survive in `target/` or git. Check: `git status` shows them deleted; `find typeshift-backend` finds nothing.
- Hidden references in live (non-archive) files to `-pl typeshift-backend` or `typeshift-backend/`. Check: grep excluding `openspec/changes/archive` and `docs/superpowers/plans` returns nothing.

---

### Task 1: Merge both POMs into root `pom.xml`

**Files:**
- Modify: `pom.xml` (replace entirely)
- Delete: `typeshift-backend/pom.xml` (in Task 2)

**Step 1: Write the single root POM**

Replace `pom.xml` with one POM combining the two current ones:
- `<parent>` = `org.springframework.boot:spring-boot-starter-parent:3.5.16`.
- `groupId hu.elte.ntb.typeshift`, `artifactId typeshift`, `version 1.0.0-SNAPSHOT`, packaging `jar`.
- Keep all `<properties>` from the old parent POM verbatim.
- Keep the `spring-ai-bom` `<dependencyManagement>` block.
- `<dependencies>` from `typeshift-backend/pom.xml` verbatim.
- `<build><plugins>` = old parent's three quality-gate plugins (Checkstyle, PMD, SpotBugs, with their `<executions>`) plus `spring-boot-maven-plugin`.
- No `<modules>`.

**Step 2: Verify POM reads and artifact coordinates resolve**

Run: `mvn -q validate`
Expected: no error (source still in old locations; that's fine until Task 2).

### Task 2: Move sources to root, delete module dir and stale targets

**Step 1: Move sources**

Run: `git mv typeshift-backend/src src`

**Step 2: Delete the module directory and stale build outputs**

Run: `git rm -q typeshift-backend/pom.xml` then remove leftovers: `rm -rf typeshift-backend target` (module dir leftovers are all untracked IDE files/stale builds: `.classpath`, `.factorypath`, `.project`, `.settings`, `*.iml`, `target/`; root `target/` is a stale build from before the module split).

Step 3: Verify

Run: `find typeshift-backend 2>/dev/null; ls`
Expected: no `typeshift-backend`, no root `target`; root has `pom.xml`, `src/`, `lombok.config`, docs/openspec dirs.

### Task 3: Update live docs

**Files:**
- Modify: `docs/database.md` (only the two `mvn -pl typeshift-backend ...` command lines)

**Step 1: Update commands**
Replace `mvn -pl typeshift-backend spring-boot:run` usages with plain `mvn spring-boot:run` (preserving the `profiles=` args).

**Step 2: Verify no live references remain**
Run: `grep -rn "typeshift-backend\|typeshift-parent" --exclude-dir=archive --exclude-dir=target . | grep -v superpowers/plans`
Expected: only matches in archived/plan history, plus the root POM name field if any (must be none — root POM is `typeshift`).

### Task 4: Verify build, tests, gates, and fat jar

**Step 1: Full build with quality gates and tests**

Run: `mvn verify`
Expected: BUILD SUCCESS; tests pass; Checkstyle/PMD/SpotBugs each execute once; jar at `target/typeshift-1.0.0-SNAPSHOT.jar`.

**Step 2: Boot the fat jar**

Run: `mvn -q package -DskipTests && java -jar target/typeshift-1.0.0-SNAPSHOT.jar & …` (start, then curl the health/status lamp and stop within seconds)
Expected: app boots with no external services, serves the shell.
