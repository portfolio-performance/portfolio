#!/usr/bin/env python3
"""User message hook to detect DAIC trigger phrases and special patterns."""
import json
import sys
import re
import os
try:
    import tiktoken
except ImportError:
    tiktoken = None
from shared_state import check_daic_mode_bool, set_daic_mode

# Load input
input_data = json.load(sys.stdin)
prompt = input_data.get("prompt", "")
transcript_path = input_data.get("transcript_path", "")
context = ""

# Get configuration (if exists)
try:
    from pathlib import Path
    from shared_state import get_project_root
    PROJECT_ROOT = get_project_root()
    CONFIG_FILE = PROJECT_ROOT / "sessions" / "sessions-config.json"
    
    if CONFIG_FILE.exists():
        with open(CONFIG_FILE, 'r') as f:
            config = json.load(f)
    else:
        config = {}
except:
    config = {}

# Default trigger phrases if not configured
DEFAULT_TRIGGER_PHRASES = ["make it so", "run that", "yert"]
trigger_phrases = config.get("trigger_phrases", DEFAULT_TRIGGER_PHRASES)

# Check if this is an /add-trigger command
is_add_trigger_command = prompt.strip().startswith('/add-trigger')

# Check API mode and add ultrathink if not in API mode (skip for /add-trigger)
if not config.get("api_mode", False) and not is_add_trigger_command:
    context = "[[ ultrathink ]]\n"

# Token monitoring
def get_context_length_from_transcript(transcript_path):
    """Get current context length from the most recent main-chain message in transcript"""
    try:
        import os
        if not os.path.exists(transcript_path):
            return 0
            
        with open(transcript_path, 'r') as f:
            lines = f.readlines()
        
        most_recent_usage = None
        most_recent_timestamp = None
        
        # Parse each JSONL entry
        for line in lines:
            try:
                data = json.loads(line.strip())
                # Skip sidechain entries (subagent calls)
                if data.get('isSidechain', False):
                    continue
                    
                # Check if this entry has usage data
                if data.get('message', {}).get('usage'):
                    entry_time = data.get('timestamp')
                    # Track the most recent main-chain entry with usage
                    if entry_time and (not most_recent_timestamp or entry_time > most_recent_timestamp):
                        most_recent_timestamp = entry_time
                        most_recent_usage = data['message']['usage']
            except json.JSONDecodeError:
                continue
        
        # Calculate context length from most recent usage
        if most_recent_usage:
            context_length = (
                most_recent_usage.get('input_tokens', 0) +
                most_recent_usage.get('cache_read_input_tokens', 0) +
                most_recent_usage.get('cache_creation_input_tokens', 0)
            )
            return context_length
    except Exception:
        pass
    return 0

# Check context usage and warn if needed (only if tiktoken is available)
if transcript_path and tiktoken and os.path.exists(transcript_path):
    context_length = get_context_length_from_transcript(transcript_path)
    
    if context_length > 0:
        # Calculate percentage of usable context (160k practical limit before auto-compact)
        usable_percentage = (context_length / 160000) * 100
        
        # Check for warning flag files to avoid repeating warnings
        from pathlib import Path
        PROJECT_ROOT = get_project_root()
        warning_75_flag = PROJECT_ROOT / ".claude" / "state" / "context-warning-75.flag"
        warning_90_flag = PROJECT_ROOT / ".claude" / "state" / "context-warning-90.flag"
        
        # Token warnings (only show once per session)
        if usable_percentage >= 90 and not warning_90_flag.exists():
            context += f"\n[90% WARNING] {context_length:,}/160,000 tokens used ({usable_percentage:.1f}%). CRITICAL: Run sessions/protocols/task-completion.md to wrap up this task cleanly!\n"
            warning_90_flag.parent.mkdir(parents=True, exist_ok=True)
            warning_90_flag.touch()
        elif usable_percentage >= 75 and not warning_75_flag.exists():
            context += f"\n[75% WARNING] {context_length:,}/160,000 tokens used ({usable_percentage:.1f}%). Context is getting low. Be aware of coming context compaction trigger.\n"
            warning_75_flag.parent.mkdir(parents=True, exist_ok=True)
            warning_75_flag.touch()

