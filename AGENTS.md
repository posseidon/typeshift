## Token Optimization & Context Retrieval Protocol

To minimize token usage and avoid reading unnecessary source files into the context window, follow this workflow when exploring or analyzing the codebase:

### 1. Structural Navigation (Codegraph)
Before opening or listing whole directories, query `@codegraph` to retrieve class hierarchies, call graphs, and dependency maps:
* **Index workspace:** `codegraph index .`
* **Query caller/callee graphs:** `codegraph query callers <SymbolName>`
* **Find implementations:** `codegraph query implementations <InterfaceName>`

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

