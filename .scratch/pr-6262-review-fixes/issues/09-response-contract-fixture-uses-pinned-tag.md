# 09: Response-contract fixture uses the pinned upstream tag

**What to build:** The response-contract test's `info` fixture carries the same pinned upstream tag that the `info` command emits (`0.89.0`). It currently has `0.89.1`, and the schema only checks that the field is a string, so the stale value passes. Fix the value; preferably build the fixture from the `CoreVersion` pinned-tag constant so it cannot drift again.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] The fixture's `pinnedUpstreamTag` equals the value `info` emits
- [ ] The CLI test suite passes
- [ ] Origin: CodeRabbit review thread on PR #6262 (ResponseContractTest, line 19)
