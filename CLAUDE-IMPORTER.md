# CLAUDE-IMPORTER.md

## PDF Importer Implementation and Modification Guide

This guide provides assistance for implementing and modifying PDF importers in Portfolio Performance. It serves as a reference for Claude.ai when working with the PDF import framework.

**Important: This guide supplements the main project documentation in `CLAUDE.md`. Always read both documents together for complete guidance on:**
- Project architecture and development setup
- Build commands and testing procedures
- Code style and conventions
- Core framework knowledge

Apply the general project standards from `CLAUDE.md` alongside the PDF-specific guidance below.

---

## 🚨 QUICK REFERENCE - START HERE

### Critical First Steps (Do These FIRST!)

1. **📖 ALWAYS read the test file first** - Use Read tool on the .txt file to see exact text
2. **📁 Check filename for method hint** - Filename indicates which method to use:
   - `Kauf01.txt` → use `addBuySellTransaction()`
   - `Verkauf03.txt` → use `addBuySellTransaction()`
   - `Dividende02.txt` → use `addDividendeTransaction()` (or extend it if needed)
3. **🔍 Find the correct DocumentType** - Grep for document header pattern
4. **🔄 Extend, don't create new** - Check if you can extend existing method (e.g., extend `addDividendeTransaction` instead of creating `addDividendeNettoTransaction`)
5. **✅ Use standard attributes** - `baseCurrency`, `termCurrency`, `exchangeRate`, `fxGross`
6. **📝 Copy test template** - Use the 7-assertion template below, don't copy from existing tests (they might be incomplete!)
7. **⚠️ Verify ALL 7 assertions** - Count them! Missing `countAccountTransfers` is the #1 mistake!

### Standard Exchange Rate Attributes (MEMORIZE THIS)
```
baseCurrency   - Base currency (EUR in EUR/CHF)
termCurrency   - Term currency (CHF in EUR/CHF)
exchangeRate   - Exchange rate value
fxGross        - Foreign currency gross value

❌ NEVER use: fxCurrency, forexCurrency
```

### Common Devisenkurs Patterns (Copy-Paste Ready)
```java
// Pattern 1: Devisenkurs (EUR/CHF) 0.986
.match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+).*$")

// Pattern 2: Devisenkurs 0.986 EUR / CHF
.match("^Devisenkurs (?<exchangeRate>[\\.,\\d]+) (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3})$")

// Pattern 3: Devisenkurs: EUR/CHF 0.986
.match("^Devisenkurs: (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) (?<exchangeRate>[\\.,\\d]+)$")
```

### Complete Exchange Rate Section (Copy-Paste Template)
```java
// Complete example from DkbPDFExtractor.java:269
.section("baseCurrency", "termCurrency", "exchangeRate", "gross", "currency").optional() //
.match("^Devisenkurs \\((?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3})\\) (?<exchangeRate>[\\.,\\d]+).*$") //
.match("^Kurswert (?<gross>[\\.,\\d]+)(\\-)? (?<currency>[\\w]{3})$") //
.assign((t, v) -> {
    var rate = asExchangeRate(v);
    type.getCurrentContext().putType(rate);

    var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
    var fxGross = rate.convert(asCurrencyCode(v.get("termCurrency")), gross);

    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
})
```

### 🔴 CRITICAL: Forex Section Ordering Rule

**⚠️ Section Order is IMPLEMENTATION-DEPENDENT - Always check existing extractor first!**

**Pattern: Amount FIRST, Forex AFTER:**
```java
// ✅ Used when forex is added as optional Unit to existing amount
.section("date")
.match("^Zahlbarkeitstag (?<date>...)$")
.assign(...)

.section("amount", "currency")
.match("^Ausmachender Betrag ...")  // ✅ Amount FIRST
.assign(...)

.section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional() //
.match("^Devisenkurs ...")  // ✅ Forex AFTER as optional enrichment
.assign(...)
```

**How to Choose the Right Order:**
1. 🔍 **FIRST: Check existing similar transactions in same extractor**
2. 📋 Copy the section ordering from similar transaction types
3. ✅ If both dividends exist: Follow their pattern
4. ⚠️ If unsure: Try Pattern 1 (Amount FIRST) - it's more common

### 7 Mandatory Test Assertions (🔴 CRITICAL - Copy This EVERY Time!)
```java
// ⚠️⚠️⚠️ Copy this ENTIRE block for EVERY test - NO EXCEPTIONS! ⚠️⚠️⚠️
// Missing even ONE assertion = incomplete test = will be rejected!
assertThat(errors, empty());                              // 1. Check no errors
assertThat(countSecurities(results), is(1L));            // 2. Count securities
assertThat(countBuySell(results), is(1L));               // 3. Count buy/sell
assertThat(countAccountTransactions(results), is(0L));   // 4. Count account txns
assertThat(countAccountTransfers(results), is(0L));      // 5. Count transfers ⚠️ OFTEN FORGOTTEN!
assertThat(results.size(), is(2));                       // 6. Total count
new AssertImportActions().check(results, "EUR");         // 7. Validate actions

// 🚨 IMPORTANT: countAccountTransfers is the MOST FORGOTTEN assertion!
// 🚨 ALWAYS include it, even when value is is(0L)!
```

### Required Imports for Forex Support (🔴 CRITICAL!)
```java
// ⚠️ When adding exchange rate/forex sections, ALWAYS verify these imports exist:
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import name.abuchen.portfolio.money.Money;

// Quick validation command:
// grep "checkAndSetGrossUnit" YourExtractor.java
// grep "import.*Money" YourExtractor.java
// Missing imports → "cannot find symbol" compilation error
```

### 5 Common Pitfalls (Avoid These!)

**Pitfall 1: Wrong DocumentType**
- ❌ Modifying Format01 when document matches Format02's DocumentType
- ✅ Grep for exact document header first: `grep -n "DOCUMENT_HEADER" ExtractorFile.java`

**Pitfall 2: Inconsistent Named Groups**
- ❌ Pattern `(?<forexCurrency>...)` but `.attributes("fxCurrency")`
- ✅ Named groups must EXACTLY match attributes: `(?<baseCurrency>...)` → `.attributes("baseCurrency")`

