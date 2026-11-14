# CLAUDE.md

## Project Overview

Portfolio Performance is an Eclipse RCP (Rich Client Platform) application for tracking and analyzing investment portfolios. The codebase uses Java 21, Maven with Tycho for Eclipse-specific builds, and follows a modular OSGi plugin-based architecture.

## Build Commands

### Prerequisites
- `JAVA_HOME` must point to a Java 21 JDK
- Use Maven Tycho for builds
- All commands use `-f portfolio-app/pom.xml`
- Use `-Plocal-dev` profile for faster local development (skips coverage, checkstyle, babel translations)

### Full Build
```bash
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev
```

### Build Core Module Only
```bash
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio -am -amd
```

### Build Core and UI Modules
```bash
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.rest,:name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.ui -am -amd
```

## Test Commands

### Run Core Tests
```bash
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd
```

### Run Single Test Class
```bash
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd \
  -Dtest=<fully.qualified.TestClassName>
```

### Run UI Tests
```bash
mvn -f portfolio-app/pom.xml verify -Plocal-dev \
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.rest,:name.abuchen.portfolio.ui,:name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.ui.tests -am -amd
```

## Code Style

- Use `var` keyword for local variables where type is obvious
- Do NOT auto-format PDF extractor files — use `@formatter:off` / `@formatter:on` where formatting is intentional; otherwise carefully insert code manually preserving existing alignment
- Do not reformat code unrelated to your change

## Externalized Strings

- Externalize user-visible text to `Messages` and translate it into every `messages_xx.properties`, in any bundle. That means labels, columns, menus, dialogs, tooltips, and error messages that reach a dialog or the error log view. This also holds inside a class annotated `@SuppressWarnings("nls")`, where the compiler does not flag it. Use the `translate` skill.
- Mark literals that are not visible: keys, identifiers, regular expressions, formats, log-only diagnostics, messages of internal exceptions. This applies in `name.abuchen.portfolio`, `.ui`, and `.bootstrap`, where the compiler warns. `rest` and `cli` expose APIs and do not translate keys.
  - Up to about three literals: `//$NON-NLS-n$` on each line.
  - More: `@SuppressWarnings("nls")` on the narrowest method, field, or class that covers them.
  - Class level is fine for importers (`AbstractPDFExtractor` subclasses, `IBFlexStatementExtractor`, `XTBCSVExtractor`), key or enum tables (`FieldCode`), and test classes.
- Unmarked literals are fine while work is in progress. When you hand work back, and in every review, each literal in the changed code is either translated or marked as not visible.
- Leave existing suppressions, markers, and untranslated text alone unless your edit makes one wrong (for example, a marked literal becomes visible text). If code you change contains visible text that is untranslated on purpose, keep it as is and name it in your summary.
