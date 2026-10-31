# 07: Insights skill handles zero-total holdings

**What to build:** The insights companion of the `pp-cli` agent skill tells the agent what to do when the holdings total is zero (an empty client, or positions netting to zero). Today the derived currency split and top-three share divide by that total. Add to the derivation step: if the total is zero, report the split and the shares as unavailable instead of emitting percentages.

**Implementer note:** This changes a skill file. Use `/writing-for-agents` before editing it.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] The derivation step covers the zero-total case and says to report the figures as unavailable
- [ ] The rest of the derivation is unchanged
- [ ] Origin: CodeRabbit review thread on PR #6262 (insights.md, line 15)
