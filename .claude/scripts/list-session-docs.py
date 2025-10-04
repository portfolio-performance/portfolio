#!/usr/bin/env python3
"""Comprehensive documentation audit for current session."""

import sys
sys.stdout.reconfigure(encoding='utf-8')
import subprocess
import json
import os
import glob
from pathlib import Path

# Get project directory
project_dir = os.environ.get('CLAUDE_PROJECT_DIR', '.')
if not project_dir or project_dir == '.':
    project_dir = os.getcwd()

# Get current task file to exclude it
try:
    with open(os.path.join(project_dir, '.claude', 'state', 'current_task.json')) as f:
        task_data = json.load(f)
        current_task = task_data.get('task')
        if current_task:
            current_task_file = f'sessions/tasks/{current_task}.md'
        else:
            current_task_file = None
except:
    current_task_file = None

# Get modified markdown files from git
result = subprocess.run(
    ['git', 'status', '--porcelain'],
    cwd=project_dir,
    capture_output=True,
    text=True
)

# Parse and filter files
modified_files = []
for line in result.stdout.strip().split('\n'):
    if not line:
        continue
    status = line[:2]
    filename = line[3:].strip()

    if filename.endswith('.md') and ('M' in status or 'A' in status) and '?' not in status:
        if current_task_file and filename == current_task_file:
            continue
        modified_files.append(filename)

# Check for undocumented slash commands
commands_dir = os.path.join(project_dir, '.claude', 'commands')
commands = []
if os.path.exists(commands_dir):
    commands = [os.path.basename(f)[:-3] for f in glob.glob(os.path.join(commands_dir, '*.md'))
                if not f.endswith('TEMPLATE.md')]

# Check if commands are documented in CC-SESSIONS.md
undocumented_commands = []
cc_sessions_file = os.path.join(project_dir, 'CC-SESSIONS.md')
if os.path.exists(cc_sessions_file):
    with open(cc_sessions_file, 'r', encoding='utf-8') as f:
        cc_content = f.read()
    for cmd in commands:
        if f'/{cmd}' not in cc_content:
            undocumented_commands.append(cmd)

# Check for undocumented agents
agents_dir = os.path.join(project_dir, '.claude', 'agents')
agents = []
if os.path.exists(agents_dir):
    agents = [os.path.basename(f)[:-3] for f in glob.glob(os.path.join(agents_dir, '*.md'))]

undocumented_agents = []
if os.path.exists(cc_sessions_file):
    for agent in agents:
        if agent not in cc_content:
            undocumented_agents.append(agent)

# Check trigger phrases consistency
config_file = os.path.join(project_dir, 'sessions', 'sessions-config.json')
config_triggers = []
if os.path.exists(config_file):
    try:
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
            config_triggers = config.get('config', {}).get('trigger_phrases', [])
    except:
        pass

# Check if triggers are documented
undocumented_triggers = []
if os.path.exists(cc_sessions_file):
    for trigger in config_triggers:
        # Check if trigger appears in docs (allowing for bold formatting)
        if trigger not in cc_content and f'**{trigger}**' not in cc_content:
            undocumented_triggers.append(trigger)

# Check for structural issues in CLAUDE.md
issues = []
claude_md = os.path.join(project_dir, 'CLAUDE.md')
if os.path.exists(claude_md):
    with open(claude_md, 'r', encoding='utf-8') as f:
        claude_content = f.read()

    required_sections = [
        'CC-Sessions Workflow System',
        'Quick Commands',
        'Build and Development Commands',
        'Architecture Overview'
    ]

    for section in required_sections:
        if section not in claude_content:
            issues.append(f'CLAUDE.md missing section: {section}')

# Output comprehensive report
print('=' * 70)
print('DOCUMENTATION AUDIT REPORT')
print('=' * 70)
print()

if modified_files:
    print('📝 MODIFIED DOCUMENTATION FILES:')
    print()
    for f in sorted(modified_files):
        print(f'  • {f}')
    print()
    print(f'Total: {len(modified_files)} file(s)')
    if current_task_file:
        print(f'(Excluded current task: {current_task_file})')
else:
    print('✓ No modified documentation files in this session.')
    if current_task_file:
        print(f'  (Current task file {current_task_file} was excluded)')

print()
print('-' * 70)
print()

# Report undocumented commands
if undocumented_commands:
    print('⚠️  UNDOCUMENTED SLASH COMMANDS:')
    print()
    for cmd in undocumented_commands:
        print(f'  • /{cmd} exists but not documented in CC-SESSIONS.md')
    print()
else:
    print('✓ All slash commands are documented')
    print()

# Report undocumented agents
if undocumented_agents:
    print('⚠️  UNDOCUMENTED AGENTS:')
    print()
    for agent in undocumented_agents:
        print(f'  • {agent} exists but not documented in CC-SESSIONS.md')
    print()
else:
    print('✓ All agents are documented')
    print()

# Report undocumented triggers
if undocumented_triggers:
    print('⚠️  UNDOCUMENTED TRIGGER PHRASES:')
    print()
    for trigger in undocumented_triggers:
        print(f'  • "{trigger}" in config but not in CC-SESSIONS.md')
    print()
else:
    print('✓ All trigger phrases are documented')
    print()

# Report structural issues
if issues:
    print('⚠️  STRUCTURAL ISSUES:')
    print()
    for issue in issues:
        print(f'  • {issue}')
    print()
else:
    print('✓ No structural issues found')
    print()

print('-' * 70)
print()

# Summary and recommendations
total_issues = len(undocumented_commands) + len(undocumented_agents) + len(undocumented_triggers) + len(issues)

if total_issues > 0 or modified_files:
    print('📋 RECOMMENDATION:')
    print()
    print('Invoke the service-documentation agent to:')
    print('  • Review modified documentation files')
    print('  • Update CLAUDE.md and CC-SESSIONS.md')
    print('  • Ensure all commands, agents, and triggers are documented')
    print('  • Verify structural completeness')
    print()
    print('See: @sessions/protocols/service-documentation.md')
    print()
    print('Would you like to proceed with documentation update? (Respond "yes")')
else:
    print('✅ Documentation is complete and up-to-date!')

print()
print('=' * 70)
