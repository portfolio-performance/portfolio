# Task Creation Protocol

## Configuration
Task naming conventions can be customized in `sessions/sessions-config.json`.
If no config exists, the defaults below are used.

## Priority Prefix System

Check `sessions/sessions-config.json` for configured prefixes.
Default prefixes:
- `h-` → High priority
- `m-` → Medium priority  
- `l-` → Low priority
- `?-` → Investigate (task may be obsolete, speculative priority)

Examples:
- `h-fix-auth-redirect.md`
- `m-implement-oauth.md`
- `l-docs-api-reference.md`
- `?-research-old-feature.md`

## Task Type Prefix Enum

Task type comes after priority prefix. Check `sessions/sessions-config.json` for branch mappings.
Default mappings:

- `implement-` → Creates feature/ branch
- `fix-` → Creates fix/ branch  
- `refactor-` → Creates feature/ branch
- `research-` → No branch needed
- `experiment-` → Creates experiment/ branch
- `migrate-` → Creates feature/ branch
- `test-` → Creates feature/ branch
- `docs-` → Creates feature/ branch

## File vs Directory Decision

**Use a FILE when**:
- Single focused goal
- Estimated < 3 days work
- No obvious subtasks at creation time
- Examples:
  - `h-fix-auth-redirect.md`
  - `m-research-mcp-features.md`
  - `l-refactor-redis-client.md`

**Use a DIRECTORY when**:
- Multiple distinct phases
- Needs clear subtasks from the start
- Estimated > 3 days work
- Examples:
  - `h-implement-auth/` (magic links + OAuth + sessions)
  - `m-migrate-to-postgres/` (schema + data + cutover)
  - `l-test-all-services/` (per-service test files)

## Creating a Task

1. **Copy template**:
   ```bash
   cp sessions/tasks/TEMPLATE.md sessions/tasks/[priority]-[task-name].md
   ```
   Or for directory:
   ```bash
   mkdir sessions/tasks/[priority]-[task-name]
   cp sessions/tasks/TEMPLATE.md sessions/tasks/[priority]-[task-name]/README.md
   ```

2. **Fill out frontmatter**:
   - task: Must match filename (including priority prefix)
   - branch: Based on task type prefix (or 'none' for research)
   - status: Start as 'pending'
   - created: Today's date
   - modules: List all services/modules that will be touched

3. **Write clear success criteria**:
   - Specific and measurable
   - Defines "done" unambiguously
   - Checkboxes for tracking

4. **Context Gathering** (STRONGLY RECOMMENDED):
   ```bash
   # Highly recommended to run immediately after task creation:
   # Use context-gathering agent on sessions/tasks/[priority]-[task-name].md
   ```
   - Agent creates comprehensive context manifest
   - Includes narrative explanation of how systems work
   - Can be deferred to task-startup if preferred
   - Context manifest helps prevent confusion and rework

## Branch Creation

**DO NOT** create branch until starting work:
```bash
# When ready to start:
git checkout -b feature/implement-cool-thing

# Update task state with CORRECT format
# IMPORTANT: Use exactly these field names:
#   - "task" (NOT "task_file") - just the task name without path or .md extension  
#   - "branch" (NOT "branch_name") - the Git branch name
#   - "services" - array of affected services/modules
#   - "updated" - current date in YYYY-MM-DD format
cat > .claude/state/current_task.json << EOF
{
  "task": "m-implement-cool-thing",
  "branch": "feature/implement-cool-thing",
  "services": ["service1", "service2"],
  "updated": "$(date +%Y-%m-%d)"
}
EOF
```

## Task Evolution

If a file task needs subtasks during work:
1. Create directory with same name
2. Move original file to directory as README.md
3. Add subtask files
4. Update active task reference if needed

## For Agents Creating Tasks

When programmatically creating tasks:
1. Read `sessions/sessions-config.json` for:
   - Priority prefixes from `config.task_prefixes.priority`
   - Type-to-branch mappings from `config.task_prefixes.types`
2. If config doesn't exist, use defaults documented above
3. Always use the template structure from `sessions/tasks/TEMPLATE.md`
4. Strongly encourage running context-gathering agent after creation