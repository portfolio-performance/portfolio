#!/usr/bin/env python3
"""Session start hook to initialize Claude Code Sessions context."""
import json
import os
import sys
import subprocess
from pathlib import Path
from shared_state import get_project_root, ensure_state_dir, get_task_state

# Get project root
PROJECT_ROOT = get_project_root()

# Get developer name from config
try:
    CONFIG_FILE = PROJECT_ROOT / 'sessions' / 'sessions-config.json'
    if CONFIG_FILE.exists():
        with open(CONFIG_FILE, 'r') as f:
            config = json.load(f)
            developer_name = config.get('developer_name', 'the developer')
    else:
        developer_name = 'the developer'
except:
    developer_name = 'the developer'

# Initialize context
context = f"""You are beginning a new context window with {developer_name}.

"""

# Quick configuration checks
needs_setup = False
quick_checks = []

# 1. Check if daic command exists
try:
    import shutil
    import os
    # Cross-platform command detection
    if os.name == 'nt':
        # Windows - check for .cmd or .ps1 versions
        if not (shutil.which('daic.cmd') or shutil.which('daic.ps1') or shutil.which('daic')):
            needs_setup = True
            quick_checks.append("daic command")
    else:
        # Unix/Mac - use which command
        if not shutil.which('daic'):
            needs_setup = True
            quick_checks.append("daic command")
except:
    needs_setup = True
    quick_checks.append("daic command")

# 2. Check if tiktoken is installed (required for subagent transcript chunking)
try:
    import tiktoken
except ImportError:
    needs_setup = True
    quick_checks.append("tiktoken (pip install tiktoken)")

# 3. Check if DAIC state file exists (create if not)
ensure_state_dir()
daic_state_file = PROJECT_ROOT / '.claude' / 'state' / 'daic-mode.json'
if not daic_state_file.exists():
    # Create default state
    with open(daic_state_file, 'w') as f:
        json.dump({"mode": "discussion"}, f, indent=2)

# 4. Clear context warning flags for new session
warning_75_flag = PROJECT_ROOT / '.claude' / 'state' / 'context-warning-75.flag'
warning_90_flag = PROJECT_ROOT / '.claude' / 'state' / 'context-warning-90.flag'
if warning_75_flag.exists():
    warning_75_flag.unlink()
if warning_90_flag.exists():
    warning_90_flag.unlink()

# 5. Check if sessions directory exists
sessions_dir = PROJECT_ROOT / 'sessions'
if sessions_dir.exists():
    # Check for active task
    task_state = get_task_state()
    if task_state.get("task"):
        task_file = sessions_dir / 'tasks' / f"{task_state['task']}.md"
        if task_file.exists():
            # Check if task status is pending and update to in-progress
            task_content = task_file.read_text()
            task_updated = False
            
            # Parse task frontmatter to check status
            if task_content.startswith('---'):
                lines = task_content.split('\n')
                for i, line in enumerate(lines[1:], 1):
                    if line.startswith('---'):
                        break
                    if line.startswith('status: pending'):
                        lines[i] = 'status: in-progress'
                        task_updated = True
                        # Write back the updated content
                        task_file.write_text('\n'.join(lines))
                        task_content = '\n'.join(lines)
                        break
            
            # Output the full task state
            context += f"""Current task state:
```json
{json.dumps(task_state, indent=2)}
```

Loading task file: {task_state['task']}.md
{"=" * 60}
{task_content}
{"=" * 60}
"""
            
            if task_updated:
                context += """
[Note: Task status updated from 'pending' to 'in-progress']
Follow the task-startup protocol to create branches and set up the work environment.
"""
            else:
                context += """
Review the Work Log at the end of the task file above.
Continue from where you left off, updating the work log as you progress.
"""
    else:
        # No active task - list available tasks
        tasks_dir = sessions_dir / 'tasks'
        task_files = []
        if tasks_dir.exists():
            task_files = sorted([f for f in tasks_dir.glob('*.md') if f.name != 'TEMPLATE.md'])
        
        if task_files:
            context += """No active task set. Available tasks:

"""
            for task_file in task_files:
                # Read first few lines to get task info
                with open(task_file, 'r') as f:
                    lines = f.readlines()[:10]
                    task_name = task_file.stem
                    status = 'unknown'
                    for line in lines:
                        if line.startswith('status:'):
                            status = line.split(':')[1].strip()
                            break
                    context += f"  • {task_name} ({status})\n"
            
            context += """
To select a task:
1. Update .claude/state/current_task.json with the task name
2. Or create a new task following sessions/protocols/task-creation.md
"""
        else:
            context += """No tasks found. 

To create your first task:
1. Copy the template: cp sessions/tasks/TEMPLATE.md sessions/tasks/[priority]-[task-name].md
   Priority prefixes: h- (high), m- (medium), l- (low), ?- (investigate)
2. Fill in the task details
3. Update .claude/state/current_task.json
4. Follow sessions/protocols/task-startup.md
"""
else:
    # Sessions directory doesn't exist - likely first run
    context += """Sessions system is not yet initialized.

Run the install script to set up the sessions framework:
.claude/sessions-setup.sh

Or follow the manual setup in the documentation.
"""

# If setup is needed, provide guidance
if needs_setup:
    context += f"""
[Setup Required]
Missing components: {', '.join(quick_checks)}

To complete setup:
1. Run the cc-sessions installer
2. Ensure the daic command is in your PATH

The sessions system helps manage tasks and maintain discussion/implementation workflow discipline.
"""

output = {
    "hookSpecificOutput": {
        "hookEventName": "SessionStart",
        "additionalContext": context
    }
}
print(json.dumps(output))