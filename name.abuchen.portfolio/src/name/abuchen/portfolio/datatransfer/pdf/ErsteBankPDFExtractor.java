package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.stripBlanks;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class ErsteBankPDFExtractor extends AbstractPDFExtractor
{
    public ErsteBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("BROKERJET");
        addBankIdentifier("Brokerjet Bank AG");
        addBankIdentifier("ERSTE BANK");
        addBankIdentifier("FB-Nr.");
        addBankIdentifier("Sparkasse Bank AG");
        addBankIdentifier("www.sparkasse.at");
        addBankIdentifier("www.erstebank.at");

        addBuySellTransaction_DocFormat01();
        addBuySellTransaction_DocFormat02();
        addSummaryStatementBuySellTransaction();
        addDividendeTransaction_DocFormat01();
        addDividendeTransaction_DocFormat02();
    }

    @Override
    public String getLabel()
    {
        return "Erste Bank Gruppe / Österreichischen Sparkassenverbands / BrokerJet";
    }

    private void addBuySellTransaction_DocFormat01()
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
                    if ("VERKAUF".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // Ausserbörslich: SE SAV - AT Funds Kurswert: USD 177,3844
                                // Wertpapier: INVESCO ASIAN INF. A Devisenprovision: USD 0,44
                                // ACC. Tradinggebühren: EUR 3,81
                                // WP-Kenn-Nr.: LU0243955886 Gesamtbetrag: USD 181,6344
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "nameContinued", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^(?<nameContinued>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Börse: New York Stock Exchange Kurswert: USD 1.522,80
                                // Wertpapier: MORGAN ST., DEAN W. FX-Kommission: USD 3,75-
                                // DL-01 WP-Kommission: USD 22,67-
                                // WP-Kenn-Nr. : US6174464486
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "nameContinued", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^(?<nameContinued>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Börse: Vienna Stock Exchange Kurswert: EUR 3.207,23
                                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                                // O.N.
                                // Gesamtbetrag: EUR 3.217,22
                                // WP-Kenn-Nr.: AT0000937503
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "name1", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^(?<name1>.*)$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Gesamtbetrag"))
                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // Börse: Xetra Frankfurt Kurswert: EUR 1.742,30
                                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99-
                                // ST
                                // WP-Kenn-Nr. : DE0005190003
                                // Gesamtbetrag: EUR 1.732,31
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "name1", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^(?<name1>.*)$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Börse: Vienna Stock Exchange Kurswert: EUR 1.049,40
                                // Wertpapier: TELEKOM AUSTRIA AKT.
                                // O.N.
                                // WP-Kenn-Nr. : AT0000720008 Gesamtbetrag: EUR 1.049,40
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "nameContinued", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*)$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Börse: Vienna Stock Exchange Kurswert: EUR 1.132,20
                                // Wertpapier: EVN STAMMAKTIEN O.N. WP-Kommission: EUR 9,99
                                // WP-Kenn-Nr. : AT0000741053
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Börse: New York Stock Exchange Kurswert: USD 2.069,90
                                // Wertpapier: CATERPILLAR INC. DL 1 FX-Kommission: USD 5,24
                                // WP-Kenn-Nr. : US1491231015 WP-Kommission: USD 25,62
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) .*: [\\w]{3} [\\.,\\d]+(\\-)?$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Ausserbörslich Kurswert: EUR 99,97
                                // Wertpapier: DWS TOP 50 WELT
                                // WP-Kenn-Nr. : DE0009769794
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "name", "isin")
                                        .match("^.* Kurswert: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .match("^Wertpapier: (?<name>.*)$")
                                        .match("^WP\\-Kenn\\-Nr.*: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                // @formatter:off
                // Stück: 105,00
                // Stück: 90 Gesamtbetrag: EUR 1.142,19
                // @formatter:on
                .section("shares")
                .match("^St.ck: (?<shares>[\\.,\\d]+)( .*)?$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Ausführungsdatum: 23.09.2015 Ausführungszeit: 09:02:20
                // Ausführungsdatum: 05. Oktober 2009 Ausführungszeit: 12:39:27
                // @formatter:on
                .section("time").optional()
                .match("^.* Ausf.hrungszeit: (?<time>[\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2})$")
                .assign((t, v) -> type.getCurrentContext().put("time", v.get("time")))

                .oneOf(
                                // @formatter:off
                                // Ausführungsdatum: 23.09.2015
                                // @formatter:on
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
                                // @formatter:off
                                // Ausführungsdatum: 05. Oktober 2009
                                // @formatter:on
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
                                // @formatter:off
                                // Gesamtbetrag : EUR 1.116,78
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^.*Gesamtbetrag.*$")
                                        .match("^.*Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Gesamtbetrag: EUR 3.217,22
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^.*Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Gesamtbetrag: USD 2.100,76
                                // Valutatag: 08. Dezember 2009 Devisenkurs: 0,6654265
                                // Lagerort: Opfikon Gesamtbetrag : EUR 1.397,90
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "exchangeRate", "currency")
                                        .match("^Gesamtbetrag: (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.',\\d]+)$")
                                        .match("^.* Devisenkurs: (?<exchangeRate>[\\.',\\d]+)$")
                                        .match("^(.* )?Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) [\\.',\\d]+$")
                                        .assign((t, v) -> {
                                            v.put("termCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Ausserbörslich: SE SAV - AT Funds Kurswert: USD 177,3844
                                // Handelstag: 22.12.2014 Devisenkurs: 1,2169
                                // Valutatag: 30.12.2014 Gesamtbetrag: EUR 149,94
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "exchangeRate", "currency")
                                        .match("^.* Kurswert: (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)$")
                                        .match("^.* Devisenkurs: (?<exchangeRate>[\\.,\\d]+)$")
                                        .match("^.* Gesamtbetrag(\\s)?: (?<currency>[\\w]{3}) [\\.,\\d]+$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Börse: New York Stock Exchange Kurswert: USD 1.522,80
                                // Devisenkurs: 0,7463201
                                // Gesamtbetrag : EUR 1.116,78
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "exchangeRate", "currency")
                                        .match("^.* Kurswert: (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.,\\d]+)$")
                                        .match("^Devisenkurs: (?<exchangeRate>[\\.',\\d]+)$")
                                        .match("^Gesamtbetrag : (?<currency>[\\w]{3}) [\\.',\\d]+$")
                                        .assign((t, v) -> {
                                            v.put("termCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                .conclude(ExtractorUtils.fixGrossValueBuySell())

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellTransaction_DocFormat02()
    {
        DocumentType type = new DocumentType("(Kauf|Verkauf)");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^(WERTPAPIER|.*W E R T P A P I E R.*)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // Is type --> "Verkauf" change from BUY to SELL
                .section("type").optional()
                .match("^.*(?<type>(Kauf|Verkauf)) .*$")
                .assign((t, v) -> {
                    if ("Verkauf".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);
                })

                .oneOf(
                                // @formatter:off
                                // AT0000809058                 IMMOFINANZ AG
                                // INHABERAKTIEN O.N.
                                // STK                        0,400     EUR       1,996972         NETTO Inland                     0,80  EUR
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "nameContinued", "currency")
                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) ([\\s]+)?(?<name>.*) [\\s]+$")
                                        .match("^(?<nameContinued>.*)$")
                                        .match("^STK ([\\s]+)?[\\.,\\d]+ ([\\s]+)?(?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+.*$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // DE0008404005 ALLIANZ SE Kupon: 29
                                // VINK.NAMENS-AKTIEN O.N.
                                // 10,00 STK 166,86 EUR 1.668,60 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "name1", "currency")
                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*) Kupon: .*$")
                                        .match("^(?<name1>.*)$")
                                        .match("^[\\.,\\d]+ STK ([\\s]+)?[\\.,\\d]+ (?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Limit:"))
                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // US00287Y1091 ABBVIE INC.
                                // REGISTERED SHARES DL -,01
                                // 5,00 STK 139,454 USD  697,27 USD
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "name1", "currency")
                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^[\\.,\\d]+ STK ([\\s]+)?[\\.,\\d]+ (?<currency>[\\w]{3}) ([\\s]+)?[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Limit:"))
                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                .oneOf(
                                // @formatter:off
                                // STK                        0,400     EUR       1,996972         NETTO Inland                     0,80  EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^STK ([\\s]+)?(?<shares>[\\.,\\d]+) ([\\s]+)?[\\w]{3} ([\\s]+)?[\\.,\\d]+ .*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // 10,00 STK 166,86 EUR 1.668,60 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^(?<shares>[\\.,\\d]+) STK ([\\s]+)?[\\.,\\d]+ [\\w]{3} ([\\s]+)?[\\.,\\d]+ [\\w]{3}$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Börse:  WIEN                                Ausführungszeit 19:30:00                Schlusstag: 08.06.2017
                                // @formatter:on
                                section -> section
                                        .attributes("time", "date")
                                        .match("^.* Ausf.hrungszeit (?<time>[\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2}) ([\\s]+)?Schlusstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Marktplatz: XETRA FFT Ausführungszeit: 14:10:34 Schlusstag: 08.09.2022
                                // Marktplatz: XETRA FFT Ausführungszeit: 9:31:02 Schlusstag: 16.01.2023
                                // @formatter:on
                                section -> section
                                        .attributes("time", "date")
                                        .match("^.* Ausf.hrungszeit: (?<time>[\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2}) Schlusstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                                ,
                                // @formatter:off
                                // Ausführungszeit: 19:30:00 Schlusstag: 29.06.2022
                                // @formatter:on
                                section -> section
                                        .attributes("time", "date")
                                        .match("^Ausf.hrungszeit: (?<time>[\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2}) Schlusstag: (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))
                        )

                .oneOf(
                                // @formatter:off
                                //  Zu   G u n st e n  1 2  3- 1  2 3-  1 2 3/  12                                        1  2 . 06  .2 0  1 7                        0,  8 0  E U R
                                //  Z u  L a s t e n  1 2  3- 1  2 3-  1 2 3/  12                                            2 7 . 0 3 . 2 0 2 0                     8 6 6 , 0 5   E U R
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^([\\s]+)?Z([\\s]+)?u"
                                                        + "(([\\s]+)?G([\\s]+)?u([\\s]+)?n([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n|"
                                                        + "([\\s]+)?L([\\s]+)?a([\\s]+)?s([\\s]+)?t([\\s]+)?e([\\s]+)?n)"
                                                        + "([\\s]+)?([\\/\\-\\d\\s]+) [\\s]{3,}([\\.\\d\\s]+) [\\s]{3,}(?<amount>[\\.,\\d\\s]+) ([\\s]+)?(?<currency>[\\w\\s]+) [\\s]{3,}$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(stripBlanks(v.get("amount"))));
                                            t.setCurrencyCode(asCurrencyCode(stripBlanks(v.get("currency"))));
                                        })
                                ,
                                // @formatter:off
                                // Zu Lasten 00001-152198 12.09.2022 1.717,38 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("amount", "currency")
                                        .match("^Zu (Lasten|Gunsten) .* [\\d]{2}.[\\d]{2}.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})$")
                                        .assign((t, v) -> {
                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                        })
                        )

                // @formatter:off
                // 5,00 STK 139,454 USD  697,27 USD
                // USD Devisenkurs Mitte 0,9955 Umgerechneter Kurswert 700,42 EUR
                // @formatter:on
                .section("termCurrency", "fxGross", "fxCurrency", "exchangeRate", "baseCurrency").optional()
                .match("^[\\.,\\d]+ STK ([\\s]+)?[\\.,\\d]+ (?<termCurrency>[\\w]{3}) ([\\s]+)?(?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3})$")
                .match("^USD Devisenkurs Mitte (?<exchangeRate>[\\.,\\d]+) Umgerechneter Kurswert [\\.,\\d]+ (?<baseCurrency>[\\w]{3}).*$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                // Limit:                                       Bestens                               Beratungsfreies Geschäft
                // Limit: Bestens beratungsfreies Geschäft
                // @formatter:on
                .section("note1", "note2").optional()
                .match("^(?<note1>Limit:) ([\\s]+)?(?<note2>Bestens).*$")
                .assign((t, v) -> t.setNote(trim(v.get("note1")) + " " + trim(v.get("note2"))))

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addSummaryStatementBuySellTransaction()
    {
        DocumentType type = new DocumentType(".*für den Zeitraum von [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\- [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*");
        this.addDocumentTyp(type);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        Block firstRelevantLine = new Block("^.* ISIN [A-Z]{2}[A-Z0-9]{9}[0-9].*$", "^Summe .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction
                // @formatter:off
                // ERSTE R ST DIV EUR R01 T ISIN AT0000A1QA79
                // @formatter:on
                .section("name", "isin")
                .match("^(?<name>.*) ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$")
                .assign((t, v) -> {
                    v.put("currency", CurrencyUnit.EUR);

                    t.setSecurity(getOrCreateSecurity(v));
                })

                // @formatter:off
                // 26.03.2020      Kauf                     23,008             -2.099,97                 0,00
                // @formatter:on
                .section("shares")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?Kauf ([\\s]+)?(?<shares>[\\.,\\d]+) .*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // 26.03.2020      Kauf                     23,008             -2.099,97                 0,00
                // @formatter:on
                .section("date")
                .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) ([\\s]+)?Kauf .*$")
                .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                // @formatter:off
                // 26.03.2020      Kauf                     23,008             -2.099,97                 0,00
                // @formatter:on
                .section("amount")
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?Kauf ([\\s]+)?[\\.,\\d]+ ([\\s]+)?\\-(?<amount>[\\.,\\d]+) ([\\s]+)?[\\.,\\d]+.*$")
                .assign((t, v) -> {
                    t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                    t.setAmount(asAmount(v.get("amount")));
                })

                .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction_DocFormat01()
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
                .oneOf(
                                // @formatter:off
                                // ISIN : CA0679011084
                                // Wertpapierbezeichnung : BARRICK GOLD CORP.
                                // Preis : USD 0.02
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "currency")
                                        .match("^ISIN : (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .match("^Wertpapierbezeichnung : (?<name>.*)$")
                                        .match("^Preis : (?<currency>[\\w]{3}) [\\.',\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Wertpapier : MUENCH.RUECKVERS.VNA O.N. Dividende Brutto : EUR 201,25
                                // Wertpapier : DWS TOP 50 WELT Ausschüttung Brutto : EUR 5,64
                                // WP-Kenn-Nr. : DE0008430026 Fremde Steuer : EUR 53,08
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "isin")
                                        .match("^Wertpapier : (?<name>.*) (Dividende|Aussch.ttung) Brutto : (?<currency>[\\w]{3}) [\\.',\\d]+$")
                                        .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (Fremde Steuer|KESt) : [\\w]{3} [\\.',\\d]+$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                                ,
                                // @formatter:off
                                // Wertpapier : RAIFF.ETHIK-AKTIEN (R) Ausschüttung Brutto : EUR 19,07
                                // WP-Kenn-Nr. : AT0000677901
                                // @formatter:on
                                section -> section
                                        .attributes("name", "currency", "isin")
                                        .match("^Wertpapier : (?<name>.*) (Dividende|Aussch.ttung) Brutto : (?<currency>[\\w]{3}) [\\.',\\d]+$")
                                        .match("^WP\\-Kenn\\-Nr\\.(\\s)?: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$")
                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))
                        )

                .oneOf(
                                // @formatter:off
                                // Anspruchsberechtigter : 35
                                // Anspruchsberechtigter : 77.638
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^Anspruchsberechtigter : (?<shares>[\\.',\\d]+)$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                                ,
                                // @formatter:off
                                // WP-Bestand : 35,000 Dividendenbetrag : EUR 148,17
                                // WP-Bestand : 20,286
                                // @formatter:on
                                section -> section
                                        .attributes("shares")
                                        .match("^WP\\-Bestand : (?<shares>[\\.',\\d]+).*$")
                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Zahltag : 15.09.2015
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Zahltag : (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                                ,
                                // @formatter:off
                                // Zahltag : 29. April 2010
                                // @formatter:on
                                section -> section
                                        .attributes("date")
                                        .match("^Zahltag : (?<date>[\\d]{2}\\. .* [\\d]{4}).*$")
                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))
                        )

                .oneOf(
                                // @formatter:off
                                // Gesamtbetrag (in : EUR 0.4
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^Gesamtbetrag .in : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Dividende Netto : USD 2,23
                                // Dividende Netto : EUR 1,73
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Dividende Netto : [\\w]{3} [\\.',\\d]+$")
                                        .match("^(.* )?Dividende Netto : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Ex-Tag : 26. Januar 2010 Dividende Netto : EUR 24,97
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Dividende Netto : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                                ,
                                // @formatter:off
                                // Auszahlungsbetrag : EUR 17,85
                                // @formatter:on
                                section -> section
                                        .attributes("currency", "amount")
                                        .match("^(.* )?Auszahlungsbetrag : (?<currency>[\\w]{3}) (?<amount>[\\.',\\d]+)$")
                                        .assign((t, v) -> {
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                            t.setAmount(asAmount(v.get("amount")));
                                        })
                        )

                .optionalOneOf(
                                // @formatter:off
                                // Brutto-Betrag : USD 0.7
                                // Devisenkurs : 0.888889
                                // Gesamtbetrag (in : EUR 0.4
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "exchangeRate", "currency")
                                        .match("^Brutto\\-Betrag : (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.',\\d]+)$")
                                        .match("^Devisenkurs : (?<exchangeRate>[\\.,\\d]+)$")
                                        .match("^Gesamtbetrag \\(in : (?<currency>[\\w]{3}) [\\.',\\d]+$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("currency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                // @formatter:off
                                // Wertpapier : MORGAN ST., DEAN W. DL-01 Dividende Brutto : USD 3,00
                                // Verwahrart : WR Devisenkurs : 1,2859000
                                // Dividende Netto : EUR 1,73
                                // @formatter:on
                                section -> section
                                        .attributes("fxCurrency", "fxGross", "exchangeRate", "currency")
                                        .match("^.* Dividende Brutto : (?<fxCurrency>[\\w]{3}) (?<fxGross>[\\.',\\d]+)$")
                                        .match("^.* Devisenkurs : (?<exchangeRate>[\\.,\\d]+)$")
                                        .match("^Dividende Netto : (?<currency>[\\w]{3}) [\\.',\\d]+$")
                                        .assign((t, v) -> {
                                            v.put("baseCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("termCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                                            Money gross = rate.convert(asCurrencyCode(v.get("currency")), fxGross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                        )

                .conclude(ExtractorUtils.fixGrossValueA())

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private void addDividendeTransaction_DocFormat02()
    {
        DocumentType type = new DocumentType("(Aussch.ttung|Quartalsdividende|Dividende|Kapitalr.ckzahlung)");
        this.addDocumentTyp(type);

        Block block = new Block("^([\\s]+ )?(Aussch.ttung|Quartalsdividende|Dividende|Kapitalr.ckzahlung)( [\\s]+)?$");
        type.addBlock(block);
        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DIVIDENDS);
            return entry;
        });

        pdfTransaction
                .oneOf(
                                // @formatter:off
                                //  I SI  N:  A T  00 0  06 7 7 9 0 1             R  A I FF  .-  N AC  H H A LT I  GK E I  TS F  . - A KT .  ( R)  A
                                //                               MITEIGENTUMSANTEILE - AUSSCHUETTEND
                                // Ertrag je Stück               1,02 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "nameContinued", "currency")
                                        .match("^([\\s]+)?I([\\s]+)?S([\\s]+)?I([\\s]+)?N([\\s]+)?: ([\\s]+)?(?<isin>[\\w\\s]{12,25}) ([\\s]+)?(?<name>.*)$")
                                        .match("^([\\s]+)?(?<nameContinued>.*)$")
                                        .match("^Ertrag je St.ck [\\s]{3,}[\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            v.put("isin", stripBlanks(v.get("isin")));
                                            v.put("name", stripBlanks(v.get("name")));
                                            v.put("nameContinued", trim(v.get("nameContinued")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                                ,
                                // @formatter:off
                                // ISIN: AT0000705660 ERSTE WWF STOCK ENV EUR R01
                                // MITEIGENTUMSANTEILE - AUSSCHUETTEND
                                // Verwahrungsart: Sammelverwahrung
                                // Ertrag je Stück: 6 EUR
                                // @formatter:on
                                section -> section
                                        .attributes("isin", "name", "name1", "currency")
                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$")
                                        .match("^(?<name1>.*)$")
                                        .match("^Ertrag je St.ck: ([\\s]+)?[\\.,\\d]+ (?<currency>[\\w]{3}).*$")
                                        .assign((t, v) -> {
                                            if (!v.get("name1").startsWith("Verwahrungsart:"))
                                                v.put("name", v.get("name") + " " + v.get("name1"));

                                            t.setSecurity(getOrCreateSecurity(v));
                                        })
                        )

                // @formatter:off
                // Menge/Währung:                62 STK
                // Menge/Währung: 25,621 STK
                // @formatter:on
                .section("shares")
                .match("^Menge\\/W.hrung: ([\\s]+)?(?<shares>[\\.,\\d]+) STK.*$")
                .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                // @formatter:off
                // Wir haben oben genannten Betrag auf Ihrem Konto 123-123-123/12 mit Valuta 16.06.2016
                // Wir haben oben genannten Betrag auf Ihrem Konto 0000-XXXXXXX mit Valuta 01.08.2022 gutgeschrieben.
                // @formatter:on
                .section("date")
                .match("^.* mit Valuta ([\\s]+)?(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$")
                .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                // @formatter:off
                //                                                       Beträge in FW        Beträge in EUR
                // Gutschrift                                                                          52,19
                //
                //  Beträge in EUR
                // Gutschrift 52,53
                // @formatter:on
                .section("currency", "amount")
                .match("^.* Betr.ge in (?<currency>[\\w]{3}).*$")
                .match("^Gutschrift ([\\s]+)?(?<amount>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    // Set local currency for addTaxesSectionsTransaction()
                    type.getCurrentContext().put("currency", v.get("currency"));

                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                    t.setAmount(asAmount(v.get("amount")));
                })

                // @formatter:off
                // Brutto USD                                                     0,70                  0,63
                // Devisenkurs USD/EUR vom 16.03.2016 1,1129
                // @formatter:on
                .section("fxCurrency", "fxGross", "gross", "termCurrency", "baseCurrency", "currency", "exchangeRate").optional()
                .match("^.* Betr.ge in (?<currency>[\\w]{3}).*$")
                .match("^Brutto (?<fxCurrency>[\\w]{3}) .* (?<fxGross>[\\.,\\d]+) [\\s]+(?<gross>[\\.,\\d]+).*$")
                .match("^Devisenkurs (?<termCurrency>[\\w]{3})\\/(?<baseCurrency>[\\w]{3}) .* [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<exchangeRate>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    ExtrExchangeRate rate = asExchangeRate(v);
                    type.getCurrentContext().putType(rate);

                    Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));
                    Money gross = rate.convert(asCurrencyCode(v.get("baseCurrency")), fxGross);

                    checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                })

                // @formatter:off
                //  Ausschüttung
                //  Quartalsdividende
                // @formatter:on
                .section("note").optional()
                .match("^.* (?<note>(Aussch.ttung|Quartalsdividende)).*$")
                .assign((t, v) -> t.setNote(trim(v.get("note"))))

                .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);

        block.set(pdfTransaction);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Steuer : Quellensteuer
                // Steuern : USD 0.18
                // @formatter:on
                .section("currency", "withHoldingTax").optional()
                .match("^Steuer : Quellensteuer$")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<withHoldingTax>[\\.,\\d]+)$")
                .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                // @formatter:off
                // Steuer : KESt1
                // Steuern : USD 0.18
                //
                // Steuer : KESt2
                // Steuern : EUR 1.97
                // @formatter:on
                .section("currency", "tax").multipleTimes().optional()
                .match("^Steuer : KESt[\\d]$")
                .match("^Steuern : (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // WP-Kenn-Nr. : DE0008430026 Fremde Steuer : EUR 53,08
                // @formatter:on
                .section("currency", "tax").optional()
                .match("^.* Fremde Steuer : (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // Dividende : EUR 5,750000 KESt : EUR 20,13
                // Ausschüttung : EUR 0,160000 KESt II : EUR 1,41
                // KESt I : EUR 1,22
                // KESt II : EUR 1,22
                // KESt III : EUR 0,00
                // @formatter:on
                .section("currency", "tax").multipleTimes().optional()
                .match("^.*KESt( [A-Z]{1,3})? : (?<currency>[\\w]{3}) (?<tax>[\\.,\\d]+)$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                //    KESt auf Kursgewinne                    67,42- EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^.* KESt auf Kursgewinne.* (?<tax>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                //  KESt auf Kursgewinne -0,51 EUR
                // @formatter:on
                .section("tax", "currency").optional()
                .match("^.* KESt auf Kursgewinne \\-(?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // KESt I pro Stück 0,0776  EUR                                                        -4,81
                // KESt II pro Stück 0,0018  EUR                                                       -0,11
                // KESt II pro Stück 3,9498 EUR -101,20
                // KESt III pro Stück 0,0988 EUR                                                       -6,13
                // @formatter:on
                .section("currency", "tax").multipleTimes().optional()
                .match("^KESt [A-Z]{1,3} pro St.ck [\\.,\\d]+ .*(?<currency>[\\w]{3}) .*\\-(?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> processTaxEntries(t, v, type))

                // @formatter:off
                // KESt I 12,5 %                                                                      -54,75
                //
                // KESt I 12,5 %                                                 -0,09                 -0,08
                // KESt II 12,5 %                                                 -0,09                 -0,08
                // KESt III 12,5 %                                                 -0,09                 -0,08
                // @formatter:on
                .section("tax").multipleTimes().optional()
                .match("^KESt [A-Z]{1,3} [\\.,\\d]+ % .*\\-(?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // @formatter:off
                // KESt I pro Stück 12,5 % -25,50
                // @formatter:on
                .section("tax").multipleTimes().optional()
                .match("^KESt [A-Z]{1,3} pro St.ck [\\.,\\d]+ % .*\\-(?<tax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processTaxEntries(t, v, type);
                })

                // @formatter:off
                // QESt 27,5 %                                                                         -7,22
                // QESt 25 %                                                     -0,18                 -0,16
                // @formatter:on
                .section("withHoldingTax").optional()
                .match("^QESt [\\.,\\d]+ % .*\\-(?<withHoldingTax>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction
                // @formatter:off
                // Ex-Tag : 29. April 2010 Zahlungsprovision : EUR 0,50
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* Zahlungsprovision : (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Wertpapier: VOESTALPINE AG AKT. Tradinggebühren: EUR 9,99
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* Tradinggeb.hren: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Wertpapier: BAY.MOTOREN WERKE AG WP-Kommission: EUR 9,99
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* WP\\-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Wertpapier: CATERPILLAR INC. DL 1 FX-Kommission: USD 5,24
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* FX\\-Kommission: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Wertpapier: INVESCO ASIAN INF. A Devisenprovision: USD 0,44
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^.* Devisenprovision: (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+)(\\-)?$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // Zahlungsprovision                                                                   -0,38
                // @formatter:on
                .section("fee").optional()
                .match("^Zahlungsprovision ([\\s]+)?\\-(?<fee>[\\.,\\d]+).*$")
                .assign((t, v) -> {
                    v.put("currency", type.getCurrentContext().get("currency"));
                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                //                                                                Fremde Spesen                     3,75- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* Fremde Spesen ([\\s]+)?(?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                //                                                                Mindestspesen                     8,95- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* Mindestspesen ([\\s]+)?(?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                //                                                            Inl. WP - Spesen                     1,51- EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* Inl\\. WP \\- Spesen ([\\s]+)?(?<fee>[\\.,\\d]+)\\- (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                //                                                   Gesamtkosten und -gebühren                    25,00+ EUR
                // @formatter:on
                .section("fee", "currency").optional()
                .match("^.* Gesamtkosten und \\-geb.hren ([\\s]+)?(?<fee>[\\.,\\d]+)\\+ (?<currency>[\\w]{3}).*$")
                .assign((t, v) -> processFeeEntries(t, v, type))

                // @formatter:off
                // 26.03.2020      Kauf                     23,008             -2.099,97                 0,00
                //                                       88,100000                -72,97                 0,00
                //                  88,100000             2.027,01                                       0,00
                // @formatter:on
                .section("fee").optional()
                .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} ([\\s]+)?Kauf .*$")
                .match("^.* [\\.,\\d]+ ([\\s]+)?\\-(?<fee>[\\.,\\d]+) ([\\s]+)[\\.,\\d]+.*$")
                .match("^.* [\\.,\\d]+ ([\\s]+)?[\\.,\\d]+ ([\\s]+)[\\.,\\d]+.*$")
                .assign((t, v) -> {
                    v.put("currency", CurrencyUnit.EUR);
                    processFeeEntries(t, v, type);
                })

                // @formatter:off
                // Summe der Dienstleistungskosten EUR 48,78 2,92 %
                // @formatter:on
                .section("currency", "fee").optional()
                .match("^Summe der Dienstleistungskosten (?<currency>[\\w]{3}) (?<fee>[\\.,\\d]+) .*$")
                .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        String language = "de";
        String country = "DE";

        int apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            int lastDot = value.lastIndexOf(".");
            int lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }

    @Override
    protected long asShares(String value)
    {
        String language = "de";
        String country = "DE";

        int apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            int lastDot = value.lastIndexOf(".");
            int lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Share, language, country);
    }

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        String language = "de";
        String country = "DE";

        int apostrophe = value.indexOf("\'");
        if (apostrophe >= 0)
        {
            language = "de";
            country = "CH";
        }
        else
        {
            int lastDot = value.lastIndexOf(".");
            int lastComma = value.lastIndexOf(",");

            // returns the greater of two int values
            if (Math.max(lastDot, lastComma) == lastDot)
            {
                language = "en";
                country = "US";
            }
        }

        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, language, country);
    }
}
