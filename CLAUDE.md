# CLAUDE.md

## Project Overview

Portfolio Performance is an Eclipse RCP (Rich Client Platform) application for tracking and analyzing investment portfolios. The codebase uses Java 21, Maven with Tycho for builds, and follows a modular plugin-based architecture.

## Build and Development Commands

### Build Commands

The Maven build is using Maven Tycho. Therefore the following commands must be used to compile and test the code.

```bash
mvn -f portfolio-app/pom.xml clean verify -Plocal-dev
```

```bash
# Build only the core module
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio -am -amd 
```

```bash
# Build the core and UI module
mvn -f portfolio-app/pom.xml clean compile -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.bootstrap,:name.abuchen.portfolio.ui -am -amd
```


### Test Commands

```bash
# Run core tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd

# Run only one class of the core tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -o -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.tests -am -amd -Dtest=<fully qualified name of the test class>

# Run UI tests
mvn -f portfolio-app/pom.xml verify -Plocal-dev -pl :portfolio-target-definition,:name.abuchen.portfolio.pdfbox1,:name.abuchen.portfolio.pdfbox3,:name.abuchen.portfolio,:name.abuchen.portfolio.ui,:name.abuchen.portfolio.junit,:name.abuchen.portfolio.ui.tests -am -amd
```

## Code Style

- Use 'var' keyword where possible
- Do not the comment $NON-NLS-1$ to suppress warnings about missing internationalization

## Architecture Overview

### Core Module Structure
```
name.abuchen.portfolio/         # Core business logic
├── model/                      # Domain model (Client, Portfolio, Security, Account)
├── datatransfer/               # Import/Export (CSV, PDF, 100+ banks)
├── money/                      # Multi-currency support
├── snapshot/                   # Performance calculations  
├── online/                     # External data feeds
├── math/                       # Financial calculations (IRR, risk metrics)
└── util/                       # Core utilities

name.abuchen.portfolio.ui/      # User interface layer
├── views/                      # Main application views and charts
├── dialogs/                    # Modal dialogs and forms
├── handlers/                   # Command handlers for menu actions
├── editor/                     # File editing infrastructure
└── wizards/                    # Multi-step workflow dialogs
```

### Key Entry Points
- **`Client.java`** - Root domain aggregate containing all user data
- **`ClientFactory.java`** - Client creation and persistence management
- **`AbstractPDFExtractor.java`** - Base class for bank statement imports
- **`PortfolioPart.java`** - Primary UI editor for portfolio files
- **`AbstractFinanceView.java`** - Base class for views

## Data Model Core Concepts

### Entity Relationships
```
Client (Root Aggregate)
├── Portfolios[] → PortfolioTransactions[] (Buy/Sell/Transfer)
├── Accounts[] → AccountTransactions[] (Deposits/Withdrawals/Dividends)  
├── Securities[] → SecurityPrices[] (Historical quotes)
├── Taxonomies[] (Classification hierarchies)
└── InvestmentPlans[] (Automated investing)
```
