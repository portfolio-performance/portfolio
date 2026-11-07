# 01: Map reflects the shipped agent skill

**What to build:** The `ai-cli` map no longer contradicts the spec. It stops listing "AI skills that teach agents to use the CLI" as out of scope, because this PR ships the single `pp-cli` skill (tickets 35 and 36). Out of scope covers only further skills: separate read, write or setup skills, or skills published outside this repo. The "Not yet specified" note no longer claims that only tickets 01-11 exist.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] The out-of-scope list no longer says the skill is to be written later or lives in an external repository
- [ ] The out-of-scope list names what is still out of scope: skills beyond the single `pp-cli` skill
- [ ] No statement in the map says only tickets 01-11 exist
- [ ] Origin: CodeRabbit review thread on PR #6262 (map.md, line 48)
