# 08: Insights skill treats a date gap as possible staleness only

**What to build:** The stale-file trap in the insights companion of the `pp-cli` agent skill stops claiming that a gap between the newest record and `meta.asOf` proves unrecorded activity. A gap proves only that nothing is recorded in that interval. The agent reports the gap as possible staleness next to the figures it could distort, and asks the user to confirm whether anything happened since.

**Implementer note:** This changes a skill file. Use `/writing-for-agents` before editing it.

**Blocked by:** 07: Insights skill handles zero-total holdings (both edit the same file)

**Status:** ready-for-agent

- [ ] The trap no longer states that activity since the newest record is unrecorded
- [ ] It tells the agent to report possible staleness, flag the affected figures and ask the user to confirm
- [ ] Origin: CodeRabbit review thread on PR #6262 (insights.md, line 24)
