# 05: One `--help` precedence rule in the help-command ticket

**What to build:** The per-command help ticket in the pp-cli ergonomics set states a single, non-contradictory `--help` rule. It currently says `--help` wins over every other argument and also that `apply --help <unknown-operation>` is a usage error. Settle it as: `--help` needs no `--file`, never loads a client file, and ignores other options, with one explicit exception: an unknown operation name after `apply --help` is still a usage error. Reflect the rule in the acceptance criteria.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] The ticket states the rule once, with the exception named
- [ ] An acceptance criterion tests the exception (unknown operation after `apply --help`)
- [ ] Origin: CodeRabbit review thread on PR #6262 (ergonomics issue 01, line 16)
