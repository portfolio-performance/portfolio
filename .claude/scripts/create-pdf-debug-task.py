#!/usr/bin/env python3
"""Create a debugging task for an existing PDF importer."""

import sys
sys.stdout.reconfigure(encoding='utf-8')
import os
import json
from datetime import date

def main():
    # Parse arguments
    args = ' '.join(sys.argv[1:]).strip()
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

    # Detect transaction types from filenames
    transaction_types = set()
    for tf in test_files:
        tf_lower = tf.lower()

        # Buy/Sell (multilingual)
        if any(x in tf_lower for x in ['kauf', 'buy', 'achat', 'acquisto', 'compra']):
            transaction_types.add('Buy/Sell')
        if any(x in tf_lower for x in ['verkauf', 'sell', 'sale', 'vente', 'venta']):
            transaction_types.add('Buy/Sell')

        # Dividends (multilingual + variants)
        if any(x in tf_lower for x in ['divid', 'distribuzione']):
            transaction_types.add('Dividends')
        if 'storno' in tf_lower and 'divid' in tf_lower:
            transaction_types.add('Dividend Reversals')
        if 'reinvest' in tf_lower:
            transaction_types.add('Dividend Reinvestment')

        # Interest (multilingual)
        if any(x in tf_lower for x in ['zins', 'interest', 'rapport', 'resconto']):
            transaction_types.add('Interest')

        # Fees (multilingual)
        if any(x in tf_lower for x in ['gebuehr', 'fee', 'gebühr']):
            transaction_types.add('Fees')

        # Account Statements (multilingual)
        if any(x in tf_lower for x in ['kontoauszug', 'accountstatement', 'account statement', 'releve', 'transacciones', 'synthese']):
            transaction_types.add('Account Statements')

        # Deposits/Withdrawals
        if any(x in tf_lower for x in ['einzahlung', 'deposit', 'reglement']):
            transaction_types.add('Deposits')
        if any(x in tf_lower for x in ['auszahlung', 'removal', 'withdrawal', 'ausbuchung']):
            transaction_types.add('Withdrawals')

        # Deliveries/Transfers
        if any(x in tf_lower for x in ['depot', 'delivery', 'lieferung', 'erhalt', 'transfer', 'uebertrag']):
            transaction_types.add('Deliveries/Transfers')

        # Taxes
        if any(x in tf_lower for x in ['steuer', 'tax', 'vorabpauschale']):
            transaction_types.add('Taxes')

        # Crypto
        if 'crypto' in tf_lower:
            transaction_types.add('Crypto')

        # Corporate Actions
        if any(x in tf_lower for x in ['fusion', 'merger', 'spinoff', 'spin-off']):
            transaction_types.add('Corporate Actions (Mergers/SpinOffs)')
        if any(x in tf_lower for x in ['split']):
            transaction_types.add('Corporate Actions (Splits)')
        if any(x in tf_lower for x in ['kapital', 'capital']):
            transaction_types.add('Corporate Actions (Capital Changes)')
        if any(x in tf_lower for x in ['umtausch', 'exchange']):
            transaction_types.add('Corporate Actions (Exchanges)')
        if any(x in tf_lower for x in ['zwang', 'compulsory']):
            transaction_types.add('Corporate Actions (Forced Actions)')
        if 'rechten' in tf_lower or 'rights' in tf_lower:
            transaction_types.add('Corporate Actions (Rights)')

        # Savings Plans (multilingual)
        if any(x in tf_lower for x in ['sparplan', 'pianod', 'plande']):
            transaction_types.add('Savings Plans')

        # Payment Transactions
        if any(x in tf_lower for x in ['zahlung', 'payment', 'ueberweisung']):
            transaction_types.add('Payment Transactions')

        # Redemptions
        if any(x in tf_lower for x in ['tilgung', 'repayment']):
            transaction_types.add('Redemptions')

    # Create task name
    task_name = f'm-fix-{bank_name.lower()}-pdf'
    branch_name = f'fix/{bank_name.lower()}-pdf'
    project_dir = os.environ.get('CLAUDE_PROJECT_DIR', '.')
    task_file = os.path.join(project_dir, 'sessions', 'tasks', f'{task_name}.md')

    # Check if task already exists
    if os.path.exists(task_file):
        print(f'⚠️  Task already exists: {task_name}')
        print(f'To work on it: "Let\'s work on {task_name}"')
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

# Fix {bank_name} PDF Importer

## Problem/Goal
Debug and fix existing {bank_name}PDFExtractor to handle new transaction patterns.

## Test Files
{test_files_list}

## Transaction Types
{transaction_types_list}

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
new AssertImportActions().check(results, "EUR");         // 7. Validate actions
```

## Work Log
- [{today}] Task created for debugging {bank_name} PDF importer
- Test files: {', '.join(test_files)}
- Transaction types: {transaction_types_list}
'''

    # Write task file
    os.makedirs(os.path.dirname(task_file), exist_ok=True)
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
    print(f'🎯 Detected transaction types:')
    print(f'   {transaction_types_list}')
    print(f'')
    print(f'Next steps:')
    print(f'  1. Read the test files in the test directory')
    print(f'  2. Start work: "Let\'s work on {task_name}"')
    print(f'  3. I will analyze patterns and propose fixes')

if __name__ == '__main__':
    main()
