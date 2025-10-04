---
task: [priority]-[type]-[descriptive-name]
branch: feature/[name]|fix/[name]|experiment/[name]|none
status: pending|in-progress|completed|blocked
created: YYYY-MM-DD
started: YYYY-MM-DD  # Added when work begins
modules: [list of modules/packages involved]
---

# [Human-Readable Title]

## Problem/Goal
[Clear description of what we're solving/building and why it matters]

**Background:**
[Optional: Context about why this task exists]

**Constraints:**
[Optional: Any limitations or requirements to consider]

## Success Criteria
- [ ] Specific, measurable outcome (e.g., "Tests pass for 3 transaction types")
- [ ] Another concrete deliverable (e.g., "Branch merged to master")
- [ ] Quality criteria (e.g., "No compile errors, follows code style")

## Context Manifest
<!--
  STRONGLY RECOMMENDED: Run context-gathering agent after task creation
  The agent will populate this section with:
  - Narrative explanation of how systems work together
  - Technical reference (patterns, classes, methods)
  - Environmental requirements
  - File locations for implementation
-->

### Current Implementation
[How things work now - filled by context-gathering agent]

### Key Patterns & Code References
[Important patterns, classes, methods to use]

### Related Files
- @path/to/file.java:123-456  # Specific line ranges
- @path/to/example.java        # Reference implementation

### Technical Notes
[Special considerations, gotchas, dependencies]

## Context Files
<!-- Quick file references - full manifest above has details -->
- @sessions/knowledge/pdf-importer.md  # For PDF importer tasks
- @sessions/knowledge/...              # Other relevant guides

## User Notes
<!-- Any specific instructions, requirements, or preferences -->

## Work Log
<!--
  Updated during work and by logging agent during context compaction
  Format: [YYYY-MM-DD HH:MM] Action/observation/decision
-->
- [YYYY-MM-DD] Task created

## Next Steps
<!--
  Updated during context compaction or when pausing work
  Clear actionable items for resuming work
-->
- [ ] First action when resuming
- [ ] Next logical step

---

## Task Type Quick Reference

**For PDF Importer Tasks:**
- Review @sessions/knowledge/pdf-importer.md for 5-Phase TDD workflow
- Use `/pdf-importer BankName Test01.txt ...` for new extractors
- Use `/pdf-debug BankName Test01.txt ...` for existing extractors
- Remember: 7 mandatory test assertions required

**For Bug Fixes:**
- Document reproduction steps in Problem/Goal
- Include error messages/stack traces
- Note affected versions or environments

**For Features:**
- Reference similar existing implementations
- Consider backwards compatibility
- Plan for tests before implementation

**For Refactoring:**
- Document current design problems
- Outline new design approach
- Plan for maintaining test coverage