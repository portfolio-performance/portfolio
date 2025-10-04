# Task Startup Protocol

When starting work on a task (new or existing):

## 0. Git Setup

Check task frontmatter for branch name and modules list, then create/checkout branches.

### Check for Super-repo Structure

```bash
# Check if we're in a super-repo with submodules
if [ -f .gitmodules ]; then
  echo "Super-repo detected - will need to manage submodule branches"
else
  echo "Standard repository - simple branch management"
fi
```

### Branch Creation/Checkout

**For main tasks** (no letter suffix):
1. Start from main branch (safe to pull since we don't commit to main)
2. Pull latest changes from origin/main
3. Create the new task branch

**For subtasks** (branch contains letter suffix like tt1a-*):
1. First check current status to ensure no uncommitted changes
   - Run `git status` and address EVERY file shown
   - If super-repo: Check inside each submodule for uncommitted changes
   - Either commit or stash ALL changes, never leave files behind
2. Ensure the parent branch exists and checkout from it
3. If the parent branch has a remote, check if it needs updating:
   - Fetch the remote version
   - Compare local and remote to determine if pulling is needed
   - Alert if branches have diverged
4. Create the subtask branch from the parent branch

### Super-repo Submodule Management (IF .gitmodules exists)

**CRITICAL: Create matching branches in ALL affected submodules**
- Check the task frontmatter for the modules list
- For each module listed:
  - Navigate to that module directory
  - Check for uncommitted changes first
  - Checkout main and pull latest
  - Create a branch with the same name as the task branch
  - Return to the parent directory

Example: If working on tt1-login-ux-flow affecting io_web and io_user_model, create tt1-login-ux-flow branches in both submodules.

**Branch Discipline Rules:**
- Task frontmatter must list ALL modules that might be edited
- All listed modules MUST have matching task branches
- Before editing any file, verify the submodule is on the correct branch
- If a module needs to be added mid-task, create its branch immediately

> Note: If resuming work on an existing branch:
> - Check git status first for uncommitted work in super-repo AND all submodules
>   - Address EVERY file shown in `git status`, not just expected files
>   - Common missed files: CLAUDE.md, .claude/state files, test outputs
>   - Either commit ALL changes or explicitly discuss with user
> - Checkout the branch in the super-repo
> - For each affected submodule, navigate to it and checkout the matching branch
> - Only pull from remote if the remote branch exists

## 1. Update Task State

After creating/checking out branches, update the .claude/state/current_task.json file.

**CORRECT FORMAT (use these exact field names):**
```json
{
  "task": "task-name",        // Just the task name, NO path, NO .md extension
  "branch": "feature/branch", // The Git branch name (NOT "branch_name")
  "services": ["service1"],   // Array of affected services/modules
  "updated": "2025-08-27"     // Current date in YYYY-MM-DD format
}
```

**COMMON MISTAKES TO AVOID:**
- ❌ Using `"task_file"` instead of `"task"`
- ❌ Using `"branch_name"` instead of `"branch"`
- ❌ Including path like `"tasks/m-task.md"` instead of just `"m-task"`
- ❌ Including `.md` file extension

## 2. Load Task Context Manifest

Read the Context Manifest section from the task file.

If the Context Manifest is missing:
- For new tasks: Run context-gathering agent (should have been done during creation)
- For old tasks: Consider running context-gathering agent to create one

## 3. Load Context & Verify Branch State

Based on the manifest:
- Read the narrative explanation to understand how everything works
- Note technical reference details for implementation
- Check environmental requirements
- Note file locations for where changes will be made
- Change the frontmatter status to "in-progress"
- Add "started: YYYY-MM-DD" to the front matter

**IF SUPER-REPO: Verify All Module Branches**
Before starting work, confirm all modules are on correct branches:
```bash
# For each submodule in the task
for module in $(cat .claude/state/current_task.json | jq -r '.services[]'); do
  cd $module
  echo "$module is on branch: $(git branch --show-current)"
  cd ..
done
```

If any module that will be edited is not on the task branch, STOP and fix it first.

## 4. Verify Understanding

Before diving in:
- Understand the success criteria
- Review the work log for previous progress
- Check for blockers or gotchas
- Confirm approach with user if needed

## 5. Work Mode

Remember:
- Follow DAIC protocol (Discussion before Implementation)
- Use code-review agent after significant changes
- Update work log as you go
- Delegate heavy analysis to agents

## Example First Message

"I've loaded the context for [task]. Based on the manifest, I understand we're working on [summary]. The last work log entry shows [status]. Should I continue with [next step] or would you like to discuss the approach?"