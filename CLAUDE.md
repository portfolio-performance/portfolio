# CLAUDE.md

> **Note:** This file provides guidance to Claude Code CLI when working on this project. For general development documentation, see CONTRIBUTING.md.

## Project Overview

Portfolio Performance is an Eclipse RCP (Rich Client Platform) application for tracking and analyzing investment portfolios. The codebase uses Java 21, Maven with Tycho for builds, and follows a modular plugin-based architecture.

## CC-Sessions Workflow System

This project uses **CC-Sessions** for structured task management and workflow enforcement.

**📖 Complete Guide:** See `@CC-SESSIONS.md` for:
- DAIC Mode (Discussion → Implementation workflow)
- Task management with persistent context
- Trigger phrases and branch enforcement
- Context compaction and agent usage

**Quick Start:**
- Create task: `"Create a task for: [description]"`
- Start work: `"Let's work on m-task-name"`
- Enable implementation: `"go ahead"` / `"los geht's"`
- Return to discussion: `daic`

**Quick Commands:**
- New PDF importer: `/pdf-importer BankName Kauf01.txt Dividende01.txt` (multilingual detection)
- Debug existing importer: `/pdf-debug BankName Kauf01.txt Buy01.txt` (multilingual detection)
- Generate release notes: `/release-notes` (bilingual German/English + XML)
- Review session docs: `/session-docs` (audit and consolidate documentation changes)
- Add trigger phrase: `/add-trigger phrase` (customize implementation mode triggers)
- Toggle API mode: `/api-mode` (controls automatic ultrathink behavior for token optimization)

**API Mode:**
The `/api-mode` command toggles between two modes for token optimization:
- **API mode enabled** (default): Ultrathink disabled to save tokens; use `[[ ultrathink ]]` manually when needed
- **API mode disabled**: Ultrathink automatically enabled for best performance (Max mode)

Toggle with `/api-mode` - changes take effect on your next message. Current state stored in `sessions/sessions-config.json`.

**PDF Importer Transaction Detection:**
Automatically detects transaction types from filenames in German, English, French, Italian, Spanish:
- Buy/Sell, Dividends, Interest, Fees, Account Statements, Deposits/Withdrawals
- Deliveries/Transfers, Taxes, Crypto, Corporate Actions, Savings Plans, Redemptions

**Specialized Guides:**
- **PDF Importer Development**: `@sessions/knowledge/pdf-importer.md`
  - 5-Phase TDD Workflow
  - 7 Mandatory Test Assertions
  - Standard Forex Attributes
  - Common Pitfalls Documentation

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

### Eclipse RCP Application Architecture

Portfolio Performance is built on **Eclipse RCP (Rich Client Platform)** with **E4 application model**, providing:

- **Plugin-based architecture**: Modular OSGi bundles with clear separation of concerns
- **Dependency Injection**: Jakarta @Inject, @PostConstruct, @PreDestroy annotations
- **Event-driven communication**: E4 event broker + PropertyChangeSupport
- **Part-based UI**: MPart instances managed by E4 application model
- **Target platform**: Eclipse dependencies managed via Tycho target definition

**Build System:**
- **Maven + Tycho**: Eclipse-specific Maven plugins for OSGi/RCP builds
- **Java 21**: Minimum required version
- **Profiles**: `local-dev` (fast iteration), `default` (full CI build with coverage)

### Module Dependency Graph

```
portfolio-target-definition      # Eclipse target platform definition
        ↓
name.abuchen.portfolio.pdfbox1   # PDFBox v1 support (legacy)
name.abuchen.portfolio.pdfbox3   # PDFBox v3 support (modern)
        ↓
name.abuchen.portfolio           # Core domain & business logic
        ↓
name.abuchen.portfolio.bootstrap # Application bootstrap & lifecycle
        ↓
name.abuchen.portfolio.ui        # User interface layer
        ↓
name.abuchen.portfolio.feature   # Eclipse feature definition
        ↓
portfolio-product                # Product assembly & packaging

Test Infrastructure:
name.abuchen.portfolio.junit     # Shared test utilities
name.abuchen.portfolio.tests     # Core module tests
name.abuchen.portfolio.ui.tests  # UI module tests
```

### Core Module Structure

