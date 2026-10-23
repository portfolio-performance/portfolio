# 35: The `pp-cli` agent skill and its drift guard

Type: task
Status: ready-for-agent
Blocked by: 34

Spec: [spec.md](../spec.md), user stories 110–116, "Agent skill" under "Implementation Decisions". ADR 0003. Vocabulary: [GLOSSARY.md](../../../GLOSSARY.md).

## What to build

A single agent skill, `pp-cli`, that teaches Copilot and Claude Code to use the CLI safely against a client file. The source lives in `name.abuchen.portfolio.cli/skills/pp-cli/`, in the Agent Skills format: `SKILL.md` plus one reference file for the write protocol that the agent loads only before running `apply`. Bundling and installing it is ticket 36.

The skill holds workflow, judgment and safety rules. It does not copy the command reference: it tells the agent to run `pp-cli describe` for commands, options, operations, response schemas and warning codes.

A test in `name.abuchen.portfolio.cli.tests` keeps the skill honest against `describe`.

## Decisions

- **One skill, named `pp-cli`** (the folder name equals the `name` field). No separate read, write or setup skills.
- **Description fires** for questions about a Portfolio Performance client file (holdings, performance, allocation, prices, transactions), for edits to its instruments, accounts or transaction records, and for `pp-cli` itself. It states that it does not apply to general investing advice or PDF import.
- **Write protocol** (in the reference file, no exceptions): `--dry-run` first, then a plain-language summary of the entity-level changes, then the real `apply` only after the user explicitly approves that batch, then a note about the backup path and `pp-cli restore`. The agent never promises what `describe` lists as limits (unsaved edits in a running app can't be detected, no PDF import, no price writes).
- **Passwords**: never asked for in chat and never on a command line. If a file needs one, the agent tells the user to set `PP_PASSWORD` and shows both the `$env:PP_PASSWORD` and `export PP_PASSWORD` forms. It stops on `password-required` and `incorrect-password` and reports them.
- **File selection**: an explicit `--file` on every command, taken from the user or from `PP_FILE` if the user set it. The agent never scans the disk for client files and asks when the path is unknown.
- **Session start**: `pp-cli info` once. A `file-version-unsupported` refusal or a core-tag mismatch is reported to the user, not worked around. If `pp-cli` is not on `PATH`, the agent points to `name.abuchen.portfolio.cli/packaging/README.md` and neither builds nor installs it.
- **Answers**: figures come from the CLI, never from the agent's own valuation, IRR or TTWROR; the period, currency and cost method echoed in `meta` are stated; every `warnings` entry (`stale-price`, `exchange-rate-missing`, ...) is passed on; the AI/CLI vocabulary from the glossary is used (Instrument, Investment account, Cash account, Transaction record).
- **Shell neutrality** (PowerShell, cmd, Git Bash, zsh): the `apply` batch is written with the agent's file tool to a UTF-8 temp file without a BOM and passed with `--input <path>`, never piped on stdin or quoted inline; paths are quoted because they may contain spaces or backslashes; the tool is always called as plain `pp-cli`.
- **Line endings**: a `.gitattributes` next to the skill forces LF, so the installed copies are identical on both platforms.
- **Troubleshooting** covers only error codes that `describe` publishes, each with the action the agent should take.
- **No glossary change and no new ADR**: a skill is not a domain term, and the shape is easy to change.

## Drift guard

A test in `name.abuchen.portfolio.cli.tests`, run through the in-process entry point, that:

- reads `SKILL.md` and the reference file from the source folder;
- checks that the frontmatter `name` equals the folder name and the `description` is non-empty;
- checks that every `pp-cli <command>`, `--option`, operation name and error code mentioned in the skill exists in the `describe` output;
- checks that no password option exists in `describe` (the skill relies on that).

## Acceptance criteria

- [ ] `name.abuchen.portfolio.cli/skills/pp-cli/` holds `SKILL.md`, the write-protocol reference file and a `.gitattributes` forcing LF
- [ ] The skill contains every rule listed under Decisions and no copy of the command reference
- [ ] The drift test passes, and fails when a command, option, operation or error code is renamed or removed in the skill's text (shown by a temporary edit)
- [ ] An agent given only the skill, a sample client file and the installed `pp-cli` completes a read question and a dry-run write, and stops for approval before the real `apply` (checked once by hand in Copilot and in Claude Code)
