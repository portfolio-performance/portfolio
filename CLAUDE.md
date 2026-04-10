# CLAUDE.md

## Project Overview

Portfolio Performance is an Eclipse RCP (Rich Client Platform) application for tracking and analyzing investment portfolios. The codebase uses Java 21, Maven with Tycho for Eclipse-specific builds, and follows a modular OSGi plugin-based architecture.

## Build Commands

### Prerequisites
- `JAVA_HOME` must point to Java 21 JDK
- Use Maven Tycho for builds
- All commands use `-f portfolio-app/pom.xml`
- Use `-Plocal-dev` profile for faster local development (skips coverage, checkstyle, babel translations)

### Full Build
```bash
export MAVEN_OPTS="-Xmx4g"
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
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.ui -am -amd
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
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.ui,:name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.ui.tests -am -amd
```

## Code Style

- Use `var` keyword for local variables where type is obvious
- Do NOT generate `$NON-NLS-1$` comments (internationalization warnings are suppressed globally)
- Do NOT auto-format PDF extractor files — use `@formatter:off` / `@formatter:on` where formatting is intentional; otherwise carefully insert code manually preserving existing alignment
- Do not reformat code unrelated to your change
