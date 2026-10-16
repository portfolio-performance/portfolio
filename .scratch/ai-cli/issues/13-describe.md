# 13: `describe` generated from the command declarations

Type: task
Status: ready-for-agent
Blocked by: 12

Spec: [spec.md](../spec.md), user stories 11–14 and "Command layer".

## What to build

`pp-cli describe` lets an agent learn the tool without outside documentation. It returns every declared command with its options and response schema, and later every write operation with its input schema. It also lists the known limits: unsaved edits inside the running app can't be detected, there is no PDF import and there are no price writes. It reports the pinned upstream tag and `maxSupportedVersion`.

The output is generated from the same declarations that drive parsing and dispatch, so it can't drift. A consistency test fails when a declared command is missing from `describe`, or when its declared response schema doesn't match the command's real output. Every later ticket adds its commands and operations to the declarations and stays green under this test.

## Acceptance criteria

- [ ] `pp-cli describe` returns the commands, options and response schemas inside the standard envelope
- [ ] Known limits, the pinned tag and `maxSupportedVersion` are included
- [ ] `describe` works without `--file`
- [ ] The consistency test covers every declared command, including `info` and `describe`, and validates real responses against the declared schemas
- [ ] The declaration format can also describe write operations and their input schemas, ready for the `apply` tickets
