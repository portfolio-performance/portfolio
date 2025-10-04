# Context Compaction Protocol

When you are instructed to compact context:

## 1. Run Maintenance Agents

Before compacting, delegate to agents:

1. **logging agent** - Update work logs in task file
   - Automatically receives full conversation context
   - Logs work progress and updates task status

2. **context-refinement agent** - Check for discoveries/drift
   - Reads transcript files automatically  
   - Will update context ONLY if changes found
   - Skip if task is complete

3. **service-documentation agent** - Update CLAUDE.md files
   - Only if service interfaces changed significantly
   - Include list of modified services

## 2. Verify/Update Task State

Ensure that .claude/state/current_task.json contains the correct current task, branch, and services.

## 3. Create Checkpoint

Document the current state:
- What was accomplished
- What remains to be done
- Any blockers or considerations
- Next concrete steps

## 4. Announce Readiness

Announce to the user that the agents have completed their work, the task state is updated, and we are ready to clear context.

## Note on Context Refinement

The context-refinement agent is speculative - it will only update the context manifest if genuine drift or new discoveries occurred. This prevents unnecessary updates while ensuring important findings are captured.