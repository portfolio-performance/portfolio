# Task Completion Protocol

When a task meets its success criteria:

## 1. Pre-Completion Checks

Verify before proceeding:
- [ ] All success criteria checked off in task file
- [ ] No unaddressed work remaining

**NOTE**: Do NOT commit yet - agents will modify files

## 2. Run Completion Agents

Delegate to specialized agents in this order:
```
1. code-review agent - Review all implemented code for security/quality
   Include: Changed files, task context, implementation approach
   
2. service-documentation agent - Update CLAUDE.md files 
   Include: List of services modified during task
   
3. logging agent - Finalize task documentation
   Include: Task completion summary, final status
```

## 3. Task Archival

After agents complete:
```bash
# Update task file status to 'completed'
# Move to done/ directory
mv sessions/tasks/[priority]-[task-name].md sessions/tasks/done/
# or for directories:
mv sessions/tasks/[priority]-[task-name]/ sessions/tasks/done/
```

## 4. Clear Task State

```bash
# Clear task state (but keep file)
cat > .claude/state/current_task.json << 'EOF'
{
  "task": null,
  "branch": null,
  "services": [],
  "updated": "$(date +%Y-%m-%d)"
}
EOF
```

## 5. Git Operations (Commit & Merge)

### Step 1: Review Unstaged Changes

```bash
# Check for any unstaged changes
git status

# If changes exist, present them to user:
# "I found the following unstaged changes: [list]
# Would you like to:
# 1. Commit all changes (git add -A)
# 2. Select specific changes to commit
# 3. Review changes first"
```

### Step 2: Determine Branch Type

1. Check the current branch name
2. Determine if this is a subtask or regular task:
   - **Subtask**: Branch name has pattern like `tt\d+[a-z]` (e.g., tt1a, tt234b)
   - **Regular task**: Branch name like `feature/*`, `fix/*`, or `tt\d+` (no letter)

### Step 3: Check for Super-repo Structure

```bash
# Check if we're in a super-repo with submodules
if [ -f .gitmodules ]; then
  echo "Super-repo detected with submodules"
  # Follow submodule commit ordering below
else
  echo "Standard repository"
  # Skip to step 4
fi
```

### Step 4: Commit and Merge (Conditional on Structure)

**IF SUPER-REPO**: Process from deepest submodules to super-repo

#### A. Deepest Submodules First (Depth 2+)
For any submodules within submodules:
1. Navigate to each modified deep submodule
2. Stage changes based on user preference from Step 1
3. Commit all changes with descriptive message
4. Merge based on branch type:
   - Subtask → merge into parent task branch
   - Regular task → merge into main
5. Push the merged branch

#### B. Direct Submodules (Depth 1)
For all modified direct submodules:
1. Navigate to each modified submodule
2. Stage changes based on user preference
3. Commit all changes with descriptive message
4. Merge based on branch type:
   - Subtask → merge into parent task branch
   - Regular task → merge into main
5. Push the merged branch

#### C. Super-repo (Root)
After ALL submodules are committed and merged:
1. Return to super-repo root
2. Stage changes based on user preference
3. Commit all changes with descriptive message
4. Merge based on branch type:
   - Subtask → merge into parent task branch
   - Regular task → merge into main
5. Push the merged branch

**IF STANDARD REPO**: Simple commit and merge
1. Stage changes based on user preference
2. Commit with descriptive message
3. Merge based on branch type (subtask → parent, regular → main)
4. Push the merged branch

### Special Cases

**Experiment branches:**
- Ask user whether to keep the experimental branch for reference
- If keeping: Just push branches without merging
- If not: Document findings first, then delete branches

**Research tasks (no branch):**
- No merging needed
- Just ensure findings are documented in task file

## 6. Select Next Task

Immediately after archival:
```bash
# List all tasks (simple and reliable)
ls -la sessions/tasks/

# Present the list to user
echo "Task complete! Here are the remaining tasks:"
# User can see which are .md files vs directories vs symlinks
```

User selects next task:
- Switch to task branch: `git checkout [branch-name]`
- Update task state: Edit `.claude/state/current_task.json` with new task, branch, and services
- Follow task-startup.md protocol

If no tasks remain:
- Celebrate completion!
- Ask user what they want to tackle next
- Create new task following task-creation.md

## Important Notes

- NEVER skip the agent steps - they maintain system integrity
- Task files in done/ serve as historical record
- Completed experiments should document learnings even if code is discarded
- If task is abandoned incomplete, document why in task file before archiving