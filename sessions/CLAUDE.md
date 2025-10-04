# Sessions System - Meta Documentation

## Purpose
This file provides guidance for working on the sessions system itself (not regular project work).

## When to Use This
Only reference this file when:
- Modifying hooks in `.claude/hooks/`
- Creating/updating agents in `.claude/agents/`
- Creating new slash commands in `.claude/commands/`
- Adding new trigger phrases
- Changing protocols in `sessions/protocols/`
- Updating sessions configuration
- Adding new knowledge to `sessions/knowledge/`

For regular project work, use `@CLAUDE.md` in the project root.

## ⚠️ IMPORTANT: Documentation Requirement
**When adding new commands or trigger phrases:**
- ALWAYS follow `@sessions/protocols/command-documentation.md`
- Update CC-SESSIONS.md before marking task complete
- Test the command/trigger before documenting
- Update Quick Reference table

## Sessions System Structure

```
sessions/
├── CLAUDE.md                    # This file (meta-documentation)
├── sessions-config.json         # Main configuration
├── knowledge/
│   ├── claude-code/            # cc-sessions framework knowledge
│   │   ├── hooks-reference.md
│   │   ├── subagents.md
│   │   ├── tool-permissions.md
│   │   └── slash-commands.md
│   └── pdf-importer.md         # Project-specific guides (5-phase TDD, 7 assertions)
├── protocols/
│   ├── task-creation.md
│   ├── task-startup.md
│   ├── task-completion.md
│   ├── context-compaction.md
│   ├── release-notes-generation.md
│   └── command-documentation.md   # Protocol for adding commands/triggers
└── tasks/
    ├── TEMPLATE.md
    └── [task-files].md

.claude/
├── hooks/                      # Hook implementations
│   ├── session-start.py
│   ├── user-messages.py
│   ├── sessions-enforce.py
│   ├── post-tool-use.py
│   ├── task-transcript-link.py
│   └── shared_state.py
├── agents/                     # Agent definitions
│   ├── context-gathering.md   # Analyzes code and creates context manifests
│   ├── logging.md              # Consolidates work logs during compaction
│   ├── context-refinement.md  # Updates context with discoveries
│   ├── code-review.md          # Reviews code for bugs and security (on request)
│   └── service-documentation.md # Audits and updates all documentation
├── commands/                   # Slash commands
│   ├── add-trigger.md
│   ├── api-mode.md
│   ├── release-notes.md
│   ├── pdf-importer.md        # New PDF extractor task creation (multilingual)
│   ├── pdf-debug.md           # Debug existing PDF extractor (multilingual)
│   ├── session-docs.md        # Review session documentation changes
│   └── TEMPLATE.md
├── scripts/                    # Python scripts for slash commands
│   ├── create-pdf-importer-task.py    # Multilingual transaction type detection
│   ├── create-pdf-debug-task.py       # Multilingual transaction type detection
│   └── list-session-docs.py
├── state/                      # Runtime state
│   ├── daic-mode.json
│   └── current_task.json
└── settings.local.json         # Local settings
```

## Key Concepts

### DAIC Mode (Discussion-Action-Implementation-Completion)
- **Discussion Mode**: Default state, blocks Edit/Write/MultiEdit tools
- **Implementation Mode**: Activated by trigger phrases, allows all tools
- State stored in `.claude/state/daic-mode.json`
- Enforced by `sessions-enforce.py` hook

### Task Management
- Tasks follow naming convention: `[priority]-[type]-[name].md`
- Priority prefixes: `h-`, `m-`, `l-`, `?-`
- Type determines branch: `implement-` → `feature/`, `fix-` → `fix/`, etc.
- Current task tracked in `.claude/state/current_task.json`

### Trigger Phrases
Configured in `sessions-config.json`:
- English: "go ahead", "ship it", "make it so", "proceed"
- German: "los geht's", "mach es"
- Switches from Discussion to Implementation mode

### Hooks Workflow
1. **SessionStart** - Loads task context at session start
2. **UserPromptSubmit** - Checks for trigger phrases, manages DAIC mode
3. **PreToolUse** - Blocks tools in Discussion mode
4. **PostToolUse** - Auto-returns to Discussion mode after implementation
5. **Stop** - Can force context compaction protocols

### Agents Usage
- **context-gathering**: Run when creating new tasks
- **logging**: Run during task completion/compaction
- **code-review**: Run before commits (when requested)
- **context-refinement**: Update task context with discoveries
- **service-documentation**: Update docs after significant changes

