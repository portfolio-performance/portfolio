# Contributing to Portfolio Performance

Thank you for considering contributing to Portfolio Performance! This guide will help you get started with development.

## Contents

- [Quick Start](#quick-start)
- [Development Setup](#development-setup)
  - [Eclipse IDE Setup](#eclipse-ide-setup)
- [Project Architecture](#project-architecture)
- [Building the Project](#building-the-project)
  - [With Eclipse](#with-eclipse)
  - [With Maven](#with-maven)
- [Contributing Code](#contributing-code)
  - [Before You Start](#before-you-start)
  - [Writing Code](#writing-code)
  - [Submitting Changes](#submitting-changes)
- [Specialized Development Guides](#specialized-development-guides)
  - [PDF Importers](#pdf-importers)
  - [Interactive Flex Query Importers](#interactive-flex-query-importers)
  - [Translations](#translations)
  - [Trade Calendars](#trade-calendars)
  - [Images and Icons](#images-and-icons)
- [Getting Help](#getting-help)
- [Appendix](#appendix)
  - [Regular Expression Reference](#regular-expression-reference)
  - [Color Code Reference](#color-code-reference)

## Quick Start

Portfolio Performance is an Eclipse RCP application for tracking and analyzing investment portfolios. To contribute:

1. **Fork and clone** the repository
2. **Set up your development environment** (Eclipse IDE required for RCP development)
3. **Check the architecture** to understand the codebase structure
4. **Make your changes** following our coding standards
5. **Test thoroughly** and submit a pull request

**Helpful Resources:**
- **[Forum (German)](https://forum.portfolio-performance.info/)** - Community discussions
- **[GitHub Issues](https://github.com/portfolio-performance/portfolio/issues)** - Bug reports and feature requests

## Development Setup

### Eclipse IDE Setup

Eclipse IDE is **required** for Portfolio Performance development as the application is built on Eclipse RCP (Rich Client Platform).

**Prerequisites:**
- **Java 21** - Required minimum version ([Download from Azul](https://www.azul.com/downloads/))
- **[Eclipse IDE for RCP and RAP Developers](https://www.eclipse.org/downloads/packages/)** - Download the RCP package

**Essential Eclipse Plugins:**

Install via Eclipse Marketplace (drag and drop the *Install* button to your workspace):
- [Infinitest](https://marketplace.eclipse.org/content/infinitest) - Continuous testing
- [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor) - Translation file editing
- [Checkstyle Plug-In](https://marketplace.eclipse.org/content/checkstyle-plug) - Code style checking
- [SonarQube for IDE](https://marketplace.eclipse.org/content/sonarqube-ide) - Code quality analysis
- [Launch Configuration DSL](https://marketplace.eclipse.org/content/launch-configuration-dsl) - Launch configuration management

**Install from Eclipse Simultaneous Release:**

`Menu` → `Help` → `Install New Software` → Select `Latest Eclipse Simultaneous Release`:
- **M2E PDE Integration** (under General Purpose Tools)
- **Eclipse e4 Tools Developer Resources** (under General Purpose Tools)

**Configure Eclipse:**

`Menu` → `Window` → `Preferences`:

- `Java` → `Editor` → `Save Actions`
  - ✓ Format Source Code → Format edited lines
  - ✓ Organize imports
- `Java` → `Editor` → `Content Assist`
  - ✓ Add import instead of qualified name
  - ✓ Use static imports
- `Java` → `Editor` → `Content Assist` → `Favorites`
  - Add these types for import suggestions:
    - `name.abuchen.portfolio.util.TextUtil`
    - `name.abuchen.portfolio.datatransfer.ExtractorUtils`
    - `name.abuchen.portfolio.datatransfer.ExtractorMatchers`
    - `name.abuchen.portfolio.datatransfer.ExtractorTestUtilities`
    - `name.abuchen.portfolio.junit.TestUtilities`
- `Java` → `Installed JREs`
  - Add Java 21 JDK

**Project Setup:**

1. **Fork and clone** your repository using [GitHub's fork workflow](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
2. **Import projects**: Within Eclipse, [clone and import all projects](https://www.vogella.com/tutorials/EclipseGit/article.html#exercise-clone-an-existing-repository)
3. **Setup target platform**:
   - Open `portfolio-target-definition` project
   - Open `portfolio-target-definition.target` file (right-click → Open With → Target Editor)
   - Click "Set as Active Target Platform" (requires Internet, may take time)
4. **Add Launch Configuration view**: `Menu` → `Window` → `Show View` → `Other...` → `Debug` → `Launch Configuration`
5. **Run the application**: Select `Eclipse Application` → `PortfolioPerformance` and right-click *Run*
6. **Run tests**: Select `JUnit Plug-in Tests` → `PortfolioPerformance_Tests` or `PortfolioPerformance_UI_Tests`

**Optional Language Packs:**

Install via `Menu` → `Help` → `Install New Software`:
- Update site: `https://download.eclipse.org/technology/babel/update-site/latest/`
- Select desired language packs
- Force language with `-nl` parameter: `eclipse.exe -nl de`

## Project Architecture

Portfolio Performance is built on **Eclipse RCP (Rich Client Platform)** with an **E4 application model**, providing a modular plugin-based architecture.

**Key Technologies:**
- **Eclipse RCP** - Rich Client Platform framework
- **OSGi Bundles** - Modular plugin architecture with clear separation of concerns
- **Maven + Tycho** - Eclipse-specific build system
- **Java 21** - Minimum required version
- **Dependency Injection** - Jakarta annotations (@Inject, @PostConstruct, @PreDestroy)
- **Event-driven communication** - E4 event broker + PropertyChangeSupport

**Module Structure:**

```
portfolio-target-definition      # Eclipse dependencies
        ↓
name.abuchen.portfolio           # Core domain & business logic
        ↓
name.abuchen.portfolio.ui        # User interface layer
        ↓
portfolio-product                # Application packaging

Test Infrastructure:
name.abuchen.portfolio.tests     # Core module tests
name.abuchen.portfolio.ui.tests  # UI module tests
```

**Finding Your Way:**

- **Domain Layer**: `name.abuchen.portfolio/src/name/abuchen/portfolio/model/` - Core entities (Client, Portfolio, Account, Security)
- **PDF Importers**: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/` - Bank statement parsers
- **UI Components**: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/` - User interface

## Building the Project

### With Eclipse

**Run the application:**
- Launch Configuration view → `Eclipse Application` → `PortfolioPerformance` → Right-click *Run*

**Run tests:**
- Core tests: `JUnit Plug-in Tests` → `PortfolioPerformance_Tests`
- UI tests: `JUnit Plug-in Tests` → `PortfolioPerformance_UI_Tests`

### With Maven

Maven is used for CI/CD builds. Local development can use Eclipse or Maven.

**Prerequisites:**
- `JAVA_HOME` points to Java 21 JDK
- Maven installed

**Standard Build:**

```bash
# Linux/macOS
export MAVEN_OPTS="-Xmx4g"
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev

# Windows
set MAVEN_OPTS="-Xmx4g"
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev
```

**Build Core Module Only:**

```bash
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio -am -amd
```

**Run Core Tests:**

```bash
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd
```

**Run Single Test Class:**

```bash
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd \
  -Dtest=<fully qualified test class name>
```

**Troubleshooting:**

If you encounter resolution problems, delete `~/.m2/repository/p2` directory (Maven Tycho 3 layout changes).

## Contributing Code

### Before You Start

1. **Fork the repository** and create a feature branch from `master`
2. **Check existing issues** to avoid duplicate work
3. **Discuss major changes** in the [forum](https://forum.portfolio-performance.info/) or create an issue first

### Writing Code

**Code Style:**
- Use `var` keyword where possible for local variables
- Format source code using Eclipse formatter (configured in project)
  - **Exception**: Do NOT reformat PDF importer files - carefully insert code manually
  - Use `@formatter:off` and `@formatter:on` to protect formatting
- Organize imports automatically (save action enabled)
- Add static imports for utility classes (ExtractorUtils, TextUtil, etc.)

**Testing Requirements:**
- **Add test cases** for all new functionality in `name.abuchen.portfolio.tests`
- **No UI tests** required (SWTBot tests not mandatory)
- **Test all calculations** and business logic thoroughly
- Follow existing test patterns (see ExtractorMatchers for PDF importers)

**Best Practices:**
- Keep commits focused and atomic
- Write self-documenting code with clear variable names
- Add comments for complex logic or non-obvious decisions
- Follow existing patterns in the codebase

### Submitting Changes

**Commit Messages:**

Write [good commit messages](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html) in English:

```
Short summary (50 chars or less)

More detailed explanation if needed. Wrap at 72 characters.
Explain what and why, not how.

Closes #123
Issue: https://forum.portfolio-performance.info/t/...
```

- Link GitHub issues: `Closes #<ISSUE NUMBER>` after empty line
- Link forum threads: `Issue: https://...` with forum post URL

**Pull Request Process:**

1. **Rebase** your branch on latest `master` (do NOT merge master into your branch)
2. **Format** your code (except PDF importers - manual formatting only)
3. **Run tests** to ensure nothing breaks
4. **Create Pull Request** via [GitHub](https://docs.github.com/en/pull-requests) or [GitHub Desktop](https://desktop.github.com/)
5. **Respond to review** comments promptly
6. **Be patient** - maintainers review in their free time

**Review Expectations:**
- Code should follow existing patterns and conventions
- Tests should pass and cover new functionality
- Documentation should be updated if needed
- Changes should be backwards compatible when possible

## Specialized Development Guides

### PDF Importers

PDF importers extract transactions from bank and broker PDF statements. Each bank/broker has its own extractor class.

**How it works:**
1. User selects PDF files via import menu or drag-and-drop
2. PDFBox converts PDF to text (one string per line)
3. Each extractor applies regex patterns to extract transactions
4. Results are presented to user for review and import

**Creating Debug Files:**

`File` → `Import` → `Debug: Create text from PDF...`

- Remove all personal data
- Do NOT insert new lines, breaks, or spaces
- Keep original PDF text structure intact

**Source Locations:**
- Extractors: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/`
- Tests: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/`
- Naming: `BankNamePDFExtractor.java` and `BankNamePDFExtractorTest.java`

**Current Reference Implementations:**
- [Baader Bank AG](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BaaderBankPDFExtractor.java) - Modern comprehensive implementation
- [Comdirect Bank AG](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/ComdirectPDFExtractor.java) - Advanced post-processing
- [Saxo Bank A/S](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/SaxoBankPDFExtractor.java) - Complex pattern matching

**Test File Naming Convention:**

Use local language with two-digit counter:
- `Buy01.txt`, `Sell01.txt` - Purchase and sale
- `Dividend01.txt` - Dividends
- `Wertpapiereingang01.txt` - Incoming securities (German example)
- `GiroKontoauszug01.txt` - Bank account statement (German example)

**Modern Test Pattern (Preferred):**

Use **ExtractorMatchers** for readable assertions:

```java
@Test
public void testWertpapierKauf01()
{
    var extractor = new ScalableCapitalPDFExtractor(new Client());

    List<Exception> errors = new ArrayList<>();

    var results = extractor.extract(PDFInputFile.loadTestCase(getClass(), "Kauf01.txt"), errors);

    assertThat(errors, empty());
    assertThat(countSecurities(results), is(1L));
    assertThat(countBuySell(results), is(1L));
    assertThat(countAccountTransactions(results), is(0L));
    assertThat(countAccountTransfers(results), is(0L));
    assertThat(countItemsWithFailureMessage(results), is(0L));
    assertThat(results.size(), is(2));
    new AssertImportActions().check(results, "EUR");

    // check security
    assertThat(results, hasItem(security( //
                    hasIsin("IE0008T6IUX0"), hasWkn(null), hasTicker(null), //
                    hasName("Vngrd Fds-ESG Dv.As-Pc Al ETF"), //
                    hasCurrencyCode("EUR"))));

    // check dividends transaction
    assertThat(results, hasItem(purchase( //
                    hasDate("2024-12-12T13:12:51"), hasShares(3.00), //
                    hasSource("Kauf01.txt"), //
                    hasNote("Ord.-Nr.: SCALsin78vS5CYz"), //
                    hasAmount("EUR", 19.49), hasGrossValue("EUR", 18.50), //
                    hasTaxes("EUR", 0.00), hasFees("EUR", 0.99))));

}
```

See [BaaderBankPDFExtractorTest](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/baaderbank/BaaderBankPDFExtractorTest.java) for examples.

**Key Utilities:**
- [AbstractPDFExtractor](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java) - Base class
- [ExtractorUtils](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ExtractorUtils.java) - Amount conversion, tax/fee processing
- [ExtractorMatchers](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/ExtractorMatchers.java) - Modern test assertions
- [TextUtil](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/util/TextUtil.java) - String manipulation for PDF text

**Important Notes:**
- Do NOT auto-format PDF extractor files (use `@formatter:off` / `@formatter:on`)
- Test files must be UTF-8 encoded
- Anonymize personal data but preserve structure
- Match all special characters with `.` (dot) in regex

### Interactive Flex Query Importers

The Interactive Broker Flex Query importer handles XML-compliant Interactive Broker Activity Statements.

**Features:**
- Secure XML parsing with XXE prevention
- Multi-asset support (Stocks, Options, Futures, Commodities, etc.)
- Multi-currency with automated conversion
- Transaction pairing and tax treatment

**Source Locations:**
- Importer: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ibflex/`
- Tests: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/ibflex/`

**Test Files:**
- XML Activity Statements with anonymized personal data
- Preserve transaction relationships and structure
- Include relevant asset categories and transaction types

**Reference:**
- [IBFlexStatementExtractor](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ibflex/IBFlexStatementExtractor.java)
- [Test Examples](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/ibflex/IBFlexStatementExtractorTest.java)
- [IB Documentation](https://ibkrguides.com/reportingreference/reportguide/tradesfq.htm)

### Translations

Portfolio Performance is translated into 13 languages using POEditor.

**Contributing Translations:**
1. Register with [POEditor project](https://poeditor.com/join/project?hash=4lYKLpEWOY)
2. Open a [ticket](https://github.com/portfolio-performance/portfolio/issues/new/choose) for new languages
3. Translations are merged before each release

**For Code Contributors:**

- Use [Source → Externalize Strings](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/reference/ref-wizard-externalize-strings.htm) in Eclipse
- Use [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor) for editing
- Translate new labels to all languages using [DeepL](https://www.deepl.com)

**Label Naming Conventions:**
- `Label` - Short labels
- `Msg` - Longer messages
- `Column` - Column headers
- `PDF`, `CSV`, `Preferences` - Specific area prefixes (use sparingly)

Follow existing naming patterns in the code area you're contributing to.

### Trade Calendars

Trade calendars track trading-free days (weekends, holidays, exchange-specific closures) for accurate performance calculations.

**Source Locations:**
- Manager: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/TradeCalendarManager.java`
- Calendar Class: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/TradeCalendar.java`
- Tests: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/util/TradeCalendarTest.java`

**Structure:**
- `code` - Unique identifier (e.g., `nyse`)
- `description` - Display label (e.g., "New York Stock Exchange")
- `weekend` - Default weekend days

**Test Requirements:**
- Test at least 3 dates per year from first to last day
- Include regular holidays and one-time trading-free days
- Comment each test with the holiday name

**Example:**
[TradeCalendarTest - NYSE](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/util/TradeCalendarTest.java)

### Images and Icons

All images and icons must be [Creative Commons CC0](https://creativecommons.org/publicdomain/zero/1.0/legalcode) licensed.

**Sources:**
- Use icons from [iconmonstr.com](https://iconmonstr.com) only
- Register all images in [Images.java](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/Images.java)

**Format Requirements:**
- **Format**: PNG with transparent background
- **Base size**: 16x16px
- **Naming**: lowercase, descriptive (e.g., `information.png`)
- **Multiple sizes**: Create at least 16x16px and 32x32px versions
  - `information.png` (16x16px)
  - `information@2x.png` (32x32px)

**Color Guidelines:**
- **Passive state**: Gray
- **Active state**: Orange (logo color)

See [Color Code Reference](#color-code-reference) for exact colors.

## Getting Help

**Community:**
- **[Forum (German)](https://forum.portfolio-performance.info/)** - Discussion and questions ([Thread on contributing](https://forum.portfolio-performance.info/t/verbesserungen-im-source-code-in-github-einbringen/7063))
- **[GitHub Issues](https://github.com/portfolio-performance/portfolio/issues)** - Bug reports and feature requests

**External Resources:**
- [Eclipse RCP Tutorial](https://www.vogella.com/tutorials/EclipseRCP/article.html)
- [Maven Tycho](https://www.eclipse.org/tycho/)
- [Git Best Practices](https://docs.github.com/en/get-started/using-git/about-git)

## Appendix

### Regular Expression Reference

Test regular expressions at [regex101.com](https://regex101.com/).

**Best Practices:**
- Match special characters (`äöüÄÖÜß`, circumflex, etc.) with `.` (dot) - PDF conversion varies
- Escape special regex characters: `$^{[(|)]}*+?\`
- `.match(" ... ")` - Use anchors `^...$`
- `.find(" ... ")` - Do NOT add anchors (added automatically)

**Patterns that Work Well:**

| Value | Example | Works Well |
| :--- | :--- | :--- |
| Date | 01.01.1970 | `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}` |
| Date | 1.1.1970 | `[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}` |
| Time | 12:01 | `[\\d]{2}\\:[\\d]{2}` |
| ISIN | IE00BKM4GZ66 | `[A-Z]{2}[A-Z0-9]{9}[0-9]` |
| WKN | A111X9 | `[A-Z0-9]{6}` |
| Valoren | 1098758 | `[A-Z0-9]{5,9}` |
| SEDOL | B5B74S0 | `[A-Z0-9]{7}` |
| CUSIP | 11135F101 | `[A-Z0-9]{9}` |
| TickerSymbol | AAPL, BRK.B | `[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?` |
| Crypto Ticker | BTC, ETH-BTC | `[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?` |
| Amount | 751,68 | `[\\.,\\d]+` or `[\\.\\d]+,[\\d]{2}` |
| Amount | 74'120.00 | `[\\.'\\d]+` |
| Amount | 20 120.00 | `[\\.\\d\\s]+` |
| Currency | EUR | `[A-Z]{3}` |
| Currency Symbol | € or $ | `\\p{Sc}` |

### Color Code Reference

| Color | Hex | RGB | Used For |
| :--- | :--- | :--- | :--- |
| Orange (logo) | ![#f18f01](https://placehold.co/15x15/f18f01/f18f01.png) `#f18f01` | `rgb(241, 143, 1)` | Activated (e.g., filter) |
| Blue (logo) | ![#0e6e8e](https://placehold.co/15x15/0e6e8e/0e6e8e.png) `#0e6e8e` | `rgb(14, 110, 142)` | Logo |
| Green (logo) | ![#9ac155](https://placehold.co/15x15/9ac155/9ac155.png) `#9ac155` | `rgb(154, 193, 85)` | Logo |
| Dark Blue | ![#95a4b3](https://placehold.co/15x15/95a4b3/95a4b3.png) `#95a4b3` | `rgb(149, 164, 179)` | Default icon color |
| Red | ![#d11d1d](https://placehold.co/15x15/d11d1d/d11d1d.png) `#d11d1d` | `rgb(209, 29, 29)` | Error, Fault |
| Yellow | ![#ffd817](https://placehold.co/15x15/ffd817/ffd817.png) `#ffd817` | `rgb(255, 216, 23)` | Warning |

---

Thank you for contributing to Portfolio Performance! 🎉
