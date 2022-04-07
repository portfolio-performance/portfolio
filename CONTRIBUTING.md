# Standards von Portfolio Performance

## Inhaltsverzeichnis
- [PDF-Importer](#PDF-Importer)
	- [Pfad zum Importer](#Pfad_zum_Importer)
	- [Transaktionsklassen (Wertpapiertransaktion)](#Transaktionsklassen_Wertpapiertransaktion)
	- [Sektionen der Transaktionsklasse (Wertpapiertransaktion)](#Sektionen_der_Transaktionsklasse_Wertpapiertransaktion_)
	- [Generelle Regeln der TestCases](#Generelle_Regeln_der_TestCases)
	- [Regular expressions](#Regular_expressions)

<a name="PDF-Importer"></a>
## PDF-Importer
Die Importer sind nach Banken/Brokern zu erstellen.


<a name="Pfad"></a>
### Pfad zum Importer
Deutsche Bank AG --> DeutscheBankPDFExtractor.java
```
name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/DeutscheBankPDFExtractor.java
```
Pfad zu den TestCases des Importers:
Deutsche Bank AG --> DeutscheBankPDFExtractorTest.java
```
name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/deutschebank/DeutscheBankPDFExtractorTest.java
```


<a name="Transaktionsklassen_Wertpapiertransaktion"></a>
### Transaktionsklassen (Wertpapiertransaktion)

Der Aufbau der Importer erfolgt nach folgendem Schema:
* Client
  * addBankIdentifier(); -> einzigartiges Erkennungsmerkmal des PDF-Debugs
* Transaktionsarten (Grundtypen)
  * addBuySellTransaction() --> Kauf und Verkauf (Einzelnabrechnung)
  * addSummaryStatementBuySellTransaction();  --> Kauf und Verkauf (Sammelabrechnungen)
  * addBuyTransactionFundsSavingsPlan(); --> Sparpläne
  * addDividendeTransaction(); --> Dividenden und Erträgnisgutschriften
  * addAdvanceTaxTransaction(); --> Vorabpauschalen
  * addCreditcardStatementTransaction(); --> Keditkartentransaktionen
  * addAccountStatementTransaction(); --> Girokontotransaktionen
  * addDepotStatementTransaction(); --> Depottransaktionen (Verrechnungskonto)
  * addTaxStatementTransaction(); --> Steuerabrechnung etc.
  * addDeliveryInOutBoundTransaction(); --> Ein- und Auslieferungen
  * addTransferInOutBoundTransaction(); --> Ein- und Auslieferungen (Umbuchung)
  * addReinvestTransaction(); --> Ertragsthesaurierung
  * addTaxReturnBlock(); --> Steuererstattung
  * addFeeReturnBlock(); --> Gebührenerstattung
* Ausgabelabel (Visualisierung)
  * getLabel() --> Bank/Broker mit vollständiger Kennung z.B. Deutsche Bank Privat- und Geschäftskunden AG
* Steuern und Gebühren
  * addTaxesSectionsTransaction(); --> Steuerbehandlung
  * addFeesSectionsTransaction(); --> Gebührenbehandlung


<a name="Sektionen_der_Transaktionsklasse_Wertpapiertransaktion_"></a>
### Sektionen der Transaktionsklasse (Wertpapiertransaktion)
Der Aufbau der Importer erfolgt nach folgendem Schema:
* Type (Optional)
  * type --> Tausch des Transaktions-Paars (z.B von Kauf zu Verkauf)
* Wertpapieridentifizierung
  * name --> Name des Wertpapiers
  * isin --> International Securities Identification Number
  * wkn --> Wertpapier-Kennnummer
  * currency --> Währung des Wertpapiers
* Anteile der Transaktion
  * shares --> Anteile
* Datum und Zeit
  * date --> Datum
  * time --> Uhrzeit
* Endbetrag (Netto)
  * amount --> Betrag z.b. 123,15
  * currency --> Währung des Endbetrags
* Fremdwährung (Brutto)
  * fxAmount --> Betrag
  * fxCurrency --> Fremdwährung 
* Wechselkurs
  * exchangeRate --> Wechselkurs der Fremdwährung
* Notizen (Optional)
  * note --> Notiz z.B Quartalsdividende

* Steuersection
   * tax --> Betrag
   * currency --> Währung
   * withHoldingTax --> einbehaltene Quellensteuer
   * creditableWithHoldingTax --> anrechenbare Quellensteuer
* Gebührensection
   * fee --> Betrag
   * currency --> Währung

Ein fertigen PDF-Importer als Grundlage wäre z.B. der [V-Bank AG](https://github.com/buchen/portfolio/blob/master/name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/VBankAGPDFExtractor.java) PDF-Importer.


<a name="Generelle_Regeln_der_TestCases"></a>
### Generelle Regeln der TestCases
1. TestCase-Dokumente (xyz.txt) werden nicht verändert oder Teile hinzugefügt oder entfernt.
2. TestCases sind vollständig zu erstellen 
   * Beispiel: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java)
      * testWertpapierKauf06();
      * testDividende05()
3. Wenn ein Wertpapier in einer Fremdwährung (Kontowährung = EUR || Wertpapierwährung = USD), dann sind zwei TestCases zu erstellen. Einmal in Kontowährung und einmal in Wertpapierwährung
   * Beispiel: [Erste Bank Gruppe](https://github.com/buchen/portfolio/blob/fe2c944b95cd0c6a2eca49534d6ed21f1586d80c/name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/erstebank/erstebankPDFExtractorTest.java)
      * testWertpapierKauf09();
      * testWertpapierKauf09WithSecurityInEUR();
      * testDividende10();
      * testDividende10WithSecurityInEUR()
4. Für Konto-, Kredit oder Depottransaktionen
   * Beispiel: [DKB AG](name.abuchen.portfolio.tests/src/name/abuchen/portfolio/datatransfer/pdf/dkb/DkbPDFExtractorTest.java)
      * testGiroKontoauszug01()


<a name="Regular_expressions"></a>
### Regular expressions
Die Regular expressions sollten korrekt und genau erstellt werden.

Alle Sonderzeichen (```\.[]{}()<>*+-=!?^$|```) sind zu escapen. 

Alle Umlaute (```äöüÄÖÜß```), sowie z.B. Zirkumflex o.a. sind durch ein ```.``` (Punkt) escapen.

**Datum: 01.01.1970**
```
Falsch
\\d+.\\d+.\\d{4}
```
```
Korrekt
[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}
```
**ISIN: IE00BKM4GZ66**
```
Falsch
\\w+
```
```
Korrekt
[\\w]{12} gut
[A-Z]{2}[A-Z0-9]{9}[0-9] sehr genau
```
**WKN: A111X9**
```
Falsch
\\w
```
```
Korrekt
[\\w]{6}
```
**Beträge: 751,68**
```
Falsch
[\\d,.]+
```
```
Korrekt
[\\.,\\d]+ gut und praktisch
[\\.\\d]+,[\\d]{2} genauer
```
**Währungen: EUR**
```
Falsch
\\w+
```
```
Korrekt
[\\w]{3} gut und praktisch
[A-Z]{3} sehr genauer
```
**Währungen: € oder $**
```
Falsch
\\D
```
```
Korrekt
\\p{Sc}
```
