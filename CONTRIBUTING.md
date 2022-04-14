# Standards von Portfolio Performance

## Inhaltsverzeichnis
- [Eclipse](#Eclipse)
	- [Voraussetzungen](#Requirements)
	- [Installation](#Install)
		- [Sprache ändern von Eclipse](#Install_Language)
	- [Plugin's](#Plugins)
		- [Hilfreiche Einstellungen in Eclipse](#Settings_in_Eclipse)
	- [Erster Start](#First_Run)
		- [Starten mit LCDSL-Plugin](#Run_PP_Application_with_LCDSL)
		- [Starten ohne LCDSL-Plugin](#Run_PP_Application_without_LCDSL)
		- [TestCases mit LCDSL-Plugin starten](#Run_JunitPP_Application_with_LCDSL)
		- [TestCases ohne LCDSL-Plugin starten](#Run_JunitPP_Application_without_LCDSL)
- [Pull Request](#Pull_Request)
	- [Pull Request mit Github Desktop](#Pull_Request_Github_Desktop)
- [Übersetzungen](#Translations)
	- [Tritt dem Übesetzungs-Team bei](#Start_Translations)
- [PDF-Importer](#PDF-Importer)
	- [Pfad zum Importer](#Path_to_Importer)
	- [Dateinamen der Importer](#Filenames_of_the_Importer)
	- [Transaktions-Paare](#Transactions_pairs)
	- [Transaktionsklassen (Wertpapiertransaktion)](#TransactionClasses_securities_transaction_)
	- [Sektionen der Transaktionsklasse (Wertpapiertransaktion)](#Sections_of_TransactionClasses_Securities_Transaction_)
	- [Mathematische Rechnungen von Beträgen](#Mathematical_calculations_of_amounts)
	- [Hilfsklasse der Importer](#Auxiliary_classes_of_the_importer)
	- [String-Manipulation](#String_manipulation)
	- [Formatierung des Source](#Formatting_source)
	- [Generelle Regeln der TestCases](#General_rules_of_TestCases)
	- [Regular expressions](#Regular_expressions)

---

<a name="Eclipse"></a>
## Eclipse

<a name="Requirements"></a>
### Voraussetzungen
- Eclipse IDE
- Java 11

---

<a name="Install"></a>
### Installation
1. Download [Eclipse IDE](https://www.eclipse.org)
2. Installiere Eclipse IDE
3. Installiere Java 11 [Open JDK](https://www.azul.com/downloads/)

<a name="Install_Language"></a>
#### Sprache ändern von Eclipse
1. `Menü` --> `Help` --> `Install New Software`
2. Kopiere in `Work with:` folgenden Link https://download.eclipse.org/technology/babel/update-site/latest/
3. Starte die Installation
4. Wähle dein Sprachpaket aus
5. Ändere im Start-Icon auf deinem Desktop die Zielausführung
	- `eclipse.exe -nl en` --> Englisch
	- `eclipse.exe -nl de` --> Deutsch
	- ...

---

<a name="Plugins"></a>
### Plugin's
Für Eclipse benötigen wir noch folgenden Plugins, welche über den Marktplatz installiert werden müssen.

1. [Eclipse PDE (Plug-in Development Environment)](https://marketplace.eclipse.org/content/eclipse-pde-plug-development-environment)
2. [Infinitest](https://marketplace.eclipse.org/content/infinitest)
3. [ResourceBundle Editor](https://marketplace.eclipse.org/content/resourcebundle-editor)
4. [Checkstyle Plug-In](https://marketplace.eclipse.org/content/checkstyle-plug)
5. [SonarLint](https://marketplace.eclipse.org/content/sonarlint)
6. [Launch Configuration DSL](https://marketplace.eclipse.org/content/launch-configuration-dsl)

---

<a name="First_Run"></a>
### Erster Start

Hilfestellung können wir geben im [Forum](https://forum.portfolio-performance.info/t/verbesserungen-im-source-code-in-github-einbringen/7063).

1. `Menü` --> `Window` --> `Preference` --> `Java` --> `Editor` --> `Installed JREs` --> `Execution Environments`
	- Klicke auf `JavaSE-11`
	- Aktiviere die Kompatible Java-Version (Java 11)
2. Downlod des Git Repository von Portfolio Performance
3. Importiere das Repository in dein Workspace
4. Öffne die `portfolio-target-definition.target`
	- Wenn sich nur die XML-Datei öffnet, klicke mit der rechten Maustaste auf `portfolio-target-definition.target` und wähle `Öffnen im Target-Editor`
5. Klicke nun im Editor (rechts oben) auf `Set as Active Target Platform`. 
 	- Dies kann eine weile dauern, da nun alle erforderlichen Abhängigkeiten konfiguriert werden. Cirka 10 - 30 Minuten. Den Status kannst du im `Progress` sehen. `Menü` --> `Window` --> `Show View` --> `Progress`

<a name="Run_PP_Application_with_LCDSL"></a>
#### Starten mit LCDSL-Plugin
1. `Menü` --> `Window` --> `Show View` --> `Other…` --> `Debug` --> `Launch Configuration`
2. Selektiere unter `Eclipse Application` --> `PortfolioPerformance`
3. Klicke im Reitermenü auf `Plug-ins` --> `Add Required Plug-ins`
4. Klicke auf `Apply` danach auf `Run`

<a name="Run_PP_Application_without_LCDSL"></a>
#### Starten ohne LCDSL-Plugin
1. `Menü` --> `Run` --> `Run Configurations`
2. Selektiere unter `Eclipse Application` --> `PortfolioPerformance`
3. Rechte Maustaste --> `(Re-)generate Eclipse launch configuration`
4. Rechte Maustaste --> `Run`

<a name="Run_JunitPP_Application_with_LCDSL"></a>
#### TestCases mit LCDSL-Plugin starten
1. `Menü` --> `Window` --> `Show View` --> `Other…` --> `Debug` --> `Launch Configuration`
2. Selektiere unter `Junit Plug-in Test` --> `PortfolioPerformance_Tests` oder `PortfolioPerformance_UI_Tests`
3. Rechte Maustaste --> `(Re-)generate Eclipse launch configuration`
4. Rechte Maustaste --> `Run`

<a name="Run_JunitPP_Application_without_LCDSL"></a>
#### TestCases ohne LCDSL-Plugin starten
1. `Menü` --> `Run` --> `Run Configurations`
2. Selektiere unter `Junit Plug-in Test` --> `PortfolioPerformance_Tests` oder `PortfolioPerformance_UI_Tests`
3. Klicke im Reitermenü auf `Plug-ins` --> `Add Required Plug-ins`
4. Klicke auf `Apply` danach auf `Run`

<a name="Settings_in_Eclipse"></a>
#### Hilfreiche Einstellungen in Eclipse
1. `Menü` -> `Window` --> `Preference` --> `Java` --> `Editor` --> `Save Actions`
 	- Aktiviere `Organize imports`
2. `Menü` -> `Window` --> `Preference` --> `Java` --> `Editor` --> `Content Assist`
	- Aktiviere `Add import insted of qualified name`
	- Aktiviere `Use static imports`
	- Klicke auf `New Type...` und füge folgende Favoriten hinzu
		- `name.abuchen.portfolio.util.TextUtil`
---

<a name="Pull_Request"></a>
## Pull-Requests
1. Kommentare und Erläuterungen im Source sind in Englisch zu hinterlegen.
2. Änderungen sind, wenn möglich mit TestCases zu validieren.
3. Variabeln, Klassen, Methoden und neue Funktionen sind eindeutig zu wählen. (Stichwort OOP)

<a name="Pull_Request_Github_Desktop"></a>
#### Pull Request mit Github Desktop
Es gibt viele Möglichkeiten einen Pull-Request in Portfolio Performance zu starten. Natürlich kann Eclipse dies auch selber.

Eine alternative Möglichkeit zu Eclipse ist, von [GitHub Desktop](https://desktop.github.com/), welche hier erläutert wird.

Ein ausführliches Tutorial findest du [hier](https://docs.github.com/en/desktop/installing-and-configuring-github-desktop/overview/creating-your-first-repository-using-github-desktop).

1. Download von [GitHub Desktop](https://desktop.github.com/)
2. Installiere GitHub Desktop
3. `Menü` -> `File` --> `Clone repository`
4. Selektiere dein Repository (Fork von Portfolio Performance) und klicke auf `Clone`
5. Erstelle einen neuen Branch über `Menü` -> `Branch` --> `New Branch` und gib diesem einen Namen
6. Kopiere nun deine Änderungen in den neuen Branch auf deinem Laufwerk
7. Klicke auf `Commit to ...` um deine Änderungen zu pushen und anschließen zu pullen
8. Gehe nun in deinem Webbrowser auf [GitHub.com](https://github.com) um anschließden deinen Pull-Request auszuführen.

---

<a name="Translations"></a>
## Übersetzungen
Für Übersetzungen und Sprachpakete von Portfolio Performance benutzen wir [POEditor](https://poeditor.com/join/project?hash=4lYKLpEWOY).

Wenn du die Übersetzungen korrigieren oder auch eine neue Sprache für Portfolio Performance hinzufügen möchtest, bitten wir dich, Übersetzungen
zu erstellen, wenn du der Sprache mächtig bist. (Muttersprache) 

Eine Übersetzung via Google Translate oder ähnlichem ist nicht förderlich. Nichts ist schlimmer, als falsche Übersetzungen in denen der Sinn verzerrt wird.

<a name="Start_Translations"></a>
### Tritt dem Übesetzungs-Team bei
1. [POEditor.com](https://poeditor.com/join/project?hash=4lYKLpEWOY)
2. Join translations
3. Logge dich mit deinem Account ein
4. Starte deine Übersetzung in deiner Sprache

Natürlich kannst du auch Übersetzungen via GitHub als Pull-Request hinzufügen. 
Nutze dazu bitte den ResourceBundle Editor in deiner Eclipse-Installation.

---

<a name="PDF-Importer"></a>
## PDF-Importer
Die Importer sind nach Banken/Brokern zu erstellen.


<a name="Path_to_Importer"></a>
### Pfad zum Importer
Importer
```
name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/
name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/
```

<a name="Filenames_of_the_Importer"></a>
### Dateinamen der Importer
Die Importerbezeichnungen sind eindeutig zu wählen.
Beispiel: Deutsche Bank 
- Importer --> `DeutscheBankPDFExtractor.java`
- TestCase --> `DeutscheBankPDFExtractorTest.java`


<a name="Transactions_pairs"></a>
### Transaktions-Paare (Wertpapiertransaktion)

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


<a name="TransactionClasses_securities_transaction_"></a>
### Transaktionsklassen (Wertpapiertransaktion)

Der Aufbau der Importer erfolgt nach folgendem Schema:
* Client
	* `addBankIdentifier();` -> einzigartiges Erkennungsmerkmal des PDF-Debugs
* Transaktionsarten (Grundtypen)
	* `addBuySellTransaction();` --> Kauf und Verkauf (Einzelnabrechnung)
	* `addSummaryStatementBuySellTransaction();`  --> Kauf und Verkauf (Sammelabrechnungen)
	* `addBuyTransactionFundsSavingsPlan();` --> Sparpläne
	* `addDividendeTransaction();` --> Dividenden und Erträgnisgutschriften
	* `addAdvanceTaxTransaction();` --> Vorabpauschalen
  	* `addCreditcardStatementTransaction();` --> Kreditkartentransaktionen
  	* `addAccountStatementTransaction();` --> Girokontotransaktionen
  	* `addDepotStatementTransaction();` --> Depottransaktionen (Verrechnungskonto)
  	* `addTaxStatementTransaction();` --> Steuerabrechnung etc.
  	* `addDeliveryInOutBoundTransaction();` --> Ein- und Auslieferungen
  	* `addTransferInOutBoundTransaction();` --> Ein- und Auslieferungen (Umbuchung)
  	* `addReinvestTransaction();` --> Ertragsthesaurierung
  	* `addTaxReturnBlock();` --> Steuererstattung
  	* `addFeeReturnBlock();` --> Gebührenerstattung
* Ausgabelabel (Visualisierung)
  	* `getLabel();` --> Bank/Broker mit vollständiger Kennung z.B. Deutsche Bank Privat- und Geschäftskunden AG
* Steuern und Gebühren
  	* `addTaxesSectionsTransaction();` --> Steuerbehandlung
  	* `addFeesSectionsTransaction();` --> Gebührenbehandlung
* Variablenmanipulation (@Override aus [AbstractPDFExtractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java) --> [PDFExtractorUtils.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExtractorUtils.java))
	* z.B. `asAmount()`, `asShares()`, `asExchangeRate()`, ...
		* Sollten Beträge und Zahlen nicht dem Standard-Format 1123,25 ensprechen, sind diese einzufügen.
		* Beispiel: [Bank SLM](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BankSLMPDFExtractor.java) (de_CH)
		* Beispiel: [Baader Bank AG](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/BaaderBankPDFExtractor.java) (de_DE + en_US)
* Prozessmanipulation (@Override aus [Extractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/Extractor.java))
	* `postProcessing();`
		* Beispiel: [Comdirect](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/ComdirectPDFExtractor.java)


<a name="Sections_of_TransactionClasses_Securities_Transaction_"></a>
### Sektionen der Transaktionsklasse (Wertpapiertransaktion)
Der Aufbau der Importer erfolgt nach folgendem Schema und die Mapping-Variabeln sind möglichst einzuhalten:
* Type (Optional)
  * `type` --> Tausch des Transaktions-Paars (z.B von Kauf zu Verkauf)
* Wertpapieridentifizierung
  * `name` --> Wertpapiername
  * `isin` --> International Securities Identification Number
  * `wkn` --> Wertpapier-Kennnummer
  * `tickerSymbol` --> Ticker-Symbol
  * `currency` --> Währung des Wertpapiers
* Anteile der Transaktion
  * `shares` --> Anteile
* Datum und Zeit
  * `date` --> Datum
  * `time` --> Uhrzeit
* Endbetrag (Netto)
  * `amount` --> Betrag z.b. 123,15
  * `currency` --> Währung des Endbetrags
* Fremdwährung (Brutto)
  * `fxAmount` --> Betrag
  * `fxCurrency` --> Fremdwährung 
* Wechselkurs
  * `exchangeRate` --> Wechselkurs der Fremdwährung
* Notizen (Optional)
  * `note` --> Notiz z.B Quartalsdividende
* Steuersection
   * `tax` --> Betrag
   * `currency` --> Währung
   * `withHoldingTax` --> einbehaltene Quellensteuer
   * `creditableWithHoldingTax` --> anrechenbare Quellensteuer
* Gebührensection
   * `fee` --> Betrag
   * `currency` --> Währung

Ein fertigen PDF-Importer als Grundlage wäre z.B. der [V-Bank AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/VBankAGPDFExtractor.java) PDF-Importer.


<a name="Auxiliary_classes_of_the_importer"></a>
### Hilfsklasse der Importer

Die Hilfsklasse über standardisierte Umrechnungen "Importerübergreifend", wird vom [AbstractPDFExtractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java) aufgerufen
und in den [PDFExtractorUtils.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExtractorUtils.java) verarbeitet.


<a name="Mathematical_calculations_of_amounts"></a>
### Mathematische Rechnungen von Beträgen

1. Bei Berechnungen von Beträge welche Währungsgleich sind, ist die [Money-Klasse](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/money/Money.java) zu verwenden.
2. Bei Berechnungen von Beträge welche Währungsungleich sind, sind die Beträge in `BigDecimal` zu berechnen und als `Money` zurück zu konvertieren.


<a name="String_manipulation"></a>
### String-Manipulation

Für die String-, oder Text-Manipulation ist der statischen Import der [TextUtil.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/util/TextUtil.java) zu verwenden.


<a name="Formatting_source"></a>
### Formatierung des Source
Eclipse biete mit der Tastenkombination [STRG]+[SHIFT]+[F] die Möglichkeit den Source zu formatieren.

Das Ergebnis ist zum Teil recht schlecht lesbar. Auch werden Kommentare o.ä., sowie hilfreiche Informationen, Erklärungen oder
Beispiel in der Formatierung zerstört. Wir bitte daher (ausschließlich bei den PDF-Importern) auf die Autoformatierung von Eclipse zu verzichten.
Beachten bitte dabei, dass das Checkstyle Plug-In dir Hilfestellung gibt.

Bitte schau dir dazu in den anderen PDF-Importern die Formatierung und den Aufbau an!
Als Beispiel [V-Bank AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/VBankAGPDFExtractor.java)


**Beispiel Eclipse Formatierung vs. manuelle Formatierung**
```Java
// Autoformatierung Eclipse
// Courtage USD -22.01
transaction.section("currency", "fee").optional().match("^Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.,\\d])")
		.assign((t, v) -> processFeeEntries(t, v, type));
}

// Manuelle Formatierung
transaction
	// Courtage USD -22.01
	.section("currency", "fee").optional()
	.match("^Courtage (?<currency>[\\w]{3}) \\-(?<fee>[\\.,\\d]+)")
	.assign((t, v) -> processFeeEntries(t, v, type));
}
```
```Java
// Autoformatierung Eclipse
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

// Manuelle Formatierung
.oneOf(
		// Endbetrag      EUR               -50,30
		section -> section
			.attributes("amount", "currency")
			.match("^.* Endbetrag ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?(\\-)?(?<amount>[\\.,\\d]+)$")
			.assign((t, v) -> {
			    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
			    t.setAmount(asAmount(v.get("amount")));
			})
		,
		//        Endbetrag                   -52,50 EUR
		// Endbetrag     :            -760,09 EUR
		// Gewinn/Verlust -267,59 EUR             Endbetrag      EUR            16.508,16
		//                                        Endbetrag      EUR                 0,95
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
// Autoformatierung Eclipse
/**
 * Information: Lime Trading Corp. is a US-based financial services
 * company. The currency is US$. All security currencies are USD. CUSIP
 * Number: The CUSIP number is the WKN number. Dividend transactions:
 * The amount of dividends is reported in gross.
 */

// Manuelle Formatierung
/**
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
 */
```

---

<a name="General_rules_of_TestCases"></a>
### Generelle Regeln der TestCases
1. TestCase-Dokumente (xyz.txt) werden nicht verändert, Teile hinzugefügt oder entfernt.
2. Die PDF-Debugs als Textdatei sind über Portfolio Performance über `Datei` --> `Importieren` --> `Debug: Text aus PDF extrahieren...` zu erzeugen.
3. Upload der PDF-Debugs (Textdateien) erfolgt im UTF-8 Format.
4. Die PDF-Debugs als Textdatei sind wiefolg zu benennen (Grundnamen, ggf. auch in Fremdsprache)
	* `Kauf01.txt, Verkauf01.txt` --> Kauf und Verkauf (Einzelabrechnungen) (e.g. Buy01.txt oder Sell01.txt)
	* `Dividende01.txt` --> Dividenden, Erträgnisgutschriften (Einzelabrechnungen)
	* `SteuermitteilungDividende01.txt` --> Steuerliche Abrechnung für Dividenden (Einzelabrechnung)
	* `SammelabrechnungKaufVerkauf01.txt` --> Kauf und Verkauf (mehrere Abrechnungen)
	* `Wertpapiereingang01.txt` --> Wertpapiereingang
	* `Wertpapierausgang01.txt` --> Wertpapierausgang
	* `Vorabpauschale01.txt` --> Vorabpauschalen
	* `GiroKontoauzug01.txt` --> Girokontoabrechnung
	* `KreditKontoauszug01.txt` --> Kreditkartenabrechnung
	* `Depotauszug01.txt` --> Depottransaktionen (Verrechnungkonto)
5. TestCase-Namen
 	* `testWertpapierKauf01()` --> Kauf
 	* `testWertpapierVerkauf01()` --> Verkauf
 	* `testWertpapierKauf01WithSecurityInEUR()` --> Kauf in Fremdwährung
 	* `testWertpapierVerkauf01WithSecurityInEUR()` --> Verkauf in Fremdwährung
 	* `testDividende01()` --> Dividenden, Erträgnisgutschriften
 	* `testDividende01WithSecurityInEUR()()` -->  Dividenden, Erträgnisgutschriften in Fremdwährung
 	* `testVorabsteuerpauschale01()` --> Vorabpauschalen
 	* `testGiroKontoauszug01()` --> Girokontoabrechnung
 	* `testKreditKontoauszug01()` --> Kreditkartenabrechnung
 	* `testDepotauszug01()` --> Depottransaktionen (Verrechnungkonto)
6. TestCases sind vollständig zu erstellen 
	* Beispiel: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java)
		* `testWertpapierKauf06()`
		* `testDividende05()`
7. Wenn ein Wertpapier in einer Fremdwährung z.B. Kontowährung = EUR und Wertpapierwährung = USD, sind zwei TestCases zu erstellen. Einmal in Kontowährung und einmal in Wertpapierwährung
	* Beispiel: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java)
		* `testWertpapierKauf09()`
		* `testWertpapierKauf09WithSecurityInEUR()`
		* `testDividende10()`
		* `testDividende10WithSecurityInEUR()`
8. Für Konto-, Kredit- oder Depottransaktionen
   	* Beispiel: [DKB AG](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/dkb/DkbPDFExtractorTest.java)
		* `testGiroKontoauszug01()`
9. Für TestCases, wo der `postProcessing()` verändert wird, z.B. zwei PDF-Debugs verglichen werden, sind zwei TestCases zu erstellen.
	* Beispiel: [Comdirect](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/comdirect/ComdirectPDFExtractorTest.java)
		* `testDividendeWithTaxTreatmentForDividende01()`
		* `testDividendeWithTaxTreatmentReversedForDividende01()`


<a name="Regular_expressions"></a>
### Regular expressions
Als guten Online-Editor können wir [https://regex101.com/](https://regex101.com/) empfehlen.

- Die Regular expressions sollten korrekt und genau erstellt werden.
- Alle Sonderzeichen (`\.[]{}()<>*+-=!?^$|`) sind zu escapen. 
- Alle Umlaute (`äöüÄÖÜß`), sowie z.B. Zirkumflex o.a. sind durch ein `.` (Punkt) escapen.
- Group Constructs `( ... )` so gering wie möglich zu halten.
- Quantifiers `a{3,6}` falls nötig, passend zu wählen.
- Character Classes `[ ... ]` falls nötig, passend zu wählen.
- Bei `.match(" ... ")` wird mit einem Anchors `^` begonnen und mit `$` beendet
- Bei `.find(" ... ")` wird nicht mit Anchors gearbeitet. Diese sind bereits enthalten.

| 	RegEx		|	Beispiel	|  	Falsch			|	Richtig					|
| :------------- 	| :-------------	| :-------------		| :-------------				|
| Datum 		| 01.01.1970		| `\\d+.\\d+.\\d{4}`		| `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}`		|
| Zeit	 		| 12:01			| `\\d+:\\d+`			| `[\\d]{2}\\:[\\d]{2}}`			|
| ISIN 			| IE00BKM4GZ66		| `\\w+`			| `[\\w]{12}`					|
|  			| 			| 				| `[A-Z]{2}[A-Z0-9]{9}[0-9]` 			|
| WKN 			| A111X9		| `\\w+`			| `[\\w]{6}`					|
| 	 		| 			| 				| `[A-Z0-9]{6}`					|
| Beträge		| 751,68		| `[\\d,.]+`			| `[\\.,\\d]+`					|
| 		 	| 			| 				| `[\\.\\d]+,[\\d]{2}`				|
| 		 	| 74'120.00		| `[\\d.']+`			| `[\\.'\\d]+`					|
| 		 	| 20 120.00		| `[\\d.\\s]+`			| `[\\.\\d\\s]+`				|
| Währungen		| EUR			| `\\w+`			| `[\\w]{3}`					|
| 	 		| 			| 				| `[A-Z]{3}`					|
| Währungen		| € oder $		| `\\D`				| `\\p{Sc}`					|
| Text			| foo maybe bar		| ```foo.*```			| ```foo( maybe bar)?```			|
| 			| FOO, Foo		| 				| ```(?i)foo```					|
