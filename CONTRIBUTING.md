# Contributing to Portfolio Performance

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
- [Development Guides](#development-guides)
  - [PDF Importers](#pdf-importers)
  - [Interactive Flex Query Importers](#interactive-flex-query-importers)
  - [Translations](#translations)
  - [Trade Calendars](#trade-calendars)
  - [Images and Icons](#images-and-icons)
  - [Centrally defining CSS colors for UI objects](#centrally-defining-css-colors-for-ui-objects)
- [Getting Help](#getting-help)
- [Appendix](#appendix)
  - [Regular Expression Reference](#regular-expression-reference)
  - [Color Code Reference](#color-code-reference)

## Quick Start

Thanks for your interest in contributing! We welcome improvements of all sizes — from small bug fixes to larger features. To make collaboration smooth and reviews efficient, please keep the following guidelines in mind:

* Keep pull requests small and **focused on a single feature or fix**. The shorter and clearer the change, the easier it is to review.
* Use the standard code formatting, but avoid reformatting code that’s unrelated to your change.
* For larger changes, open a draft pull request early to outline your idea and gather feedback before investing too much time polishing.
* Follow existing code patterns and conventions throughout the project.
* Write concise comments where they help future readers understand important decisions.
* Add tests for calculation logic, especially in the `name.abuchen.portfolio` bundle. Avoid modifying existing tests unless a breaking or incompatible change makes it necessary.

## Development Setup

### Eclipse IDE Setup

Eclipse IDE is required for Portfolio Performance development as the application is built on Eclipse RCP (Rich Client Platform).

**Prerequisites**
- **Java 21** - Required minimum version ([Download from Azul](https://www.azul.com/downloads/))
- **[Eclipse IDE for RCP and RAP Developers](https://www.eclipse.org/downloads/packages/)** - Download the RCP package

**Plugins from the Eclipse Marketplace**

Install via Eclipse Marketplace (drag and drop the `Install` button to your workspace):
- [Infinitest](https://marketplace.eclipse.org/content/infinitest) - Continuous testing
- [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor) - Translation file editing
- [Checkstyle Plug-In](https://marketplace.eclipse.org/content/checkstyle-plug) - Code style checking
- [SonarQube for IDE](https://marketplace.eclipse.org/content/sonarqube-ide) - Code quality analysis
- [Launch Configuration DSL](https://marketplace.eclipse.org/content/launch-configuration-dsl) - Launch configuration management

**Plugins from the Eclipse Update Site**

`Menu` → `Help` → `Install New Software` → Select `Latest Eclipse Simultaneous Release`:
- **M2E PDE Integration** (under General Purpose Tools)
- **Eclipse e4 Tools** (under General Purpose Tools)

**Configure Eclipse**

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

**Project Setup**

1. **Fork** your repository using [GitHub's fork workflow](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
2. **Import projects**: Within Eclipse, [clone and import all projects](https://www.vogella.com/tutorials/EclipseGit/article.html#exercise-clone-an-existing-repository)
3. **Setup target platform**:
   - Open `portfolio-target-definition` project
   - Open `portfolio-target-definition.target` file (right-click → Open With → Target Editor)
   - Click "Set as Active Target Platform" (requires Internet, may take time)
4. **Add Launch Configuration view**: `Menu` → `Window` → `Show View` → `Other...` → `Debug` → `Launch Configuration`
5. **Run the application**: Select `Eclipse Application` → `PortfolioPerformance` and right-click *Run*
6. **Run tests**: Select `JUnit Plug-in Tests` → `PortfolioPerformance_Tests` or `PortfolioPerformance_UI_Tests`

**Optional Language Packs**

Install via `Menu` → `Help` → `Install New Software`:
- Update site: `https://download.eclipse.org/technology/babel/update-site/latest/`
- Select desired language packs
- Force language with `-nl` parameter: `eclipse.exe -nl de`

## Project Architecture

Portfolio Performance is built on **Eclipse RCP (Rich Client Platform)** with an **E4 application model**, providing a modular plugin-based architecture.

**Key Technologies**
- **Eclipse RCP** - Rich Client Platform framework
- **OSGi Bundles** - Modular plugin architecture with clear separation of concerns
- **Maven + Tycho** - Eclipse-specific build system
- **Java 21** - Minimum required version
- **Dependency Injection** - Jakarta annotations (@Inject, @PostConstruct, @PreDestroy)
- **Event-driven communication** - E4 event broker + PropertyChangeSupport

**Module Structure**

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

**Finding Your Way**

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
mvn -f portfolio-app/pom.xml clean verify

# Windows
set MAVEN_OPTS="-Xmx4g"
mvn -f portfolio-app/pom.xml clean verify
```

**Build Core Module Only:**

```bash
mvn -f portfolio-app/pom.xml clean compile \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio -am -amd
```

**Run Core Tests:**

```bash
mvn -f portfolio-app/pom.xml verify -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd
```

**Run Single Test Class:**

```bash
mvn -f portfolio-app/pom.xml verify -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd \
  -Dtest=<fully qualified test class name>
```

You can use the profile `-Plocal-dev` for a faster locale build. However, keep in mind that this skips the translation bundles, checkstyle checks, and other checks.

## Contributing Code

### Code Style
- Use `var` keyword where possible for local variables
- Format source code using Eclipse formatter (configured in project)
  - **Exception**: Do NOT reformat PDF importer files - carefully insert code manually
  - Use `@formatter:off` and `@formatter:on` to protect formatting
- Organize imports automatically (save action enabled)
- Add static imports for utility classes (ExtractorUtils, TextUtil, etc.)

### Testing Requirements
- **Add test cases** for new functionality in `name.abuchen.portfolio.tests`
- **No UI tests** required
- **Test all calculations** and business logic thoroughly
- Use custom matchers when testing PDF importers

### Submitting Changes

**Commit Messages**

Write [commit messages](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html) in English:

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
4. **Create Pull Request** via [GitHub](https://docs.github.com/en/pull-requests) or [GitHub Desktop](https://desktop.github.com/)

## Development Guides

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
    assertThat(countSkippedItems(results), is(0L));
    assertThat(results.size(), is(2));
    new AssertImportActions().check(results, "EUR");

    // check security
    assertThat(results, hasItem(security( //
                    hasIsin("IE0008T6IUX0"), hasWkn(null), hasTicker(null), //
                    hasName("Vngrd Fds-ESG Dv.As-Pc Al ETF"), //
                    hasCurrencyCode("EUR"))));

    // check transaction
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

**Source Locations:**
- Importer: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ibflex/`
- Tests: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/ibflex/`

**Reference:**
- [IBFlexStatementExtractor](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ibflex/IBFlexStatementExtractor.java)
- [Test Examples](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/ibflex/IBFlexStatementExtractorTest.java)
- [IB Documentation](https://ibkrguides.com/reportingreference/reportguide/tradesfq.htm)

### Translations

Portfolio Performance is translated into multiple languages using POEditor.

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

### Trade Calendars

Trade calendars track trading-free days (weekends, holidays, exchange-specific closures).

**Source Locations:**
- Manager: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/TradeCalendarManager.java`
- Calendar Class: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/TradeCalendar.java`
- Tests: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/util/TradeCalendarTest.java`

**Structure:**
- `code` - Unique identifier (e.g., `nyse`)
- `description` - Display label (e.g., "New York Stock Exchange")
- `weekend` - Default weekend days

### Images and Icons

All images and icons must be [Creative Commons CC0](https://creativecommons.org/publicdomain/zero/1.0/legalcode) licensed.

**Sources**
- Use icons from [iconmonstr.com](https://iconmonstr.com) only
- Register all images in [Images.java](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/Images.java)
- Use **SVG** format with width and height set to 16px; add a fill color if needed
- Note: For historical reasons, some icons are still available in PNG format with two resolutions (@1x and @2x). New icons should use SVG format only.

**Color Guidelines**
- **Passive state**: #393E42 (dark, cool gray)
- **Active state**: #F18F01 (orange)
- **Error state**: #D11D1D (dark red)

See [Color Code Reference](#color-code-reference) for exact colors.

## Centrally defining CSS colors for UI objects

### Goal

A color should be defined centrally in `light.css` and `dark.css`.

Java code should not decide the final design color.  
Java should only receive, store, and use the color from CSS.

This template can be used for charts, views, widgets, dialogs, and other UI classes.

> This template uses one consistent example across all steps:
>
> | Item | Example |
> |------|---------|
> | CSS target name | `YourCssTarget` |
> | CSS property | `your-color-property` |
> | Target class | `YourTargetClass` |
> | Target field | `yourColorProperty` |
> | Target setter | `setYourColorProperty(Color color)` |
> | Adapter | `YourElementAdapter` |
> | Handler | `YourCSSHandler` |

### Step 1: Define the color in CSS

Files:

`name.abuchen.portfolio.ui/css/shared/light.css`  
`name.abuchen.portfolio.ui/css/shared/dark.css`

Template:

```css
YourCssTarget {
    your-color-property: #123456;
}
```

Check:

- `YourCssTarget` = CSS target name
- `your-color-property` = CSS property
- The same property should exist in both theme files

Important:

The property name does **not** decide which CSS block is used.  
The CSS target name decides that.

### Step 2: Register the property in `plugin.xml`

File:

`name.abuchen.portfolio.ui/plugin.xml`

Template:

```xml
<extension point="org.eclipse.e4.ui.css.core.propertyHandler">
   <handler
         adapter="name.abuchen.portfolio.ui.theme.YourElementAdapter"
         composite="false"
         handler="name.abuchen.portfolio.ui.theme.YourCSSHandler">
      <property-name name="your-color-property"/>
   </handler>
</extension>
```

Check:

- `adapter` = the `ElementAdapter` class for the target object
- `handler` = the Java class that processes the CSS property
- `property-name` must match the CSS name exactly

Important:

If the property is not registered here, the handler will not process it.

### Step 3: Check the CSS target name in the `ElementAdapter`

File:

`name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/theme/YourElementAdapter.java`

Open the adapter class from `plugin.xml`.

In this project, the CSS target name is typically defined by:

```java
@Override
public String getLocalName()
{
    return "YourCssTarget";
}
```

Check:

- The adapter must return the correct CSS target name
- The CSS block in `light.css` and `dark.css` must use the same name

Example rule:

- If `getLocalName()` returns `"YourCssTarget"`, the CSS block must be:

```css
YourCssTarget {
    your-color-property: #123456;
}
```

Important:

The property name does **not** select the CSS block.  
The value from `getLocalName()` selects the CSS block.

### Step 4: Check that the target class is connected in `ElementProvider`

File:

`name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/theme/ElementProvider.java`

Template:

```java
if (element instanceof YourTargetClass target)
    return new YourElementAdapter(target, engine);
```

Check:

- `YourTargetClass` is the real Java class that should receive the CSS color
- `YourElementAdapter` is the adapter registered in `plugin.xml`
- The adapter must wrap the same target object

Important:

This is the real connection between the final Java class and the CSS engine.

If this entry is missing, the CSS engine may never create the adapter for your target class.

### Step 5: Process the property in the CSS handler and pass it to the target class

File:

`name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/theme/YourCSSHandler.java`

Typical import:

```java
import your.package.path.YourTargetClass;
```

```java
import org.eclipse.swt.graphics.Color;
```

Possible additional imports:

```java
import name.abuchen.portfolio.ui.theme.YourElementAdapter;
import name.abuchen.portfolio.ui.theme.ColorSourceTracker;
```

Template:

```java
if (!(element instanceof YourElementAdapter adapter))
    return false;

YourTargetClass target = adapter.getYourTargetClass();

switch (property)
{
    case "your-color-property":
        target.setYourColorProperty(getColor(value));
        return true;
    default:
        return false;
}
```

Check:

- The handler receives the `ElementAdapter`, not the raw target class
- The handler must get the real target class from the adapter
- The handler must read the CSS property
- The handler must pass the color into the correct target object

Important:

The handler does **not** decide which CSS block is used.  
It only processes a property that was already matched by the CSS engine.

### Step 6: Store and use the color in the target class

File:

`name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/.../YourTargetClass.java`

In this project, the target class usually does both:

1. store the CSS color
2. later use the stored color on the real UI or chart object

Example:

```java
private Color yourColorProperty;

public void setYourColorProperty(Color color)
{
    this.yourColorProperty = color;
}
```

Later in the same class:

```java
chartObject.setLineColor(yourColorProperty);
```

Or:

```java
uiObject.setForeground(yourColorProperty);
```

Check:

- The CSS handler must write the color into a field or variable in the target class
- The same field or variable must later be used by the visible UI or chart logic

Important:

In many cases, storing and using the color happens in the same target class.

### Step 7: Check for hard-coded overrides

Search for:

```text
Colors.
SWT.COLOR_
new RGB(
Colors.getColor(
setForeground(
setBackground(
setLineColor(
```

Check:

- A later hard-coded color can override the CSS value
- CSS may be loaded correctly, but Java may still win visually

Important:

The last applied color often decides the visible result.

### Optional: Track CSS application for debugging

Template:

```java
ColorSourceTracker.markCssApplied("YourCssTarget", "your-color-property");
```

Check:

- Only mark CSS if the CSS property was really processed
- Do not mark CSS if only a fallback color is used

Important:

Debug tracking should reflect the real source of the final visible color.

### Full chain

```text
Java object
-> ElementProvider creates ElementAdapter
-> getLocalName() returns CSS target name
-> CSS engine finds matching CSS block
-> plugin.xml routes property to the CSS handler
-> CSS handler gets the real target class from the adapter
-> Target class stores color
-> Target class uses color on the final UI object
```

### Quick validation

A CSS color setup is complete only if all answers are **Yes**:

1. Is the color defined in both `light.css` and `dark.css`?
2. Is the property registered in `plugin.xml`?
3. Does `getLocalName()` return the correct CSS target name?
4. Does `ElementProvider` create the correct adapter for the target class?
5. Does the CSS handler get the target class from the adapter?
6. Does the target class store the color?
7. Does the target class use the stored color on the final UI object?
8. Is there no later hard-coded override?

### Core rule

**The property name does not select the CSS block.**  
**`getLocalName()` in the `ElementAdapter` selects the CSS block.**

## Appendix

### Regular Expression Reference

Test regular expressions at [regex101.com](https://regex101.com/).

**Best Practices:**
- Match special characters (`äöüÄÖÜß`, circumflex, etc.) with `.` (dot) - PDF conversion varies
- Escape special regex characters: `$^{[(|)]}*+?\`
- `.match(" ... ")` - Use anchors `^...$`
- `.find(" ... ")` - Do NOT add anchors (added automatically)

**Patterns that Work Well**

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
