package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.pdf.PDFExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ErsteBankPDFExtractor extends AbstractPDFExtractor
{
    public ErsteBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BROKERJET"); //$NON-NLS-1$
        addBankIdentifier("Brokerjet Bank AG"); //$NON-NLS-1$
        addBankIdentifier("ERSTE BANK"); //$NON-NLS-1$

        addBuySellTransaction();
        addOlderBuySellTransaction();
        addDividendeTransaction();
        addOlderDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Erste Bank Gruppe / BrokerJet"; //$NON-NLS-1$
    }

    private void addBuySellTransaction()
    {
        DocumentType type = new DocumentType("(IHR )?(KAUF|VERKAUF)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(IHR )?(KAUF|VERKAUF).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^(IHR )?(?<type>(KAUF|VERKAUF))$")
                .assign((t, v) -> {
                    if (v.get("type").equals("VERKAUF"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // Börse: New York Stock Exchange Kurswert: USD 1.522,80
                // Wertpapier: MORGAN ST., DEAN W. FX-Kommission: USD 3,75-
                // DL-01 WP-Kommission: USD 22,67-
                // WP-Kenn-Nr. : US6174464486
                .section("currency", "name", "name1", "isin").optional()
                .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP\\-Kommission|Gesamtbetrag|Devisenprovision|FX\\-Kommission): [\\w]{3} [\\.',\\d\\s]+(\\-)?$")
                .match("^(?<name1>.*) (Tradinggeb.hren|WP\\-Kommission|Gesamtbetrag|Devisenprovision|FX\\-Kommission): [\\w]{3} [\\.',\\d\\s]+(\\-)?")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Tradinggebühren")
                                    || !v.get("name1").startsWith("WP-Kommission")
                                    || !v.get("name1").startsWith("Gesamtbetrag")
                                    || !v.get("name1").startsWith("Devisenprovision")
                                    || !v.get("name1").startsWith("FX-Kommission"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Börse: Vienna Stock Exchange Kurswert: EUR 3.207,23
                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                // O.N.
                // Gesamtbetrag: EUR 3.217,22
                // WP-Kenn-Nr.: AT0000937503
                .section("currency", "name", "name1", "isin").optional()
                .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP\\-Kommission|Gesamtbetrag|Devisenprovision|FX\\-Kommission): [\\w]{3} [\\.',\\d\\s]+(\\-)?$")
                .match("^(?<name1>.*)$")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12})$")
                .assign((t, v) -> {
                    if (!v.get("name1").startsWith("Gesamtbetrag"))
                        v.put("name", v.get("name") + " " + v.get("name1"));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Börse: Vienna Stock Exchange Kurswert: EUR 1.132,20
                // Wertpapier: EVN STAMMAKTIEN O.N. WP-Kommission: EUR 9,99
                // WP-Kenn-Nr. : AT0000741053
                .section("currency", "name", "isin").optional()
                .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP\\-Kommission|Gesamtbetrag|Devisenprovision|FX\\-Kommission): [\\w]{3} [\\.',\\d\\s]+(\\-)?$")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Ausserbörslich: SE SAV - AT Funds Kurswert: EUR 98,19052
                // Wertpapier: DWS TOP 50 WELT
                // WP-Kenn-Nr. : DE0009769794
                .section("currency", "name", "isin").optional()
                .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^Wertpapier: (?<name>.*)$")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Börse: New York Stock Exchange Kurswert: USD 2.069,90
                // Wertpapier: CATERPILLAR INC. DL 1 FX-Kommission: USD 5,24
                // WP-Kenn-Nr. : US1491231015 WP-Kommission: USD 25,62
                .section("currency", "name", "isin").optional()
                .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^Wertpapier: (?<name>.*) (Tradinggeb.hren|WP\\-Kommission|Gesamtbetrag|Devisenprovision|FX\\-Kommission): [\\w]{3} [\\.',\\d\\s]+(\\-)?$")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12}) (Tradinggeb.hren|WP\\-Kommission|Gesamtbetrag|Devisenprovision|FX\\-Kommission): [\\w]{3} [\\.',\\d\\s]+(\\-)?$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Börse: Vienna Stock Exchange Kurswert: EUR 1.049,40
                // Wertpapier: TELEKOM AUSTRIA AKT.
                // O.N.
                // WP-Kenn-Nr. : AT0000720008 Gesamtbetrag: EUR 1.049,40
                .section("currency", "name", "nameContinued", "isin").optional()
                .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^Wertpapier: (?<name>.*)$")
                .match("^(?<nameContinued>.*)$")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12}) (Tradinggeb.hren|WP\\-Kommission|Gesamtbetrag|Devisenprovision|FX\\-Kommission): [\\w]{3} [\\.',\\d\\s]+(\\-)?$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Stück: 105,00
                // Stück: 90 Gesamtbetrag: EUR 1.142,19
                .section("shares")
                .match("^St.ck: (?<shares>[\\.',\\d\\s]+)( .*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausführungsdatum: 23.09.2015 Ausführungszeit: 09:02:20
                // Ausführungsdatum: 05. Oktober 2009 Ausführungszeit: 12:39:27
                .section("time").optional()
                .match("^.* Ausf.hrungszeit: (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // Ausführungsdatum: 23.09.2015
                                section -> section
                                        .attributes("date")
                                        .match("^Ausf.hrungsdatum: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date")));
                                        })
                                ,
                                // Ausführungsdatum: 05. Oktober 2009
                                section -> section
                                        .attributes("date")
                                        .match("^Ausf.hrungsdatum: (?<date>[\\d]{2}\\. .* [\\d]{4}) .*$")
                                        .assign((t, v) -> {
                                            if (type.getCurrentContext().get("time") != null)
                                                t.setDate(asDate(v.get("date"), type.getCurrentContext().get("time")));
                                            else
                                                t.setDate(asDate(v.get("date")));
                                        })
                        )

                .oneOf(
                                // Gesamtbetrag : EUR 1.116,78
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Gesamtbetrag.*$")
                                        .match("^(.* )?Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) (?<amount>[\\.',\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Gesamtbetrag: EUR 3.217,22
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) (?<amount>[\\.',\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Börse: New York Stock Exchange Kurswert: USD 2.069,90
                // Valutatag: 08. Dezember 2009 Devisenkurs: 0,6654265
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^.* Kurswert: (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.',\\d\\s]+)$")
                .match("^(Valutatag: .* )?Devisenkurs: (?<exchangeRate>[\\.',\\d\\s]+)$")
                .match("^(.* )?Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));
                    v.put("termCurrency", asCurrencyCode(v.get("currency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Ausserbörslich: SE SAV - AT Funds Kurswert: USD 177,3844
                // Handelstag: 22.12.2014 Devisenkurs: 1,2169
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^.* Kurswert: (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.',\\d\\s]+)$")
                .match("^Handelstag: .* Devisenkurs: (?<exchangeRate>[\\.',\\d\\s]+)$")
                .match("^(.* )?Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .conclude(PDFExtractorUtils.fixGrossValueBuySell())
                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addOlderBuySellTransaction()
    {
        DocumentType type = new DocumentType("Verkauf");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^[\\s]+ Verkauf .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^[\\s]+ (?<type>Verkauf) .*$")
                .assign((t, v) -> {
                    if (v.get("type").equals("Verkauf"))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                // AT0000809058                 IMMOFINANZ AG  
                // INHABERAKTIEN O.N. 
                // STK                        0,400     EUR       1,996972         NETTO Inland                     0,80  EUR 
                .section("isin", "name", "nameContinued", "currency").optional()
                .match("^(?<isin>[\\w]{12}) [\\s]{3,}(?<name>.*) [\\s]+$")
                .match("^(?<nameContinued>.*)$")
                .match("^STK [\\s]{3,}[\\.,\\d]+ [\\s]{3,}(?<currency>[\\w]{3}) [\\s]{3,}[\\.,\\d]+ .*$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // STK                        0,400     EUR       1,996972         NETTO Inland                     0,80  EUR 
                .section("shares")
                .match("^STK [\\s]{3,}(?<shares>[\\.,\\d]+) [\\s]{3,}[\\w]{3} [\\s]{3,}[\\.,\\d]+ .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Ausführungsdatum: 23.09.2015 Ausführungszeit: 09:02:20
                // Ausführungsdatum: 05. Oktober 2009 Ausführungszeit: 12:39:27
                .section("time", "date")
                .match("^.* Ausf.hrungszeit (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}) [\\s]{3,}Schlusstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\s]+$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                //  Zu   G u n st e n  1 2  3- 1  2 3-  1 2 3/  12                                        1  2 . 06  .2 0  1 7                        0,  8 0  E U R 
                .section("amount", "currency")
                .match("^([\\s]+)?Z([\\s]+)?u([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n ([\\s]+)?([\\/\\-\\d\\s]+) [\\s]{3,}([\\.\\d\\s]+) [\\s]{3,}(?<amount>[\\.,\\d\\s]+) ([\\s]+)?(?<currency>[\\w\\s]+) [\\s]{3,}$")
                .assign((t, v) -> {
                    t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                    t.setCurrencyCode(asCurrencyCode(stripBlanks(v.get("currency"))));
                })

                //  Limit:                                       Bestens                               Beratungsfreies Geschäft 
                .section("note1", "note2").optional()
                .match("^(?<note1>Limit:) [\\s]{3,}(?<note2>Bestens) [\\s]{3,}.*$")
                .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        DocumentType type = new DocumentType("(BARDIVIDENDE|FONDS\\-AUSSCH.TTUNG|FONDS \\- AUSSCH.TTUNG)");
        this.addDocumentTyp(type);

        Block block = new Block("^(BARDIVIDENDE|FONDS\\-AUSSCH.TTUNG|FONDS \\- AUSSCH.TTUNG)$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                // ISIN : CA0679011084
                // Wertpapierbezeichnung : BARRICK GOLD CORP.
                // Preis : USD 0.02
                .section("isin", "name", "currency").optional()
                .match("^ISIN : (?<isin>[\\w]{12})$")
                .match("^Wertpapierbezeichnung : (?<name>.*)$")
                .match("^Preis : (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Wertpapier : MUENCH.RUECKVERS.VNA O.N. Dividende Brutto : EUR 201,25
                // Wertpapier : DWS TOP 50 WELT Ausschüttung Brutto : EUR 5,64
                // WP-Kenn-Nr. : DE0008430026 Fremde Steuer : EUR 53,08
                .section("name", "currency", "isin").optional()
                .match("^Wertpapier : (?<name>.*) (Dividende|Aussch.ttung) Brutto : (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12}) (Fremde Steuer|KESt) : [\\w]{3} [\\.',\\d\\s]+$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                // Wertpapier : RAIFF.ETHIK-AKTIEN (R) Ausschüttung Brutto : EUR 19,07
                // WP-Kenn-Nr. : AT0000677901
                .section("name", "currency", "isin").optional()
                .match("^Wertpapier : (?<name>.*) (Dividende|Aussch.ttung) Brutto : (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[\\w]{12})$")
                .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                .oneOf(
                                // Anspruchsberechtigter : 35
                                // Anspruchsberechtigter : 77.638
                                section -> section
                                        .attributes("shares")
                                        .match("^Anspruchsberechtigter : (?<shares>[\\.',\\d\\s]+)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // WP-Bestand : 35,000 Dividendenbetrag : EUR 148,17
                                // WP-Bestand : 20,286
                                section -> section
                                        .attributes("shares")
                                        .match("^WP\\-Bestand : (?<shares>[\\.',\\d\\s]+).*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // Zahltag : 15.09.2015
                                section -> section
                                        .attributes("date")
                                        .match("^Zahltag : (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // Zahltag : 29. April 2010
                                section -> section
                                        .attributes("date")
                                        .match("^Zahltag : (?<date>[\\d]{2}\\. .* [\\d]{4}).*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // Gesamtbetrag (in : EUR 0.4
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Gesamtbetrag .in : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Dividende Netto : USD 2,23
                                // Dividende Netto : EUR 1,73
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Dividende Netto : [\\w]{3} [\\.',\\d\\s]+$")
                                        .match("^(.* )?Dividende Netto : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Netto : EUR 24,97
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Dividende Netto : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // Auszahlungsbetrag : EUR 17,85
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Auszahlungsbetrag : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d\\s]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                // Brutto-Betrag : USD 0.7
                // Devisenkurs : 0.888889
                // Gesamtbetrag (in : EUR 0.4
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^Brutto\\-Betrag : (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.',\\d]+)$")
                .match("^Devisenkurs : (?<exchangeRate>[\\.,\\d]+)$")
                .match("^Gesamtbetrag \\(in : (?<currency>[\\w]{3}) [\\.',\\d\\s]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));
                    v.put("termCurrency", asCurrencyCode(v.get("currency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                // Wertpapier : MORGAN ST., DEAN W. DL-01 Dividende Brutto : USD 3,00
                // Verwahrart : WR Devisenkurs : 1,2859000
                // Dividende Netto : EUR 1,73
                .section("fxCurrency", "fxGross", "exchangeRate", "currency").optional()
                .match("^.* Dividende Brutto : (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.',\\d]+)$")
                .match("^.* Devisenkurs : (?<exchangeRate>[\\.,\\d]+)$")
                .match("^Dividende Netto : (?<currency>[\\w]{3}) [\\.',\\d]+$")
                .assign((t, v) -> {
                    v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                    v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                .conclude(PDFExtractorUtils.fixGrossValueA())
                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addOlderDividendeTransaction()
    {
        DocumentType type = new DocumentType("(Aussch.ttung|Quartalsdividende|Dividende|Kapitalr.ckzahlung)");
        this.addDocumentTyp(type);

        Block block = new Block("^[\\s]+ (Aussch.ttung|Quartalsdividende|Dividende|Kapitalr.ckzahlung) [\\s]+$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                //  I SI  N:  A T  00 0  06 7 7 9 0 1             R  A I FF  .-  N AC  H H A LT I  GK E I  TS F  . - A KT .  ( R)  A                                                                                       
                //                               MITEIGENTUMSANTEILE - AUSSCHUETTEND 
                // Ertrag je Stück               1,02 EUR    
                .section("isin", "name", "nameContinued", "currency").optional()
                .match("^([\\s]+)?I([\\s]+)?S([\\s]+)?I([\\s]+)?N([\\s]+)?: ([\\s]+)?(?<isin>[\\w\\s]{12,25}) [\\s]{3,}(?<name>.*)$")
                .match("^([\\s]+)?(?<nameContinued>.*)$")
                .match("^Ertrag je St.ck [\\s]{3,}[\\.,\\d]+ (?<currency>[\\w]{3}) .*$")
                .assign((t, v) -> {
                    v.put("isin", stripBlanks(v.get("isin")));
                    v.put("name", stripBlanks(v.get("name")));
                    v.put("nameContinued", trim(v.get("nameContinued")));

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // Menge/Währung:                62 STK   
                .section("shares")
                .match("^Menge\\/W.hrung: [\\s]{3,}(?<shares>[\\.,\\d]+) STK [\\s]+$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // Wir haben oben genannten Betrag auf Ihrem Konto 123-123-123/12 mit Valuta 16.06.2016  
                .section("date")
                .match("^.* mit Valuta ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\s]+$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                //                                                       Beträge in FW        Beträge in EUR  
                // Gutschrift                                                                          52,19  
                .section("currency", "amount").optional()
                .match("^.* Betr.ge in (?<currency>[\\w]{3}) [\\s]+$")
                .match("^Gutschrift [\\s]{3,}(?<amount>[\\.,\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    // Set local currency
                    type.getCurrentContext().put("currency", v.get("currency"));

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // Brutto USD                                                     0,70                  0,63 
                // Devisenkurs USD/EUR vom 16.03.2016 1,1129 
                .section("fxCurrency", "fxGross", "gross", "termCurrency", "baseCurrency", "currency", "exchangeRate").optional()
                .match("^.* Betr.ge in (?<currency>[\\w]{3}) [\\s]+$")
                .match("^Brutto (?<fxCurrency>[\\w]{3}) .* (?<fxGross>[\\.,\\d]+) [\\s]+(?<gross>[\\.,\\d]+) [\\s]+$")
                .match("^Devisenkurs (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<exchangeRate>[\\.,\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    PDFExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(asExchangeRate(v));

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("baseCurrency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type);
                })

                //  Ausschüttung 
                //  Quartalsdividende 
                .section("note").optional()
                .match("^[\\s]+ (?<note>(Aussch.ttung|Quartalsdividende)) [\\s]+$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Steuer : Quellensteuer
                // Steuern : USD 0.18
                .section("currency", "withHoldingTax").optional()
                .match("^Steuer : Quellensteuer$")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // Steuer : KESt1
                // Steuern : USD 0.18
                .section("currency", "tax").optional()
                .match("^Steuer : KESt1$")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Steuer : KESt2
                // Steuern : EUR 1.97
                .section("currency", "tax").optional()
                .match("^Steuer : KESt2$")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Steuer : KESt3
                // Steuern : EUR 0.00
                .section("currency", "tax").optional()
                .match("^Steuer : KESt3$")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // WP-Kenn-Nr. : DE0008430026 Fremde Steuer : EUR 53,08
                .section("currency", "tax").optional()
                .match("^.* Fremde Steuer : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Dividende : EUR 5,750000 KESt : EUR 20,13
                .section("currency", "tax").optional()
                .match("^.* KESt : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // Ausschüttung : EUR 0,160000 KESt II : EUR 1,41
                .section("currency", "tax").optional()
                .match("^.* KESt II : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                //    KESt auf Kursgewinne                    67,42- EUR  
                .section("tax", "currency").optional()
                .match("^.* KESt auf Kursgewinne ([\\s]+)?(?<tax>[\\.',\\d\\s]+)\\- (?<currency>[\\w]{3})([\\s]+)?$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt I : EUR 1,22
                .section("currency", "tax").optional()
                .match("^KESt I : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt II : EUR 1,22
                .section("currency", "tax").optional()
                .match("^KESt II : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt III : EUR 0,00
                .section("currency", "tax").optional()
                .match("^KESt III : (?<currency>[\\w]{3}) (?<tax>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt I pro Stück 0,0776  EUR                                                        -4,81  
                .section("currency", "tax").optional()
                .match("^KESt I pro St.ck [\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt II pro Stück 0,0018  EUR                                                       -0,11 
                .section("currency", "tax").optional()
                .match("^KESt II pro St.ck [\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt III pro Stück 0,0988 EUR                                                       -6,13 
                .section("currency", "tax").optional()
                .match("^KESt III pro St.ck [\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // KESt I 12,5 %                                                 -0,09                 -0,08 
                .section("tax").optional()
                .match("^KESt I [\\.,\\d]+ % [\\s]{3,}\\-[\\.',\\d]+ [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // KESt II 12,5 %                                                 -0,09                 -0,08 
                .section("tax").optional()
                .match("^KESt II [\\.,\\d]+ % [\\s]{3,}\\-[\\.',\\d]+ [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // KESt III 12,5 %                                                 -0,09                 -0,08 
                .section("tax").optional()
                .match("^KESt III [\\.,\\d]+ % [\\s]{3,}\\-[\\.',\\d]+ [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // KESt I 12,5 %                                                                      -54,75 
                .section("tax").optional()
                .match("^KESt I [\\.,\\d]+ % [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // KESt II 12,5 %                                                                      -54,75 
                .section("tax").optional()
                .match("^KESt II [\\.,\\d]+ % [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // KESt III 12,5 %                                                                      -54,75 
                .section("tax").optional()
                .match("^KESt III [\\.,\\d]+ % [\\s]{3,}\\-(?<tax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // QESt 27,5 %                                                                         -7,22              
                .section("withHoldingTax").optional()
                .match("^QESt [\\.,\\d]+ % [\\s]{3,}\\-(?<withHoldingTax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                })

                // QESt 25 %                                                     -0,18                 -0,16 
                .section("withHoldingTax").optional()
                .match("^QESt [\\.,\\d]+ % [\\s]{3,}\\-[\\.',\\d]+ [\\s]{3,}\\-(?<withHoldingTax>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                .section("currency", "fee").optional()
                .match("^.* Zahlungsprovision : (?<currency>[\\w]{3}) (?<fee>[\\.',\\d\\s]+)$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR
                // 9,99
                .section("currency", "fee").optional()
                .match("^.* Tradinggeb.hren: (?<currency>[\\w]{3}) (?<fee>[\\.',\\d\\s]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR
                // 9,99
                .section("currency", "fee").optional()
                .match("^.* WP\\-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\.',\\d\\s]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: CATERPILLAR INC. DL 1 FX-Kommission: USD
                // 5,24
                .section("currency", "fee").optional()
                .match("^.* FX\\-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\.',\\d\\s]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Wertpapier: INVESCO ASIAN INF. A Devisenprovision:
                // USD 0,44
                .section("currency", "fee").optional()
                .match("^.* Devisenprovision: (?<currency>[\\w]{3}) (?<fee>[\\.',\\d\\s]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // Zahlungsprovision                                                                   -0,38 
                .section("fee").optional()
                .match("^Zahlungsprovision [\\s]{3,}\\-(?<fee>[\\.',\\d]+) [\\s]+$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processFeeEntries(t, v, type);
                })

                //                                                                Fremde Spesen                     3,75- EUR 
                .section("fee", "currency").optional()
                .match("^.* Fremde Spesen [\\s]{3,}(?<fee>[\\.',\\d]+)\\- (?<currency>[\\w]{3}) [\\s]+$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                //                                                                Mindestspesen                     8,95- EUR 
                .section("fee", "currency").optional()
                .match("^.* Mindestspesen [\\s]{3,}(?<fee>[\\.',\\d]+)\\- (?<currency>[\\w]{3}) [\\s]+$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                //                                                            Inl. WP - Spesen                     1,51- EUR 
                .section("fee", "currency").optional()
                .match("^.* Inl\\. WP \\- Spesen [\\s]{3,}(?<fee>[\\.',\\d]+)\\- (?<currency>[\\w]{3}) [\\s]+$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        value = value.trim().replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$

        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int apostrophe = value.indexOf("\'"); //$NON-NLS-1$
        if (apostrophe >= 0)
        {
            language = "de"; //$NON-NLS-1$
            country = "CH"; //$NON-NLS-1$
        }
        else
        {
            int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
            int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en"; //$NON-NLS-1$
                country = "US"; //$NON-NLS-1$
            }
        }

        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected long asShares(String value)
    {
        value = value.trim().replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$

        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int apostrophe = value.indexOf("\'"); //$NON-NLS-1$
        if (apostrophe >= 0)
        {
            language = "de"; //$NON-NLS-1$
            country = "CH"; //$NON-NLS-1$
        }
        else
        {
            int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
            int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en"; //$NON-NLS-1$
                country = "US"; //$NON-NLS-1$
            }
        }

        return PDFExtractorUtils.convertToNumberLong(value, Values.Share, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        value = value.trim().replaceAll("\\s", ""); //$NON-NLS-1$ //$NON-NLS-2$

        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int apostrophe = value.indexOf("\'"); //$NON-NLS-1$
        if (apostrophe >= 0)
        {
            language = "de"; //$NON-NLS-1$
            country = "CH"; //$NON-NLS-1$
        }
        else
        {
            int lastDot = value.lastIndexOf("."); //$NON-NLS-1$
            int lastComma = value.lastIndexOf(","); //$NON-NLS-1$

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en"; //$NON-NLS-1$
                country = "US"; //$NON-NLS-1$
            }
        }

        return PDFExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
