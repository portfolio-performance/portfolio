---
allowed-tools: Bash(jq:*), Bash(cat:*)
description: Toggle API mode (enables/disables automatic ultrathink)
---

!`current=$(cat $CLAUDE_PROJECT_DIR/sessions/sessions-config.json | jq -r '.api_mode'); new=$([ "$current" = "true" ] && echo "false" || echo "true"); jq ".api_mode = $new" $CLAUDE_PROJECT_DIR/sessions/sessions-config.json > /tmp/config.tmp && mv /tmp/config.tmp $CLAUDE_PROJECT_DIR/sessions/sessions-config.json && echo "API mode toggled: $current → $new"`

API mode configuration updated. The change will take effect in your next message.

- **API mode enabled**: Ultrathink disabled to save tokens (manual control with `[[ ultrathink ]]`)
- **API mode disabled**: Ultrathink automatically enabled for best performance (Max mode)