# DAIC keyword detection
current_mode = check_daic_mode_bool()

# Implementation triggers (only work in discussion mode, skip for /add-trigger)
if not is_add_trigger_command and current_mode and any(phrase in prompt.lower() for phrase in trigger_phrases):
    set_daic_mode(False)  # Switch to implementation
    context += "[DAIC: Implementation Mode Activated] You may now implement ONLY the immediately discussed steps. DO NOT take **any** actions beyond what was explicitly agreed upon. If instructions were vague, consider the bounds of what was requested and *DO NOT* cross them. When you're done, run the command: daic\n"

# Emergency stop (works in any mode)
if any(word in prompt for word in ["SILENCE", "STOP"]):  # Case sensitive
    set_daic_mode(True)  # Force discussion mode
    context += "[DAIC: EMERGENCY STOP] All tools locked. You are now in discussion mode. Re-align with your pair programmer.\n"

# Iterloop detection
if "iterloop" in prompt.lower():
    context += "You have been instructed to iteratively loop over a list. Identify what list the user is referring to, then follow this loop: present one item, wait for the user to respond with questions and discussion points, only continue to the next item when the user explicitly says 'continue' or something similar\n"

# Protocol detection - explicit phrases that trigger protocol reading
prompt_lower = prompt.lower()

# Context compaction detection
if any(phrase in prompt_lower for phrase in ["compact", "restart session", "context compaction"]):
    context += "If the user is asking to compact context, read and follow sessions/protocols/context-compaction.md protocol.\n"

# Task completion detection
if any(phrase in prompt_lower for phrase in ["complete the task", "finish the task", "task is done", 
                                               "mark as complete", "close the task", "wrap up the task"]):
    context += "If the user is asking to complete the task, read and follow sessions/protocols/task-completion.md protocol.\n"

# Task creation detection
if any(phrase in prompt_lower for phrase in ["create a new task", "create a task", "make a task",
                                               "new task for", "add a task", "new job", "create job"]):
    context += "If the user is asking to create a task, read and follow sessions/protocols/task-creation.md protocol.\n"

# Task switching detection
if any(phrase in prompt_lower for phrase in ["switch to task", "work on task", "change to task"]):
    context += "If the user is asking to switch tasks, read and follow sessions/protocols/task-startup.md protocol.\n"

# Task detection patterns (optional feature)
if config.get("task_detection", {}).get("enabled", True):
    task_patterns = [
        r"(?i)we (should|need to|have to) (implement|fix|refactor|migrate|test|research)",
        r"(?i)create a task for",
        r"(?i)add this to the (task list|todo|backlog)",
        r"(?i)we'll (need to|have to) (do|handle|address) (this|that) later",
        r"(?i)that's a separate (task|issue|problem)",
        r"(?i)file this as a (bug|task|issue)"
    ]
    
    task_mentioned = any(re.search(pattern, prompt) for pattern in task_patterns)
    
    if task_mentioned:
        # Add task detection note
        context += """
[Task Detection Notice]
The message may reference something that could be a task.

IF you or the user have discovered a potential task that is sufficiently unrelated to the current task, ask if they'd like to create a task file.

Tasks are:
• More than a couple commands to complete
• Semantically distinct units of work
• Work that takes meaningful context
• Single focused goals (not bundled multiple goals)
• Things that would take multiple days should be broken down
• NOT subtasks of current work (those go in the current task file/directory)

If they want to create a task, follow the task creation protocol.
"""

# Output the context additions
if context:
    output = {
        "hookSpecificOutput": {
            "hookEventName": "UserPromptSubmit",
            "additionalContext": context
        }
    }
    print(json.dumps(output))

sys.exit(0)
