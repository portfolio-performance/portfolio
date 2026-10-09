# What prior art exists for programmatic access to client files?

Type: research
Status: resolved

## Question

What existing third-party tools, libraries, CLIs, MCP servers or scripts read or write Portfolio Performance client files (GitHub, PP forum, PyPI/npm/Maven), and what did the maintainer say about headless/CLI/API use upstream (issues, forum threads)? For each: language, read/write coverage, calculated-figure support, maintenance status, and how it copes with format versions.

## Comments

Research findings: branch `research/prior-art`, file `.scratch/ai-cli/research/prior-art.md`.

## Answer

- About 25 third-party tools read client files; only quovibe (TypeScript re-implementation) and vgritsel12/portfolio-performance-automation (pinned PP Java engine) produce TTWROR/IRR.
- XML side is built on pfalcon/ppxml2db (XML↔SQLite, needs "XML with id attributes"); pp-terminal (CLI + MCP, PyPI) is the most active tool on top of it.
- Binary side: several tools copy `client.proto` and handle ZIP/`PPPBV1`/AES themselves; magnum/ppapi even writes encrypted files.
- Writers are rare and fragile; none applies PP's input validation.
- At least four read-only MCP servers exist (pp-terminal, Nokke32/pp-mcp, dds-org/pp-mcp, pyfolio-performance-mcp); none returns PP-computed performance.
- Format handling is ad hoc; reverse-engineered scaling, tested versions lag (56 vs current 70).
- Maintainer: open to a CLI in 2016 but never built one. His own draft CLI PR #5550 (March 2026) he calls "of limited use", and he rejects a CLI that writes "bypassing the input validation".
- Since July 2026 he is building an **in-app localhost REST API (PR #5870, open)** with IRR/TTWROR and published agent skills, planned as experimental. He called a read-only CLI over the same API "a reasonable starting point".
- Gap: a read-only CLI running PP's own engine on a closed file without a GUI, sharing #5870's vocabulary and response shapes.
