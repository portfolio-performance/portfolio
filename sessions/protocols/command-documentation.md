# Command and Trigger Documentation Protocol

## Purpose
Ensure all new slash commands and trigger phrases are properly documented when added to the cc-sessions system.

## When to Use
- **ALWAYS** when creating a new slash command in `.claude/commands/`
- **ALWAYS** when adding a new trigger phrase via `/add-trigger`
- Before completing any task that modifies cc-sessions behavior

## Checklist for New Commands

When creating a new slash command:

### 1. Create Command File
Location: `.claude/commands/your-command.md`

Use this template:
```markdown
---
allowed-tools: Bash(tool:*), OtherTool
argument-hint: "what user should provide"
description: Brief description of what this command does
---

!`your bash command here`

Optional message to user after command execution.
```

### 2. Document in CC-SESSIONS.md

Add to the "Slash Commands" section:

```markdown
### `/your-command`
Brief description of what this does.

**Usage:**
```
/your-command [arguments]
```

**Example:**
```
/your-command example-value
```

**What it does:**
- Bullet point 1
- Bullet point 2
```

### 3. Update Quick Reference Table

Add to the table at the end of CC-SESSIONS.md:

```markdown
| Your action | `/your-command args` |
```

### 4. Test the Command

Before documenting:
```bash
# In Claude Code CLI
/your-command test-value

# Verify:
# - Command executes without errors
# - Expected output is produced
# - State files are updated correctly
```

## Checklist for New Trigger Phrases

When adding a new trigger phrase:

### 1. Add via Slash Command
```
/add-trigger your new phrase
```

### 2. Document in CC-SESSIONS.md

Update the trigger phrases section:

```markdown
**English:**
- "**existing phrase**"
- "**your new phrase**"  ← Add here

**German:**
- "**existing phrase**"
```

### 3. Update sessions/sessions-config.json

Verify the phrase was added:
```bash
cat sessions/sessions-config.json | grep "your new phrase"
```

### 4. Document Context

Add a comment in sessions/sessions-config.json explaining WHY:
```json
{
  "config": {
    "trigger_phrases": [
      "go ahead",
      "your new phrase"  // Added for [reason]
    ]
  }
}
```

### 5. Test the Trigger

```
You: [In Discussion Mode]
You: your new phrase
Claude: [Should switch to Implementation Mode]
```

## Documentation Locations

### Primary Documentation (User-Facing)
- **CC-SESSIONS.md** → Main guide
  - Slash Commands section
  - Trigger Phrases section
  - Quick Reference table

### Secondary Documentation (Meta)
- **sessions/CLAUDE.md** → Meta documentation
  - Sessions system modifications
  - Hook/Agent changes

### Configuration
- **sessions/sessions-config.json** → Active configuration
  - Add comments for context
  - Maintain alphabetical order

### Protocol Reference
- **sessions/protocols/command-documentation.md** → This file
  - Workflow for future additions

## Template: New Command Documentation

Copy this when adding a new command:

````markdown
### `/command-name`
One-sentence description of what this command does.

**Usage:**
```
/command-name [required-arg] [optional-arg]
```

**Arguments:**
- `required-arg` - Description of required argument
- `optional-arg` - Description of optional argument (optional)

**Example:**
```
/command-name example-value
```

**What it does:**
1. Step 1 of what happens
2. Step 2 of what happens
3. Expected outcome

**When to use:**
- Use case 1
- Use case 2
````

## Template: New Trigger Phrase Documentation

Copy this when adding a new trigger:

```markdown
**[Language]:**
- "**existing trigger**"
- "**new trigger**" - [Brief context: why this was added]
```

## Validation Checklist

Before completing documentation:

- [ ] Command/trigger tested and working
- [ ] Added to CC-SESSIONS.md
- [ ] Added to Quick Reference table
- [ ] Configuration file updated
- [ ] Comments explain WHY it was added
- [ ] Example usage provided
- [ ] Edge cases documented

## Best Practices

### Do:
- ✅ Test before documenting
- ✅ Provide clear examples
- ✅ Explain WHY it was added
- ✅ Keep descriptions concise (1-2 sentences)
- ✅ Update all relevant locations

### Don't:
- ❌ Document untested commands
- ❌ Skip the Quick Reference table
- ❌ Forget to commit documentation with code
- ❌ Use jargon without explanation

## Example: Complete Documentation Flow

### Scenario: Adding `/task-status` command

**1. Create command file:**
```bash
# .claude/commands/task-status.md
---
allowed-tools: Bash(cat:*), Bash(jq:*)
description: Show current task status and details
---

!`cat $CLAUDE_PROJECT_DIR/.claude/state/current_task.json | jq -r '"Task: \(.task // "none") | Branch: \(.branch // "none") | Status: \(.status // "unknown")"'`
```

**2. Test it:**
```bash
/task-status
# Output: Task: m-implement-feature | Branch: feature/implement-feature | Status: in-progress
```

**3. Add to CC-SESSIONS.md:**
```markdown
### `/task-status`
Show current task name, branch, and status.

**Usage:**
```
/task-status
```

**Example:**
```
/task-status
# Output: Task: m-implement-feature | Branch: feature/implement-feature | Status: in-progress
```
```

**4. Update Quick Reference:**
```markdown
| Check task status | `/task-status` |
```

**5. Document in sessions/CLAUDE.md:**
```markdown
## Custom Commands
- `/task-status` - Added to quickly check current task without reading full file
```

**6. Commit everything together:**
```bash
git add .claude/commands/task-status.md
git add CC-SESSIONS.md
git add sessions/CLAUDE.md
git commit -m "Add /task-status command for quick task inspection"
```

## Integration with Task Completion

When completing a task that added commands/triggers:

1. Check if documentation was updated
2. Verify all locations are consistent
3. Add to Work Log in task file:
   ```markdown
   ## Work Log
   - [2025-10-04] Added `/new-command` - documented in CC-SESSIONS.md
   ```

## Maintaining Consistency

### Regular Audits
Periodically check:
```bash
# List all commands
ls .claude/commands/

# Check they're all documented
grep "/command-name" CC-SESSIONS.md

# Verify trigger phrases match
diff <(jq -r '.config.trigger_phrases[]' sessions/sessions-config.json | sort) \
     <(grep -oP '"\*\*\K[^*]+(?=\*\*")' CC-SESSIONS.md | sort)
```

### Documentation Drift Prevention
- Always document with code changes
- Review documentation in pull requests
- Test documented examples
- Keep Quick Reference synchronized

## Summary

**Golden Rule:**
> Every new command or trigger phrase gets documented in CC-SESSIONS.md before the task is marked complete.

**Quick Checklist:**
1. Create/modify command/trigger
2. Test it works
3. Document in CC-SESSIONS.md
4. Update Quick Reference
5. Add context comments
6. Commit together
