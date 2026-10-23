---
name: pp-cli
description: Read and edit a Portfolio Performance client file with the pp-cli tool. Use when the user asks about holdings, performance, allocation, prices or transactions in their client file, asks to add or change its instruments, accounts or transaction records, or mentions pp-cli. Not for general investing advice or PDF statement import.
---

# pp-cli

`pp-cli` reads and writes a Portfolio Performance client file. It is the single source of truth for two things: the figures, and its own interface. `pp-cli describe` lists every command, option, write operation with its input schema, response schema, warning code, error code and known limit. Look things up there; this skill holds only the workflow and the safety rules.

## Session start

1. Run `pp-cli describe` and keep its output as your reference for the session. If the shell can't find `pp-cli`, tell the user that it is built and installed by following `name.abuchen.portfolio.cli/packaging/README.md` and stop; building and installing it is the user's step, not yours.
2. Settle the client file: the path the user gave, or the `PP_FILE` environment variable if the user set it. Otherwise ask for the path; the user names the file, and you leave the disk unsearched. Pass it as `--file "<path>"` on every command, so each command names its file.
3. Run `pp-cli info --file "<path>"` once. Mention its `pinnedUpstreamTag`: the CLI's core must match the user's desktop release, and a mismatch is reported to the user as it is. A `file-version-unsupported` refusal means the CLI is older than the file: report `fileVersion` and `maxSupportedVersion` and stop until the user updates the CLI.

## Passwords

An encrypted client file gets its password from the `PP_PASSWORD` environment variable, which the user sets in the environment your shell inherits:

- PowerShell: `$env:PP_PASSWORD = '<password>'`
- bash or zsh: `export PP_PASSWORD='<password>'`

On `password-required`, show the user both forms and wait. On `incorrect-password`, report it and wait for the user to correct the variable. The password stays out of the conversation and out of every command line: never ask for it in chat, type it into a command, or pipe it to `pp-cli`.

## Answers

- **Figures** come from `pp-cli` responses. Quote them; valuations, IRR and TTWROR are the tool's to compute. If no command returns a figure, say so.
- **Basis**: state what `meta` echoes for the figure: the period (`from`, `to`), the valuation `date`, the `currency` and the `costMethod`.
- **Warnings**: pass on every entry of `warnings` (such as `stale-price` or `exchange-rate-missing`) with what it means for the answer.
- **Limits**: anything under `limits` in `pp-cli describe` is out of reach. Say so plainly and offer what the tool can do.
- **Vocabulary**: Instrument (Security in the app), Investment account (Portfolio in the app), Cash account (Account in the app), Transaction record (one business event with all its sides, such as a buy with its investment-account and cash-account side).

## Shell

The same commands run in PowerShell, cmd, Git Bash and zsh when you:

- call the tool as plain `pp-cli`, with no path or extension;
- wrap every path in double quotes, since paths may contain spaces or backslashes;
- write any JSON for the tool to a file with your file tool and pass the file's path. Stdin pipes and inline JSON break differently in each shell, so `--input` always gets a file path.

## Changes

Before any `pp-cli apply` or `pp-cli restore`, read [write-protocol.md](write-protocol.md) and follow it step by step.

## Troubleshooting

A failure is a problem+json document on stdout with a `code` and a non-zero exit code. Its `detail` and the `errors` list of `pp-cli describe` explain it.

| Code | Action |
|---|---|
| `file-required`, `file-not-found` | Ask the user for the client file's path. |
| `file-version-unsupported` | Report it as in Session start and stop. |
| `password-required`, `incorrect-password` | Follow Passwords. |
| `unknown-command`, `unknown-option`, `invalid-option-value`, `option-required`, `conflicting-options` | Correct the call from `pp-cli describe`. |
| `reference-ambiguous` | Show the user the candidates and ask which one is meant. |
| `reference-not-found` | List the entities of that kind (such as `pp-cli instruments --file "<path>"`) and confirm the right one with the user. |
| `validation-failed` | Explain each entry of `failures`, fix the batch with the user, and restart the write protocol. |
| `consistency-check-failed` | Report the inconsistencies; nothing was written. |
| `file-open-in-app` | Ask the user to close the file in Portfolio Performance, then retry. `--force` is the user's call alone. |
| `file-locked` | Report the lock file from `detail`; the user decides whether another `pp-cli` is still running. |
| `file-changed` | Read the file again and restart the write protocol from the dry run. |
| `write-failed`, `internal-error` | Report `detail` to the user as it is. |
| any other code | Report `detail` and the code's entry under `errors` in `pp-cli describe`, then wait for the user. |
