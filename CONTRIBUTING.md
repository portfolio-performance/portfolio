# Contributing to Portfolio Performance

## Contents

- [Development Setup](#development-setup)
	- [Install Eclipse](#install-eclipse)
	- [Install Plug-ins](#install-eclipse-plugins)
	- [Configure Eclipse](#configure-eclipse)
- [Project Setup](#project-setup)
	- [Source Code](#source-code)
	- [Target Platform](#setup-target-platform)
	- [Launch Application](#launch-portfolio-performance)
	- [Build with Maven](#build-with-maven)
- [Contribute Code](#contribute-code)
- [Translations](#translations)
- [PDF Importer](#pdf-importer)
	- [Source Location](#source-location)
	- [Imported Transactions](#imported-transactions)
	- [Anatomy of a PDF Importer](#anatomy-of-a-pdf-importer)
	- [Naming Conventions for Detected Values](#naming-conventions-for-detected-values)
	- [Auxiliary classes](#auxiliary-classes)
	- [Formatting of PDF Importer](#formatting-of-pdf-importer)
	- [Test Cases](#test-cases)
	- [Regular Expressions](#regular-expressions)


## Development Setup

### Install Eclipse

* Java 11, for example from [Azul](https://www.azul.com/downloads/)

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
   eclipse.exe -nl de
   ```


### Install Eclipse Plugins

Optionally, install via the Eclipse Marketplace (drag and drop the *Install* button to your Eclipse workspace)

* [Eclipse PDE (Plug-in Development Environment)](https://marketplace.eclipse.org/content/eclipse-pde-plug-development-environment) (skip if you installed the *Eclipse IDE for RCP and RAP Developers*)
* [Infinitest](https://marketplace.eclipse.org/content/infinitest)
* [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor)
* [Checkstyle Plug-In](https://marketplace.eclipse.org/content/checkstyle-plug)
* [SonarLint](https://marketplace.eclipse.org/content/sonarlint)
* [Launch Configuration DSL](https://marketplace.eclipse.org/content/launch-configuration-dsl)
* Eclipse e4 Tools Developer Resources
	- `Menu` --> `Help` --> `Install New Software`
	- Pick *Latest Eclipse Simultaneous Release* from the dropdown menu 
	- Under *General Purpose Tools* select the *Eclipse e4 Tools Developer Resources*


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
		- `name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils`
* `Java` --> `Editor` --> `Installed JREs`
	- Add the Java 11 JDK


## Project Setup
 
For further disucssion, check out the thread in the [(German) Forum](https://forum.portfolio-performance.info/t/verbesserungen-im-source-code-in-github-einbringen/7063).


### Source Code

To contribute to Portfolio Performacne, you create a fork, clone the repository, make and push changes to your repository, and then create a pull request.

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

It is not required to use [Maven](https://maven.apache.org) as you can develop using the Eclipse IDE with the setup above. The Maven build is used for the [Github Actions](https://github.com/buchen/portfolio/actions) build.

The Maven build works fine when `JAVA_HOME` points to an (Open-)JDK 11 installation.

Linux/macOS
```
export MAVEN_OPTS="-Xmx2g"
mvn -f portfolio-app/pom.xml clean verify
```

```
set MAVEN_OPTS="-Xmx2g"
mvn -f portfolio-app\pom.xml -Denforcer.skip=true clean verify
````


## Contribute Code

* Write a [good commit message](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html) in English
* If the change is related to a Github issue, add a line `Issue: #<ISSUE NUMBER>` after an empty line
* If the change is related to an thread in the forum, add a line `Issue: https://...` with the link to the post in the forum
* Format the source code. The formatter configuration is part of the project source code. Exception: Do *not* reformat the PDF importer source code. Instead, carefully insert new code into the existing formatting.
* Add [test cases](https://github.com/buchen/portfolio/tree/master/name.abuchen.portfolio.tests) where applicable. Today, there are no tests that test the SWT UI. But add tests for all calculations.
* Do not merge the the master branch into your feature branch. Instead, [rebase](https://docs.github.com/en/get-started/using-git/about-git-rebase) your local changes to the head of the master branch.
* [Create a Pull Request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request) - for example using [GitHub Desktop](https://desktop.github.com/) using this [tutorial](https://docs.github.com/en/desktop/installing-and-configuring-github-desktop/overview/creating-your-first-repository-using-github-desktop)


## Translations

The project uses Java property files to translate the application into multiple langauges.

There are two ways to contribute translations:
* Register and translate using [POEditor](https://poeditor.com/join/project?hash=4lYKLpEWOY). If you only want to contribute to one language (or fix the translation for existing labels), this is the easiest way. On regular basis we pull the tranlations from POEditor into the source code.
* Update the property files directly. Open the default property file (the one without the language). The *Resource Bundle Editor* (installed above) will detect all existing languages and display a consolidated editor.

When adding new labels,
* right-click in the source editor [Source -> Externalize Strings](https://help.eclipse.org/latest/topic/org.eclipse.jdt.doc.user/reference/ref-wizard-externalize-strings.htm?cp=1_4_10_3)
* use the formatting excactly as done by the Resource Bundle Editor 
* use [DeepL](https://www.deepl.com) to translate new labels into all existing languages


## PDF Importer

Importers are created for each supported bank and/or broker. The process works like this:
* The users selects one or more PDF files via the import menu (or drags and drops multiple PDF files to the sidebar navigation)
* Each PDF file are converted to an array of strings; one entry per line
* Each importer is presented with the strings and applies the regular expresssions to extract transactions

If you want to add an importer for a new bank or a new transaction type, check out the existing importers for naming conventions, structure, formatting, etc.


### Source Location

PDF importer: `name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/`
Test cases: `name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/`

The naming convention is BANK**Extractor** and BANK**ExtractorTest** for extractor class and test class respectively.


### Imported Transactions

PP separates between [PortfolioTransaction](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/model/PortfolioTransaction.java) (booked on a securities account) and [AccountTransaction](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/model/AccountTransaction.java) (booked on a cash account). The available types are defined as enum within the file, for example for purchase (BUY) and sale (SELL) of securities, etc.


### Anatomy of a PDF Importer

The structure of the PDF importers is as follows:

* Client
	* `addBankIdentifier` --> unique recognition feature of the PDF document
* Transaction types (basic types)
	* `addBuySellTransaction` --> Purchase and sale ( single settlement )
	* `addSummaryStatementBuySellTransaction`  --> Purchase and sale ( multiple settlements )
	* `addBuyTransactionFundsSavingsPlan` --> Savings plans
	* `addDividendeTransaction` --> Dividends
	* `addTaxTreatmentForDividendeTransaction` --> Tax treatment for dividends
	* `addAdvanceTaxTransaction` --> Advance tax payment
  	* `addCreditcardStatementTransaction` --> Credit card transactions
  	* `addAccountStatementTransaction` --> Giro account transactions
  	* `addDepotStatementTransaction` --> Securities account transactions ( Settlement account )
  	* `addTaxStatementTransaction` --> Tax settlement
  	* `addDeliveryInOutBoundTransaction` --> Inbound and outbound deliveries
  	* `addTransferInOutBoundTransaction` --> Transfer in and outbound deliveries
  	* `addReinvestTransaction` --> Reinvestment transaction
  	* `addTaxReturnBlock` --> Tax refund
  	* `addFeeReturnBlock` --> Fee refund
* Bank name
  	* `getLabel` --> display label of bank/broker, e.g., *Deutsche Bank Privat- und Geschäftskunden AG*
* Taxes and fees
  	* `addTaxesSectionsTransaction` --> handling of taxes
  	* `addFeesSectionsTransaction` --> handling of fees
* Overwrite the value extractor methods if the documents work with non-standard (English, German) locales:
	* Example: [Bank SLM](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BankSLMPDFExtractor.java) (de_CH)
	* Example:  [Baader Bank AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BaaderBankPDFExtractor.java) (de_DE + en_US)
* Add post processing on imported transaction using a `postProcessing` method:
	* Example: [Comdirect](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/ComdirectPDFExtractor.java)


### Naming Conventions for Detected Values

The importers are structured according to the following scheme and the mapping variables are to be adhered to as far as possible:
* Type (Optional)
  * `type` --> Exchange of the transaction pair ( e.g. from purchase to sale )
* Security identification
  * `name` --> Security name
  * `isin` --> International Securities Identification Number
  * `wkn` --> Security code number
  * `tickerSymbol` --> Ticker symbol ( Optional )
  * `currency` --> Security currency
* Shares of the transaction
  * `shares` --> Shares
* Date and time
  * `date` --> Date
  * `time` --> Time ( Optional )
* Total amount (With fees and taxes)
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
  * `note` --> Notes e.g. quarterly dividend
* Tax section
   * `tax` --> Amount
   * `currency` --> Currency
   * `withHoldingTax` --> Withholding tax
   * `creditableWithHoldingTax` --> Creditable withholding tax
* Fee section
   * `fee` --> Amount
   * `currency` --> Currency

A finished PDF importer as a basis would be e.g. the [V-Bank AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/VBankAGPDFExtractor.java) PDF importer.


### Auxiliary classes

The utility class about standardized conversions, is called by the [AbstractPDFExtractor.java](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java)
and processed in the [PDFExtractorUtils.java](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExtractorUtils.java).
The [PDFExchangeRate](https://github.com/buchen/portfolio/blob/8d86513b6a4dcd8af0348f73e1b9c7df8af2cd83/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExchangeRate.java) helps processing for foreign currencies.

Use the [Money](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/money/Money.java) class when working with amounts (it includes the currency and the value rounded to cents). Use *BigDecimal* for exchange rates and the conversion between currencies. 

Use [TextUtil](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/util/TextUtil.java) class for some string manipulation such as trimming strings and stripping whitespace characters. The text created from PDF files has some corner cases that are not supported by the usual Java methods.


### Formatting of PDF Importer

Due to the many comments with text fragments from the PDF documents, we do not auto-format the PDF importer class files. Instead, carefully insert new code into the existing formatting manually. To protect formatting from automatic formatting, use the `@formatter:off` and `@formatter:on`.

Please take a look at the formatting and structure in the other PDF importers! Example: [V-Bank AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/VBankAGPDFExtractor.java)


### Test Cases

Via the application menu, users can create a test case file. The test file is the extracted text from the PDF documents. Users then anonymize the text by replacing personal idenfiable information and account numbers with alternative text.

* The test files should not be modified beyond the anonymization
* All source code (including the test files) are stored in UTF-8 encoding
* Follow the naming convention for test files (type in the local language, two digit counter):
	* `Buy01.txt, Sell01.txt` --> Purchase and sale (single settlements) ( e.g. Buy01.txt or Sell01.txt )
	* `Dividend01.txt` --> Dividends (single statements)
	* `SteuermitteilungDividende01.txt` --> Tax settlement for dividends (single settlement)
	* `SammelabrechnungKaufVerkauf01.txt` --> Purchase and sale (multiple settlements)
	* `Wertpapiereingang01.txt` --> Incoming securities
	* `Wertpapierausgang01.txt` --> Outgoing securities
	* `Vorabpauschale01.txt` --> Advance taxes
	* `GiroKontoauzug01.txt` --> Giro account statement
	* `KreditKontoauszug01.txt` --> Credit card account statement
	* `Depotauszug01.txt` --> security account transaction history (settlement account)
* Samples 
	*  one transaction per PDF: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java) - see `testWertpapierKauf06()` and `testDividende05()`
	* supporting securities with multiple currencies: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java) with `testWertpapierKauf09()` / `testWertpapierKauf09WithSecurityInEUR()` and `testDividende10()`/`testDividende10WithSecurityInEUR()`
		* Background: in the PP model, the currency of the transaction always must match the currency of the security and its historical prices. However, sometimes securities are purchased on an different exchange with prices in an another currency. The importer try to handle this case automatically. This is reflected in the two test cases
	* multiple transactions per PDF: [DKB AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/dkb/DkbPDFExtractorTest.java) with `testGiroKontoauszug01()`
	* if transactions are created based on two separate PDF files, use post processing: [Comdirect](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/comdirect/ComdirectPDFExtractorTest.java) with `testDividendeWithTaxTreatmentForDividende01()` and `testDividendeWithTaxTreatmentReversedForDividende01()`


### Regular Expressions

To test regular expression you can use [https://regex101.com/](https://regex101.com/).

Beside general good practices for regular expresions, keep in mind:
* all special characters in the PDF document (`äöüÄÖÜß` as well as e.g. circumflex or similar) should be matched by a `.` (dot) because the PDF to text conversion can create different results 
* expression in `.match(" ... ")` is started with an anchor `^` and ended with `$`
* with `.find(" ... ")` do not add anchors as they will be automatically added

Keep in mind that the regular expressions work against text that is automatically created from PDF files. Due to the nature of the process, there can always be slight differences in the text files. The following table collects the regular expressions that **worked well** to match typical values.  

| Value         | Example      | Not Helpful         | Works Well |
| :---------    | :----------- | :------------------ | :------------- |
| Date          | 01.01.1970   | `\\d+.\\d+.\\d{4}`  | `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}` |
|               | 1.1.1970     | `\\d+.\\d+.\\d{4}`  | `[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}` |
| Time          | 12:01        | `\\d+:\\d+`         | `[\\d]{2}\\:[\\d]{2}}` |
| ISIN          | IE00BKM4GZ66 | `\\w+`              | `[A-Z]{2}[A-Z0-9]{9}[0-9]` |
|               |              |                     | `[\\w]{12}` |
| WKN           | A111X9       | `\\w+`              | `[A-Z0-9]{6}` |
|               |              |                     | `[\\w]{6}` |
| Amount        | 751,68       | `[\\d,.]+`          | `[\\.,\\d]+` |
|               |              |                     | `[\\.\\d]+,[\\d]{2}` |
|               | 74'120.00    | `[\\d.']+`          | `[\\.'\\d]+` |
|               | 20 120.00    | `[\\d.\\s]+`        | `[\\.\\d\\s]+` |
| Currency      | EUR          | `\\w+`              | `[A-Z]{3}` |
|               |              |                     | `[\\w]{3}` |
| Currency      | € or $       | `\\D`               | `\\p{Sc}` |
