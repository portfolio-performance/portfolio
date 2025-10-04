# Service Documentation Protocol

## Purpose
Ensure project documentation (CLAUDE.md, CC-SESSIONS.md, protocols, knowledge files) remains synchronized with code changes and system modifications.

## When to Use
- **ALWAYS** during task completion when documentation was added/modified
- **ALWAYS** during context compaction when significant changes occurred
- **On demand** when user requests documentation review via `/session-docs`
- **When drift detected** between code and documentation

## Comprehensive Documentation Check

### 1. Structure Verification

#### CLAUDE.md (Root)
Check all sections are current:
- [ ] CC-Sessions Workflow System section
- [ ] Quick Commands list (all slash commands documented)
- [ ] API Mode documentation (if applicable)
- [ ] PDF Importer Transaction Detection
- [ ] Specialized Guides references
- [ ] Build and Development Commands
- [ ] Architecture Overview
- [ ] Module Dependency Graph
- [ ] Core Module Structure
- [ ] Key Entry Points with file paths and line numbers
- [ ] Data Model Core Concepts

#### CC-SESSIONS.md
Check all sections are current:
- [ ] Prerequisites
- [ ] Quick Start (task creation, working on tasks, DAIC workflow)
- [ ] Trigger Phrases (English and other languages)
- [ ] Slash Commands (all commands documented with examples)
- [ ] DAIC Mode explanation
- [ ] Context Compaction
- [ ] Agents Usage
- [ ] Quick Reference Table (complete and accurate)

#### sessions/CLAUDE.md
Check all sections are current:
- [ ] Sessions System Structure diagram
- [ ] Custom Commands list (all slash commands listed)
- [ ] Agents list (all agents documented)
- [ ] Hooks workflow description

