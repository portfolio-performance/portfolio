# 04: Revert unrelated IDE `.project` changes

**What to build:** The PR diff no longer contains IDE-generated noise. Fourteen `.project` files (17 added lines each) gained an m2e builder, an m2e nature and a `filteredResources` entry with a timestamped id and the `__CREATED_BY_JAVA_LANGUAGE_SERVER__` marker, written by the VS Code Java language server. They are unrelated to the CLI. Restore them to the `master` versions.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `git diff master --name-only -- '*.project'` lists nothing
- [ ] No `.project` file for the new CLI modules is added unless `master` convention requires one
- [ ] Origin: CodeRabbit review thread on PR #6262 (`.project`, line 60)