```
name.abuchen.portfolio/                 # Core business logic (domain layer)
├── model/                              # Domain model (Client, Portfolio, Security, Account, Transaction)
├── datatransfer/                       # Import/Export infrastructure
│   ├── pdf/                           # PDF extractors (100+ banks: DKB, Trade Republic, etc.)
│   ├── csv/                           # CSV import/export
│   ├── ibflex/                        # Interactive Brokers Flex Query
│   └── actions/                       # Import action handlers
├── money/                              # Multi-currency support (Money, ExchangeRate, CurrencyUnit)
├── snapshot/                           # Performance calculations & analytics
│   ├── filter/                        # Client filtering (security, classification, transaction)
│   ├── balance/                       # Balance calculations
│   ├── security/                      # Security-level performance
│   └── trades/                        # Trade analysis
├── online/                             # External data feeds & quote providers
│   └── impl/                          # Quote feed implementations (Yahoo, Alpha Vantage, etc.)
├── math/                               # Financial calculations (IRR, NPV, risk metrics)
├── checks/                             # Validation framework (Check/Issue/QuickFix pattern)
├── events/                             # Event-driven communication (ChangeEvent, SecurityChangeEvent)
├── json/                               # JSON/Protobuf persistence layer
├── oauth/                              # OAuth authentication for external services
└── util/                               # Core utilities (TextUtil, Pair, Interval)

name.abuchen.portfolio.ui/              # User interface layer (presentation)
├── views/                              # Main application views
│   ├── dashboard/                     # Dashboard widgets & configuration
│   ├── taxonomy/                      # Taxonomy/classification views
│   ├── payments/                      # Payment analysis views
│   ├── holdings/                      # Holdings views
│   ├── trades/                        # Trade analysis views
│   └── securitychart/                 # Security charting
├── dialogs/                            # Modal dialogs and forms
├── handlers/                           # Command handlers (40+ menu/toolbar actions)
│   └── tools/                         # Tool command handlers
├── editor/                             # File editing infrastructure (PortfolioPart, ClientInput)
├── wizards/                            # Multi-step workflow dialogs
│   ├── client/                        # Client creation/migration wizards
│   ├── datatransfer/                  # Import/export wizards
│   ├── security/                      # Security management wizards
│   └── splits/                        # Stock split wizards
├── parts/                              # UI parts (reusable UI components)
├── addons/                             # Eclipse E4 addons
├── preferences/                        # Settings & preferences pages
├── util/                               # UI utilities (Colors, chart builders, formatters)
└── theme/                              # Theming support (dark mode, etc.)

name.abuchen.portfolio.bootstrap/       # Application lifecycle management
name.abuchen.portfolio.junit/           # Test utilities (TestUtilities, mock data generators)
name.abuchen.portfolio.tests/           # JUnit tests for core module
name.abuchen.portfolio.ui.tests/        # SWTBot tests for UI module
name.abuchen.portfolio.pdfbox1/         # PDFBox 1.x integration (legacy banks)
name.abuchen.portfolio.pdfbox3/         # PDFBox 3.x integration (modern banks)
name.abuchen.portfolio.feature/         # Eclipse feature definition
portfolio-target-definition/            # Target platform (Eclipse dependencies)
portfolio-product/                      # Product configuration & branding
```

### Key Entry Points

#### Domain Layer (Core Business Logic)
- **`Client.java`** - Root aggregate containing all user data (portfolios, accounts, securities, taxonomies)
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/model/Client.java`
  - Responsibilities: Domain root, data container, property change notifications

- **`ClientFactory.java`** - Client persistence management (load/save)
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/model/ClientFactory.java`
  - Responsibilities: XStream XML persistence, Protobuf serialization, encryption, versioning

- **`Portfolio.java`** - Portfolio entity with transactions
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/model/Portfolio.java`
  - Responsibilities: Portfolio transactions (buy/sell/transfer), balance tracking

- **`Account.java`** - Account entity with transactions
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/model/Account.java`
  - Responsibilities: Account transactions (deposits/withdrawals/dividends/fees), balance tracking

- **`Security.java`** - Security entity with prices
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/model/Security.java`
  - Responsibilities: Security master data, historical prices, quote feed configuration

#### Data Transfer Layer (Import/Export)
- **`AbstractPDFExtractor.java`** - Base class for PDF bank statement parsers
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java`
  - Responsibilities: PDF parsing framework, transaction extraction, forex handling