#### sessions/knowledge/*
Check knowledge files:
- [ ] pdf-importer.md (5-Phase TDD, 7 assertions, forex attributes)
- [ ] claude-code/* framework documentation

#### sessions/protocols/*
Check protocol files:
- [ ] All protocols present and current
- [ ] No outdated instructions

### 2. Slash Commands Audit

For each command in `.claude/commands/`:
- [ ] Command file exists and is valid
- [ ] Documented in CC-SESSIONS.md Slash Commands section
- [ ] Listed in Quick Reference table
- [ ] Listed in sessions/CLAUDE.md structure
- [ ] Listed in CLAUDE.md Quick Commands (if user-facing)
- [ ] Has description and examples
- [ ] Tool permissions specified

### 3. Trigger Phrases Audit

Compare all locations:
- [ ] sessions/sessions-config.json (source of truth)
- [ ] CC-SESSIONS.md DAIC Mode section
- [ ] Quick Reference table

Verify:
- All trigger phrases from config are documented
- No documented phrases missing from config
- Language categorization is correct

### 4. Agents Audit

For each agent in `.claude/agents/`:
- [ ] Agent file exists and is complete
- [ ] Listed in sessions/CLAUDE.md structure
- [ ] Documented in CC-SESSIONS.md Agents section
- [ ] Usage instructions clear
- [ ] When-to-use guidelines present

### 5. Code Reference Verification

Scan all documentation for file references:
- [ ] File paths are valid (files exist)
- [ ] Line number references are accurate
- [ ] Module references are current
- [ ] No references to deleted/moved files

### 6. Configuration Consistency

Check configuration alignment:
- [ ] sessions/sessions-config.json matches documented behavior
- [ ] .claude/settings.local.json matches documented approvals
- [ ] Hook registrations match documentation
- [ ] Task prefixes documented match config

### 7. New Features Integration

For new features added this session:
- [ ] Added to appropriate documentation sections
- [ ] Cross-referenced where needed
- [ ] Examples provided
- [ ] Quick Reference updated

## Documentation Update Process

### Step 1: Scan Session Changes
```bash
# Get all modified markdown files
git status --porcelain | grep '\.md'

# List new slash commands
git diff --name-only HEAD | grep '.claude/commands/'

# List modified agents
git diff --name-only HEAD | grep '.claude/agents/'

# Check config changes
git diff sessions/sessions-config.json
```

### Step 2: Verify Structure Completeness

For each documentation file:
1. Read current content
2. Compare with template/expected structure
3. Identify missing sections
4. Check for outdated information

### Step 3: Cross-Reference Validation

Ensure consistency across:
- CLAUDE.md ↔ CC-SESSIONS.md
- CC-SESSIONS.md ↔ sessions/CLAUDE.md
- sessions/sessions-config.json ↔ all docs
- Slash commands ↔ all documentation locations

### Step 4: Update Documentation

Priority order:
1. **Critical**: Slash commands, trigger phrases, agents
2. **High**: Architecture changes, new features
3. **Medium**: Code references, examples
4. **Low**: Clarifications, formatting

### Step 5: Validate Updates

After updates:
- [ ] All cross-references work
- [ ] File paths are valid
- [ ] Line numbers are accurate
- [ ] No broken links
- [ ] Examples are testable

## Output Report Format

### Summary
```
Documentation Audit: [Date]

Files Checked: [count]
Issues Found: [count]
Updates Made: [count]
```

### Detailed Findings

#### Structure Gaps
- [ ] CLAUDE.md: Missing section X
- [ ] CC-SESSIONS.md: Outdated trigger phrase list

#### Command/Agent Discrepancies
- [ ] `/new-command` exists but not documented
- [ ] Agent X documented but file missing

#### Reference Errors
- [ ] File path invalid: path/to/file.py:123
- [ ] Module reference outdated: old_module.py

#### Configuration Drift
- [ ] Trigger phrase "xyz" in docs but not config
- [ ] Task prefix "abc-" in config but not documented

### Changes Made

List each file updated with brief description:
- `CLAUDE.md`: Added /api-mode to Quick Commands
- `CC-SESSIONS.md`: Updated trigger phrases list
- `sessions/CLAUDE.md`: Added new agent to structure

### Recommendations

Suggest improvements:
- Consider adding examples for command X
- Update architecture diagram for module Y
- Add cross-reference from A to B

## Integration with Other Protocols

### Task Completion
Before marking task complete:
1. Run service-documentation agent
2. Verify all new features documented
3. Update Work Log with documentation changes

### Context Compaction
During compaction:
1. Review all session documentation changes
2. Consolidate updates
3. Ensure consistency across files

### Command Documentation
When adding commands/triggers:
1. Follow command-documentation.md protocol
2. Then run service-documentation for verification

## Best Practices

### Do:
✅ Check all documentation locations
✅ Verify file paths and line numbers
✅ Update Quick Reference table
✅ Maintain cross-file consistency
✅ Provide specific examples
✅ Test documented commands

### Don't:
❌ Skip structural verification
❌ Assume documentation is current
❌ Update one file and forget others
❌ Leave broken references
❌ Document untested features
❌ Duplicate information across files

## Quality Gates

Documentation is complete when:
- [ ] All new commands/agents documented
- [ ] All modified features updated
- [ ] Quick Reference table current
- [ ] File references validated
- [ ] Cross-references consistent
- [ ] Configuration aligned
- [ ] Examples tested

## Automation Hints

The `/session-docs` command should:
1. List modified documentation files
2. Check for undocumented slash commands
3. Verify trigger phrase consistency
4. Validate agent documentation
5. Report structural gaps
6. Suggest service-documentation agent invocation

## Summary

**Golden Rule:**
> Documentation is a first-class artifact. Every code change that adds/modifies features MUST update all relevant documentation locations before task completion.

**Quick Checklist:**
1. Scan session changes
2. Verify structure completeness
3. Validate cross-references
4. Update all locations
5. Generate audit report
6. Validate updates
