#!/usr/bin/env python3
"""Shared state management for Claude Code Sessions hooks."""
import json
from pathlib import Path
from datetime import datetime

# Get project root dynamically
def get_project_root():
    """Find project root by looking for .claude directory."""
    current = Path.cwd()
    while current.parent != current:
        if (current / ".claude").exists():
            return current
        current = current.parent
    # Fallback to current directory if no .claude found
    return Path.cwd()

PROJECT_ROOT = get_project_root()

# All state files in .claude/state/
STATE_DIR = PROJECT_ROOT / ".claude" / "state"
DAIC_STATE_FILE = STATE_DIR / "daic-mode.json"
TASK_STATE_FILE = STATE_DIR / "current_task.json"

# Mode description strings
DISCUSSION_MODE_MSG = "You are now in Discussion Mode and should focus on discussing and investigating with the user (no edit-based tools)"
IMPLEMENTATION_MODE_MSG = "You are now in Implementation Mode and may use tools to execute the agreed upon actions - when you are done return immediately to Discussion Mode"

def ensure_state_dir():
    """Ensure the state directory exists."""
    STATE_DIR.mkdir(parents=True, exist_ok=True)

def check_daic_mode_bool() -> bool:
    """Check if DAIC (discussion) mode is enabled. Returns True for discussion, False for implementation."""
    ensure_state_dir()
    try:
        with open(DAIC_STATE_FILE, 'r') as f:
            data = json.load(f)
            return data.get("mode", "discussion") == "discussion"
    except (FileNotFoundError, json.JSONDecodeError):
        # Default to discussion mode if file doesn't exist
        set_daic_mode(True)
        return True

def check_daic_mode() -> str:
    """Check if DAIC (discussion) mode is enabled. Returns mode message."""
    ensure_state_dir()
    try:
        with open(DAIC_STATE_FILE, 'r') as f:
            data = json.load(f)
            mode = data.get("mode", "discussion")
            return DISCUSSION_MODE_MSG if mode == "discussion" else IMPLEMENTATION_MODE_MSG
    except (FileNotFoundError, json.JSONDecodeError):
        # Default to discussion mode if file doesn't exist
        set_daic_mode(True)
        return DISCUSSION_MODE_MSG

def toggle_daic_mode() -> str:
    """Toggle DAIC mode and return the new state message."""
    ensure_state_dir()
    # Read current mode
    try:
        with open(DAIC_STATE_FILE, 'r') as f:
            data = json.load(f)
            current_mode = data.get("mode", "discussion")
    except (FileNotFoundError, json.JSONDecodeError):
        current_mode = "discussion"
    
    # Toggle and write new value
    new_mode = "implementation" if current_mode == "discussion" else "discussion"
    with open(DAIC_STATE_FILE, 'w') as f:
        json.dump({"mode": new_mode}, f, indent=2)
    
    # Return appropriate message
    return IMPLEMENTATION_MODE_MSG if new_mode == "implementation" else DISCUSSION_MODE_MSG

def set_daic_mode(value: str|bool):
    """Set DAIC mode to a specific value."""
    ensure_state_dir()
    if value == True or value == "discussion":
        mode = "discussion"
        name = "Discussion Mode"
    elif value == False or value == "implementation":
        mode = "implementation"
        name = "Implementation Mode"
    else:
        raise ValueError(f"Invalid mode value: {value}")
    
    with open(DAIC_STATE_FILE, 'w') as f:
        json.dump({"mode": mode}, f, indent=2)
    return name

# Task and branch state management
def get_task_state() -> dict:
    """Get current task state including branch and affected services."""
    try:
        with open(TASK_STATE_FILE, 'r') as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return {"task": None, "branch": None, "services": [], "updated": None}

def set_task_state(task: str, branch: str, services: list):
    """Set current task state."""
    state = {
        "task": task,
        "branch": branch,
        "services": services,
        "updated": datetime.now().strftime("%Y-%m-%d")
    }
    ensure_state_dir()
    with open(TASK_STATE_FILE, 'w') as f:
        json.dump(state, f, indent=2)
    return state

def add_service_to_task(service: str):
    """Add a service to the current task's affected services list."""
    state = get_task_state()
    if service not in state.get("services", []):
        state["services"].append(service)
        ensure_state_dir()
        with open(TASK_STATE_FILE, 'w') as f:
            json.dump(state, f, indent=2)
    return state