- **`Extractor.java`** - Generic extractor interface
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/Extractor.java`
  - Responsibilities: Import contract for CSV, PDF, and other formats

- **`ImportAction.java`** - Import action representation
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ImportAction.java`
  - Responsibilities: Represents extractable transaction/security, provides user choices

#### UI Layer (Presentation)
- **`PortfolioPart.java`** - Primary UI editor for portfolio files
  - Location: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/editor/PortfolioPart.java`
  - Responsibilities: Main editor part, navigation, view lifecycle, save/load coordination

- **`AbstractFinanceView.java`** - Base class for all finance views
  - Location: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/views/AbstractFinanceView.java`
  - Responsibilities: View lifecycle, reporting period selection, context menu integration

- **`ClientInput.java`** - Client input abstraction for editor
  - Location: `name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/editor/ClientInput.java`
  - Responsibilities: File-backed client representation, dirty state tracking

#### Command Handlers (Menu/Toolbar Actions)
- **`handlers/`** - 40+ command handlers implementing menu and toolbar actions
  - Examples: `ImportPDFHandler.java`, `ExportHandler.java`, `NewFileHandler.java`, `UpdateQuotesHandler.java`
  - Pattern: E4 command handler pattern with @Execute methods

#### Performance & Analytics
- **`ClientPerformanceSnapshot.java`** - Performance calculations
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/snapshot/ClientPerformanceSnapshot.java`
  - Responsibilities: IRR, absolute performance, earnings, taxes, fees

- **`PortfolioSnapshot.java`** - Portfolio state at point in time
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/snapshot/PortfolioSnapshot.java`
  - Responsibilities: Holdings, valuations, positions at specific date

- **`IRR.java`** - Internal Rate of Return calculations
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/math/IRR.java`
  - Responsibilities: Newton-Raphson IRR calculation

#### Validation Framework
- **`Checker.java`** - Validation framework coordinator
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/checks/Checker.java`
  - Responsibilities: Run checks, collect issues, provide quick fixes

- **`Check.java`** - Individual validation check interface
- **`Issue.java`** - Validation issue representation
- **`QuickFix.java`** - Automated fix for validation issues

#### Event System
- **`ChangeEvent.java`** - Generic change event
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/events/ChangeEvent.java`

- **E4 Event Broker** - Eclipse E4 event bus for UI updates
- **PropertyChangeSupport** - Java Beans property changes (in domain model)

## Data Model Core Concepts

### Entity Relationships
```
Client (Root Aggregate)
├── Securities[]                → SecurityPrices[] (historical quotes)
├── Watchlists[]               → Security references (tracking lists)
├── Portfolios[]               → PortfolioTransactions[] (buy/sell/transfer)
├── Accounts[]                 → AccountTransactions[] (deposits/dividends/fees)
├── Taxonomies[]               → Classifications[] (hierarchical categorization)
├── Dashboards[]               → Dashboard widgets & configuration
├── InvestmentPlans[]          → Automated investing schedules
├── Settings                   → Client-specific settings
└── Properties                 → Key-value metadata
```

### Transaction Types & Flows

**PortfolioTransaction Types:**
- `BUY` - Purchase of securities (increases position)
- `SELL` - Sale of securities (decreases position)
- `DELIVERY_INBOUND` - Securities received without payment (e.g., transfer, inheritance)
- `DELIVERY_OUTBOUND` - Securities delivered without payment (e.g., donation)
- `TRANSFER_IN` - Transfer from another portfolio (with linked TRANSFER_OUT)
- `TRANSFER_OUT` - Transfer to another portfolio (with linked TRANSFER_IN)

**AccountTransaction Types:**
- `DEPOSIT` - Cash inflow (external deposit)
- `REMOVAL` - Cash outflow (external withdrawal)
- `INTEREST` - Interest income
- `INTEREST_CHARGE` - Interest expense
- `DIVIDEND` - Dividend payment from security
- `FEES` - Account fees, custody fees
- `FEES_REFUND` - Fee refunds
- `TAXES` - Tax payments (withholding, capital gains)
- `TAX_REFUND` - Tax refunds
- `BUY` - Cash outflow for security purchase (linked to PortfolioTransaction)
- `SELL` - Cash inflow from security sale (linked to PortfolioTransaction)
- `TRANSFER_IN` - Cash transfer from another account
- `TRANSFER_OUT` - Cash transfer to another account

**Cross-References:**
- Portfolio BUY/SELL transactions are linked to corresponding Account BUY/SELL transactions
- Transfers maintain referential integrity between source and target accounts/portfolios

### Persistence Mechanisms

**XStream XML (Legacy):**
- File extension: `.xml`
- Human-readable XML format
- Used for backward compatibility
- Supports encrypted files (AES encryption)

**Protobuf (Modern):**
- File extension: `.portfolio` (binary), `.json` (JSON)
- More efficient serialization via Protocol Buffers
- Defined in `name.abuchen.portfolio/protos/`
- JSON package (`name.abuchen.portfolio/src/name/abuchen/portfolio/json/`) handles conversion

**Encryption:**
- AES encryption for sensitive data
- SecretKey management in Client instance
- SaveFlag.ENCRYPTED controls encryption state

**Versioning:**
- Client.version tracks file format version (currently v67)
- ClientFactory.upgradeModel() handles migration from older versions
- Backward compatibility maintained across versions

### Event System Architecture

**PropertyChangeSupport (Domain Layer):**
- Client, Portfolio, Account, Security fire property change events
- Listeners: UI components, calculated fields, validation framework
- Pattern: Java Beans observer pattern

**E4 Event Broker (UI Layer):**
- Eclipse E4 publish-subscribe event bus
- Topics defined in `ChangeEventConstants`
- Events: `ChangeEvent`, `SecurityChangeEvent`, `SecurityCreatedEvent`
- Used for cross-part communication, view updates

**Event Flow:**
```
User Action (UI)
    ↓
