# Where does the CLI live and what code does it build on?

Type: grilling
Status: open
Blocked by: 01, 02

## Question

Should the CLI be (a) a new module/bundle inside this repo, (b) a separate project depending on PP core artifacts, (c) a separate project that reads the client file independently (and in which language), or (d) a CLI aligned with the maintainer's in-app REST API (PR #5870), sharing its resource names and response shapes, possibly contributed upstream as the "read-only CLI" he suggested? Weigh calculated-figure reuse, file-version tracking, upstream acceptance, the maintainer's stance against writes that bypass input validation, and build/packaging cost.

Inputs: [Can PP core load, calculate and save a client file outside the workbench?](01-headless-core-feasibility.md), [What prior art exists for programmatic access to client files?](02-prior-art.md).
