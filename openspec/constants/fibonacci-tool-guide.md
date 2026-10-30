# Tool selection and complexity guide

## Tools
- superpowers:brainstorming: exploratory. Use only to discover what stories belong in an epic.
- superpowers:writing-plans: sequences steps. Use before executing when the step order is unclear.
- superpowers:executing-plans: produces artefacts from a clear plan. Default for shape-known work.
- opsx:plan + opsx:apply: precise specs. Use when a story has security, algorithmic logic, multiple failure paths, external integrations, or non-obvious acceptance criteria.

## Complexity scale (Fibonacci)
1 trivial, 2 simple, 3 straightforward, 5 moderate, 8 complex, 11 very complex.
Any story estimated above 11 must be split before writing.

## Efficiency note
opsx produces the highest-quality output at the highest token cost. Use it on the roughly one third of stories that carry real risk. Everything else uses executing-plans, which costs less and is sufficient when the ADRs already define the shape.