Handler modifies domain model
    ↓
Domain model fires PropertyChangeEvent
    ↓
Client publishes E4 ChangeEvent via broker
    ↓
UI parts receive event and refresh
```

### Validation System (Check/Issue/QuickFix Pattern)

**Framework:**
- **Checker**: Runs all registered checks, collects issues
- **Check**: Individual validation rule (e.g., missing exchange rates, unused securities)
- **Issue**: Validation problem with severity and description
- **QuickFix**: Automated remediation action

**Examples:**
- `checks/impl/MissingExchangeRatesCheck.java` - Detects missing forex data
- `checks/impl/MissingSecurityPricesCheck.java` - Detects securities without prices
- `checks/impl/UnusedSecuritiesCheck.java` - Finds securities without transactions

**Usage:**
- Run on client load, on-demand via UI
- Issues displayed in dedicated view
- One-click quick fixes for common problems

### Multi-Currency Support

**Core Types:**
- **`Money`** - Amount + currency (e.g., "100.00 EUR")
  - Immutable value object
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/money/Money.java`

- **`CurrencyUnit`** - Currency identifier (ISO 4217 codes)
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/money/CurrencyUnit.java`

- **`ExchangeRate`** - Currency conversion rate
  - Location: `name.abuchen.portfolio/src/name/abuchen/portfolio/money/ExchangeRate.java`
  - Supports base/term currency pairs (e.g., EUR/USD)

**Exchange Rate Providers:**
- European Central Bank (ECB)
- Manual exchange rates
- Stored in Client for historical accuracy

**Transaction Forex:**
- Transactions support dual-currency recording (gross amount in foreign currency, base currency conversion)
- Attributes: `fxGross`, `baseCurrency`, `termCurrency`, `exchangeRate`

### Performance Calculations

**Key Metrics:**
- **IRR (Internal Rate of Return)**: Time-weighted return calculation
  - Implementation: Newton-Raphson method in `math/IRR.java`

- **TWR (Time-Weighted Return)**: Performance independent of cash flows
  - Implementation: `snapshot/ClientPerformanceSnapshot.java`

- **Absolute Performance**: Simple gain/loss calculation

- **Risk Metrics**: Volatility, max drawdown, Sharpe ratio
  - Implementation: `math/Risk.java`

**Snapshot Architecture:**
- `ClientSnapshot` - Client state at specific date
- `PortfolioSnapshot` - Portfolio holdings and valuations
- `AccountSnapshot` - Account balance at date
- `SecurityPosition` - Holding quantity and value
- `PerformanceIndex` - Time series of performance data

**Calculation Flow:**
```
Client + ReportingPeriod
    ↓
ClientPerformanceSnapshot.create()
    ↓
Builds snapshots for each date in period
    ↓
Calculates IRR, TWR, earnings, fees, taxes
    ↓
Returns PerformanceIndex (time series)
```
