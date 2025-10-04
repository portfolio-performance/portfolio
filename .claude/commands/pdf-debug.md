---
allowed-tools: Bash(python3:*), Bash(echo:*), Bash(cat:*), Bash(ls:*), Bash(grep:*)
argument-hint: "BankName Kauf01.txt Buy01.txt ..."
description: Create task for debugging existing PDF importer with test files
---

!`python3 -c "
import sys
import os
import json
from datetime import date

# Parse arguments
args = '$ARGUMENTS'.strip()
if not args:
    print('❌ Error: Missing arguments')
    print('Usage: /pdf-debug BankName Kauf01.txt Buy01.txt ...')
    print('Example: /pdf-debug DKB Kauf01.txt Dividende01.txt')
    sys.exit(1)

parts = args.split()
if len(parts) < 2:
    print('❌ Error: Need at least BankName and one test file')
    print('Usage: /pdf-debug BankName Kauf01.txt Buy01.txt ...')
    sys.exit(1)

bank_name = parts[0]
test_files = parts[1:]

# Create task name
task_name = f'm-fix-{bank_name.lower()}-pdf'
branch_name = f'fix/{bank_name.lower()}-pdf'
task_file = os.path.join(os.environ.get('CLAUDE_PROJECT_DIR', '.'), 'sessions', 'tasks', f'{task_name}.md')

# Check if task already exists
if os.path.exists(task_file):
    print(f'⚠️  Task already exists: {task_name}')
    print(f'To work on it: \"Let\\'s work on {task_name}\"')
    sys.exit(0)

# Create task content
today = date.today().strftime('%Y-%m-%d')
test_files_list = '\n'.join([f'- {tf}' for tf in test_files])

content = f'''---
task: {task_name}
branch: {branch_name}
status: pending
created: {today}
modules: [name.abuchen.portfolio.datatransfer.pdf, name.abuchen.portfolio.tests]
---

# Fix {bank_name} PDF Importer

## Problem/Goal
Debug and fix existing {bank_name}PDFExtractor to handle new transaction patterns.

## Test Files
{test_files_list}

## Success Criteria
- [ ] Read all test .txt files FIRST to understand patterns
- [ ] Identify which DocumentType needs modification
- [ ] Extend existing transaction methods (don't create new ones)
- [ ] All 7 mandatory test assertions pass
- [ ] Tests pass: `mvn verify -Plocal-dev -Dtest={bank_name}PDFExtractorTest`
- [ ] Code follows project standards

## Context Files
- @sessions/knowledge/pdf-importer.md                    # Complete implementation guide
- @CLAUDE.md                                             # Project standards
- @portfolio-app/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/{bank_name}PDFExtractor.java
- @portfolio-app/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/{bank_name}PDFExtractorTest.java

## Critical Reminders from pdf-importer.md
1. **READ test .txt file FIRST** - See exact text patterns
2. **Check filename for method hint** - Kauf01.txt → addBuySellTransaction()
3. **Find correct DocumentType** - Grep for document header pattern
4. **Extend, don't create new** - Check if you can extend existing methods
5. **Use standard attributes** - baseCurrency, termCurrency, exchangeRate, fxGross
6. **ALL 7 test assertions required** - countAccountTransfers is often forgotten!
7. **Security must have currency** - Every Security must always have an explicit currency

## Standard Exchange Rate Attributes
```java
baseCurrency   // EUR in EUR/CHF
termCurrency   // CHF in EUR/CHF
exchangeRate   // Rate value
fxGross        // Foreign currency amount

❌ NEVER use: fxCurrency, forexCurrency
```

## 7 Mandatory Test Assertions
```java
assertThat(errors, empty());                              // 1. Check no errors
assertThat(countSecurities(results), is(1L));            // 2. Count securities
assertThat(countBuySell(results), is(1L));               // 3. Count buy/sell
assertThat(countAccountTransactions(results), is(0L));   // 4. Count account txns
assertThat(countAccountTransfers(results), is(0L));      // 5. Count transfers ⚠️
assertThat(results.size(), is(2));                       // 6. Total count
new AssertImportActions().check(results, \"EUR\");         // 7. Validate actions
```

## Work Log
- [{today}] Task created for debugging {bank_name} PDF importer
- Test files: {', '.join(test_files)}
'''

# Write task file
with open(task_file, 'w', encoding='utf-8') as f:
    f.write(content)

print(f'✅ Task created: {task_name}')
print(f'📁 File: {task_file}')
print(f'🌿 Branch: {branch_name}')
print(f'')
print(f'📋 Test files to analyze:')
for tf in test_files:
    print(f'   - {tf}')
print(f'')
print(f'Next steps:')
print(f'  1. Read the test files in the test directory')
print(f'  2. Start work: \"Let\\'s work on {task_name}\"')
print(f'  3. I will analyze patterns and propose fixes')
" && echo ""`

Task created successfully!

**What happens next:**
1. Task file created in `sessions/tasks/`
2. Ready to start with: `"Let's work on <task-name>"`
3. Context-gathering agent will analyze the existing extractor
4. You approve the fix approach with trigger phrase
5. Tests run to verify the fix
