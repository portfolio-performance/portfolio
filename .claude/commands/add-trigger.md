---
allowed-tools: Bash(python3:*), Bash(echo:*)
argument-hint: "trigger phrase"
description: Add a new trigger phrase for switching to implementation mode
---

!`python3 -c "import json,sys,os; phrase='$ARGUMENTS'; config_file=os.path.join(os.environ.get('CLAUDE_PROJECT_DIR','.'),'sessions','sessions-config.json'); phrase=phrase.strip(); sys.exit(1) if not phrase else None; sys.exit(1) if not os.path.exists(config_file) else None; data=json.load(open(config_file)); data.setdefault('trigger_phrases',[]); data['trigger_phrases']=list(set(data['trigger_phrases']+[phrase])); json.dump(data,open(config_file,'w'),indent=2)" && echo "The user just added the discussion mode trigger '$ARGUMENTS'. Tell them their trigger was added successfully and nothing else, then await their next message." || echo "The trigger phrase command failed. Tell the user it failed to add '$ARGUMENTS' and to check that sessions-config.json exists."`