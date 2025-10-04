---
allowed-tools: Bash(python3:*)
argument-hint: "BankName Kauf01.txt Dividende01.txt ..."
description: Create task for implementing new PDF importer with test files
---

!`python3 .claude/scripts/create-pdf-importer-task.py $ARGUMENTS`

Task created successfully!

**What happens next:**
1. Task file created in `sessions/tasks/`
2. Ready to start with: `"Let's work on <task-name>"`
3. Context-gathering agent will find similar extractor patterns
4. Follow TDD workflow: Tests first (RED), then implementation (GREEN)
5. All 7 assertions required for each test method
