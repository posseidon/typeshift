# Single-Project Maven Restructure — Design

**Intent:** Collapse the Maven two-POM setup (aggregator `typeshift-parent` + module `typeshift-backend`) into one single Spring Boot project at the repo root, so packaging and deploying the jar to Azure Web App is: `mvn package` → `java -jar typeshift-*.jar`.

## Why
The aggregator POM buys nothing here: there is exactly one real module, and the original skeleton's `typeshift-frontend` module was already dropped. Two POMs and a subdirectory only complicate every build/run command (`mvn -pl typeshift-backend ...`) and any deploy tooling that expects the jar at a predictable path.

## Goals / Non-goals
- Goal: one root `pom.xml`, sources under root `src/`, no Maven parent of our own.
- Goal: build, tests, and all quality gates (Checkstyle, PMD, SpotBugs) behave exactly as before.
- Non-goal: touching any Java code, resources, application configuration, or package layout (`hu.elte.ntb.typeshift` domains stay).
- Non-goal: updating historical specs/plans under `openspec/changes/archive` and `docs/superpowers/plans` — they are records of what was true at the time.

## Approaches considered
1. **Flatten at the root (chosen):** merge both POMs into one root POM, `git mv typeshift-backend/src → src`, delete the module dir. Simplest possible; all commands become root-level `mvn ...`.
2. **Keep module dir, drop only the parent POM:** would leave a POM inside `typeshift-backend/` — still two POMs, commands unchanged. Rejected: doesn't simplify anything.
3. **Rename everything to `typeshift-parent`-style coords:** pure churn, no benefit. Rejected.

Decisions you made: artifact is named **`typeshift`** (jar: `typeshift-1.0.0-SNAPSHOT.jar`).

## What the builder does
- Root `pom.xml`: re-parent to `spring-boot-starter-parent` (3.5.16), artifactId `typeshift`, version `1.0.0-SNAPSHOT`. Carry over from the old parent POM: `properties`, `spring-ai-bom` dependencyManagement, and the Checkstyle/PMD/SpotBugs build plugin configs. Carry over from the old module POM: all `dependencies`, `lombok` optional, and `spring-boot-maven-plugin`. Remove `<modules>`.
- `git mv typeshift-backend/src src` (keeps history; uncommitted working-tree edits move along).
- Delete `typeshift-backend/` entirely (its POM, stale IDE files: `.classpath`, `.factorypath`, `.project`, `.settings`, `.iml`, `target`), and the stale root `target/`. `lombok.config` already sits at the root and stays.
- Update `docs/database.md`: run commands become plain `mvn spring-boot:run ...` (drop `-pl typeshift-backend`).

## Verification
- `mvn verify` from the root: builds, all tests pass, quality gates run exactly once (not double-executed via reactor aggregation).
- Fat jar lands at `target/typeshift-1.0.0-SNAPSHOT.jar` and `java -jar` boots the app.
- `grep` confirms no live files reference `-pl typeshift-backend` or `typeshift-parent` (archives excepted).
