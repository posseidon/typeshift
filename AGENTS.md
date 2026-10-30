## Agent Skill: Ponytail (Lazy Senior Dev)
Before writing any code, you must climb the decision ladder. Choose the lowest possible rung to solve the user's problem:
1. **Do Nothing:** Can the requirement be met by changing a configuration or leveraging existing system behavior?
2. **Reuse Existing Code:** Can an existing utility or service in this codebase solve the problem? (You must search the codebase first).
3. **Use Standard Library/Native API:** Can standard framework features handle this without new dependencies?
4. **Modify Existing Code:** Can we slightly extend an existing function to cover this use case?
5. **Use Existing Dependency:** Can a package already in our build files solve this?
6. **Add New Dependency:** Only for complex, domain-specific problems using industry standards.
7. **Write Custom Code (Last Resort):** Keep it minimal. Never skip strict validation, error handling, or static analysis compliance.

## Project Structure (Domain-Driven Design)

Organize code by **domain**, never by technical layer. Every feature lives in
its own package that carries the layers it needs (`controller`, `model`,
`repository`, `service`) instead of spreading across the top-level
`controller/`, `service/`, `repository/`, and `model/` packages. A flat,
layer-first layout is tolerable for a few controllers and unreadable as the
application grows.

Target layout (package root `hu.elte.ntb.typeshift`):

```
hu.elte.ntb.typeshift
├── account               # shared account/user domain
│   ├── model             # UserAccount
│   ├── repository        # UserAccountRepository
│   └── service           # UserAccountService
├── config                # application + security config (AuthProperties, ...)
├── dashboard
│   └── controller        # IndexController
├── health
│   ├── controller        # HealthController
│   └── model             # HealthSnapshot
├── learning              # learning-profile domain
│   ├── model             # LearningProfile
│   ├── repository        # LearningProfileRepository
│   └── service           # LearningProfileService
├── login
│   ├── controller        # LoginController
│   ├── model             # LoginResult
│   └── service           # LoginService, LoginAttemptService
├── profile
│   ├── controller        # ProfileController
│   └── model             # ProfileView, ChangePasswordOutcome
├── security              # cross-cutting, flat package: AuthCookie,
│                         #   JwtAuthenticationFilter, JwtTokenService,
│                         #   RevocationService, RevokedToken,
│                         #   RevokedTokenRepository
├── signup
│   ├── controller        # SignupController
│   └── model             # SignupOutcome
└── shared                # types truly shared by 2+ domains (last resort)
```

### Rules
- **One domain, one package.** A domain is a cohesive slice of behavior (login,
  signup, profile, learning, account); all its layers live together under it.
- **New feature → new domain package** with only the layers it needs. Never
  pre-create empty `repository`/`service` sub-packages.
- **Cross-cutting concerns** (auth cookie, JWT, token revocation, Spring
  configuration) stay in `security` or `config`, never inside a domain.
- **`shared` is not a dumping ground.** Promote a type there only when two or
  more domains genuinely need it.
- **Dependencies point inward:** domains may depend on `shared`, `config`, and
  `security`; one domain reaches another only through that domain's public
  `service`.

The tree above is the current layout; every new class goes in its domain
package from the start.

## Code Analysis & Navigation (CodeGraph)
This workspace is integrated with CodeGraph via the `codegraph` MCP server (`codegraph serve --mcp`), which exposes one Read-equivalent tool: `codegraph_explore`. Query it with a natural-language question or symbol/file names to get verbatim source plus call paths and blast radius in one call.
When planning architectural changes, implementing Spring Boot services, or refactoring:
* **Dependency Mapping:** Call `codegraph_explore` with the symbols or files you're about to change to map call chains, imports, and dependencies before modifying core logic.
* **Security & Quality:** Use `codegraph_explore` to surface CPG-derived relationships and validate alignment with our strict static analysis quality gates (PMD, SpotBugs, Checkstyle).
* **Validation:** Re-run `codegraph_explore` on the touched symbols to link modifications back to the required evidence.

## Token Optimization & Context Retrieval Protocol

To minimize token usage and avoid reading unnecessary source files into the context window, follow this workflow when exploring or analyzing the codebase:

### 1. Structural Navigation (Codegraph)
Before opening or listing whole directories, call `codegraph_explore` (or the `codegraph` CLI) to retrieve symbol source, call graphs, and dependency maps:
* **Explore an area / read a symbol:** `codegraph_explore` with a question or names (CLI: `codegraph explore <query>`)
* **Index workspace:** `codegraph index .`
* **Query callers:** `codegraph callers <SymbolName>`
* **Query callees:** `codegraph callees <SymbolName>`
* **Find a symbol / implementations:** `codegraph query <SymbolName>`

### 2. Semantic Symbol Search (Serena)
Use Serena to locate precise method signatures and target code definitions without loading full file bodies:
* **Locate symbol definition:** `serena find <symbol_or_function_name>`
* **Search definitions by pattern:** `serena search --type method "<pattern>"`

### 3. Context Compression & Packing (Context7)
When passing source files or documentation to the model context, preprocess them with `context7` to strip non-essential whitespace, redundant comments, and boilerplate:
* **Pack compressed context for a directory:** `context7 pack src/ -o .context-summary.md`
* **Filter files for specific intent:** `context7 filter --focus "authentication flow" src/`

### Primary Rule for Agents:
> **Never read entire source files blindly (`cat`, `read_file`).** 
> 1. Use `serena` or `codegraph` to pin down precise line ranges/symbols.
> 2. Read only the specific targeted symbol or use `context7` output to construct the minimal context required for the task.

