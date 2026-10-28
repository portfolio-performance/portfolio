---
name: pp-cli
description: Read and edit a Portfolio Performance client file with the pp-cli tool. Use when the user asks about holdings, performance, allocation, prices or transactions in their client file, asks to add or change its instruments, accounts or transaction records, or mentions pp-cli. Not for general investing advice or PDF statement import.
---

# pp-cli

`pp-cli` reads and writes a Portfolio Performance client file. It is the single source of truth for two things: the figures, and its own interface. `pp-cli --help` lists the commands, global options, error codes and known limits; `pp-cli <command> --help` gives one command's options, response fields, warning codes and error codes; `pp-cli apply --help <operation>` gives one write operation's input schema and failure codes. Look things up there; this skill holds only the workflow and the safety rules.

## Session start

1. Run `pp-cli --help` and read it as your reference for the session; ask for a command's own help before you first use it. If the shell can't find `pp-cli`, tell the user that it is built and installed by following `name.abuchen.portfolio.cli/packaging/README.md` and stop; building and installing it is the user's step, not yours.
2. Settle the client file: the path the user gave, or the `PP_FILE` environment variable if the user set it. Otherwise ask for the path; the user names the file, and you leave the disk unsearched. Pass it as `--file "<path>"` on every command, so each command names its file.
3. Run `pp-cli info --file "<path>"` once. Mention its `pinnedUpstreamTag`: the CLI's core must match the user's desktop release, and a mismatch is reported to the user as it is. A `file-version-unsupported` refusal means the CLI is older than the file: report `fileVersion` and `maxSupportedVersion` and stop until the user updates the CLI.

## Passwords

An encrypted client file gets its password from the `PP_PASSWORD` environment variable, which the user sets in the environment your shell inherits:

- PowerShell: `$env:PP_PASSWORD = '<password>'`
- bash or zsh: `export PP_PASSWORD='<password>'`

On `password-required`, show the user both forms and wait. On `incorrect-password`, report it and wait for the user to correct the variable. The password stays out of the conversation and out of every command line: never ask for it in chat, type it into a command, or pipe it to `pp-cli`.

## Answers

- **Figures** come from `pp-cli` responses. Quote them; valuations, IRR and TTWROR are the tool's to compute. If no command returns a figure, say so. A figure you derive from responses (a sum, a split, a share) is yours: compute it with a tool, not in your head, and label it as derived.
- **Basis**: state what `meta` echoes for the figure: the period (`from`, `to`), the valuation `date`, the `currency` and the `costMethod`.
- **Warnings**: pass on every entry of `warnings` (such as `stale-price` or `exchange-rate-missing`) with what it means for the answer.
- **Limits**: anything under Limits in `pp-cli --help` is out of reach. Say so plainly and offer what the tool can do.
- **Vocabulary**: Instrument (Security in the app), Investment account (Portfolio in the app), Cash account (Account in the app), Transaction record (one business event with all its sides, such as a buy with its investment-account and cash-account side).

For an overview of the file or a request for insights, read [insights.md](insights.md) and follow its call sequence and traps.

## Shell

The same commands run in PowerShell, cmd, Git Bash and zsh when you:

- call the tool as plain `pp-cli`, with no path or extension;
- wrap every path in double quotes, since paths may contain spaces or backslashes;
- write any JSON for the tool to a file with your file tool and pass the file's path. Stdin pipes and inline JSON break differently in each shell, so `--input` always gets a file path;
- read CLI output directly, and project large responses down to the fields you need in the same command, with whatever JSON tool the shell offers (such as `jq`, PowerShell's `ConvertFrom-Json`, or a Python one-liner). Output saved to disk may be blocked by the environment;
- keep every `--input` file in a temp directory made for this session and delete it before you finish, since it holds financial data.

## Changes

Before any `pp-cli apply` or `pp-cli restore`, read [write-protocol.md](write-protocol.md) and follow it step by step.

## Troubleshooting

A failure is a problem+json document on stdout with a `code` and a non-zero exit code. Its `detail` explains it. `pp-cli --help` says what each code means; `pp-cli <command> --help` lists only the code names that command can return.

| Code | Action |
|---|---|
| `file-required`, `file-not-found` | Ask the user for the client file's path. |
| `file-version-unsupported` | Report it as in Session start and stop. |
| `password-required`, `incorrect-password` | Follow Passwords. |
| `unknown-command`, `unknown-option`, `invalid-option-value`, `option-required`, `conflicting-options` | Correct the call from `pp-cli <command> --help`. |
| `reference-ambiguous` | Show the user the candidates and ask which one is meant. |
| `reference-not-found` | List the entities of that kind (such as `pp-cli instruments --file "<path>"`) and confirm the right one with the user. |
| `validation-failed` | Explain each entry of `failures`; `pp-cli apply --help <operation>` describes its code. Fix the batch with the user, and restart the write protocol. |
| `consistency-check-failed` | Report the inconsistencies; nothing was written. |
| `file-open-in-app` | Ask the user to close the file in Portfolio Performance, then retry. `--force` is the user's call alone. |
| `file-locked` | Report the lock file from `detail`; the user decides whether another `pp-cli` is still running. |
| `file-changed` | Read the file again and restart the write protocol from the dry run. |
| `write-failed`, `internal-error` | Report `detail` to the user as it is. |
| any other code | Report `detail` and the code's meaning from `pp-cli --help`, then wait for the user. |
