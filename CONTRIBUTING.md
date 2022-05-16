# Portfolio Performance Standards

## Contents
- [Eclipse](#Eclipse)
	- [Prerequisites](#Prerequisites)
	- [Install](#Install)
		- [Change language of Eclipse](#Install_Language)
	- [Plugins](#Plugins)
		- [Helpful settings for the Eclipse IDE](#Settings_in_Eclipse)
	- [First launch](#First_launch)
		- [Launch Portfolio Performance with LCDSL plugin](#Launch_PP_Application_with_LCDSL)
		- [Launch Portfolio Performance without LCDSL plugin](#Launch_PP_Application_without_LCDSL)
		- [Launch test cases with LCDSL plugin](#Launch_JunitPP_Application_with_LCDSL)
		- [Launch test cases without LCDSL plugin](#Launch_JunitPP_Application_without_LCDSL)
- [Pull requests](#Pull_requests)
	- [Pull requests with Github Desktop](#Pull_request_with_Github_Desktop)
- [Translations](#Translations)
	- [Join the translation team](#Join_the_translation_team)
- [PDF importer](#PDF_importer)
	- [Path to PDF importer](#Path_to_PDF_importer)
	- [PDF importer file names](#PDF_importer_file_names)
	- [Transaction pairs](#Transaction_pairs)
	- [Transaction classes (securities transaction)](#TransactionClasses_securities_transaction)
	- [Transaction class sections ( securities transaction )](#Transaction_class_sections_securities_transaction)
	- [PDF importer auxiliary class](#PDF_importer_auxiliary_class)
	- [Mathematical calculations of amounts](#Mathematical_calculations_of_amounts)
	- [String manipulation](#String_manipulation)
	- [Formatting of the source code](#Formatting_of_the_source_code)
	- [General rules of the test cases](#General_rules_of_test_cases)
	- [Regular expressions](#Regular_expressions)

---


<a name="Eclipse"></a>
## Eclipse

<a name="Prerequisites"></a>
### Prerequisites
- Eclipse IDE
- Java 11

---


<a name="Install"></a>
### Install
1. Download [Eclipse IDE](https://www.eclipse.org)
2. Install Eclipse IDE
3. Install Java 11 [Open JDK](https://www.azul.com/downloads/)

<a name="Install_Language"></a>
#### Change language of Eclipse
1. `Menu` --> `Help` --> `Install New Software`
2. Copy in `Work with:` the following link https://download.eclipse.org/technology/babel/update-site/latest/
3. Start installation
4. Choose your language pack
5. In the start icon on your desktop change the target execution
	- `eclipse.exe -nl en` --> English
	- `eclipse.exe -nl de` --> German
	- ...

---


<a name="Plugins"></a>
### Plugins
For the Eclipse IDE we also need the following plugins, which have to be installed via the marketplace.

1. [Eclipse PDE (Plug-in Development Environment)](https://marketplace.eclipse.org/content/eclipse-pde-plug-development-environment)
2. [Infinitest](https://marketplace.eclipse.org/content/infinitest)
3. [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor)
4. [Checkstyle Plug-In](https://marketplace.eclipse.org/content/checkstyle-plug)
5. [SonarLint](https://marketplace.eclipse.org/content/sonarlint)
6. [Launch Configuration DSL](https://marketplace.eclipse.org/content/launch-configuration-dsl)
7. As standard, the "Eclipse e4 Tools Developer Resources" are not installed. They are installed as follows.
	- `Menu` --> `Help` --> `Install New Software`
	- Open the drop-down menu of `Work with:`
	- Select the Eclipse installation
	- Under 'General Purpose Tools' select the 'Eclipse e4 Tools Developer Resources'
	- Click on `Next`, then on `Install`

<a name="Settings_in_Eclipse"></a>
#### Helpful settings for the Eclipse IDE
1. `Menu` -> `Window` --> `Preference` --> `Java` --> `Editor` --> `Save Actions`
 	- Activate `Organize imports`
2. `Menu` -> `Window` --> `Preference` --> `Java` --> `Editor` --> `Content Assist`
	- Activate `Add import insted of qualified name`
	- Activate `Use static imports`
	- Click on `New Type...` and add the following favorites
		- `name.abuchen.portfolio.util.TextUtil`
---


<a name="First_launch"></a>
### First launch

For problems with launching from the Eclipse IDE for Portfolio Performance, we have a collection thread in the forum. Currently this is only in German language.
[Forum ( German )](https://forum.portfolio-performance.info/t/verbesserungen-im-source-code-in-github-einbringen/7063)

1. `Menu` --> `Window` --> `Preference` --> `Java` --> `Editor` --> `Installed JREs` --> `Execution Environments`
	- Click on `JavaSE-11`
	- Activate the compatible Java version ( Java 11 )
	- Save the setting
2. Create your own [fork](https://docs.github.com/en/get-started/quickstart/fork-a-repo) of Portfolio Performance via GitHub
3. Downlod des Git Repository von Portfolio Performance
4. Import the repository into your Eclipse workspace
	- `Menu` --> `Window` --> `Show View` --> `Other…` --> `Git` --> `Git Repositories`
	- Click on `Clone a Git repository`
		- URI: https://github.com/YOUR-GIT-USERNAME/portfolio.git
		- Host: github.com
		- Repository path: YOUR-GIT-USERNAME/portfolio.git
	- Click on `Next`
	- Click on `Deselect All`
	- Select `master`
	- Click on `Next`
	- Choose your destination directory
	- Select `Import all existing Eclipse projects after clone finishes`
	- Select `Add project to working sets` ( If a working set does not yet exist, it must be created. )
	- Click `Finish` and the download from the Git repository will start.
5. Open thee `portfolio-target-definition.target`
	- If only the XML file opens, right-click on 'portfolio-target-definition.target' and select 'Open in target editor.
6. Now click on 'Set as Active Target Platform' in the editor ( top right ). 
 	- This can take a while, because all necessary dependencies are configured now. About 10 - 30 minutes. 
 	- You can see the status in `Progress`. 
 		- `Menu` --> `Window` --> `Show View` --> `Progress`.

<a name="Launch_PP_Application_with_LCDSL"></a>
#### Launch Portfolio Performance with LCDSL plugin
1. `Menu` --> `Window` --> `Show View` --> `Other…` --> `Debug` --> `Launch Configuration`
2. Select under `Eclipse Application` --> `PortfolioPerformance`
3. Right mouse button --> `( Re- )generate Eclipse launch configuration`
4. Right mouse button --> `Run`

<a name="Launch_PP_Application_without_LCDSL"></a>
#### Launch Portfolio Performance without LCDSL plugin
1. `Menu` --> `Run` --> `Run Configurations`
2. Choose from `Eclipse Application` --> `PortfolioPerformance`
3. Click on `Plug-ins` --> `Add Required Plug-ins` in the tab menu
4. `Apply` then click on `Run`

<a name="Launch_JunitPP_Application_with_LCDSL"></a>
#### Launch test cases with LCDSL plugin
1. `Menu` --> `Window` --> `Show View` --> `Other…` --> `Debug` --> `Launch Configuration`
2. Choose from `Junit Plug-in Test` --> `PortfolioPerformance_Tests` or `PortfolioPerformance_UI_Tests`
3. Right mouse button --> `( Re- )generate Eclipse launch configuration`
4. Right mouse button --> `Run`

<a name="Launch_JunitPP_Application_without_LCDSL"></a>
#### Launch test cases without LCDSL plugin
1. `Menu` --> `Run` --> `Run Configurations`
2. Select under `Junit Plug-in Test` --> `PortfolioPerformance_Tests` or `PortfolioPerformance_UI_Tests`
3. Click on `Plug-ins` --> `Add Required Plug-ins` in the tab menu
4. `Apply` then click on `Run`

---


<a name="Pull_requests"></a>
## Pull requests
1. Comments and explanations in the source must be provided in English.
2. Changes in the source code are to be validated with test cases, if possible.
3. Variables, classes, methods and new functions are to be selected unambiguously. ( [OOP](https://en.wikipedia.org/wiki/Object-oriented_programming) )

<a name="Pull_request_with_Github_Desktop"></a>
#### Pull request with Github Desktop
There are many ways to start a pull request in Portfolio Performance. Of course, the Eclipse IDE can do this itself.

An alternative option to Eclipse is from [GitHub Desktop](https://desktop.github.com/), which we explain here.

You can find a full tutorial [here](https://docs.github.com/en/desktop/installing-and-configuring-github-desktop/overview/creating-your-first-repository-using-github-desktop).

1. Download [GitHub Desktop](https://desktop.github.com/)
2. Install GitHub Desktop on your PC.
3. `Menu` --> `File` --> `Clone repository`
4. Select your repository ( fork of Portfolio Performance ) and click on `Clone`.
5. Create a new branch via `Menu` --> `Branch` --> `New Branch` and give it a name.
6. Copy the changed/new files to the new branch on your hard drive.
7. Click on `Commit to ...` to push your changes.
8. Open your web browser and navigate to your account on [GitHub.com](https://github.com) to execute your pull request.

---


<a name="Translations"></a>
## Translations
For translations and language packages of Portfolio Performance we use [POEditor](https://poeditor.com/join/project?hash=4lYKLpEWOY).

If you want to correct the translations or even add a new language for Portfolio Performance, we ask you to create translations
if you are familiar with the language. ( native language ).

A translation via Google Translate or similar is not conducive. Nothing is worse than wrong translations in which the meaning is distorted.

<a name="Join_the_translation_team"></a>
### Join the translation team
1. [POEditor.com](https://poeditor.com/join/project?hash=4lYKLpEWOY)
2. Join translations
3. Login with your account
4. Start your translation in your language

Of course you can also add translations via GitHub as a pull request. 
Please use the ResourceBundle editor in your Eclipse installation.

---


<a name="PDF_importer"></a>
## PDF importer
Importers are to be created by bank/broker.

<a name="Path_to_PDF_importer"></a>
### Path to PDF importer
_PDF importer_

`name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/`

_Test cases:_

`name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/`


<a name="PDF_importer_file_names"></a>
### PDF importer file names
The PDF importer names must be chosen unambiguously.

**Example: Deutsche Bank**

_PDF importer:_ `DeutscheBankPDFExtractor.java`

_TestCase:_ `DeutscheBankPDFExtractorTest.java`


<a name="Transaction_pairs"></a>
### Transaction pairs (securities transaction)

[PortfolioTransaction](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/model/PortfolioTransaction.java)
* BUY, SELL
* TRANSFER_IN, TRANSFER_OUT
* DELIVERY_INBOUND, DELIVERY_OUTBOUND

[AccountTransaction](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/model/AccountTransaction.java)
* DEPOSIT, REMOVAL
* INTEREST, INTEREST_CHARGE
* DIVIDENDS
* TAXES, TAX_REFUND
* FEES, FEES_REFUND


<a name="TransactionClasses_securities_transaction"></a>
### Transaction classes (securities transaction)

The structure of the PDF importers is as follows:
* Client
	* `addBankIdentifier();` --> unique recognition feature of the PDF document
* Transaction types (basic types)
	* `addBuySellTransaction();` --> Purchase and sale ( single settlement )
	* `addSummaryStatementBuySellTransaction();`  --> Purchase and sale ( multiple settlements )
	* `addBuyTransactionFundsSavingsPlan();` --> Savings plans
	* `addDividendeTransaction();` --> Dividends
	* `addTaxTreatmentForDividendeTransaction();` --> Tax treatment for dividends
	* `addAdvanceTaxTransaction();` --> Advance tax payment
  	* `addCreditcardStatementTransaction();` --> Credit card transactions
  	* `addAccountStatementTransaction();` --> Giro account transactions
  	* `addDepotStatementTransaction();` --> Securities account transactions ( Settlement account )
  	* `addTaxStatementTransaction();` --> Tax settlement
  	* `addDeliveryInOutBoundTransaction();` --> Inbound and outbound deliveries
  	* `addTransferInOutBoundTransaction();` --> Transfer in and outbound deliveries
  	* `addReinvestTransaction();` --> Reinvestment transaction
  	* `addTaxReturnBlock();` --> Tax refund
  	* `addFeeReturnBlock();` --> Fee refund
* Bank name
  	* `getLabel();` --> Bank/broker with full identifier e.g. Deutsche Bank Privat- und Geschäftskunden AG
* Taxes and fees
  	* `addTaxesSectionsTransaction();` --> Tax handling
  	* `addFeesSectionsTransaction();` --> Fee handling
* Variable manipulation (@Override from [AbstractPDFExtractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java) --> [PDFExtractorUtils.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExtractorUtils.java))
	* e.g. `asAmount()`, `asShares()`, `asExchangeRate()`, ...
		* If amounts and numbers are not in the standard 1123,25 format, insert them.
		* Example: [Bank SLM](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BankSLMPDFExtractor.java) (de_CH)
		* Example:  [Baader Bank AG](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BaaderBankPDFExtractor.java) (de_DE + en_US)
* Process manipulation ( @Override from [Extractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/Extractor.java) )
	* `postProcessing();`
		* Example: [Comdirect](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/ComdirectPDFExtractor.java)


<a name="Transaction_class_sections_securities_transaction"></a>
### Transaction class sections ( securities transaction )
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


<a name="PDF_importer_auxiliary_class"></a>
### PDF importer auxiliary class

The utility class about standardized conversions, is called by the [AbstractPDFExtractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java)
and processed in the [PDFExtractorUtils.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExtractorUtils.java).
The [PDFExchangeRate](https://github.com/buchen/portfolio/blob/8d86513b6a4dcd8af0348f73e1b9c7df8af2cd83/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExchangeRate.java) takes over the processing for foreign currencies.

<a name="Mathematical_calculations_of_amounts"></a>
### Mathematical calculations of amounts

1. When calculating amounts that are the same currency, use the [Money class](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/money/Money.java).
2. When calculating amounts which are currency unequal, the amounts are to be calculated in 'BigDecimal' and converted back as 'Money'.


<a name="String_manipulation"></a>
### String manipulation

For string or text manipulation, use the static import of [TextUtil.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/util/TextUtil.java).


<a name="Formatting_of_the_source_code"></a>
### Formatting of the source code
Eclipse offers the possibility to format the source with the key combination [CTRL]+[SHIFT]+[F].

The result is partly quite badly readable. Also, comments or the like, as well as helpful information, explanations or
example are destroyed in the formatting. We therefore ask you to dispense with Eclipse's auto-formatting ( only for the PDF importers ).
Please note that the Checkstyle plug-in will help you.

To protect formatting from automatic formatting, use the `@formatter:off` and `@formatter:on`.

**Example**
```Java
//@formatter:off
// Your notes and comments
//@formatter:on
```

Comments with the double `/** ... */` are generally in front of classes or in front of methods to generate JavaDoc.
Inline, i.e. in the code, we only use `//` for comments. 

Please take a look at the formatting and structure in the other PDF importers!
Example: [V-Bank AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/VBankAGPDFExtractor.java)


**Example: Eclipse formatting vs. manual formatting**
```Java
// Autoformatierung Eclipse
// Courtage USD -22.01
transaction.section("currency", "fee").optional().match("^Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.,\\d])")
		.assign((t, v) -> processFeeEntries(t, v, type));
}

// Manual formatting
transaction
	// Courtage USD -22.01
	.section("currency", "fee").optional()
	.match("^Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.,\\d]+)")
	.assign((t, v) -> processFeeEntries(t, v, type));
}
```

```Java
// Eclipse auto-formatting
.oneOf(
		// Endbetrag EUR -50,30
		section -> section.attributes("amount", "currency").match(
				"^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(\\-)?(?<amount>[\\.,\\d]+)$")
				.assign((t, v) -> {
				    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
				    t.setAmount(asAmount(v.get("amount")));
				}),
		// Endbetrag -52,50 EUR
		// Endbetrag : -760,09 EUR
		// Gewinn/Verlust -267,59 EUR Endbetrag
		// EUR 16.508,16
		// Endbetrag EUR 0,95
		section -> section.attributes("amount", "currency").match(
				"^(.* )?Endbetrag ([:\\s]+)?(\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
				.assign((t, v) -> {
				    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
				    t.setAmount(asAmount(v.get("amount")));
				}))

// Manual formatting
.oneOf(
		// @formatter:off
		// Endbetrag      EUR               -50,30
		// @formatter:on
		section -> section
			.attributes("amount", "currency")
			.match("^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(\\-)?(?<amount>[\\.,\\d]+)$")
			.assign((t, v) -> {
			    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
			    t.setAmount(asAmount(v.get("amount")));
			})
		,
		// @formatter:off
		//        Endbetrag                   -52,50 EUR
		// Endbetrag     :            -760,09 EUR
		// Gewinn/Verlust -267,59 EUR             Endbetrag      EUR            16.508,16
		//                                        Endbetrag      EUR                 0,95
		// @formatter:on
		section -> section
			.attributes("amount", "currency")
			.match("^(.* )?Endbetrag ([:\\s]+)?(\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
			.assign((t, v) -> {
			    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
			    t.setAmount(asAmount(v.get("amount")));
			})
	)
```
```Java
// Eclipse auto-formatting
/**
 * Information: Lime Trading Corp. is a US-based financial services
 * company. The currency is US$. All security currencies are USD. CUSIP
 * Number: The CUSIP number is the WKN number. Dividend transactions:
 * The amount of dividends is reported in gross.
 */

// Manual formatting
/** @formatter:off
 * Information:
 * Lime Trading Corp. is a US-based financial services company.
 * The currency is US$.
 * 
 * All security currencies are USD.
 * 
 * CUSIP Number:
 * The CUSIP number is the WKN number.
 * 
 * Dividend transactions:
 * The amount of dividends is reported in gross.
 * @formatter:on/
```

---


<a name="General_rules_of_test_cases"></a>
### General rules of the test_cases
1. TestCase documents ( xyz.txt ) are not modified, parts added or removed.
2. The PDF debugs as a text file are available through Portfolio Performance via `File` --> `Import` --> `Debug: Extract text from PDF...`.
3. Upload of PDF debugs ( text files ) is done in UTF-8 format.
4. The PDF debugs as a text file are to be named as follows ( basic names, if necessary also in foreign language )
	* `Buy01.txt, Sell01.txt` --> Purchase and sale ( single settlements ) ( e.g. Buy01.txt or Sell01.txt )
	* `Dividend01.txt` --> Dividends ( single statements )
	* `SteuermitteilungDividende01.txt` --> Tax settlement for dividends ( single settlement )
	* `SammelabrechnungKaufVerkauf01.txt` --> Purchase and sale ( multiple settlements )
	* `Wertpapiereingang01.txt` --> Incoming securities
	* `Wertpapierausgang01.txt` --> Outgoing securities
	* `Vorabpauschale01.txt` --> Advance taxes
	* `GiroKontoauzug01.txt` --> Giro account statement
	* `KreditKontoauszug01.txt` --> Credit card account statement
	* `Depotauszug01.txt` --> Portfolio transactions ( Settlement account)
5. TestCase-Namen
 	* `testWertpapierKauf01()` --> Purchase
 	* `testWertpapierVerkauf01()` --> Sales
 	* `testWertpapierKauf01WithSecurityInEUR()` --> Purchase in foreign currency
 	* `testWertpapierVerkauf01WithSecurityInEUR()` --> Sale in foreign currency
 	* `testDividende01()` --> Dividends
 	* `testDividende01WithSecurityInEUR()` -->  Dividends in foreign currency
 	* `testVorabsteuerpauschale01()` --> Advance taxes
 	* `testGiroKontoauszug01()` --> Giro account statement
 	* `testKreditKontoauszug01()` --> Credit card account statement
 	* `testDepotauszug01()` --> Portfolio transactions ( Settlement account)
6. TestCases are to be created completely 
	* Example: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java)
		* `testWertpapierKauf06()`
		* `testDividende05()`
7. If a security is in a foreign currency, e.g. account currency = EUR and security currency = USD, two TestCases have to be created. Once in account currency and once in security currency.
	* Example: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java)
		* `testWertpapierKauf09()`
		* `testWertpapierKauf09WithSecurityInEUR()`
		* `testDividende10()`
		* `testDividende10WithSecurityInEUR()`
8. For account, credit or portfolio transactions
   	* Example: [DKB AG](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/dkb/DkbPDFExtractorTest.java)
		* `testGiroKontoauszug01()`
9. For TestCases where the `postProcessing()` is changed, e.g. two PDF debugs are compared, two TestCases have to be created.
	* Example: [Comdirect](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/comdirect/ComdirectPDFExtractorTest.java)
		* `testDividendeWithTaxTreatmentForDividende01()`
		* `testDividendeWithTaxTreatmentReversedForDividende01()`


<a name="Regular_expressions"></a>
### Regular expressions
As a good online editor we can recommend [https://regex101.com/](https://regex101.com/).

- Regular expressions should be created correctly and accurately.
- All special characters ( `.[]{}()<>*+-=!?^$|` ) have to be escaped. 
- All special characters ( `äöüÄÖÜß` ), as well as e.g. circumflex or similar have to be escaped by a `.` (dot). 
- Group Constructs ` ( ... ) ` as low as possible.
- Quantifiers `a{3,6}` to be selected appropriately if necessary.
- Character Classes `[ ... ]` if necessary.
- In `.match(" ... ")` is started with an anchor `^` and ended with `$`.
- With `.find(" ... ")` anchors are not used. These are already included.

| 	RegEx		|	Example		|  	Wrong			|	Correct					|
| :------------- 	| :-------------	| :-------------		| :-------------				|
| Date	 		| 01.01.1970		| `\\d+.\\d+.\\d{4}`		| `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}`		|
| 	 		| 1.1.1970		| `\\d+.\\d+.\\d{4}`		| `[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}`		|
| Time	 		| 12:01			| `\\d+:\\d+`			| `[\\d]{2}\\:[\\d]{2}}`			|
| ISIN 			| IE00BKM4GZ66		| `\\w+`			| `[A-Z]{2}[A-Z0-9]{9}[0-9]`			|
|  			| 			| 				| `[\\w]{12}` 					|
| WKN 			| A111X9		| `\\w+`			| `[A-Z0-9]{6}`					|
| 	 		| 			| 				| `[\\w]{6}`					|
| Amount		| 751,68		| `[\\d,.]+`			| `[\\.,\\d]+`					|
| 		 	| 			| 				| `[\\.\\d]+,[\\d]{2}`				|
| 		 	| 74'120.00		| `[\\d.']+`			| `[\\.'\\d]+`					|
| 		 	| 20 120.00		| `[\\d.\\s]+`			| `[\\.\\d\\s]+`				|
| Currency		| EUR			| `\\w+`			| `[A-Z]{3}`					|
| 	 		| 			| 				| `[\\w]{3}`					|
| Currency		| € or $		| `\\D`				| `\\p{Sc}`					|
| Text			| foo maybe bar		| `foo .*`			| `foo( maybe bar)?`				|
| 			|  	   		| 				| `foo.*`					|
| 			| FOO, Foo		| 				| `(?i)foo`					|
