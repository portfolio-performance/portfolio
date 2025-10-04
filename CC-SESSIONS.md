# CC-Sessions User Guide

## ⚠️ Important: What is CC-Sessions?

**CC-Sessions is a workflow system for Claude Code CLI** - NOT for Eclipse IDE!

### Claude Code vs. Eclipse IDE

| Tool | Usage in this Project |
|------|----------------------|
| **Claude Code CLI** | AI assistance for code development with CC-Sessions |
| **Eclipse IDE** | Develop and debug Portfolio Performance RCP app |

**CC-Sessions runs exclusively in Claude Code** (terminal/CLI tool from Anthropic).
Eclipse IDE remains unchanged - no plugins needed!

### What does CC-Sessions do?

CC-Sessions extends Claude Code with:
- **Discussion before implementation** (DAIC Mode)
- **Task management with persistent context**
- **Branch enforcement** for each task
- **Automatic context compaction** when token limit approached

## Prerequisites

- ✅ **Claude Code CLI** installed (https://claude.com/claude-code)
- ✅ **Python 3** (for hooks)
- ✅ **Git** (for branch management)
- ❌ **NO Eclipse IDE plugins needed**

## Typical Workflow

1. **In Claude Code CLI**: Create tasks, generate/modify code
2. **In Eclipse IDE**: Test, debug, manually refine code
3. **In Claude Code CLI**: Continue with next task

Both tools work on the same repository, but CC-Sessions only runs in Claude Code!

## Quick Start

### 1. Create a New Task

```
You: Create a task for: [task description]
```
or
```
You: New Job: [task description]
```

**Example:**
```
You: Create a task for: Implement DKB PDF importer for dividend transactions
You: New Job: Implement DKB PDF importer for dividend transactions
```

Claude will:
- Create task file in `sessions/tasks/`
- Add priority prefix (h-, m-, l-, ?-)
- Start context-gathering agent (optional)

### 2. Work on Task

```
You: Let's work on m-implement-dkb-importer
```

Claude loads:
- Task context from file
- Associated Git branch
- Previously gathered information

### 3. Use DAIC Workflow

#### **Discussion Mode** (Default)
Claude starts in **Discussion Mode**:
- Edit/Write/MultiEdit are **blocked**
- Claude must discuss plan first
- You give approval with trigger phrase

#### **Activate Implementation Mode**
Say one of the trigger phrases:

**English:**
- "**go ahead**"
- "**ship it**"
- "**make it so**"
- "**let's do it**"
- "**proceed**"
- "**run**"
- "**go**"

**German:**
- "**los geht's**"
- "**mach es**"

After trigger phrase:
- ✅ Edit/Write/MultiEdit are unlocked
- Claude implements the discussed plan
- After completion: Automatic reminder to switch back

#### **Return to Discussion Mode**
After implementation:
```bash
daic
```

Or Claude switches back automatically.

## Complete Workflow Example

### Full Task Flow

```
1. You: Create a task for: Add forex support to TradeRepublic importer

2. Claude: Creates task "m-implement-traderepublic-forex"
           Starts context-gathering agent
           Analyzes existing code

3. You: Let's work on m-implement-traderepublic-forex

4. Claude: [Discussion Mode]
           Analyzes Dividende03.txt
           Finds Devisenkurs pattern
           Proposes:
           - Forex section after Amount section
           - Standard attributes: baseCurrency, termCurrency, exchangeRate
           - Test with hasForexGrossValue()

5. You: go ahead

6. Claude: [Implementation Mode]
           - Extends addDividendeTransaction()
           - Adds forex section
           - Creates test with 7 assertions
           - Runs tests: mvn verify -Dtest=...

           [DAIC Reminder] When you're done implementing, run: daic

7. You: daic

8. Claude: [Discussion Mode]
           Back in Discussion Mode.
           All tests successful?
```

## Task Management

### Task File Structure

```markdown
---
task: m-implement-feature
branch: feature/implement-feature
status: pending|in-progress|completed|blocked
created: 2025-10-04
modules: [module1, module2]
---

# Feature Implementation

## Problem/Goal
What should be achieved?

## Success Criteria
- [ ] Criterion 1
- [ ] Criterion 2

## Context Files
- @sessions/knowledge/pdf-importer.md
- @specific/file.java

## Work Log
- [2025-10-04] Task created
```

### Task Priorities

- `h-` → **High Priority** (Critical/Blocking)
- `m-` → **Medium Priority** (Normal features/fixes)
- `l-` → **Low Priority** (Nice-to-haves)
- `?-` → **Investigate** (Unclear if needed)

### Task Types

| Prefix | Branch Type | Usage |
|--------|------------|-------|
| `implement-` | `feature/` | New features |
| `fix-` | `fix/` | Bug fixes |
| `refactor-` | `feature/` | Code refactoring |
| `research-` | `none` | Research tasks |
| `experiment-` | `experiment/` | Experiments |
| `test-` | `feature/` | Test extensions |
| `docs-` | `feature/` | Documentation |

**Examples:**
- `h-fix-authentication-bug.md` → Branch: `fix/authentication-bug`
- `m-implement-oauth.md` → Branch: `feature/implement-oauth`
- `l-refactor-database-client.md` → Branch: `feature/refactor-database-client`
- `?-research-new-api.md` → No branch

## Context-Gathering Agent

Recommended at task start:

```
You: Let's work on m-implement-feature

Claude: Should I start the context-gathering agent?

You: yes
```

The agent:
- Analyzes relevant code files
- Creates context manifest with patterns
- Documents current implementation
- Finds similar examples

**Manifest Example:**
```markdown
## Context Manifest

### Current Implementation
- Forex handling in DkbPDFExtractor.java:269
- Uses baseCurrency, termCurrency, exchangeRate
- Pattern: Amount FIRST, then Forex section

### Key Patterns
- asExchangeRate(v) → Creates ExchangeRate
- checkAndSetGrossUnit(gross, fxGross, t, ctx)
- Money.of(currency, amount)

### Related Files
- @BaaderBankPDFExtractor.java:150-200
```

## Context Compaction

At ~75% token usage Claude warns:
```
⚠️ Context at 75% - Consider wrapping up soon
```

### Manual Compaction

```
You: let's compact

Claude: [Starts context-compaction protocol]
        - Logging agent writes Work Log
        - Context-refinement updates Manifest
        - Saves Next Steps

You: /clear

Claude: [Reloads task context]
        Resuming work on m-implement-feature.
        Last session: Added forex sections
        Next: Run tests and verify
```

## Branch Enforcement

Sessions enforces correct branch:

```
You: Update the login function

Claude: [Attempts Edit]

❌ BLOCKED: Branch mismatch!
   Task requires: feature/implement-oauth
   You're on: main

Claude: git checkout feature/implement-oauth
        [Now Edit possible]
```

## Project-Specific Guides

### PDF Importer Development

See: `@sessions/knowledge/pdf-importer.md`

**Workflow:**
1. Read test .txt file FIRST
2. Find DocumentType
3. Extend existing methods (don't create new)
4. Use standard forex attributes
5. All 7 test assertions required

**Quick Reference:**
```java
// Standard Exchange Rate Attributes
baseCurrency   // EUR in EUR/CHF
termCurrency   // CHF in EUR/CHF
exchangeRate   // Rate value
fxGross        // Foreign currency amount

// 7 Mandatory Assertions
assertThat(errors, empty());
assertThat(countSecurities(results), is(1L));
assertThat(countBuySell(results), is(1L));
assertThat(countAccountTransactions(results), is(0L));
assertThat(countAccountTransfers(results), is(0L));  // ← Often forgotten!
assertThat(results.size(), is(2));
new AssertImportActions().check(results, "EUR");
```

## Slash Commands

CC-Sessions provides custom slash commands:

### `/add-trigger`
Add a new trigger phrase for switching to implementation mode.

**Usage:**
```
/add-trigger your custom phrase
```

**Example:**
```
/add-trigger let's implement
```

### `/api-mode`
Toggle API mode to control automatic ultrathink (token saving).

**Usage:**
```
/api-mode
```

**Modes:**
- **API mode OFF** (default): Ultrathink automatically enabled
- **API mode ON**: Ultrathink disabled, manual control with `[[ ultrathink ]]`

### `/release-notes`
Generate release notes from git commits (Portfolio Performance specific).

**Usage:**
```
/release-notes
```

Creates formatted release notes in XML format for metainfo.xml.

### `/pdf-importer`
Create task for implementing a new PDF importer with test files.

**Usage:**
```
/pdf-importer BankName Kauf01.txt Dividende01.txt ...
```

**Example:**
```
/pdf-importer TradeRepublic Kauf01.txt Dividende01.txt Zinsen01.txt
```

**What it does:**
1. Creates task file `m-implement-bankname-pdf.md`
2. Detects transaction types from filenames
3. Sets up feature branch `feature/implement-bankname-pdf`
4. Pre-fills task with TDD workflow and test templates
5. Includes all 7 mandatory test assertions
6. Ready for context-gathering agent

**When to use:**
- Implementing a brand new PDF extractor
- Have anonymized test .txt files ready
- Need structured TDD workflow

### `/pdf-debug`
Create task for debugging/fixing existing PDF importer with new test cases.

**Usage:**
```
/pdf-debug BankName Kauf01.txt Buy01.txt ...
```

**Example:**
```
/pdf-debug DKB Kauf01.txt Dividende03.txt
```

**What it does:**
1. Creates task file `m-fix-bankname-pdf.md`
2. Lists test files to analyze
3. Sets up fix branch `fix/bankname-pdf`
4. Pre-fills task with debugging checklist
5. References existing extractor and test files
6. Includes critical reminders from pdf-importer.md

**When to use:**
- Existing extractor fails on new transaction format
- Need to extend existing DocumentType
- Have new test cases that expose bugs

## Common Commands

### Maven (Auto-approved)
```bash
# Core tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -Dtest=TestClass

# Specific test
mvn verify -Plocal-dev -Dtest=BankPDFExtractorTest#testDividend01
```

### Task Management
```bash
# View task list
ls sessions/tasks/

# Read task details
cat sessions/tasks/m-implement-feature.md

# Check current task
cat .claude/state/current_task.json

# Check DAIC mode
cat .claude/state/daic-mode.json
```

### Git Workflow
```bash
# Create branch for task
git checkout -b feature/implement-feature

# Task state updated automatically by hooks
```

## Configuration

### Customize Trigger Phrases

Edit `sessions/sessions-config.json`:
```json
{
  "config": {
    "trigger_phrases": [
      "go ahead",
      "your custom phrase"
    ]
  }
}
```

Or use `/add-trigger` slash command.

### New Task Types

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

### Change Blocked Tools

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

## Debugging

### Start Debug Mode
```bash
claude --debug
```

### Check Hook Logs
```bash
# DAIC Mode
cat .claude/state/daic-mode.json

# Current Task
cat .claude/state/current_task.json

# Test hooks manually
echo '{"prompt":"test"}' | .claude/hooks/user-messages.py
```

### Common Issues

#### Tools not blocked
```bash
# Check DAIC Mode
cat .claude/state/daic-mode.json
# Should be {"mode": "discussion"}
```

#### Trigger phrase doesn't work
```bash
# Check configuration
cat sessions/sessions-config.json | grep trigger_phrases
```

#### Task doesn't load
```bash
# Check task format
cat sessions/tasks/m-your-task.md
# Frontmatter correct? task: field present?
```

## Best Practices

### 1. **Always discuss before implementing**
- Let Claude explain the plan
- Review suggestions
- Give explicit approval with trigger phrase

### 2. **Use Context Manifest**
- Start context-gathering agent for new tasks
- Manifest prevents "Claude forgets pattern"
- Saves tokens and time

### 3. **Keep tasks small**
- Multiple small tasks better than one huge task
- Easier to review
- Better Git history

### 4. **Maintain Work Log**
- Agent writes automatically during compaction
- Manually add for important decisions
- Helps with task resume

### 5. **Tests before implementation**
- Follow TDD workflow from pdf-importer.md
- Write tests → See red → Implement → See green

### 6. **Don't commit runtime state**
- Runtime state in `.claude/state/` is session-specific
- Task files in `sessions/tasks/` are local work
- Only `TEMPLATE.md` should be in version control
- `.gitignore` already configured correctly

## Directory Structure

```
portfolio/
├── CLAUDE.md                    # Project standards
├── CC-SESSIONS.md              # This guide
├── sessions/
│   ├── CLAUDE.md               # Sessions system meta-docs
│   ├── sessions-config.json    # Configuration
│   ├── knowledge/
│   │   ├── claude-code/        # Framework knowledge
│   │   └── pdf-importer.md     # Project guides
│   ├── protocols/              # Workflow protocols
│   └── tasks/                  # Task files
│       ├── TEMPLATE.md
│       └── [task-files].md
└── .claude/
    ├── hooks/                  # Hook implementations
    ├── agents/                 # Agent definitions
    ├── commands/               # Slash commands
    ├── state/                  # Runtime state
    └── settings.local.json     # Local settings
```

## Further Help

### Documentation
- PDF Importer: `@sessions/knowledge/pdf-importer.md`
- Hooks Reference: `@sessions/knowledge/claude-code/hooks-reference.md`
- Subagents Guide: `@sessions/knowledge/claude-code/subagents.md`
- Sessions Meta: `@sessions/CLAUDE.md`

### Troubleshooting
1. Check `.claude/state/` files
2. Run `claude --debug`
3. Test hooks manually
4. Read hook logs in stderr

### Quick Reference

| Action | Command |
|--------|---------|
| New task | "Create a task for: ..." / "New Job: ..." |
| Start task | "Let's work on m-task-name" |
| Enable implementation | "go ahead" / "run" / "go" / "los geht's" |
| Return to discussion | `daic` |
| Compact context | "let's compact" then `/clear` |
| Run tests | `mvn verify -Plocal-dev -Dtest=...` |
| Check DAIC | `cat .claude/state/daic-mode.json` |
| Add trigger | `/add-trigger phrase` |
| Toggle API mode | `/api-mode` |
| Generate release notes | `/release-notes` |
| New PDF importer | `/pdf-importer BankName Test01.txt ...` |
| Debug PDF importer | `/pdf-debug BankName Test01.txt ...` |

---

**Tip:** Start with small tasks, use trigger phrases consistently, and let the agents do the heavy lifting!
