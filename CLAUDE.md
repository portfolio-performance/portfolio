# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Portfolio Performance is an Eclipse RCP (Rich Client Platform) application for tracking and analyzing investment portfolios. The codebase uses Java 21, Maven with Tycho for Eclipse-specific builds, and follows a modular OSGi plugin-based architecture.

## Build Commands

### Prerequisites
- `JAVA_HOME` must point to Java 21 JDK
- Use Maven Tycho (not standard Maven) for builds
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
        ↓
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
        ↓
name.abuchen.portfolio.ui/        # User interface layer (Eclipse RCP/SWT)
├── views/                        # Main application views and charts
├── dialogs/                      # Modal dialogs and forms
├── handlers/                     # Command handlers for menu actions
├── editor/                       # File editing infrastructure
└── wizards/                      # Multi-step workflow dialogs
        ↓
portfolio-product/                # Application packaging and distribution

Test Infrastructure:
name.abuchen.portfolio.junit/     # Shared test utilities
name.abuchen.portfolio.tests/     # Core module tests
name.abuchen.portfolio.ui.tests/  # UI module tests (SWTBot)
```

### Key Entry Points
- **Client.java** (`model/`) - Root aggregate containing all user data (portfolios, accounts, securities)
- **ClientFactory.java** (`model/`) - Client persistence (XML serialization)
- **AbstractPDFExtractor.java** (`datatransfer/pdf/`) - Base class for bank statement importers
- **PortfolioPart.java** (UI) - Primary editor for portfolio files
- **AbstractFinanceView.java** (UI) - Base class for dashboard views

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
- Do NOT use `$NON-NLS-1$` comments (internationalization warnings are suppressed globally)
- Use Eclipse formatter (auto-format on save for edited lines)

### Import Organization
Add these types to static import favorites (for auto-suggestions):
- `name.abuchen.portfolio.util.TextUtil.*`
- `name.abuchen.portfolio.datatransfer.ExtractorUtils.*`
- `name.abuchen.portfolio.datatransfer.ExtractorMatchers.*`
- `name.abuchen.portfolio.datatransfer.ExtractorTestUtilities.*`
- `name.abuchen.portfolio.junit.TestUtilities.*`

## Common Development Patterns

### Testing
- All new functionality requires test cases in `name.abuchen.portfolio.tests`
- UI tests (SWTBot) are NOT required
- Test all calculations and business logic thoroughly

### Dependency Injection
```java
@Inject
private EPartService partService;

@PostConstruct
public void init() {
    // Initialization after injection
}

@PreDestroy
public void dispose() {
    // Cleanup
}
```

### Event Communication
```java
// Publishing events
@Inject IEventBroker eventBroker;
eventBroker.post(UIConstants.Event.Security.CREATED, security);

// Subscribing to events
@Inject
@Optional
public void onSecurityChanged(@UIEventTopic(UIConstants.Event.Security.UPDATED) Security security) {
    // Handle event
}
```

## Troubleshooting

### Maven Build Issues
If you encounter P2 resolution problems:
```bash
# Delete Tycho 3 cache (layout changes)
rm -rf ~/.m2/repository/p2
```

### Common Pitfalls
1. **Transactions must match security currency** - Securities have a base currency; all transactions must match
2. **Portfolio/Account transaction pairing** - BUY/SELL transactions create both portfolio and account entries

## Reference Documentation
- [CONTRIBUTING.md](CONTRIBUTING.md) - Comprehensive development guide
- [Forum (German)](https://forum.portfolio-performance.info/) - Community discussions
- [GitHub Issues](https://github.com/portfolio-performance/portfolio/issues) - Bug reports
