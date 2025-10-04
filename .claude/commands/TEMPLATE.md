# Command Template

This is a template for creating new slash commands in CC-Sessions.

## Instructions

1. Copy this file to a new name: `your-command.md`
2. Fill in the frontmatter fields
3. Write your bash command
4. Add user message (optional)
5. Test the command
6. **Follow `@sessions/protocols/command-documentation.md`**
7. Document in CC-SESSIONS.md before completing task

---
## Template Format:

```markdown
---
allowed-tools: Bash(tool:*), OtherTool
argument-hint: "what user should provide"
description: Brief description of what this command does (shown in /help)
---

!`your bash command here using $ARGUMENTS and $CLAUDE_PROJECT_DIR`

Optional message shown to user after command execution.
Use this to explain what happened or what to do next.
```

## Frontmatter Fields

### `allowed-tools`
List of tools this command is allowed to use.
- Format: `Bash(command:*)` for bash commands
- Separate multiple with commas: `Bash(cat:*), Bash(jq:*)`
- Use wildcards for flexibility: `Bash(git:*)`

**Common patterns:**
```yaml
allowed-tools: Bash(cat:*), Bash(jq:*)        # JSON manipulation
allowed-tools: Bash(python3:*), Bash(echo:*)  # Python scripts
allowed-tools: Bash(git:*)                    # Git operations
allowed-tools: Bash(mvn:*)                    # Maven builds
```

### `argument-hint`
What should the user provide as arguments?
- Shown in command palette/autocomplete
- Keep it brief and descriptive
- Examples: `"trigger phrase"`, `"version number"`, `"file path"`

### `description`
Brief one-sentence description.
- Shown in `/help` and command list
- Describe what it does, not how
- Example: `"Toggle API mode (enables/disables automatic ultrathink)"`

## Command Body

### Using Variables

**`$ARGUMENTS`** - Everything the user typed after the command name:
```bash
# User types: /add-trigger lets do it
# $ARGUMENTS = "lets do it"

!`echo "User provided: $ARGUMENTS"`
```

**`$CLAUDE_PROJECT_DIR`** - Absolute path to project root:
```bash
!`cat $CLAUDE_PROJECT_DIR/sessions/sessions-config.json`
```

### Multi-line Commands

Use semicolons or && for chaining:
```bash
!`cd $CLAUDE_PROJECT_DIR && cat sessions-config.json | jq '.trigger_phrases'`
```

Or use newlines inside the command:
```bash
!`python3 -c "
import json
import sys
print('Hello from Python')
"`
```

### Error Handling

Show different messages on success/failure:
```bash
!`your-command && echo "Success message" || echo "Error message"`
```

### JSON Manipulation

Use `jq` for JSON:
```bash
!`cat $CLAUDE_PROJECT_DIR/sessions/sessions-config.json | jq -r '.trigger_phrases[]'`
```

## User Message

Optional text shown after command execution:
```markdown
The trigger phrase was added successfully.

You can now use this phrase to switch to implementation mode.
Changes take effect on your next message.
```

**Tips:**
- Explain what happened
- Tell user what to do next
- Mention if restart needed
- Keep it concise

## Example Commands

### Simple Info Display
```markdown
---
allowed-tools: Bash(cat:*), Bash(jq:*)
description: Show current task status
---

!`cat $CLAUDE_PROJECT_DIR/.claude/state/current_task.json | jq -r '"Task: \(.task // "none") | Branch: \(.branch // "none")"'`
```

### Toggle Configuration
```markdown
---
allowed-tools: Bash(jq:*), Bash(cat:*), Bash(mv:*)
description: Toggle dark mode in UI
---

!`current=$(cat $CLAUDE_PROJECT_DIR/config.json | jq -r '.dark_mode'); new=$([ "$current" = "true" ] && echo "false" || echo "true"); jq ".dark_mode = $new" config.json > /tmp/config.tmp && mv /tmp/config.tmp config.json && echo "Dark mode: $current → $new"`

Dark mode configuration updated. Restart the UI to see changes.
```

### Add to List
```markdown
---
allowed-tools: Bash(python3:*)
argument-hint: "item to add"
description: Add item to the list
---

!`python3 -c "import json,sys,os; item='$ARGUMENTS'; config_file=os.path.join(os.environ.get('CLAUDE_PROJECT_DIR','.'),'list.json'); data=json.load(open(config_file)); data.setdefault('items',[]); data['items'].append(item); json.dump(data,open(config_file,'w'),indent=2)"`

Added "$ARGUMENTS" to the list.
```

## Testing Checklist

Before documenting:
- [ ] Command executes without errors
- [ ] Arguments are properly captured
- [ ] File paths resolve correctly
- [ ] Error handling works
- [ ] User message is clear
- [ ] Command is idempotent (safe to run multiple times)

## Documentation Checklist

**Must complete before marking task done:**
- [ ] Followed `@sessions/protocols/command-documentation.md`
- [ ] Added to CC-SESSIONS.md Slash Commands section
- [ ] Updated Quick Reference table
- [ ] Added example usage
- [ ] Tested in actual Claude Code CLI
- [ ] Committed command file and documentation together

## Common Pitfalls

❌ **Don't:**
- Use relative paths (use `$CLAUDE_PROJECT_DIR` instead)
- Forget to quote variables: `$ARGUMENTS` → `"$ARGUMENTS"`
- Make commands with side effects that can't be undone
- Skip the documentation step

✅ **Do:**
- Test multiple times before documenting
- Handle edge cases (empty arguments, missing files)
- Provide clear user feedback
- Follow existing command patterns
- Document with examples

## Getting Help

See existing commands for reference:
- `.claude/commands/add-trigger.md` - Simple argument processing
- `.claude/commands/api-mode.md` - Toggle pattern with jq
- `.claude/commands/release-notes.md` - Complex git-based command

Read the protocol:
- `@sessions/protocols/command-documentation.md`
