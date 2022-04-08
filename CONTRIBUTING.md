# Standards von Portfolio Performance

## Inhaltsverzeichnis
- [PDF-Importer](#PDF-Importer)
	- [Pfad zum Importer](#Pfad_zum_Importer)
	- [Dateinamen der Importer](#Dateinamen_der_Importer)
	- [Transaktionsklassen (Wertpapiertransaktion)](#Transaktionsklassen_Wertpapiertransaktion)
	- [Transaktions-Paare](#Transaktions_Paare)
	- [Sektionen der Transaktionsklasse (Wertpapiertransaktion)](#Sektionen_der_Transaktionsklasse_Wertpapiertransaktion_)
	- [Mathematische Rechnungen von Beträgen](#Mathematische_Rechnungen_von_Beträgen)
	- [Hilfsklasse der Importer](#Hilfsklasse_der_Importer)
	- [String-Manipulation](#String_Manipulation)
	- [Generelle Regeln der TestCases](#Generelle_Regeln_der_TestCases)
	- [Regular expressions](#Regular_expressions)

---

<a name="PDF-Importer"></a>
## PDF-Importer
Die Importer sind nach Banken/Brokern zu erstellen.

---

<a name="Pfad_zum_Importer"></a>
### Pfad zum Importer
Importer
```
name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/
name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/
```

<a name="Dateinamen_der_Importer"></a>
### Dateinamen der Importer
Die Importerbezeichnungen sind eindeutig zu wählen.
Beispiel: Deutsche Bank 
- Importer --> `DeutscheBankPDFExtractor.java`
- TestCase --> `DeutscheBankPDFExtractorTest.java`

---

<a name="Transaktions_Paare"></a>
### Transaktions-Paare (Wertpapiertransaktion)

* DEPOSIT, REMOVAL
* INTEREST, INTEREST_CHARGE
* DIVIDENDS
* TAXES, TAX_REFUND
* FEES, FEES_REFUND
* BUY, SELL
* TRANSFER_IN, TRANSFER_OUT

---

<a name="Transaktionsklassen_Wertpapiertransaktion"></a>
### Transaktionsklassen (Wertpapiertransaktion)

Der Aufbau der Importer erfolgt nach folgendem Schema:
* Client
	* `addBankIdentifier();` -> einzigartiges Erkennungsmerkmal des PDF-Debugs
* Transaktionsarten (Grundtypen)
	* `addBuySellTransaction()` --> Kauf und Verkauf (Einzelnabrechnung)
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
  	* `getLabel()` --> Bank/Broker mit vollständiger Kennung z.B. Deutsche Bank Privat- und Geschäftskunden AG
* Steuern und Gebühren
  	* `addTaxesSectionsTransaction();` --> Steuerbehandlung
  	* `addFeesSectionsTransaction();` --> Gebührenbehandlung
* Variablenmanipulation (@Override aus [AbstractPDFExtractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java))
	* z.B. `asAmount()`, `asShares()`, `asExchangeRate()`
* Prozessmanipulation (@Override aus [Extractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/Extractor.java))
	* `postProcessing();`
		* Beispiel: [Comdirect](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/ComdirectPDFExtractor.java)

---

<a name="Sektionen_der_Transaktionsklasse_Wertpapiertransaktion_"></a>
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

---

<a name="Hilfsklasse_der_Importer"></a>
### Hilfsklasse der Importer

Die Hilfsklasse über standardisierte Umrechnungen "Importerübergreifend", wird vom [AbstractPDFExtractor.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/AbstractPDFExtractor.java) aufgerufen
und in den [PDFExtractorUtils.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/PDFExtractorUtils.java) verarbeitet.

---

<a name="Mathematische_Rechnungen_von_Beträgen"></a>
### Mathematische Rechnungen von Beträgen

1. Bei Berechnungen von Beträge welche Währungsgleich sind, ist die [Money-Klasse](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/money/Money.java) zu verwenden.
2. Bei Berechnungen von Beträge welche Währungsungleich sind, sind die Beträge in `BigDecimal` zu berechnen und als `Money` zurück zu konvertieren.

---

<a name="String_Manipulation"></a>
### String-Manipulation

Für die String-, oder Text-Manipulation ist der statischen Import der [TextUtil.java](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio/src/name/abuchen/portfolio/util/TextUtil.java) zu verwenden.

---

<a name="Generelle_Regeln_der_TestCases"></a>
### Generelle Regeln der TestCases
1. TestCase-Dokumente (xyz.txt) werden nicht verändert, Teile hinzugefügt oder entfernt.
2. Die PDF-Debugs als Textdatei sind über Portfolio Performance über Datei --> Importieren --> Debug: Text aus PDF extrahieren... zu erzeugen.
3. Upload der PDF-Debugs (Textdateien) im UTF-8 Format.
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
		* `testDividende05();`
7. Wenn ein Wertpapier in einer Fremdwährung (Kontowährung = EUR || Wertpapierwährung = USD), dann sind zwei TestCases zu erstellen. Einmal in Kontowährung und einmal in Wertpapierwährung
	* Beispiel: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java)
		* `testWertpapierKauf09();`
		* `testWertpapierKauf09WithSecurityInEUR();`
		* `testDividende10();`
		* `testDividende10WithSecurityInEUR();`
8. Für Konto-, Kredit- oder Depottransaktionen
   	* Beispiel: [DKB AG](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/dkb/DkbPDFExtractorTest.java)
		* `testGiroKontoauszug01();`

---

<a name="Regular_expressions"></a>
### Regular expressions
Als guten Online-Editor können wir [https://regex101.com/](https://regex101.com/) empfehlen.

- Die Regular expressions sollten korrekt und genau erstellt werden.
- Alle Sonderzeichen (`\.[]{}()<>*+-=!?^$|`) sind zu escapen. 
- Alle Umlaute (`äöüÄÖÜß`), sowie z.B. Zirkumflex o.a. sind durch ein ```.``` (Punkt) escapen.
- Group Constructs `( ... )` so gering wie möglich zu halten.
- Quantifiers `[ ... ]` falls nötig, passend zu wählen
- Bei `.match(" ... ")` wird mit einem Anchors `^` begonnen und mit `$` beendet
- Bei `.find(" ... ")` wird nicht mit Anchors gearbeitet. Diese sind bereits enthalten.

| 	RegEx		|	Beispiel	|  	Falsch			|	Richtig					|
| :------------- 	| :-------------	| :-------------		| :-------------				|
| Datum 		| 01.01.1970		| `\\d+.\\d+.\\d{4}`		| `[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}`		|
| ISIN 			| IE00BKM4GZ66		| `\\w+`			| `[\\w]{12}`					|
|  			| 			| 				| `[A-Z]{2}[A-Z0-9]{9}[0-9]` 			|
| WKN 			| A111X9		| `\\w+`			| `[\\w]{6}`					|
| 	 		| 			| 				| `[A-Z0-9]{6}`					|
| Beträge		| 751,68		| `[\\d,.]+`			| `[\\.,\\d]+`					|
| 	 		| 			| 				| `[\\.\\d]+,[\\d]{2}`				|
| Währungen		| EUR			| `\\w+`			| `[\\w]{3}`					|
| 	 		| 			| 				| `[A-Z]{3}`					|
| Währungen		| € oder $		| `\\D`				| `\\p{Sc}`					|
| Text			| foo maybe bar		| ```foo.*```			| ```foo( maybe bar)?```			|
| 			| FOO, Foo		| 				| ```(?i)foo```					|
