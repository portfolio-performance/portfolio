# 06: Sort ticket's default-output criterion allows additive `updatedAt`

**What to build:** The sort-and-order ticket in the pp-cli ergonomics set stops requiring byte-identical default output while also adding `updatedAt` to default responses. Reword the "defaults reproduce today's output exactly" line and the matching acceptance criterion to: defaults keep the previous ordering and fields apart from the documented additive `updatedAt`, and existing tests stay unchanged except for that field (or, alternatively, byte-identity is checked only on records that have no `updatedAt`).

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] The "defaults reproduce" line and the byte-identical acceptance criterion no longer contradict the `updatedAt` addition
- [ ] The ticket names the one allowed difference (the additive `updatedAt`)
- [ ] Origin: CodeRabbit review thread on PR #6262 (ergonomics issue 03, line 29)
