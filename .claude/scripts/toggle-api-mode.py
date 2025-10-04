#!/usr/bin/env python3
"""Toggle API mode (enables/disables automatic ultrathink)."""

import sys
sys.stdout.reconfigure(encoding='utf-8')
import json
import os

# Get project directory
project_dir = os.environ.get('CLAUDE_PROJECT_DIR', '.')
if not project_dir or project_dir == '.':
    project_dir = os.getcwd()

# Path to config file
config_path = os.path.join(project_dir, 'sessions', 'sessions-config.json')

try:
    # Read current config
    with open(config_path, 'r', encoding='utf-8') as f:
        config = json.load(f)

    # Get current API mode (default to false if not present)
    current = config.get('api_mode', False)

    # Toggle it
    new = not current
    config['api_mode'] = new

    # Write back
    with open(config_path, 'w', encoding='utf-8') as f:
        json.dump(config, f, indent=2, ensure_ascii=False)
        f.write('\n')  # Add trailing newline

    # Output status
    print(f'API mode toggled: {str(current).lower()} → {str(new).lower()}')
    print()
    print('The change will take effect in your next message.')
    print()
    if new:
        print('- **API mode enabled**: Ultrathink disabled to save tokens (manual control with `[[ ultrathink ]]`)')
    else:
        print('- **API mode disabled**: Ultrathink automatically enabled for best performance (Max mode)')

except FileNotFoundError:
    print(f'Error: Config file not found at {config_path}', file=sys.stderr)
    sys.exit(1)
except json.JSONDecodeError as e:
    print(f'Error: Invalid JSON in config file: {e}', file=sys.stderr)
    sys.exit(1)
except Exception as e:
    print(f'Error toggling API mode: {e}', file=sys.stderr)
    sys.exit(1)