## Customization Guidelines

### Adding Trigger Phrases
Edit `sessions/sessions-config.json`:
```json
{
  "config": {
    "trigger_phrases": [
      "your new phrase"
    ]
  }
}
```

### Adding Task Type Prefixes
Edit `sessions/sessions-config.json`:
```json
{
  "config": {
    "task_prefixes": {
      "types": {
        "new-type-": "feature/"
      }
    }
  }
}
```

### Modifying Blocked Tools
Edit `sessions/sessions-config.json`:
```json
{
  "config": {
    "blocked_tools": [
      "Edit",
      "Write",
      "MultiEdit"
    ]
  }
}
```

### Creating New Hooks
1. Create script in `.claude/hooks/`
2. Make executable: `chmod +x .claude/hooks/your-hook.py`
3. Reference in `.claude/settings.json` or project settings
4. Test with `--debug` flag

### Creating New Agents
1. Create markdown file in `.claude/agents/`
2. Follow existing agent structure (prompt template)
3. Document in `sessions/knowledge/claude-code/subagents.md`

### Creating New Slash Commands
**⚠️ ALWAYS follow `@sessions/protocols/command-documentation.md`**

Quick steps:
1. Create command file in `.claude/commands/your-command.md`
2. Test the command works
3. Document in CC-SESSIONS.md (Slash Commands section)
4. Update Quick Reference table
5. Add to this file's structure diagram
6. Commit documentation with code

**Command file template:**
```markdown
---
allowed-tools: Bash(tool:*)
argument-hint: "what user provides"
description: Brief description
---

!`your bash command`

Optional user message after execution.
```

**Example:** See `.claude/commands/add-trigger.md` or `api-mode.md`

## Debugging Sessions System

### Check Current State
```bash
# Check DAIC mode
cat .claude/state/daic-mode.json

# Check current task
cat .claude/state/current_task.json

# View hook logs (run with --debug)
claude --debug
```

### Test Hooks
```bash
# Test hook manually
echo '{"session_id":"test","hook_event_name":"UserPromptSubmit","prompt":"test"}' | \
  .claude/hooks/user-messages.py
```

### Common Issues
- **Hooks not firing**: Check hook registration in settings
- **Tools not blocked**: Verify DAIC mode state
- **Task not loading**: Check current_task.json format
- **Trigger phrases not working**: Check sessions-config.json

## Best Practices

### When Working on Sessions System
1. Test changes in a separate branch first
2. Document changes in this file
3. Update knowledge files if adding features
4. Keep hooks simple and focused
5. Use shared_state.py for state management

### When Creating Knowledge Files
1. Place project-specific guides in `sessions/knowledge/`
2. Keep framework docs in `sessions/knowledge/claude-code/`
3. Reference with `@sessions/knowledge/filename.md`
4. Update this file's structure diagram

### When Modifying Protocols
1. Test protocol flow end-to-end
2. Ensure agents are called correctly
3. Verify state updates properly
4. Document in protocol file itself

## Integration with Portfolio Performance

The sessions system is configured for this Java/Maven project:
- Maven commands auto-approved via `settings.local.json`
- Python hooks allowed for automation
- PDF importer guide at `sessions/knowledge/pdf-importer.md`
- Release notes generation at `sessions/protocols/release-notes-generation.md`
  - Bilingual format (German/English) for GitHub releases
  - XML format for metainfo.xml
- Build system: Maven with Tycho (Eclipse RCP)

## References

### Core Documentation
- Main project docs: `@CLAUDE.md` (root)
- User guide: `@CC-SESSIONS.md`
- PDF importer guide: `@sessions/knowledge/pdf-importer.md`

### Framework Knowledge
- Hook reference: `@sessions/knowledge/claude-code/hooks-reference.md`
- Subagents guide: `@sessions/knowledge/claude-code/subagents.md`
- Tool permissions: `@sessions/knowledge/claude-code/tool-permissions.md`
- Slash commands: `@sessions/knowledge/claude-code/slash-commands.md`

### Protocols
- Task creation: `@sessions/protocols/task-creation.md`
- Task startup: `@sessions/protocols/task-startup.md`
- Task completion: `@sessions/protocols/task-completion.md`
- Context compaction: `@sessions/protocols/context-compaction.md`
- Release notes: `@sessions/protocols/release-notes-generation.md`
- **Command documentation: `@sessions/protocols/command-documentation.md`** ← Use this!
