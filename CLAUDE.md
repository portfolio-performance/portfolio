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
# Windows
set MAVEN_OPTS="-Xmx4g"
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev

# Linux/macOS
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
  -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.ui,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.ui.tests -am -amd
```

## Architecture Overview

### Technology Stack
- **Eclipse RCP** - Rich Client Platform with E4 application model
- **OSGi Bundles** - Modular plugin architecture
- **Maven Tycho** - Eclipse-specific build tooling
- **Java 21** - Minimum required version
- **Dependency Injection** - Jakarta annotations (@Inject, @PostConstruct, @PreDestroy)
- **Event Communication** - E4 event broker + PropertyChangeSupport

### Module Structure
```
portfolio-target-definition/      # Eclipse RCP dependencies (P2 repository)

name.abuchen.portfolio/           # Core domain & business logic (no UI dependencies)
├── model/                        # Domain entities (Client, Portfolio, Security, Account)
├── datatransfer/                 # Import/Export (CSV, PDF, 100+ banks)
│   ├── pdf/                      # PDF extractors (bank statements)
│   └── ibflex/                   # Interactive Brokers Flex Query
├── money/                        # Multi-currency support (Money, CurrencyConverter)
├── snapshot/                     # Performance calculations (IRR, TWR)
├── online/                       # External data feeds (quote providers)
├── math/                         # Financial calculations (risk metrics)
└── util/                         # Core utilities (TextUtil, TradeCalendar)

name.abuchen.portfolio.ui/        # User interface layer (Eclipse RCP/SWT)
├── views/                        # Main application views and charts
├── dialogs/                      # Modal dialogs and forms
├── handlers/                     # Command handlers for menu actions
├── editor/                       # File editing infrastructure
└── wizards/                      # Multi-step workflow dialogs

portfolio-product/                # Application packaging and distribution
```

### Test Infrastructure
```
name.abuchen.portfolio.junit/     # Shared test utilities
name.abuchen.portfolio.tests/     # Core module tests
name.abuchen.portfolio.ui.tests/  # UI module tests (SWTBot)
```

### Key Entry Points
- **Client.java** - Root aggregate containing all user data (portfolios, accounts, securities)
- **ClientFactory.java** - Client persistence (XML serialization)
- **AbstractPDFExtractor.java** - Base class for bank statement importers
- **PortfolioPart.java** - Primary editor for portfolio files
- **AbstractFinanceView.java** - Base class for dashboard views

### Data Model
```
Client (Root Aggregate)
├── List<Portfolio> → PortfolioTransaction[] (BUY, SELL, TRANSFER_IN, TRANSFER_OUT, DELIVERY_INBOUND, DELIVERY_OUTBOUND)
├── List<Account> → AccountTransaction[] (DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, BUY, SELL, TRANSFER_IN, TRANSFER_OUT ...)
├── List<Security> → SecurityPrice[] (historical quotes)
│   └── SecurityEvent[] (stock splits, dividends)
├── List<Taxonomy> (classification hierarchies for securities)
├── List<InvestmentPlan> (automated recurring investments)
└── Settings (preferences, bookmarks, dashboards)
```

**Important Relationships:**
- Portfolio transactions are always paired with account transactions (e.g., BUY creates both PortfolioTransaction and AccountTransaction)
- Securities have a base currency; transactions must match the security's currency
- Cross-currency transactions use ExchangeRate for conversion

## Code Style

- Use `var` keyword for local variables where type is obvious
- Do NOT generate `$NON-NLS-1$` comments (internationalization warnings are suppressed globally)

### Import Organization

Add these types to static import favorites (for auto-suggestions):
- `name.abuchen.portfolio.util.TextUtil.*`
- `name.abuchen.portfolio.datatransfer.ExtractorUtils.*`
- `name.abuchen.portfolio.datatransfer.ExtractorMatchers.*`
- `name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.*`
- `name.abuchen.portfolio.junit.TestUtilities.*`
