# 12: Walking skeleton: entry point, envelope and `info`

Type: task
Status: ready-for-agent
Blocked by: None (can start immediately)

Spec: [spec.md](../spec.md), sections "Module and architecture", "Command layer", "Output conventions" and "Formats, versions and passwords". ADRs 0002 and 0003.

## What to build

The thinnest end-to-end `pp-cli`. A new, additive Java 21 module in the reactor runs the core engine on a plain classpath. It has one in-process entry point that takes arguments, environment, stdin, stdout, stderr and a clock, and returns an exit code. A thin `main` adapts the process to it. A test bundle exercises everything through that entry point (seam 1).

Commands and options are declared once, and those declarations drive parsing and dispatch. This ticket implements one command, `info`, for an unencrypted client file. It returns the `{data, meta, warnings}` envelope with the file version, `maxSupportedVersion`, the detected `format`, the base currency, entity counts and the pinned upstream tag. Failures come back as problem+json errors with a stable `code` and a non-zero exit code.

Every load runs the version guard, so a file newer than core's current version is refused with `file-version-unsupported`. This includes unencrypted binary files, which core itself doesn't guard.

## Acceptance criteria

- [ ] The new module builds in the existing Maven/Tycho reactor with no change to any existing core file
- [ ] Its tests run through the in-process entry point only, using an injected clock and captured stdout and stderr
- [ ] `pp-cli info --file <path>` returns the envelope with version, `maxSupportedVersion`, `format`, base currency, counts and pinned tag
- [ ] `PP_FILE` is used when `--file` is absent; if neither is given, the result is a structured usage error
- [ ] Format is detected from the extension and magic bytes for the unencrypted formats (`xml`, `xml-zip`, `binary`, including XML with ID references)
- [ ] A file newer than supported, XML or binary, is refused with `file-version-unsupported`, `fileVersion` and `maxSupportedVersion`
- [ ] A missing file, an unknown command and a bad option each give a problem+json error with a stable `code` and a non-zero exit code
- [ ] stdout contains only JSON; core's `PortfolioLog` output and any other logs go to stderr
- [ ] Exchange rates use the offline ECB data, with no online feeds, token store or persisted caches
