---
allowed-tools: Bash(python3:*)
argument-hint: "BankName Kauf01.txt Buy01.txt ..."
description: Create task for debugging existing PDF importer with test files
---

!`python3 .claude/scripts/create-pdf-debug-task.py $ARGUMENTS`

Task created successfully!

**What happens next:**
1. Task file created in `sessions/tasks/`
2. Ready to start with: `"Let's work on <task-name>"`
3. Context-gathering agent will analyze the existing extractor
4. You approve the fix approach with trigger phrase
5. Tests run to verify the fix
