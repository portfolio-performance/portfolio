# 06: Skill update: help-based discovery, insights recipe, scratch-file hygiene

Type: task
Status: done
Blocked by: 01, 03, 04

Spec: [spec.md](../spec.md). Use the `writing-for-agents` skill. Edit `name.abuchen.portfolio.cli/skills/pp-cli/`, which is the source the installer copies.

## What to build

**SKILL.md**
- Session start: replace "run `pp-cli describe`" with "run `pp-cli --help` once, and `pp-cli <command> --help` before using a command for the first time". The CLI stays the single source of truth for its interface.
- Answers and Troubleshooting: replace every reference to `describe` (limits, errors) with the matching `--help`.
- Shell: add a rule. Read CLI output directly. If it must be saved to parse it, use a temp directory for this session and delete it before finishing. The same rule covers `--input` files, since both hold financial data.
- Add a one-line pointer to `insights.md`, worded so that it triggers on overview and insights requests.

**insights.md** (new, disclosed reference)
- Call sequence: `info`, `holdings`, `taxonomies` then `allocation` per taxonomy, `performance` for 1Y, YTD and all-time, `performance --by instrument`, and `transactions --order desc --limit N`.
- Traps:
  - All-time TTWROR is cumulative and IRR is annualised, so don't compare them.
  - Allocation rows are cumulative across nesting, so add up only the top-level rows plus unassigned.
  - `share` is a share of the client total, not of the parent classification. A share of the parent is the row's `value` divided by its parent's `value`.
  - Flag IRR on closed or briefly held positions (extreme values, TTWROR −100%) instead of quoting it.
  - Quote converted figures for instruments in a minor currency unit.

## Acceptance criteria

- [x] No skill file mentions `describe`
- [x] `SkillDriftTest` passes against the reworked checks from issue 01
- [x] `insights.md` is linked from `SKILL.md`, and every command and option it names exists
- [x] `SKILL.md` stays lean: the pointer and the Shell rule add no more than a few lines

## Comments
