# Contributing to Portfolio Performance

## Contents


  - [Development setup](#development-setup)
    - [Install Eclipse](#install-eclipse)
    - [Install Eclipse plug-ins](#install-eclipse-plug-ins)
      - [Install from the simultaneous release update site](#install-from-the-simultaneous-release-update-site)
    - [Configure Eclipse](#configure-eclipse)
  - [Project setup](#project-setup)
    - [Source code](#source-code)
    - [Setup Target Platform](#setup-target-platform)
    - [Launch Portfolio Performance](#launch-portfolio-performance)
    - [Build with Maven](#build-with-maven)
  - [Contribute code](#contribute-code)
  - [Translations](#translations)
    - [Label descriptions](#label-descriptions)
  - [Images, Logo and color](#images-logo-and-color)
    - [Format and size](#format-and-size)
    - [Color code and status](#color-code-and-status)
  - [Interactive-Flex-Query Importer](#interactive-flex-query-importer)
    - [Source location](#source-location)
    - [Imported transactions](#imported-transactions)
    - [Naming conventions for detected elements](#naming-conventions-for-detected-elements)
    - [Test cases](#test-cases)
  - [PDF importer](#pdf-importer)
    - [Debug information](#debug-information)
    - [Source location](#source-location-1)
    - [Imported transactions](#imported-transactions-1)
    - [Anatomy of a PDF importer](#anatomy-of-a-pdf-importer)
    - [Naming conventions for detected values](#naming-conventions-for-detected-values)
    - [Auxiliary classes](#auxiliary-classes)
    - [Formatting of PDF importer](#formatting-of-pdf-importer)
    - [Test cases](#test-cases-1)
    - [Regular expressions](#regular-expressions)
  - [Trade calendar](#trade-calendar)
    - [Source location](#source-location-2)
    - [Anatomy of a trade calendar](#anatomy-of-a-trade-calendar)
    - [Test cases](#test-cases-2)

## Development setup


### Install Eclipse

* Java 21, for example from [Azul](https://www.azul.com/downloads/)

* [Eclipse IDE](https://www.eclipse.org/downloads/packages/) - PP is build using the Eclipse RCP (Rich Client Platform) framework. Therefore it generally does not make sense to use other IDEs. Download the **Eclipse IDE for RCP and RAP Developers** package.

Optionally, install language packs for Eclipse:
 * `Menu` --> `Help` --> `Install New Software`
 * Use the following update site:
   ```
   https://download.eclipse.org/technology/babel/update-site/latest/
   ```
 * Select the language packs you want to install
 * By default, Eclipse uses the host operating system language (locale).
   To force the use of another language, use the **-nl** parameter:
   ```
   eclipse -nl de
   ```


### Install Eclipse plug-ins

Optionally, install via the Eclipse Marketplace (drag and drop the *Install* button to your Eclipse workspace)

* [Eclipse PDE (Plug-in Development Environment)](https://marketplace.eclipse.org/content/eclipse-pde-plug-development-environment) (skip if you installed the *Eclipse IDE for RCP and RAP Developers*)
* [Infinitest](https://marketplace.eclipse.org/content/infinitest)
* [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor)
* [Checkstyle Plug-In](https://marketplace.eclipse.org/content/checkstyle-plug)
* [SonarQube for IDE](https://marketplace.eclipse.org/content/sonarqube-ide)
* [Launch Configuration DSL](https://marketplace.eclipse.org/content/launch-configuration-dsl)
* [Darkest Dark Theme with DevStyle](https://marketplace.eclipse.org/content/darkest-dark-theme-devstyle) (Programming and chilling in DarkMode :sunglasses:)


#### Install from the simultaneous release update site

`Menu` --> `Help` --> `Install New Software`

Pick `Latest Eclipse Simultaneous Release` from the dropdown menu.

* M2E PDE Integration (skip if you installed the *Eclipse IDE for RCP and RAP Developers*)
	- Under `General Purpose Tools` select the `M2E PDE Integration`
* Eclipse e4 Tools Developer Resources
	- Under `General Purpose Tools` select the `Eclipse e4 Tools Developer Resources`


### Configure Eclipse

Configure the following preferences (`Menu` --> `Window` --> `Preferences`)

* `Java` --> `Editor` --> `Save Actions`
	- Activate `Format Source Code` and then `Format edited lines`
 	- Activate `Organize imports`
* `Java` --> `Editor` --> `Content Assist`
	- Activate `Add import instead of qualified name`
	- Activate `Use static imports`
* `Java` --> `Editor` --> `Content Assist` --> `Favorites`
	- Click on `New Type...` and add the following favorites
		- `name.abuchen.portfolio.util.TextUtil`
		- `name.abuchen.portfolio.datatransfer.ExtractorUtils`
		- `name.abuchen.portfolio.datatransfer.ExtractorMatchers`
		- `name.abuchen.portfolio.datatransfer.ExtractorTestUtilities`
		- `name.abuchen.portfolio.junit.TestUtilities`
* `Java` --> `Installed JREs`
	- Add the Java 21 JDK


## Project setup
 
For further discussion, check out the thread in the [(German) Forum](https://forum.portfolio-performance.info/t/verbesserungen-im-source-code-in-github-einbringen/7063).


### Source code

To contribute to Portfolio Performance, create a fork, clone the repository, make and push changes to your repository, and then create a pull request.

* [Create your own fork](https://docs.github.com/en/get-started/quickstart/fork-a-repo)
* Within Eclipse, [clone your repository](https://www.vogella.com/tutorials/EclipseGit/article.html#exercise-clone-an-existing-repository). In the last step, choose to *import all existing Eclipse projects*.
* Within Eclipse, [import projects from an existing repository](https://www.vogella.com/tutorials/EclipseGit/article.html#exercise-import-projects-from-an-existing-repository)


### Setup Target Platform

* Open the `portfolio-target-definition` project
* Open the `portfolio-target-definition.target` file with the Target Editor (this may take a while as it requires Internet access). If you just get an XML file, use right click and chose Open With *Target Editor*
* In the resulting editor, click on the "Set as Active Target Platform" link at the top right (this may also take a while)


### Launch Portfolio Performance

PP uses [Eclipse Launch Configuration DSL](https://github.com/mduft/lcdsl) to define Eclipse launch configurations in a OS independent way.

First, add the *Launch Configuration* view to your workspace:
* `Menu` --> `Window` --> `Show View` --> `Other...` --> `Debug` --> `Launch Configuration`

**To run the application**, select `Eclipse Application` --> `PortfolioPerformance` and right-click *Run*.

**To run the tests**, select under `JUnit Plug-in Tests` --> `PortfolioPerformance_Tests` or `PortfolioPerformance_UI_Tests`.


### Build with Maven

It is not required to use [Maven](https://maven.apache.org) as you can develop using the Eclipse IDE with the setup above. The Maven build is used for the [Github Actions](https://github.com/portfolio-performance/portfolio/actions) build.

The Maven build works fine when `JAVA_HOME` points to an (Open-)JDK 21 installation.

Linux/macOS
```
export MAVEN_OPTS="-Xmx4g"
mvn -f portfolio-app/pom.xml clean verify
```

Windows
```
set MAVEN_OPTS="-Xmx4g"
mvn -f portfolio-app/pom.xml -Denforcer.skip=true clean verify
```

Hint: if you run into resolution problems, try deleting the `~/.m2/repository/p2` directory. Apparently, when switching to Maven Tycho 3, there are some layout changes.


## Contribute code

* Write a [good commit message](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html) in English
* If the change is related to a Github issue, add a line `Closes #<ISSUE NUMBER>` after an empty line
* If the change is related to an thread in the forum, add a line `Issue: https://...` with the link to the post in the forum
* Format the source code. The formatter configuration is part of the project source code. Exception: Do *not* reformat the PDF importer source code. Instead, carefully insert new code into the existing formatting.
* Add [test cases](https://github.com/portfolio-performance/portfolio/tree/master/name.abuchen.portfolio.tests) where applicable. Today, there are no tests that test the SWT UI. But add tests for all calculations.
* Do not merge the the master branch into your feature branch. Instead, [rebase](https://docs.github.com/en/get-started/using-git/about-git-rebase) your local changes to the head of the master branch.
* [Create a Pull Request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request) - for example using [GitHub Desktop](https://desktop.github.com/) using this [tutorial](https://docs.github.com/en/desktop/installing-and-configuring-github-desktop/overview/creating-your-first-repository-using-github-desktop)


## Translations

Currently, Portfolio Performance is translated into 13 languages.

To contribute a new language or assist with translations:

* Register with the [POEditor project](https://poeditor.com/join/project?hash=4lYKLpEWOY).
* Open a [ticket](https://github.com/portfolio-performance/portfolio/issues/new/choose) if you plan to a add a new language and need test builds.
* Before we create a new release, we download and merge the updated translations into the code base.

If you are contributing code:

- Use [Source -> Externalize Strings](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/reference/ref-wizard-externalize-strings.htm?cp=1_4_10_3) to externalize strings.
- Utilize [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor) to edit and format resource files, as the update process from POEditor relies on the same format.
- Translate new labels into all existing languages using [DeepL](https://www.deepl.com).

### Label descriptions

For naming externalized labels:

- Use ```Label```, ```Msg```, ```Column``` as common prefixes for short labels, longer messages, and column headers respectively.
- Use specific prefixes like ```PDF```, ```CSV```, ```Preferences```, etc. if the area is big and distinct enough, but avoid creating new areas.
- Follow the label naming pattern used in the code area you are contributing to.


## Images, Logo and color

Images and logos used must be subject to [Creative Commons CC0](https://creativecommons.org/publicdomain/zero/1.0/legalcode).

* We only use icons from [iconmonstr.com](https://iconmonstr.com).
* If a color change icon is used, the passive state is gray and the active state is orange.
* Please add all used images, logos and icons in the [Images](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.ui/src/name/abuchen/portfolio/ui/Images.java) file.


### Format and size

Images, logos and icons are to be created as Portable Network Graphic ([PNG](https://en.wikipedia.org/wiki/Portable_Network_Graphics)) format.

* The background must be transparent.
* The basic format is 16x16px.
* A designation is to be chosen as name. (e.g. information.png)
* The file name must be written in lower case letters.
* It must be created in at least two sizes. 16x16px and 32x32px.

Designation basic format as an example:

* `information.png` (16x16px)
* `information@2x.png` (32x32px)
* ...


### Color code and status

| Color	| Color code (hex)	| Color code (RGB)	| Used for	|
| :---------	| :----------- 	| :----------- 	| :----------- 	|
| orange (logo)	| ![#f18f01](https://placehold.co/15x15/f18f01/f18f01.png) `#f18f01`	| `rgb(241, 143, 1)`	| Activated (ex: filter)	|
| blue (logo)	| ![#0e6e8e](https://placehold.co/15x15/0e6e8e/0e6e8e.png) `#0e6e8e`	| `rgb(14, 110, 142)`	|							|
| green (logo)	| ![#9ac155](https://placehold.co/15x15/9ac155/9ac155.png) `#9ac155`	| `rgb(154, 193, 85)`	|							|
| dark blue		| ![#95a4b3](https://placehold.co/15x15/95a4b3/95a4b3.png) `#95a4b3`	| `rgb(149, 164, 179)`	| Default color for icons	|
| red			| ![#d11d1d](https://placehold.co/15x15/d11d1d/d11d1d.png) `#d11d1d`	| `rgb(209, 29, 29)`	| Error, Fault				|
| yellow		| ![#ffd817](https://placehold.co/15x15/ffd817/ffd817.png) `#ffd817`	| `rgb(255, 216, 23)`	| Warning					|


## Interactive-Flex-Query Importer

The Interactive Broker Flex Query importer handles XML-compliant Interactive Broker Activity Statements. The IBFlexStatementExtractor implements sophisticated XML parsing with security features and multi-currency support.

**Key Features:**
* Secure XML parsing with XXE prevention and DOCTYPE restrictions
* Exchange mapping from Interactive Broker to Yahoo Finance symbols
* Multi-asset support: Stocks, Funds, Options, Certificates, Futures, Warrants, Commodities
* Automated currency conversion and transaction matching
* Post-processing for transaction pairing and tax treatment

**Asset Category Support:**
* STK (Stocks), FUND (Funds), IND (Indices), OPT (Options)
* IOPT (Certificates), FUT (Futures), FOP (Future Options)
* WAR (Warrants), CMDTY (Commodities), CASH transactions

### Source location

Interactive-Flex-Query importer: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ibflex/`

Test cases: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/ibflex/`


### Imported transactions

The IBFlexStatementExtractor processes complex transaction types:

**Portfolio Transactions:**
* Buy/Sell transactions with multi-currency support
* Options transactions (calls, puts, assignments, expirations)
* Corporate actions (stock splits, spin-offs, mergers)
* Automated exchange rate conversion when needed

**Account Transactions:**
* Deposits/Withdrawals (based on amount sign)
* Dividends and Payment In Lieu Of Dividends
* Withholding Tax (including tax refunds)
* Broker Interest (received/paid)
* Management/Advisor Fees (including refunds)

**Advanced Processing:**
* Transaction pairing for complex trades
* Security matching across different exchanges
* Automated currency conversion using exchange rates
* Post-processing to merge related transactions


### Naming conventions for detected elements

The importer extracts the following elements:

* `type` --> The transaction type e.g. buy, sell, deposit, removal, dividends, taxes, fees
* `buySell` --> The portfolio transaction type e.g. buy, sell.
* `symbol` --> The symbol of the instrument traded.
* `isin` --> International Securities Identification Number
* `conid` --> The conid of the instrument traded. Same as the security code number (wkn).
* `cusip` --> The CUSIP of the instrument traded. Same as the security code number (wkn).
* `quantity` --> The number of units for the transaction.
* `multiplier` --> The multiplier of the contract traded.
* `proceeds` --> Calculated by multiplying the quantity and the transaction price. The proceeds figure will be negative for buys and positive for sales.
* `reportDate` --> The date of the statement.
* `tradeDate` --> The date of the execution.
* `tradeTime` --> The time of the execution.
* `dateTime` --> The date and time of the statement.
* `netCash` --> Net cash is calculated by adding the proceeds plus tax plus commissions. (buy and sell)
* `amount` --> Amount for account transactions
* `currency` --> Currency of the total amount.
* `fxRateToBase` --> The conversion rate from asset currency to base currency.
* `description` --> The description of the instrument traded.
* `taxes` --> The total amount of tax for the transaction.
* `ibCommission` --> The total amount of commission for the transaction.
* `ibCommissionCurrency` --> The currency denomination of the trade.
* `accountId` --> The ID of the account.
* `assetCategory` --> e.g. Stock, Options, Futures
* `tradeID` --> The ID of the trade.
* `transactionID` --> The ID of the transaction.

All available elements can be found at [ibkrguides.com](https://ibkrguides.com/reportingreference/reportguide/tradesfq.htm).

### Test cases

Interactive Broker test files are XML Activity Statements. Users anonymize personal information and account numbers while preserving the XML structure and data relationships.

**Test File Requirements:**
* Valid XML structure with proper escaping
* Anonymized account IDs, account numbers, and personal data
* Preserved transaction relationships and amounts
* Include all relevant asset categories and transaction types

**Naming Convention:**
* `testIBFlexStatementFile01.xml` (numbered sequence)
* `testOptionsTransactions01.xml` (specific feature focus)
* `testMultiCurrency01.xml` (currency conversion scenarios)

**Test Coverage:**
* Basic buy/sell transactions across asset categories
* Complex options scenarios (calls, puts, assignments)
* Multi-currency transactions with conversion
* Corporate actions and special situations
* Account transactions (deposits, fees, dividends)

**Current Test Examples:**
* [IBFlexStatementExtractorTest](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/ibflex/IBFlexStatementExtractorTest.java)
* Tests include comprehensive transaction type coverage
* Exchange mapping validation
* Currency conversion verification


## PDF importer

Importers are created for each supported bank and/or broker. The process works like this:
* The user selects one or more PDF files via the import menu (or drags and drops multiple PDF files to the sidebar navigation)
* Each PDF file is converted to an array of strings; one entry per line using PDFBox3 (or PDFBox1 for legacy)
* The PDFImportAssistant coordinates multiple extractors and presents each file to all registered extractors
* Each extractor applies regular expressions to extract transactions and returns matching results
* Results are collected, deduplicated, and presented to the user for import

If you want to add an importer for a new bank or a new transaction type, check out the current reference implementations for naming conventions, structure, formatting, etc.


### Debug information

There is a quick way to generate the required debug information from a pdf file:

`File` => `Import` => `Debug: Create text from PDF...`

Make sure to remove all personal data.
However, make also sure that you do not insert any new lines, line breaks, spaces or similar.

### Source location

PDF importer: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/`

Test cases: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/`

The naming convention is BANK**Extractor** and BANK**ExtractorTest** for extractor class and test class respectively.


### Imported transactions

Portfolio Performance separates between [PortfolioTransaction](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/model/PortfolioTransaction.java) (booked on a securities account) and [AccountTransaction](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/model/AccountTransaction.java) (booked on a cash account). The available types are defined as enum within the file, for example for purchase (BUY) and sale (SELL) of securities, etc.


### Anatomy of a PDF importer

The structure of the PDF importers is as follows:

* Client
	* `addBankIdentifier` --> Unique recognition feature of the PDF document
* Bank name
  	* `getLabel` --> display label of bank/broker, e.g. *Deutsche Bank Privat- und Geschäftskunden AG*
* Transaction types (basic types)
	* `addBuySellTransaction` --> Purchase and sale (single settlement)
	* `addSummaryStatementBuySellTransaction`  --> Purchase and sale (multiple settlements)
	* `addBuyTransactionFundsSavingsPlan` --> Savings plans
	* `addDividendeTransaction` --> Dividends
	* `addTaxTreatmentForDividendeTransaction` --> Tax treatment for dividends
	* `addAdvanceTaxTransaction` --> Advance tax payment
  	* `addCreditcardStatementTransaction` --> Credit card transactions
  	* `addAccountStatementTransaction` --> Giro account transactions
  	* `addDepotStatementTransaction` --> Securities account transactions (settlement account)
  	* `addTaxStatementTransaction` --> Tax settlement
  	* `addDeliveryInOutBoundTransaction` --> Inbound and outbound deliveries
  	* `addTransferInOutBoundTransaction` --> Transfer in and outbound deliveries
  	* `addReinvestTransaction` --> Reinvestment transaction
  	* `addTaxReturnBlock` --> Tax refund
  	* `addFeeReturnBlock` --> Fee refund
* Taxes and fees
  	* `addTaxesSectionsTransaction` --> handling of taxes
  	* `addFeesSectionsTransaction` --> handling of fees
* Overwrite the value extractor methods if the documents work with non-standard (English, German) locales:
	* Example: [Bank SLM](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BankSLMPDFExtractor.java) (de_CH)
	* Example:  [Baader Bank AG](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BaaderBankPDFExtractor.java) (de_DE + en_US)
* Add post processing on imported transaction using a `postProcessing` method:
	* Example: [ComDirect](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/ComdirectPDFExtractor.java)


### Naming conventions for detected values

The importers are structured according to the following scheme and the mapping variables are to be adhered to as far as possible:

* Type (Optional)
  * `type` --> Exchange of the transaction pair (e.g. from purchase to sale)
* Security identification
  * `name` --> Security name
  * `isin` --> International Securities Identification Number
  * `wkn` --> Security code number
  * `tickerSymbol` --> Tickersymbol
  * `currency` --> Security currency
* Shares of the transaction
  * `shares` --> Shares
* Date and time
  * `date` --> Date
  * `time` --> Time (optional)
* Total amount (with fees and taxes)
  * `amount` --> Amount e.g. 123,15
  * `currency` --> Currency of the total amount
* Foreign currency
  * `gross` --> Total amount in transaction currency without fees and taxes
  * `currency` --> Currency of the total amount
  * `fxGross` --> Total amount in foreign currency without fees and taxes
  * `fxCurrency` --> Currency of the total amount in foreign currency
* Exchange rate
  * `exchangeRate` --> Foreign currency exchange rate
  * `baseCurrency` --> Base currency
  * `termCurrency` --> Foreign currency
* Notes (Optional)
  * `note` --> Notes e.g. quarterly dividend, limits, transaction number
* Tax section
   * `tax` --> Amount
   * `currency` --> Currency
   * `withHoldingTax` --> Withholding tax
   * `creditableWithHoldingTax` --> Creditable withholding tax
* Fee section
   * `fee` --> Amount
   * `currency` --> Currency

**Current reference implementations** (most up-to-date patterns):
* [Baader Bank AG](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BaaderBankPDFExtractor.java) - Comprehensive modern implementation with multiple transaction types
* [Comdirect Bank AG](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/ComdirectPDFExtractor.java) - Advanced post-processing and document pairing
* [Saxo Bank A/S](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/SaxoBankPDFExtractor.java) - Modern pattern matching with complex `.oneOf()` sections


### Auxiliary classes

The utility classes handle standardized conversions and processing:

* [AbstractPDFExtractor](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java) - Base class for all PDF extractors
* [PDFImportAssistant](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFImportAssistant.java) - Coordinates the import process and manages all extractors
* [PDFInputFile](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFInputFile.java) - Handles PDF-to-text conversion and test case loading
* [ExtractorUtils](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ExtractorUtils.java) - Utility functions for amount conversion, tax/fee processing
* [ExtrExchangeRate](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ExtrExchangeRate.java) - Handles foreign currency processing
* [ExtractorMatchers](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/ExtractorMatchers.java) - Modern matcher patterns for test cases

Use the [Money](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/money/Money.java) class when working with amounts (includes currency and value rounded to cents). Use *BigDecimal* for exchange rates and currency conversions.

Use [TextUtil](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/util/TextUtil.java) for string manipulation such as trimming and stripping whitespace. PDF text conversion has corner cases not supported by standard Java methods.


### Formatting of PDF importer

Due to the many comments with text fragments from the PDF documents, we do not auto-format the PDF importer class files. Instead, carefully insert new code into the existing formatting manually. To protect formatting from automatic formatting, use the `@formatter:off` and `@formatter:on`.

Please take a look at the formatting and structure in the other PDF importers! Example: [V-Bank AG](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/VBankAGPDFExtractor.java)


### Test cases

Via the application menu, users can create a test case file. The test file is the extracted text from the PDF documents. Users then anonymize the text by replacing personal idenfiable information and account numbers with alternative text.

* The test files should not be modified beyond the anonymization
* All source code (including the test files) is stored in UTF-8 encoding
* Follow the naming convention for test files (type in the local language, two digit counter):
	* `Buy01.txt, Sell01.txt` --> Purchase and sale (single settlements) (e.g. SecurityBuy01.txt or SecuritySale01.txt)
	* `Dividend01.txt` --> Dividends (single statements)
	* `SteuermitteilungDividende01.txt` --> Tax settlement for dividends (single settlement)
	* `SammelabrechnungKaufVerkauf01.txt` --> Purchase and sale (multiple settlements)
	* `Wertpapiereingang01.txt` --> Incoming securities
	* `Wertpapierausgang01.txt` --> Outgoing securities
	* `Vorabpauschale01.txt` --> Advance taxes
	* `GiroKontoauzug01.txt` --> Giro account statement
	* `KreditKontoauszug01.txt` --> Credit card account statement
	* `Depotauszug01.txt` --> security account transaction history (settlement account)
* **Modern test patterns** (preferred - use ExtractorMatchers):
	* [Baader Bank](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/baaderbank/BaaderBankPDFExtractorTest.java) - Modern ExtractorMatchers usage with `purchase()`, `security()`, `hasAmount()`, etc.
	* [Comdirect](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/comdirect/ComdirectPDFExtractorTest.java) - Complex scenarios with document pairing and post-processing
	* [Saxo Bank](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/saxobank/SaxoBankPDFExtractorTest.java) - Multi-language and complex pattern matching tests
	*  one transaction per PDF (old version): [Erste Bank Gruppe](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java) - see `testWertpapierKauf06()` and `testDividende05()`
	* supporting securities with multiple currencies: [Erste Bank Gruppe](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java) with `testWertpapierKauf09()` / `testWertpapierKauf09WithSecurityInEUR()` and `testDividende10()`/`testDividende10WithSecurityInEUR()`
		* Background: in the PP model, the currency of the transaction always must match the currency of the security and its historical prices. However, sometimes securities are purchased on an different exchange with prices in an another currency. The importer try to handle this case automatically. This is reflected in the two test cases
	* multiple transactions per PDF: [DKB AG](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/dkb/DkbPDFExtractorTest.java) with `testGiroKontoauszug01()`
	* if transactions are created based on two separate PDF files, use post processing: [Targobank](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/targobank/TargobankPDFExtractorTest.java) with:
		* `testDividende01()` (single import)
		* `testDividende01WithSecurityInEUR()` (single import)
		* `testTaxTreatmentForDividende01()` (single import)
		* `testDividendeWithTaxTreatmentForDividende01()` (simultaneously import)
		* `testDividendeWithTaxTreatmentForDividende01WithSecurityInEUR()` (simultaneously import)
		* `testDividendeWithTaxTreatmentForDividende01WithSecurityInUSD()` (simultaneously import)
		* `testDividendeWithTaxTreatmentReversedForDividende01WithSecurityInEUR()` (simultaneously import)
		* `testDividendeWithTaxTreatmentReversedForDividende01WithSecurityInUSD()` (simultaneously import)

### Regular expressions

To test regular expression you can use [https://regex101.com/](https://regex101.com/).

Beside general good practices for regular expressions, keep in mind:
* all special characters in the PDF document (`äöüÄÖÜß` as well as e.g. circumflex or similar) should be matched by a `.` (dot) because the PDF to text conversion can create different results 
* the special characters `$^{[(|)]}*+?\` in the PDF document are to be escaped
* expression in `.match(" ... ")` is started with an anchor `^` and ended with `$`
* with `.find(" ... ")` do not add anchors as they will be automatically added

Keep in mind that the regular expressions work against text that is automatically created from PDF files. Due to the nature of the process, there can always be slight differences in the text files. The following table collects the regular expressions that **worked well** to match typical values.

| Value	| Example	| Not Helpful	| Works Well	|
| :-----------	| :-----------	| :------------	| :-----------	|
| Date			| 01.01.1970	| `\\d+.\\d+.\\d{4}`	| `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}`				|
|				| 1.1.1970		| `\\d+.\\d+.\\d{4}`	| `[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}`		|
| Time			| 12:01			| `\\d+:\\d+`			| `[\\d]{2}\\:[\\d]{2}}`						|
| ISIN			| IE00BKM4GZ66	| `\\w+`				| `[A-Z]{2}[A-Z0-9]{9}[0-9]`					|
| WKN			| A111X9		| `\\w+`				| `[A-Z0-9]{6}`									|
| Valoren		| 1098758		| `\\w+`				| `[A-Z0-9]{5,9}`									|
| SEDOL			| B5B74S0		| `\\w+`				| `[A-Z0-9]{7}`									|
| CUSIP			| 11135F101		| `\\w+`				| `[A-Z0-9]{9}`									|
| TickerSymbol	| AAPL, BRK.B	| `\\w+`				| `[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?`			|
| 				| BTC, ETH-BTC	| `\\w+`				| `[A-Z0-9]{1,5}(?:[\\-\\/][A-Z0-9]{1,5})?`	|
| Amount		| 751,68		| `[\\d,.]+`			| `[\\.,\\d]+`									|
|				|				|						| `[\\.\\d]+,[\\d]{2}`							|
|				| 74'120.00		| `[\\d.']+`			| `[\\.'\\d]+`									|
|				| 20 120.00		| `[\\d.\\s]+`		| `[\\.\\d\\s]+`									|
| Currency      | EUR			| `\\w+`				| `[A-Z]{3}`										|
| Currency      | € or $		| `\\D`					| `\\p{Sc}`										|


## Trade calendar

Using the application menu, the user can select a trading calendar accordingly globally or for each individual security.
The calendar takes into account the weekends and regional holidays when there is no stock exchange trading.
The trading-free days of the stock exchange itself, if it is a stock exchange calendar, are also taken into account.
The individual trading-free days are stored in a HashMap and made available for further processing, e.g. reporting period, performance index and so on.


### Source location

Trade calendar Manager: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/TradeCalendarManager.java`

Trade calendar Class: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/TradeCalendar.java`

Holiday types: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/HolidayType.java`

Holiday class: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/Holiday.java`

Holiday name: `name.abuchen.portfolio/src/name/abuchen/portfolio/util/HolidayName.java`

Tests: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/util/TradeCalendarTest.java`


### Anatomy of a trade calendar

The structure of the trade calendars is as follows:

* Identification
	* `code` --> Identification nyse --> New York Stock Exchange
* Label
	* `description` --> Display label of trade calendar, e.g., *New York Stock Exchange*
* Days of weekend
	* `weekend` --> Sets the default days for the weekend in the Trade Calendar, e.g., *Saturday and Sunday*

The [HolidayTypes](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/util/HolidayType.java) helps to edit holidays and in the [HolidayName](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/util/HolidayName.java) there are all holidays or trade-free days.


### Test cases

The test cases are checking individual dates that have been stored in TradeCalendarManager.
In these cases we check whether the date to be checked is a trading day for this calendar or not.
We assume that every day is a trade day. (Except e.g. regular weekends).

The structure of the test cases is as follows:

* Starting from the first day of the year with at least three test checks each, e.g. 01.01.20xx, 01.01.20xy, 01.01.20xz until 31.12.20xy of the respective year.
* The regular trading-free days are followed by the one-time trading-free days.
* The respective trade-free day to the test is named in the comment.
* Samples 
	* Trade calendar: [New York Stock Exchange](https://github.com/portfolio-performance/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/util/TradeCalendarTest.java) - see `testTradeCalenderNYSE()`
