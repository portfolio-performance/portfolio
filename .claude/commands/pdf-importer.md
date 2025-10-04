---
allowed-tools: Bash(python3:*), Bash(echo:*), Bash(cat:*), Bash(ls:*), Bash(grep:*)
argument-hint: "BankName Kauf01.txt Dividende01.txt ..."
description: Create task for implementing new PDF importer with test files
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
    print('Usage: /pdf-importer BankName Kauf01.txt Dividende01.txt ...')
    print('Example: /pdf-importer TradeRepublic Kauf01.txt Dividende01.txt Zinsen01.txt')
    sys.exit(1)

parts = args.split()
if len(parts) < 2:
    print('❌ Error: Need at least BankName and one test file')
    print('Usage: /pdf-importer BankName Kauf01.txt Dividende01.txt ...')
    sys.exit(1)

bank_name = parts[0]
test_files = parts[1:]

# Detect transaction types from filenames
transaction_types = set()
for tf in test_files:
    if 'kauf' in tf.lower() or 'buy' in tf.lower():
        transaction_types.add('Buy/Sell')
    if 'divid' in tf.lower():
        transaction_types.add('Dividends')
    if 'zins' in tf.lower() or 'interest' in tf.lower():
        transaction_types.add('Interest')
    if 'gebühr' in tf.lower() or 'fee' in tf.lower():
        transaction_types.add('Fees')

# Create task name
task_name = f'm-implement-{bank_name.lower()}-pdf'
branch_name = f'feature/implement-{bank_name.lower()}-pdf'
task_file = os.path.join(os.environ.get('CLAUDE_PROJECT_DIR', '.'), 'sessions', 'tasks', f'{task_name}.md')

# Check if task already exists
if os.path.exists(task_file):
    print(f'⚠️  Task already exists: {task_name}')
    print(f'To work on it: \"Let\\'s work on {task_name}\"')
    sys.exit(0)

# Create task content
today = date.today().strftime('%Y-%m-%d')
test_files_list = '\n'.join([f'- {tf}' for tf in test_files])
transaction_types_list = ', '.join(sorted(transaction_types)) if transaction_types else 'To be determined'

content = f'''---
task: {task_name}
branch: {branch_name}
status: pending
created: {today}
modules: [name.abuchen.portfolio.datatransfer.pdf, name.abuchen.portfolio.tests]
---

# Implement {bank_name} PDF Importer

## Problem/Goal
Create a new PDF importer for {bank_name} to support transaction import from PDF statements.

## Test Files
{test_files_list}

## Transaction Types
{transaction_types_list}

## Success Criteria
- [ ] Read ALL test .txt files FIRST to understand patterns
- [ ] Create {bank_name}PDFExtractor.java extending AbstractPDFExtractor
- [ ] Implement DocumentType detection
- [ ] Implement transaction methods for each type
- [ ] Use standard forex attributes (baseCurrency, termCurrency, exchangeRate, fxGross)
- [ ] Create {bank_name}PDFExtractorTest.java with tests for each file
- [ ] All 7 mandatory test assertions pass for EACH test
- [ ] Tests pass: `mvn verify -Plocal-dev -Dtest={bank_name}PDFExtractorTest`
- [ ] Code follows project standards (CLAUDE.md)

## Context Files
- @sessions/knowledge/pdf-importer.md                    # Complete TDD workflow guide
- @CLAUDE.md                                             # Project standards
- @portfolio-app/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/  # Extractor examples
- @portfolio-app/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/  # Test examples

## Reference Implementations
- @BaaderBankPDFExtractor.java                          # Standard patterns
- @ScalableCapitalPDFExtractor.java                     # Multi-language support
- @ComdirectPDFExtractor.java                           # Complex scenarios
- @DkbPDFExtractor.java                                 # Forex handling

## 5-Phase TDD Workflow (from pdf-importer.md)

### Phase 1: Preparation
1. Read ALL test .txt files completely
2. Identify document headers and structure patterns
3. Find similar existing extractor as reference
4. Plan DocumentType structure

### Phase 2: Write Tests First (RED)
1. Create {bank_name}PDFExtractorTest.java
2. Add @Test methods for each test file
3. Use 7-assertion template
4. Run tests → Should FAIL (red)

### Phase 3: Implement Incrementally (GREEN)
1. Create {bank_name}PDFExtractor.java
2. Add DocumentType with header pattern
3. Implement first transaction method
4. Run tests → Fix until GREEN
5. Repeat for each transaction type

### Phase 4: Debug Common Failures
- Check DocumentType header matches exactly
- Verify named groups match attributes exactly
- Ensure Security has currency assigned
- Verify all 7 assertions present

### Phase 5: Final Verification
- All tests green
- No compilation errors
- Code formatted consistently
- Commit with descriptive message

## Critical Reminders from pdf-importer.md
1. **READ test .txt file FIRST** - See exact text patterns before coding
2. **Check filename for method hint** - Kauf01.txt → addBuySellTransaction()
3. **Find correct DocumentType** - Grep for document header pattern
4. **Use standard attributes** - baseCurrency, termCurrency, exchangeRate, fxGross
5. **ALL 7 test assertions required** - countAccountTransfers is often forgotten!
6. **Security must have currency** - Every Security must always have an explicit currency
7. **Follow TDD** - Tests FIRST (red), then implementation (green)

## Standard Exchange Rate Attributes
```java
baseCurrency   // EUR in EUR/CHF
termCurrency   // CHF in EUR/CHF
exchangeRate   // Rate value
fxGross        // Foreign currency amount

❌ NEVER use: fxCurrency, forexCurrency
```

## 7 Mandatory Test Assertions Template
```java
@Test
public void testKauf01() throws IOException
{{
    var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), \"Kauf01.txt\"), errors);

    assertThat(errors, empty());                              // 1. Check no errors
    assertThat(countSecurities(results), is(1L));            // 2. Count securities
    assertThat(countBuySell(results), is(1L));               // 3. Count buy/sell
    assertThat(countAccountTransactions(results), is(0L));   // 4. Count account txns
    assertThat(countAccountTransfers(results), is(0L));      // 5. Count transfers ⚠️
    assertThat(results.size(), is(2));                       // 6. Total count
    new AssertImportActions().check(results, \"EUR\");         // 7. Validate actions

    // Additional specific assertions...
}}
```

## Work Log
- [{today}] Task created for implementing {bank_name} PDF importer
- Test files: {', '.join(test_files)}
- Transaction types: {transaction_types_list}
'''

# Write task file
with open(task_file, 'w', encoding='utf-8') as f:
    f.write(content)

print(f'✅ Task created: {task_name}')
print(f'📁 File: {task_file}')
print(f'🌿 Branch: {branch_name}')
print(f'')
print(f'📋 Test files to implement:')
for tf in test_files:
    print(f'   - {tf}')
print(f'')
print(f'🎯 Detected transaction types:')
print(f'   {transaction_types_list}')
print(f'')
print(f'Next steps:')
print(f'  1. Place anonymized test .txt files in test directory')
print(f'  2. Start work: \"Let\\'s work on {task_name}\"')
print(f'  3. Context-gathering agent will analyze similar extractors')
print(f'  4. Follow 5-phase TDD workflow from pdf-importer.md')
" && echo ""`

Task created successfully!

**What happens next:**
1. Task file created in `sessions/tasks/`
2. Ready to start with: `"Let's work on <task-name>"`
3. Context-gathering agent will find similar extractor patterns
4. Follow TDD workflow: Tests first (RED), then implementation (GREEN)
5. All 7 assertions required for each test method