**Pitfall 3: Wrong Exchange Rate Attributes**
- ❌ Using `fxCurrency` (doesn't exist in standard patterns)
- ✅ Use `baseCurrency` / `termCurrency` (proven pattern from 58+ extractors)

**Pitfall 4: Security Name Too Restrictive**
- ❌ Pattern only matches "Registered Shares" but document has "Accum Shs Unhedged USD"
- ✅ Use flexible patterns: `(?<name>.*?)` or broader regex

**Pitfall 5: Incomplete Standard Test Assertions (🔴 MOST COMMON ERROR!)**
- ❌ Missing `countAccountTransfers` - the MOST FORGOTTEN assertion!
- ❌ Skipping ANY of the 7 mandatory assertions
- ❌ Looking at existing tests that might be incomplete - OLD tests may be wrong!
- ✅ ALWAYS copy the complete template from Quick Reference (lines 66-80)
- ✅ VERIFY: Does your test have ALL 7 assertions? Count them!
  ```java
  assertThat(errors, empty());                              // ⚠️ REQUIRED #1
  assertThat(countSecurities(results), is(1L));            // ⚠️ REQUIRED #2
  assertThat(countBuySell(results), is(1L));               // ⚠️ REQUIRED #3
  assertThat(countAccountTransactions(results), is(0L));   // ⚠️ REQUIRED #4
  assertThat(countAccountTransfers(results), is(0L));      // ⚠️ REQUIRED #5 ← OFTEN FORGOTTEN!
  assertThat(results.size(), is(2));                       // ⚠️ REQUIRED #6
  new AssertImportActions().check(results, "EUR");         // ⚠️ REQUIRED #7
  ```

---

## 📑 Table of Contents - Jump to Section

**Quick Start (Read First):**
- [QUICK REFERENCE - START HERE](#-quick-reference---start-here)
- [MODIFICATION WORKFLOW](#-modification-workflow---follow-this-sequence)

**Implementation Guides:**
- [Architecture and Entry Points](#architecture-and-entry-points)
- [Systematic Workflow for Modifications](#systematic-workflow-for-modifications)
- [Pattern Matching Methods](#pattern-matching-methods)
- [Test Case Implementation](#test-case-implementation)

**Reference Materials:**
- [Regex Patterns](#regex-patterns)
- [Reference Implementations](#reference-implementations)
- [Build and Test Commands](#build-and-test-commands)

---

## 📋 MODIFICATION WORKFLOW - Follow This Sequence

### 🎯 Practical Implementation Workflow (Test-Driven Development)

**Philosophy: Write Tests FIRST, then implement patterns incrementally**

```
PHASE 1: PREPARATION (Before Writing ANY Code)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ 1.1 READ TEST .TXT FILE FIRST ⚠️ MOST CRITICAL STEP!
      Use Read tool on: name.abuchen.portfolio.tests/.../Kauf03.txt
      └─ Identify exact text patterns:
         • Document header (for DocumentType)
         • Security name format
         • Date fields (Zahlbarkeitstag? Handelstag?)
         • Forex section (Devisenkurs format)
         • Amount format
         • Any intermediate lines between patterns

□ 1.2 GREP FOR EXISTING EXTRACTOR
      grep -n "class.*PDFExtractor" to find extractor file
      └─ Open and READ existing extractor code

□ 1.3 FIND SIMILAR TRANSACTION IN EXTRACTOR
      Search for existing Dividende/Kauf methods
      └─ Check their section ordering (Amount FIRST or Forex FIRST?)
      └─ Copy their pattern structure as template


PHASE 2: WRITE TESTS (Make Them Fail First!)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ 2.1 CREATE TEST METHOD
      Add test method to ExtractorTest.java
      └─ Use naming: testWertpapierKauf03() / testDividende03()
      └─ Copy 7-assertion template from CLAUDE-IMPORTER.md (line 97-107)
      └─ Fill in expected values from .txt file:
         • hasDate() - use Zahlbarkeitstag, NOT Devisenkursdatum!
         • hasShares() - exact shares from PDF
         • hasAmount() - Ausmachender Betrag
         • hasGrossValue() - before taxes/fees
         • hasForexGrossValue() - ONLY if forex exists

□ 2.2 ADD WithSecurityInEUR TEST (if forex exists)
      └─ Pre-create Security with EUR currency
      └─ NO hasForexGrossValue() in this test!
      └─ Add CheckCurrenciesAction validation with Account + setCurrencyCode()

□ 2.3 RUN TESTS - EXPECT FAILURES!
      mvn verify -Dtest=BankPDFExtractorTest
      └─ Tests SHOULD fail - that's correct!
      └─ Note error messages for next phase


PHASE 3: IMPLEMENT EXTRACTOR PATTERNS (Incrementally!)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ 3.1 CHECK/UPDATE DOCUMENTTYPE PATTERN
      Find: new DocumentType("...")
      └─ Does it match your document header?
      └─ If not: Extend pattern: "(OldPattern|NewPattern)"
      └─ Run test → Check if document is recognized

□ 3.2 CHECK/UPDATE BLOCK TRIGGER
      Find: new Block("^...$")
      └─ Does it match first line of your document?
      └─ Use flexible pattern (city name, not street!)
      └─ Example: "^.*Hammelburg.*$" instead of "^Am Marktplatz.*$"
      └─ Run test → Check if block triggers

□ 3.3 ADD/UPDATE SECURITY SECTION
      Check existing .section("isin", "name", "wkn", "currency")
      └─ Does pattern match your security name format?
      └─ Make pattern flexible: (?<name>.*) instead of exact match
      └─ Run test → Check if security is extracted

□ 3.4 ADD/UPDATE DATE SECTION
      Check existing .section("date")
      └─ Match correct date field (Zahlbarkeitstag, NOT Devisenkursdatum!)
      └─ Run test → Check if date is correct

□ 3.5 ADD/UPDATE SHARES SECTION
      Check existing .section("shares")
      └─ Match shares pattern from PDF
      └─ Run test → Check if shares are correct

□ 3.6 ADD/UPDATE AMOUNT SECTION
      Check existing .section("amount", "currency")
      └─ Match "Ausmachender Betrag" pattern
      └─ Run test → Check if amount is correct

□ 3.7 ADD FOREX SECTION (if needed)
      ⚠️ CHECK: Where do similar transactions put forex?
      └─ Amount FIRST → Add forex AFTER amount section
      └─ Forex FIRST → Add forex BEFORE amount section

      Add required imports:
      └─ import static ...ExtractorUtils.checkAndSetGrossUnit;
      └─ import ...money.Money;

      Pattern structure:
      └─ .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "gross").optional()
      └─ .match("^Devisenkurs ...")
      └─ .match("^Aussch.ttung ...") // or skip intermediate lines
      └─ .assign((t, v) -> {
             var rate = asExchangeRate(v);
             type.getCurrentContext().putType(rate);
             var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
             var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
             checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
         })

      Run test → Check if forex is extracted

□ 3.8 ADD TAXES/FEES SECTIONS
      Check existing tax/fee sections
      └─ Extend or add new patterns
      └─ Run test → Check if taxes/fees correct


PHASE 4: DEBUG COMMON FAILURES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
If test fails with specific errors:

❌ "NullPointerException"
   → Missing import or wrong named group
   → Check: grep "import.*checkAndSetGrossUnit"
   → Check: Named groups match .attributes() exactly

❌ "forexGrossValue = null" in WithSecurityInEUR test
   → Forex section in wrong position
   → Move forex section according to extractor pattern (Amount FIRST or Forex FIRST)

❌ "Pattern doesn't match"
   → Read .txt file again - are there intermediate lines?
   → Use .match() to skip intermediate lines: .match("^Devisenkursdatum .*$")
   → Or use .find() instead of .match() for flexibility

❌ "Date is wrong" (e.g., 2025-10-02 instead of 2025-10-01)
   → Using wrong date field!
   → Use Zahlbarkeitstag/Valutadatum, NOT Devisenkursdatum!

❌ "Security not found"
   → Pattern too restrictive
   → Make more flexible: (?<name>.*) instead of exact string

❌ "Block not triggered"
   → Block pattern too specific (street address instead of city)
   → Use flexible pattern: "^.*Hammelburg.*$"


PHASE 5: FINAL VERIFICATION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
□ 5.1 RUN ALL TESTS FOR THIS EXTRACTOR
      mvn verify -Dtest=BankPDFExtractorTest
      └─ All tests green? ✅ Continue
      └─ Any test red? → Fix before proceeding

□ 5.2 VERIFY TEST HAS ALL 7 ASSERTIONS
      Count assertions in your test:
      1. assertThat(errors, empty())
      2. assertThat(countSecurities(results), is(...))
      3. assertThat(countBuySell(results), is(...))
      4. assertThat(countAccountTransactions(results), is(...))
      5. assertThat(countAccountTransfers(results), is(...)) ← Often forgotten!
      6. assertThat(results.size(), is(...))
      7. new AssertImportActions().check(results, "EUR")

□ 5.3 VERIFY TEST ORDERING
      Tests must be sorted:
      1. By transaction type (Kauf → Verkauf → Dividende)
      2. By number (01, 02, 03)
      3. WithSecurityInEUR grouped with base test
```

### Step-by-Step Checklist (Quick Reference)

```
□ Step 1: READ TEST FILE FIRST
  └─ Use Read tool on test .txt file
  └─ Identify: Devisenkurs format, security name pattern, transaction type

□ Step 2: FIND CORRECT DOCUMENTTYPE
  └─ Grep for document header in extractor file
  └─ Identify which method handles this document (Format01, Format02, etc.)

□ Step 3: SEARCH FOR SIMILAR PATTERNS
  └─ Grep for similar keywords in extractor
  └─ Check if pattern already exists → EXTEND instead of duplicating

□ Step 4: VERIFY ATTRIBUTES
  └─ Check named groups match .attributes() exactly
  └─ Use standard names: baseCurrency, termCurrency, exchangeRate, fxGross

□ Step 5: ADD/MODIFY PATTERN
  └─ Use .section().optional() for optional sections
  └─ Use .optionalOneOf() for multiple formats
  └─ Follow line break pattern with //

□ Step 6: TEST INCREMENTALLY
  └─ Add one section at a time
  └─ Run test after each change
  └─ Use mvn verify -Dtest=TestClass#testMethod
```

### Decision Tree: What Type of Change?

```
Is this a NEW extractor?
├─ YES → Follow "New Extractor Implementation" (see below)
└─ NO → Is this a NEW transaction type for existing extractor?
    ├─ YES → ⚠️ WAIT! Check if you can EXTEND existing method first!
    │         Example: "Bardividende Netto" → Extend addDividendeTransaction
    │         ✅ Change DocumentType to: "(Ertragsgutschrift|Bardividende Netto)"
    │         ✅ Update pattern to match both formats
    │         ❌ DON'T create addDividendeNettoTransaction()
    │   └─ Only create NEW method if transaction type is fundamentally different
    └─ NO → Modifying existing pattern?
        ├─ Pattern doesn't match at all?
        │   └─ Check: Correct DocumentType? Correct Block? Named groups match?
        ├─ Exchange rate section missing?
        │   └─ Use standard attributes: baseCurrency, termCurrency, exchangeRate
        └─ Security name not matching?
            └─ Make pattern more flexible: (?<name>.*?)
```

### Quick Commands (Copy-Paste)

```bash
# Find DocumentType
grep -n "new DocumentType.*KEYWORD" ExtractorFile.java

# Find similar patterns
grep -n "PATTERN_TEXT" ExtractorFile.java -A 5 -B 5

# Run single test
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd -Dtest=TestClass#testMethod
```

---

## Architecture and Entry Points

### PDF-Import Architecture Overview

The PDF import system processes bank statements and broker documents to extract investment transactions:

```
PDF Import Process Flow:
PDF File → PDFInputFile → AbstractPDFExtractor → PDFImportAssistant → Client Model
           (Convert)      (Pattern Matching)    (Coordinate All)    (Results)
```

**Key Components:**
- **PDFInputFile**: Converts PDF to text using PDFBox3 (or PDFBox1 for legacy files)
- **AbstractPDFExtractor**: Base class providing transaction builders and bank identification
- **PDFImportAssistant**: Manages extractor registration and coordinates processing
- **PDFParser**: Provides DocumentType, Block, and Transaction pattern matching classes

### Basic Class Structure

```java
@SuppressWarnings("nls")
public class [BankName]PDFExtractor extends AbstractPDFExtractor
{
    public [BankName]PDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("[Bank Identifier]");

        addBuySellTransaction();
        addDividendeTransaction();
        // additional transaction types as needed
    }

    @Override
    public String getLabel()
    {
        return "[Bank Display Name]";
    }
}
```

### Core Classes and Utilities

- **`AbstractPDFExtractor`**: Base class for all PDF extractors with transaction builders and bank identification
- **`PDFParser.java`**: Core parser for PDF structure with `DocumentType`, `Block`, `Transaction` classes for pattern matching
- **`PDFImportAssistant.java`**: Manages PDF import process, coordinates multiple extractors, handles result collection
- **`PDFInputFile.java`**: Handles PDF file conversion to text using PDFBox and test case loading from resources
- **`ExtractorUtils.java`**: Essential utility functions for amount parsing and validation
- **`ExchangeRate.java`**: Currency conversion for multi-currency transactions with date-specific rates
- **`ExtractorMatchers.java`**: Modern matchers for test assertions (security, purchase, dividend, etc.)
- **`Money.java`**: Core money class with currency support for precise financial calculations

### PDF Import Process

The PDF import process works as follows:
1. **PDFImportAssistant** coordinates multiple extractors for each PDF file
2. **PDFInputFile** converts PDF files to text using PDFBox3 or PDFBox1 (legacy)
3. Each extractor applies regex patterns to extract transaction data
4. Results are collected and presented to the user for import

### Identifying the Correct Block

When modifying an extractor, **always identify which block** handles your document type:

1. **Check DocumentType first**: Match your document header (e.g., "RESUMEN DE ESTADO DE CUENTA") to the DocumentType pattern
2. **Find the corresponding block**: Each DocumentType has associated blocks (Format01, Format02, etc.)
3. **Verify block scope**:
   - `Format01` usually handles single-line patterns
   - `Format02` usually handles multi-line patterns with `setMaxSize(3-5)`
4. **Use grep to locate**: Search for the DocumentType pattern in the extractor file

**Example:**
```bash
# Find which DocumentType matches "RESUMEN"
grep -n "RESUMEN DE ESTADO DE CUENTA" TradeRepublicPDFExtractor.java
# Result: Line 1906 in addAccountStatementTransaction_Format02()
```

**Key Insight**: The DocumentType determines which method (and blocks) process your document. Always verify you're modifying the correct block for your document type.

### Common Pitfalls When Identifying Blocks

**⚠️ See "4 Common Pitfalls" in Quick Reference section at the top for complete details.**

Key reminders when identifying blocks:
- Always verify you're modifying the correct DocumentType/Block
- Named groups MUST exactly match `.attributes()` parameters
- Use standard exchange rate attributes: `baseCurrency`, `termCurrency`, `exchangeRate`, `fxGross`
- Read the actual test file first to see exact text patterns

### Key Entry Points for PDF Importer Development

Focus on these core entry points when implementing new PDF extractors:

- **`AbstractPDFExtractor.java`** - Base class for all PDF extractors with transaction builder methods
- **`PDFImportAssistant.java`** - **CRITICAL**: Registration point for new extractors (must add new extractors here)
- **`PDFInputFile.java`** - Entry point for PDF processing and test case loading from resources
- **`Client.java`** - Root domain model where extracted data integrates
- **`ExtractorUtils.java`** - Essential utilities for amount parsing and validation

## Implementation Standards and References

### Reference Implementations

Use these proven extractors as references for different implementation patterns:

#### **For Standard Implementations**
- **`BaaderBankPDFExtractor.java`** - Comprehensive modern patterns
- **`ScalableCapitalPDFExtractor.java`** - Clean multi-language approach
- **`SaxoBankPDFExtractor.java`** - Advanced pattern matching with `.oneOf()`

#### **For Complex Scenarios**
- **`ComdirectPDFExtractor.java`** - Document pairing and post-processing
- **`TargobankPDFExtractor.java`** - Transaction-tax document merging
- **`CetesDirectoPDFExtractor.java`** - Separate tax line processing
- **`BourseDirectPDFExtractor.java`** - ISIN-based cross-line linking

### General Approach

1. **Examine existing implementations** for similar bank/document types
2. **Start with simple patterns** and add complexity as needed
3. **Follow established conventions** for naming and structure
4. **Use proven regex patterns** from the reference implementations

## Systematic Workflow for Modifications

### Step 0: Read Test Files First (CRITICAL)

**⚠️ See "MODIFICATION WORKFLOW" section at the top for the complete checklist.**

This step is now part of the standard workflow checklist. Always start by reading the actual test file before making any modifications.

### Step 1: Identify Document Type and Block

Before modifying any extractor, locate the correct DocumentType and block:

```bash
# 1. Find DocumentType that matches your document header
grep -n "new DocumentType.*KEYWORD" ExtractorFile.java

# 2. Find similar patterns in the extractor
grep -n "YOUR_PATTERN_TEXT" ExtractorFile.java -A 5 -B 5

# 3. Check existing blocks and their structure
grep -n "depositRemovalBlock\|buySellBlock" ExtractorFile.java
```

### Step 2: Search for Similar Patterns Before Creating New Ones

**Always search for existing patterns that handle similar cases:**

```bash
# Example: Before adding "Transferencia", search for related patterns:
grep -n "Outgoing transfer for" ExtractorFile.java
```

**Key principle**: **Extend existing patterns rather than duplicating logic.**

If you find a similar pattern:
- Add your keyword to the existing regex: `(Überweisung|Transferencia)`
- Use `.optionalOneOf()` for variants
- Consolidate into existing sections

**⚠️ See "MODIFICATION WORKFLOW" section at the top for complete debugging checklist.**

### Step 3: Verify Block Assignment

- **Single-line transactions** → Usually Format01 blocks
- **Multi-line transactions** → Usually Format02/Format03 blocks
- Check `setMaxSize()` value to understand line expectations:
  ```java
  depositRemovalBlock.setMaxSize(4); // Expects up to 4 lines
  ```

## Implementation Methods by Reference

### For Simple Standard Processing
**Reference: `BaaderBankPDFExtractor.java`**
- Standard tax/fee sections within transactions
- Multi-language pattern matching (German/English)
- Comprehensive transaction type coverage

### For Multi-Language Documents
**Reference: `ScalableCapitalPDFExtractor.java` or `SaxoBankPDFExtractor.java`**
- Clean multi-language implementations
- Pattern matching for different document variants
- Multiple bank identifier handling

### For Standard Tax and Fee Processing
**Reference: `BaaderBankPDFExtractor.java`**

Standard approach using dedicated `addTaxesSectionsTransaction()` and `addFeesSectionsTransaction()` methods:

```java
// In main transaction method - call tax and fee sections
addTaxesSectionsTransaction(pdfTransaction, type);
addFeesSectionsTransaction(pdfTransaction, type);

// Dedicated tax sections method
private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
{
    transaction //
        // @formatter:off
        // Kapitalertragsteuer EUR 127,73 -
        // @formatter:on
        .section("tax", "currency").optional() //
        .match("^Kapitalertrags(s)?teuer (?<currency>[A-Z]{3}) (?<tax>[\\.,\\d]+) \\-$") //
        .assign((t, v) -> processTaxEntries(t, v, type))

        // @formatter:off
        // Span. Finanztransaktionssteuer EUR 1,97
        // @formatter:on
        .section("tax", "currency").optional() //
        .match("^.* Finanztransaktionssteuer (?<currency>[A-Z]{3}) (?<tax>[\\.,\\d]+)$") //
        .assign((t, v) -> processTaxEntries(t, v, type));
}

// Dedicated fee sections method
private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
{
    transaction //
        // @formatter:off
        // Provision EUR 0,21
        // @formatter:on
        .section("currency", "fee").optional() //
        .match("^Provision (?<currency>[A-Z]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
        .assign((t, v) -> processFeeEntries(t, v, type))

        // @formatter:off
        // Börsengebühren EUR 13,13
        // @formatter:on
        .section("currency", "fee").optional() //
        .match("^B.rsengeb.hren (?<currency>[A-Z]{3}) (?<fee>[\\.,\\d]+)( \\-)?$") //
        .assign((t, v) -> processFeeEntries(t, v, type));
}
```

**Key Principles:**
- **Separate Methods**: Create dedicated `addTaxesSectionsTransaction()` and `addFeesSectionsTransaction()` methods
- **Multiple Sections**: Use multiple `.section().optional()` for different tax and fee types
- **Utility Processing**: Use `processTaxEntries()` and `processFeeEntries()` for validation
- **Pattern Matching**: Match specific tax/fee line patterns with precise regex
- **Reusability**: Call these methods from multiple transaction types (buy/sell, dividend, etc.)

### For Document Pairing and Post-Processing
**Reference: `ComdirectPDFExtractor.java` or `TargobankPDFExtractor.java`**
- Multi-document transaction pairing
- Post-processing for document merging
- Currency conversion across separate documents
- Transaction-tax treatment coordination

### For Advanced Pattern Matching
**Reference: `SaxoBankPDFExtractor.java`**
- Complex `.oneOf()` section patterns for document variants
- DocumentContext currency extraction
- Multiple document format handling in single extractor

### For Special Implementation Patterns

#### Document Pairing and Merging (ComdirectPDFExtractor Pattern)
**Reference: `ComdirectPDFExtractor.java`**

Some banks provide transaction data across multiple documents that must be paired and merged:

```java
/**
 * @implNote ComDirect provides two documents for the transaction.
 *           The security transaction and the taxes treatment.
 *           Both documents are provided as one PDF or as two PDFs.
 *
 *           The security transaction includes the fees, but not the correct taxes
 *           and the taxes treatment includes all taxes (including withholding tax),
 *           but not all fees.
 *
 *           Therefore, we use the documents based on their function and merge both documents.
 */
```

**Key Implementation Features:**
- **Static Record Pattern**: `private static record TransactionTaxesPair(Item transaction, Item tax)`
- **Post-Processing**: Merge transaction and tax documents via `matchTransactionPair()`
- **Currency Conversion**: Handle currency differences between transaction and tax documents
- **Attribute Constants**: Use constants like `ATTRIBUTE_GROSS_TAXES_TREATMENT` for data passing
- **Document Cleanup**: Delete intermediate tax documents after merging

**When to Use:**
- Banks provide separate documents for transactions and tax treatments
- Need to merge fees from one document with taxes from another
- Currency conversion required between paired documents
- Data integrity requires combining multiple document sources

#### Pattern Matching Methods
- **`.oneOf()`** - Select from multiple pattern alternatives (one must match)
- **`.optionalOneOf()`** - Optional selection from multiple alternatives
- **`.section().optional()`** - Optional single pattern section
- **`.match()` vs `.find()`** - Anchored vs non-anchored pattern matching

### Pattern Consolidation Checklist

Before adding a new section, ask yourself these questions:

#### 1. Does a similar pattern already exist?

```bash
# Search for your key pattern text
grep -n "YOUR_KEY_PATTERN" ExtractorFile.java
```

If you find matches, review them to see if they can be extended.

#### 2. Can I extend an existing section?

Options for extending patterns:
- **Add to optional pattern**: `|(SEPA|Transferencia)?`
- **Use `.oneOf()` for variants**: Handle multiple document formats
- **Extend existing regex**: Add keywords to alternation groups

#### 3. Is the pattern truly unique?

Only create a new section if:
- ✅ Different transaction type (DEPOSIT vs REMOVAL vs BUY/SELL)
- ✅ Different document structure (different line count/order)
- ✅ Different data extraction logic (unique fields)
- ❌ Just a different keyword (extend existing pattern instead)

**Example - Good Consolidation:**

```java
// ❌ BEFORE: Separate sections (duplicated logic)
section -> section
    .match("^Überweisung Outgoing transfer for")

section -> section
    .match("^Transferencia Outgoing transfer for")

// ✅ AFTER: Consolidated (DRY principle)
section -> section
    .match("^(Überweisung|Transferencia) Outgoing transfer for")
```

**Real-world example from this implementation:**
```java
// Instead of creating a new section for "Transferencia", we extended:
.match("^(PayOut to transit" //
    + "|(SEPA Echtzeitüberweisung |Überweisung |Transferencia )?Outgoing transfer for"
```

#### Common Pattern Structures
```java
// Multiple document formats with .oneOf()
.oneOf(
    section -> section.attributes("name", "isin")
                     .match("^Format1: (?<name>.*) (?<isin>.*)$"),
    section -> section.attributes("name", "isin")
                     .match("^Format2: (?<isin>.*) (?<name>.*)$"))

// Optional tax section
.section("tax").optional()
.match("^.*TAX.*(?<tax>[\\d,]+)$")

// Optional selection between different tax formats
.optionalOneOf(
    section -> section.match("^.*Withholding Tax.*$"),
    section -> section.match("^.*Source Tax.*$"))
```

#### DocumentType Patterns

**Simple Document Types:**
```java
// Simple document identifier
new DocumentType("Wertpapierabrechnung")

// Multiple document types
new DocumentType("(Wertpapierabrechnung|Contract note)")

// Complex pattern with date
new DocumentType("Effektenabrechnung [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}")
```

**Advanced DocumentType with Line Breaks:**
```java
// Multiple transaction types with proper line breaks
new DocumentType("((Wertpapierabrechnung|Transaction Statement): " //
                + "(Kauf|Verkauf|Purchase|Sale)|Zeichnung)")

// With DocumentContext processing and line breaks
new DocumentType("Document Identifier", //
    documentContext -> documentContext //
        .section("currency") //
        .match("^.*Currency: (?<currency>[A-Z]{3}).*$") //
        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))))
```

**Complex DocumentType with Line-by-Line Processing:**
```java
// With line-by-line processing for complex scenarios (e.g., tax processing)
new DocumentType("Document Identifier", (context, lines) -> {
    // Custom processing for tax lines across document
    var helper = new TaxAmountTransactionHelper();
    context.putType(helper);

    // Process each line for tax extraction
    for (String line : lines) {
        if (line.matches(".*TAX.*")) {
            // Extract and process tax information
        }
    }
})
```

**Test DocumentType Patterns with Line Breaks:**
```java
// Modern test pattern with consistent line breaks
assertThat(results, hasItem(security( //
    hasIsin("FR0011550185"), hasWkn(null), hasTicker(null), //
    hasName("BNPP S&P500EUR ETF"), //
    hasCurrencyCode("EUR"))));

// Transaction assertion with line breaks
assertThat(results, hasItem(purchase( //
    hasDate("2024-12-10T09:04:28"), hasShares(173.00), //
    hasSource("ReleveDeCompte01.txt"), //
    hasNote(null), //
    hasAmount("EUR", 4978.30), hasGrossValue("EUR", 4973.82), //
    hasTaxes("EUR", 0.00), hasFees("EUR", 4.48))));
```


## Localization Methods by Reference

### For Standard Locales (German/English)
**Reference: `BaaderBankPDFExtractor.java`**
- Override `asAmount()` and `asShares()` methods
- Use `ExtractorUtils.convertToNumberLong()` with locale parameters

### For Special Locales
**Reference: `CetesDirectoPDFExtractor.java`**
- Mexican localization with `AdditionalLocales.MEXICO`
- Custom date format handling
- Locale-specific number formatting patterns

### For Swiss/Multi-Regional
**Reference: `BankSLMPDFExtractor.java`**
- Swiss localization (de_CH) patterns
- Regional number formatting variations

## Test Case Implementation

### Test Class Structure and Organization

**🔴 CRITICAL: Test Ordering Rules**
Tests MUST be sorted by:
1. **Transaction type** (Kauf → Verkauf → Dividende → Vorabpauschale → etc.)
2. **Numeric order** within each type (01, 02, 03, ...)
3. **WithSecurityInEUR tests** grouped together AFTER their base tests

**Correct Order Example:**
```
testWertpapierKauf01
testWertpapierVerkauf01
testWertpapierVerkauf02
testWertpapierVerkauf03          ← Numeric order!
testDividende01
testDividende02
testDividende02WithSecurityInEUR  ← Grouped together!
testDividende03
testDividende03WithSecurityInEUR
testVorabpauschale01
```

**❌ Wrong Order:**
```
testWertpapierKauf01
testDividende01                   ← Wrong! Verkauf should come first
testWertpapierVerkauf03          ← Wrong! Should be after Verkauf01, 02
testDividende03WithSecurityInEUR ← Wrong! Should be grouped with testDividende03
```

```java
@SuppressWarnings("nls")
public class [BankName]PDFExtractorTest
{
    @Test
    public void testTransaction01()
    {
        var extractor = new [BankName]PDFExtractor(new Client());
        List<Exception> errors = new ArrayList<>();

        var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TestFile01.txt"), errors);

        // ⚠️ MANDATORY: ALL 7 standard assertions in EXACT order - NEVER skip any!
        assertThat(errors, empty());                              // 1. Check no errors
        assertThat(countSecurities(results), is(1L));            // 2. Count securities
        assertThat(countBuySell(results), is(1L));               // 3. Count buy/sell transactions
        assertThat(countAccountTransactions(results), is(0L));   // 4. Count account transactions
        assertThat(countAccountTransfers(results), is(0L));      // 5. Count account transfers ⚠️
        assertThat(results.size(), is(2));                       // 6. Total result count
        new AssertImportActions().check(results, "EUR");         // 7. Validate import actions

        // Security check
        assertThat(results, hasItem(security( //
                        hasIsin("DE0007164600"), hasWkn("716460"), hasTicker(null), //
                        hasName("SAP SE"), //
                        hasCurrencyCode("EUR"))));

        // Transaction check
        assertThat(results, hasItem(purchase( //
                        hasDate("2024-12-10T09:04:28"), hasShares(100.00), //
                        hasSource("TestFile01.txt"), //
                        hasNote(null), //
                        hasAmount("EUR", 15000.00), hasGrossValue("EUR", 14950.50), //
                        hasTaxes("EUR", 24.50), hasFees("EUR", 25.00))));
    }
}
```

### Modern ExtractorMatchers

```java
import static name.abuchen.portfolio.datatransfer.ExtractorMatchers.*;

// Securities (always test first)
assertThat(results, hasItem(security( //
    hasIsin("FR0011550185"), //
    hasWkn(null), //
    hasTicker(null), //
    hasName("BNPP S&P500EUR ETF"), //
    hasCurrencyCode("EUR"))));

// Purchase Transactions
assertThat(results, hasItem(purchase( //
    hasDate("2025-08-05T16:35:33"), hasShares(7.00), //
    hasSource("ReleveDeCompte02.txt"), //
    hasNote(null), //
    hasAmount("EUR", 363.59), hasGrossValue("EUR", 361.15), //
    hasTaxes("EUR", 1.45), hasFees("EUR", 0.99))));

// Sale Transactions
assertThat(results, hasItem(sale( //
    hasDate("2025-08-06T14:25:10"), hasShares(5.00), //
    hasSource("Sale01.txt"), //
    hasNote("Partial sale"), //
    hasAmount("EUR", 510.50), hasGrossValue("EUR", 515.00), //
    hasTaxes("EUR", 2.50), hasFees("EUR", 2.00))));

// Dividend Transactions
assertThat(results, hasItem(dividend( //
    hasDate("2025-08-06T00:00"), hasShares(3.00), //
    hasSource("Dividende01.txt"), //
    hasNote(null), //
    hasAmount("EUR", 4.08), hasGrossValue("EUR", 4.80), //
    hasTaxes("EUR", 0.72), hasFees("EUR", 0.00))));

// Interest Transactions
assertThat(results, hasItem(interest( //
    hasDate("2025-08-01T00:00"), //
    hasSource("Interest01.txt"), //
    hasNote("Savings interest"), //
    hasAmount("EUR", 12.50), //
    hasTaxes("EUR", 3.12), hasFees("EUR", 0.00))));

// Deposit Transactions
assertThat(results, hasItem(deposit( //
    hasDate("2025-08-01T00:00"), //
    hasSource("Deposit01.txt"), //
    hasNote("Account funding"), //
    hasAmount("EUR", 1000.00), //
    hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));

// Removal/Withdrawal Transactions
assertThat(results, hasItem(removal( //
    hasDate("2025-08-15T00:00"), //
    hasSource("Withdrawal01.txt"), //
    hasNote("Account withdrawal"), //
    hasAmount("EUR", 500.00), //
    hasTaxes("EUR", 0.00), hasFees("EUR", 2.50))));

// Fee Transactions
assertThat(results, hasItem(fee( //
    hasDate("2025-08-31T00:00"), //
    hasSource("Fee01.txt"), //
    hasNote("Account maintenance fee"), //
    hasAmount("EUR", 15.00), //
    hasTaxes("EUR", 0.00), hasFees("EUR", 0.00))));
```

### Test Implementation Methods by Reference

#### For Standard Test Structure
**Reference: `BaaderBankPDFExtractorTest.java`**
- Modern ExtractorMatchers usage
- Comprehensive test coverage patterns
- Standard assertion structure

#### For Complex Multi-Document Tests
**Reference: `ComdirectPDFExtractorTest.java`**

##### Special Comdirect Test Patterns:

**Document Pairing Test Scenarios:**
- Transaction and tax treatment document merging
- Multiple PDF coordination testing
- Post-processing verification

**WithSecurityInEUR Pattern:**
```java
@Test
public void testWertpapierKauf04WithSecurityInEUR()
{
    // Pre-create security with specific currency in client
    var security = new Security("Amazon.com Inc. Registered Shares DL -,01", "EUR");
    security.setIsin("US0231351067");
    security.setWkn("906866");

    var client = new Client();
    client.addSecurity(security);  // Pre-populate client with security

    var extractor = new ComdirectPDFExtractor(client);

    List<Exception> errors = new ArrayList<>();

    var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf04.txt"), errors);

    assertThat(errors, empty());
    assertThat(countSecurities(results), is(0L));  // No new securities created
    assertThat(countBuySell(results), is(1L));
    assertThat(countAccountTransactions(results), is(0L));
    assertThat(countAccountTransfers(results), is(0L));
    assertThat(results.size(), is(1));
    new AssertImportActions().check(results, "EUR");

    // check buy sell transaction
    assertThat(results, hasItem(purchase( //
        hasDate("2020-04-16T18:33"), hasShares(2.00), //
        hasSource("Kauf04.txt"), //
        hasNote("Ord.-Nr.: -001 | R.-Nr.: "), //
        hasAmount("EUR", 4444.15), hasGrossValue("EUR", 4412.36), //
        hasTaxes("EUR", 0.00), hasFees("EUR", 18.93 + (13.90 / 1.080600)), //
        check(tx -> {
            var c = new CheckCurrenciesAction();
            var s = c.process((PortfolioTransaction) tx, new Portfolio());
            assertThat(s, is(Status.OK_STATUS));
        }))));
}
```

**Key Features:**
- **Pre-populated Securities**: Client contains security before extraction
- **Zero Security Count**: `countSecurities(results), is(0L)` indicates no new securities created
- **Currency Validation**: `CheckCurrenciesAction` ensures proper currency handling
- **Foreign Exchange**: Handle currency conversion in fees and amounts
- **Source File Reversal**: Test document processing order with `_SourceFilesReversed` variants

#### For Foreign Currency Test Cases (WithSecurityIn...() Pattern)
**Reference: `ComdirectPDFExtractorTest.java`, `ScalableCapitalPDFExtractorTest.java`**

Foreign currency tests validate proper handling when security currency differs from account currency:

```java
@Test
public void testTransactionWithSecurityInEUR()
{
    // Pre-create security with target currency (different from transaction currency)
    var security = new Security("Foreign Stock Name", "EUR");
    security.setIsin("US1234567890");
    security.setWkn("ABC123");

    var client = new Client();
    client.addSecurity(security);  // Important: Pre-populate to test currency conversion

    var extractor = new [Bank]PDFExtractor(client);

    List<Exception> errors = new ArrayList<>();

    var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "TestFile.txt"), errors);

    // Standard assertions including currency conversion tests
    assertThat(errors, empty());
    assertThat(countSecurities(results), is(0L));  // No new securities created
    assertThat(countBuySell(results), is(1L));
    assertThat(countAccountTransactions(results), is(0L));
    assertThat(countAccountTransfers(results), is(0L));
    assertThat(results.size(), is(1));
    new AssertImportActions().check(results, "EUR");

    // Transaction with currency validation
    assertThat(results, hasItem(purchase( //
        hasDate("2025-01-15T10:30"), hasShares(10.00), //
        hasSource("TestFile.txt"), //
        hasNote("Multi-currency transaction"), //
        hasAmount("EUR", 1000.00), hasGrossValue("EUR", 980.00), // Account currency
        hasForexGrossValue("USD", 850.00), // Original transaction currency
        hasTaxes("EUR", 15.00), hasFees("EUR", 5.00), //
        check(tx -> {
            // Validate proper currency handling
            var c = new CheckCurrenciesAction();
            var s = c.process((PortfolioTransaction) tx, new Portfolio());
            assertThat(s, is(Status.OK_STATUS));
        }))));
}
```

**Key Test Features:**
- **Pre-populated Security**: Security exists with target currency before extraction
- **Zero Security Count**: No new securities should be created (`countSecurities(results), is(0L)`)
- **Currency Validation**: Use `CheckCurrenciesAction` to validate multi-currency transactions
- **Forex Amounts**: Test both local and foreign currency amounts with `hasForexGrossValue()`
- **Exchange Rate Handling**: Verify proper currency conversion calculations

**⚠️ IMPORTANT: CheckCurrenciesAction Differs by Transaction Type!**

For **PortfolioTransaction** (Buy/Sell):
```java
check(tx -> {
    var c = new CheckCurrenciesAction();
    var s = c.process((PortfolioTransaction) tx, new Portfolio());
    assertThat(s, is(Status.OK_STATUS));
})
```

For **AccountTransaction** (Dividends/Interest/...):
```java
check(tx -> {
    var c = new CheckCurrenciesAction();
    var account = new Account();
    account.setCurrencyCode("EUR");  // ⚠️ MUST set currency for Account!
    var s = c.process((AccountTransaction) tx, account);
    assertThat(s, is(Status.OK_STATUS));
})
```

**Key Differences:**
- PortfolioTransaction uses `new Portfolio()` (no currency needed)
- AccountTransaction uses `new Account()` and **MUST** call `setCurrencyCode("EUR")` first!

### 🚨 Common Mistake: hasForexGrossValue in WithSecurityInEUR Tests

**CRITICAL:** Do NOT expect `hasForexGrossValue()` in WithSecurityInEUR tests!

**Base Test (security created from PDF in USD):**
```java
@Test
public void testDividende03() {
    var extractor = new BankPDFExtractor(new Client());  // No pre-existing security
    var results = extractor.extract(...);

    // Security will be created in USD (from PDF)
    assertThat(results, hasItem(security(hasCurrencyCode("USD"))));

    // ✅ Has forexGrossValue because security is USD, account is EUR
    assertThat(results, hasItem(dividend(
        hasAmount("EUR", 1.13),
        hasGrossValue("EUR", 1.38),
        hasForexGrossValue("USD", 1.63),  // ✅ Forex conversion happened
    )));
}
```

**WithSecurityInEUR Test (security pre-exists in EUR):**
```java
@Test
public void testDividende03WithSecurityInEUR() {
    var security = new Security("...", "EUR");  // Security currency = EUR
    security.setIsin("...");

    var client = new Client();
    client.addSecurity(security);  // Pre-populate with EUR security

    var extractor = new BankPDFExtractor(client);
    var results = extractor.extract(...);

    // ❌ NO hasForexGrossValue - security currency matches account currency!
    assertThat(results, hasItem(dividend(
        hasAmount("EUR", 1.13),
        hasGrossValue("EUR", 1.38),
        // ✅ NO hasForexGrossValue here - transaction is direct EUR-to-EUR
        hasTaxes("EUR", 0.25),
    )));
}
```

**Rule:**
- **Security currency = Account currency** → NO forexGrossValue (no conversion needed)
- **Security currency ≠ Account currency** → HAS forexGrossValue (conversion required)

**Real Example:** MerkurPrivatBankPDFExtractorTest.java:354-360 vs 388-400

#### For Multi-Language Test Cases
**Reference: `SaxoBankPDFExtractorTest.java`**
- Multi-language document testing
- Complex pattern matching verification
- DocumentContext testing patterns

## Regex Patterns

### Multi-Line Patterns with Intermediate Lines

**Common Issue:** Unexpected lines between expected patterns cause matches to fail

**Example Problem:**
```
Line 1: Devisenkurs EUR / USD  1,1802
Line 2: Devisenkursdatum 02.10.2025
Line 3: Ausschüttung 1,63 USD 1,38+ EUR
```

**✅ Correct Solution:**
```java
// If Devisenkursdatum is always present but you don't need it:
.match("^Devisenkurs (?<baseCurrency>[A-Z]{3}) \\/ (?<termCurrency>[A-Z]{3})[\\s]+(?<exchangeRate>[\\.,\\d]+)$")
.match("^Aussch.ttung (?<fxGross>[\\.,\\d]+) [A-Z]{3} (?<gross>[\\.,\\d]+)\\+ [A-Z]{3}$")
```

**Key Points:**
- ALWAYS read the complete test .txt file first
- Don't assume consecutive lines - check for intermediate content
- Use `.*$` to match lines you don't need data from
- Use `.find()` for flexibility if line order varies

### Proven Patterns (from CONTRIBUTING.md)

Based on real-world experience with PDF text extraction, these patterns **work well**:

```java
// Date patterns
"[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}"              // 01.01.1970
"[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}"          // 1.1.1970

// Time patterns
"[\\d]{2}\\:[\\d]{2}"                         // 12:01

// Security identifiers
"[A-Z]{2}[A-Z0-9]{9}[0-9]"                    // ISIN: IE00BKM4GZ66
"[A-Z0-9]{6}"                                 // WKN: A111X9
"[A-Z0-9]{5,9}"                               // Valoren: 1098758
"[A-Z0-9]{7}"                                 // SEDOL: B5B74S0
"[A-Z0-9]{9}"                                 // CUSIP: 11135F101

// Ticker symbols
"[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?"             // AAPL, BRK.B
"[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?"    // BTC, ETH-BTC

// Amount patterns
"[\\.,\\d]+"                                 // 751,68 (general)
"[\\.\\d]+,[\\d]{2}"                         // 751.68 (German with 2 decimals)
"[\\.'\\d]+"                                 // 74'120.00 (Swiss format)
"[\\.\\d\\s]+"                               // 20 120.00 (with spaces)

// Currency patterns
"[A-Z]{3}"                                    // EUR, USD
"\\p{Sc}"                                     // €, $, etc.
```

**Important Notes:**
- Use `.` (dot) for special characters like `äöüÄÖÜß` due to PDF conversion variations
- Escape special regex characters: `$^{[(|)]}*+?\`
- Use anchors `^` and `$` with `.match()`, but not with `.find()` (auto-added)

### Pattern Debugging

**⚠️ See "QUICK REFERENCE" section at the top for:**
- Standard exchange rate attributes (baseCurrency, termCurrency, exchangeRate, fxGross)
- Common Devisenkurs patterns (copy-paste ready)
- Common pitfalls checklist

**Debug Output for Complex Patterns:**
```java
// Use this to debug pattern matching issues:
System.out.println("Debug: Pattern match for line: " + line);
System.out.println("Debug: Groups found: " + matcher.groupCount());
for (int i = 1; i <= matcher.groupCount(); i++) {
    System.out.println("Group " + i + ": " + matcher.group(i));
}
```

### Block Identification Debugging

When patterns don't match or tests fail with "Unbekannter oder nicht unterstützter Buchungstyp":

#### 1. Verify the block is triggered

Add debug output to confirm the block pattern matches:
```java
System.out.println("Block triggered: depositRemovalBlock_Format02");
```

#### 2. Check maxSize for multi-line patterns

Multi-line patterns require sufficient `setMaxSize()`:
```java
depositRemovalBlock.setMaxSize(4); // Allows up to 4 lines to be matched
```

**Common issue**: Pattern spans 3 lines but maxSize is set to 1.

#### 3. Verify DocumentType context

Check which DocumentType processes your file:
```bash
# Find DocumentType that matches your document
grep -n "KONTO.*BERSICHT\|RESUMEN\|ACCOUNT STATEMENT" ExtractorFile.java
```

**Key insight**: If your document has "RESUMEN DE ESTADO DE CUENTA" but you're modifying Format01 (which handles "KONTOAUSZUG"), your pattern will never match!

#### 4. Common mistakes and solutions

| Problem | Cause | Solution |
|---------|-------|----------|
| Pattern never matches | Wrong block (Format01 vs Format02) | Verify DocumentType → block mapping |
| "Unbekannter Buchungstyp" error | Block not triggered | Check block regex matches first line |
| Multi-line pattern fails | Insufficient maxSize | Increase setMaxSize() |
| Pattern matches but no data | Wrong section attributes | Verify named groups match `.attributes()` |

#### 5. Debugging workflow

```bash
# Step 1: Find your DocumentType
grep -n "RESUMEN DE ESTADO DE CUENTA" ExtractorFile.java
# Result: Line 1906 → addAccountStatementTransaction_Format02()

# Step 2: Verify block patterns in that method
grep -n "new Block" ExtractorFile.java | grep -A 2 "1906"

# Step 3: Check if similar patterns exist
grep -n "Outgoing transfer for" ExtractorFile.java

# Step 4: Verify maxSize for multi-line patterns
grep -n "setMaxSize" ExtractorFile.java
```

## Reference Examples

### **Standard Reference Implementations**
Choose appropriate references based on complexity and requirements:

**Simple to Moderate Complexity:**
- **`BaaderBankPDFExtractor.java`** - Comprehensive modern patterns
- **`ScalableCapitalPDFExtractor.java`** - Multi-language support
- **`SaxoBankPDFExtractor.java`** - Advanced pattern matching

**Complex Requirements:**
- **`ComdirectPDFExtractor.java`** - Document pairing and post-processing
- **`TargobankPDFExtractor.java`** - Multi-document tax handling
- **`CetesDirectoPDFExtractor.java`** - Separate tax line processing

**All implementations include corresponding test files with modern ExtractorMatchers patterns.**

## Build and Test Commands

### Maven Commands

```bash
# Core tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd

# Single test
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd -Dtest=[TestClass]#[testMethod]
```

## PDFImportAssistant Integration

### Registration Requirement
**Every new PDF extractor must be registered in PDFImportAssistant.java:**

```java
// Add new extractor to the list in PDFImportAssistant constructor
extractors.add(new [BankName]PDFExtractor(client));
```

### Multiple Bank Identifiers
Use multiple identifiers when banks have different names or formats:
```java
addBankIdentifier("Scalable Capital GmbH");
addBankIdentifier("Scalable Capital Bank GmbH");
addBankIdentifier("Baader Bank Aktiengesellschaft");
addBankIdentifier("Baader Bank AG");
```

### Import Process Flow
1. PDFInputFile converts PDF files to text using PDFBox3 or PDFBox1 (legacy)
2. PDFImportAssistant runs all registered extractors against each file
3. Each extractor returns matching results or empty list
4. Results are collected, deduplicated, and presented to user

## Best Practices

### Code Style
- Use `var` keyword where possible
- No `$NON-NLS-1$` comments for internationalization suppression
- Follow existing naming conventions
- Use `@formatter:off` and `@formatter:on` to protect formatting
- **CRITICAL: Use `//` for line breaks** in long method chains to improve readability

#### Line Break Pattern with `//`
```java
pdfTransaction //
        .subject(() -> {
            var portfolioTransaction = new BuySellEntry();
            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
            return portfolioTransaction;
        })

        .section("isin", "name") //
        .documentContext("currency") //
        .match("^.*(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*(?<name>.*).*$") //
        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

        .section("shares") //
        .match("^.*QUANTITE.*\\+(?<shares>[\\d\\s]+(,[\\d]{2})?)$") //
        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
```
This pattern is used consistently across all PDF extractors for better readability.

### Performance
- Use `documentContext` for reusable data
- Minimize regex complexity
- Test with real PDF data
- Consider line-by-line processing for large documents

### Maintainability
- Document complex regex patterns
- Use meaningful variable names
- Implement toString() for debug classes
- Group related sections logically

### Testing

#### Date Field Selection Guide

**Critical:** Use the correct date field from PDF for transaction date

| PDF Field Name | Description | Use for Transaction Date? |
|---------------|-------------|---------------------------|
| **Zahlbarkeitstag** | Payment execution date | ✅ **YES** - Primary choice |
| **Valutadatum** | Value date (when money moves) | ✅ **YES** - Primary choice |
| **Handelstag** | Trade execution date | ✅ **YES** - For buy/sell |
| **Buchungstag** | Booking date | ✅ **YES** - Alternative |
| **Devisenkursdatum** | Exchange rate reference date | ❌ **NO** - Not transaction date! |
| **Ex-Tag** | Ex-dividend date | ❌ **NO** - Informational only |
| **Geschäftsjahr** | Fiscal year | ❌ **NO** - Not a date |

**Rule of Thumb:** Use the date when **money actually moved**, not reference/informational dates.

**Example from MerkurPrivatBank Dividende03.txt:**
```
Zahlbarkeitstag 01.10.2025      ← ✅ Use this for hasDate("2025-10-01")
Bestandsstichtag 17.09.2025     ← ❌ Don't use
Ex-Tag 18.09.2025               ← ❌ Don't use
Devisenkursdatum 02.10.2025     ← ❌ Don't use - this is just the FX rate date
```

**Test Assertion:**
```java
hasDate("2025-10-01T00:00")  // ✅ Correct - using Zahlbarkeitstag
hasDate("2025-10-02T00:00")  // ❌ Wrong - using Devisenkursdatum
```

#### Test Case Organization and Sorting
Follow consistent naming patterns for test methods and files:

**Standard Transaction Tests:**
1. `testWertpapierKauf01()` / `testBuy01()` - Basic purchase tests
2. `testWertpapierVerkauf01()` / `testSale01()` - Basic sale tests
3. `testDividende01()` / `testDividend01()` - Basic dividend tests

**Multi-Document Tests (with tax treatments):**
4. `testWertpapierKaufMitSteuerbehandlung01()` - Purchase with separate tax document
5. `testWertpapierVerkaufMitSteuerbehandlung01()` - Sale with separate tax document

**Foreign Currency Tests (WithSecurityInEUR):**
6. `testWertpapierKauf01WithSecurityInEUR()` / `testBuy01WithSecurityInEUR()` - Pre-populated security tests
7. `testDividende01WithSecurityInEUR()` / `testDividend01WithSecurityInEUR()` - Foreign currency dividend tests

**⚠️ WHEN to add WithSecurityInEUR tests:**
- ALWAYS required when PDF has foreign currency dividend (e.g., security in USD, account in EUR)
- Tests currency conversion with pre-populated security (security already exists with EUR currency)
- Ensures `CheckCurrenciesAction` validation works correctly
- Example: `testDividende02WithSecurityInEUR()` tests Dividende02.txt with security pre-created in EUR

**Document Order Tests:**
8. `testDividende01MitSteuerbehandlung_SourceFilesReversed()` - Test document processing order

**Specialized Tests:**
9. `testDepositoryFee()`, `testInterest()` - Account transaction tests
10. `testTaxRefund()`, `testTaxCorrection()` - Tax-specific tests

**Test File Naming Conventions:**
- **German**: `Kauf01.txt`, `Verkauf01.txt`, `Dividende01.txt`, `Zinsen01.txt`
- **English**: `Buy01.txt`, `Sale01.txt`, `Dividend01.txt`, `Interest01.txt`
- **Multi-language banks**: Use the primary language of the bank's documents
- **Tax documents**: `Steuerbehandlung01.txt`, `TaxTreatment01.txt`
- **Account statements**: `Kontoauszug01.txt`, `AccountStatement01.txt`

#### Test Implementation Guidelines
- Test all transaction types separately
- Use realistic test data that matches actual bank documents
- Test edge cases (missing taxes, different currencies, zero amounts)
- Anonymize personal data in test files (replace account numbers, names)
- Follow consistent naming conventions for both test methods and test files
- Include both simple and complex scenarios for comprehensive coverage
- Match the language of test files to the bank's document language

**⚠️ CRITICAL Test Assertion Rules:**
1. **ALWAYS include ALL 7 standard assertions** in this exact order - NO EXCEPTIONS:
   ```java
   assertThat(errors, empty());                              // 1. MANDATORY
   assertThat(countSecurities(results), is(XL));            // 2. MANDATORY
   assertThat(countBuySell(results), is(XL));               // 3. MANDATORY
   assertThat(countAccountTransactions(results), is(XL));   // 4. MANDATORY
   assertThat(countAccountTransfers(results), is(XL));      // 5. MANDATORY - often forgotten!
   assertThat(results.size(), is(XL));                      // 6. MANDATORY
   new AssertImportActions().check(results, "EUR");         // 7. MANDATORY
   ```
2. **Do NOT skip ANY assertion** - every single one is required for consistency
3. Adjust the numbers (XL) based on what your test expects, but NEVER omit any line
4. Copy the entire block from the template to avoid mistakes

### Security
- Never commit real personal data
- Anonymize account numbers and personal information
- Use placeholder data that maintains format structure

### Currency Handling
- Always specify currency explicitly
- Use Money class for amounts with currency
- Handle multi-currency transactions properly
- Consider exchange rate conversions

### Error Handling
- Validate extracted data
- Handle optional fields gracefully
- Provide meaningful error messages
- Log debug information for troubleshooting

### Documentation Standards
- Add `@formatter:off` and `@formatter:on` around PDF sample text
- Include PDF sample text as comments for each regex pattern
- Document the document type identifier clearly
- Use meaningful section names that reflect the PDF content

## Contributing Guidelines

Based on the current CONTRIBUTING.md:

1. **File Location**: PDF importers go in `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/`
2. **Test Location**: Test cases go in `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/`
3. **Naming Convention**: Use `[BankName]PDFExtractor` and `[BankName]PDFExtractorTest`
4. **Formatting**: Do NOT auto-format PDF extractor files - manually maintain formatting
5. **Test Files**: Use extracted text from PDF files, anonymize personal data
6. **Character Handling**: Use `.` for special characters due to PDF conversion variations

---

## 🎯 QUICK CHECKLIST - Use This Every Time

Before starting any modification:
- [ ] ✅ Read the test .txt file FIRST
- [ ] ✅ Grep for DocumentType to find correct block
- [ ] ✅ Search for similar patterns (extend, don't duplicate)
- [ ] ✅ Use standard attributes: `baseCurrency`, `termCurrency`, `exchangeRate`, `fxGross`
- [ ] ✅ Named groups MUST match `.attributes()` exactly
- [ ] ✅ Test incrementally after each change

**Most Common Mistakes to Avoid:**
1. ❌ Missing `countAccountTransfers` assertion (⚠️ THIS IS THE #1 ERROR!)
2. ❌ Using `fxCurrency` or `forexCurrency` (doesn't exist!)
3. ❌ Modifying wrong block (check DocumentType first)
4. ❌ Not reading test file first (leads to wrong regex)
5. ❌ Duplicating patterns instead of extending existing ones
6. ❌ Copying from existing tests that might be incomplete
7. ❌ Creating separate methods when you should extend existing ones

**Emergency Reference:**
- Standard attributes: `baseCurrency`, `termCurrency`, `exchangeRate`, `fxGross`
- Exchange rate method: `asExchangeRate(v)` → `type.getCurrentContext().putType(rate)`
- Gross conversion: `rate.convert(asCurrencyCode(v.get("termCurrency")), gross)`
- Final step: `checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext())`

---

## 📚 Real-World Examples and Lessons Learned

### Example 1: Extending vs. Creating New Methods (TradegateAG)

**Scenario:** New PDF file `Dividende03.txt` with "Bardividende Netto" header

**❌ Wrong Approach:**
```java
// DON'T create a new method!
private void addDividendeNettoTransaction() {
    final var type = new DocumentType("Bardividende Netto");
    // ... implementation
}
```

**✅ Correct Approach:**
```java
// EXTEND existing method by updating DocumentType pattern
private void addDividendeTransaction() {
    // Change from:
    final var type = new DocumentType("Ertragsgutschrift");

    // To:
    final var type = new DocumentType("(Ertragsgutschrift|Bardividende Netto)");

    // Update currency pattern to match both formats:
    .match("^(Aussch.ttung|Dividende) [\\.,\\d]+ (?<currency>[A-Z]{3}) pro St.ck$")
}
```

**Lesson:** Always check if you can extend existing patterns before creating new methods!

---

### Example 2: The countAccountTransfers Mistake

**Scenario:** All tests pass but missing mandatory assertion

**❌ Wrong - Missing Assertion:**
```java
@Test
public void testWertpapierVerkauf01() {
    // ...extract...
    assertThat(errors, empty());                              // ✓
    assertThat(countSecurities(results), is(1L));            // ✓
    assertThat(countBuySell(results), is(1L));               // ✓
    assertThat(countAccountTransactions(results), is(0L));   // ✓
    // ❌ MISSING: countAccountTransfers!
    assertThat(results.size(), is(2));                       // ✓
    new AssertImportActions().check(results, "EUR");         // ✓
}
```

**✅ Correct - All 7 Assertions:**
```java
@Test
public void testWertpapierVerkauf01() {
    // ...extract...
    assertThat(errors, empty());                              // 1
    assertThat(countSecurities(results), is(1L));            // 2
    assertThat(countBuySell(results), is(1L));               // 3
    assertThat(countAccountTransactions(results), is(0L));   // 4
    assertThat(countAccountTransfers(results), is(0L));      // 5
    assertThat(results.size(), is(2));                       // 6
    new AssertImportActions().check(results, "EUR");         // 7
}
```

**Lesson:** Even if tests pass, ALWAYS include ALL 7 assertions. Old tests might be incomplete!

---

### Example 3: Test Sorting (TradegateAG)

**❌ Wrong Order:**
```java
testWertpapierKauf01
testWertpapierVerkauf01
testDividende03                    // ❌ Wrong! Not in numeric order
testWertpapierVerkauf03           // ❌ Wrong! Should be after Verkauf01
testDividende03WithSecurityInEUR  // ❌ Wrong! Not grouped with other WithSecurityInEUR
```

**✅ Correct Order:**
```java
testWertpapierKauf01
testWertpapierVerkauf01
testWertpapierVerkauf02
testWertpapierVerkauf03           // ✓ Numeric order
testDividende01
testDividende02
testDividende02WithSecurityInEUR  // ✓ Grouped together
testDividende03                    // ✓ Numeric order
testDividende03WithSecurityInEUR  // ✓ Grouped together
testVorabpauschale01
```

**Lesson:** Follow strict ordering: transaction type → numeric → WithSecurityInEUR grouped!

---

### Example 4: Understanding Filename Hints

**Files:** `Verkauf03.txt` and `Dividende03.txt`

**Rule:** Filename indicates which method to use!
- `Verkauf03.txt` → Transaction type = "Verkauf" → Use `addBuySellTransaction()`
- `Dividende03.txt` → Transaction type = "Dividende" → Use `addDividendeTransaction()` (or extend it)

**Lesson:** The filename tells you which void method should process it!