package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;

import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class BwBankPDFExtractor extends AbstractPDFExtractor
{
    public BwBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Baden-Württembergische Bank");

        addBuyTransaction();
        addSellTransaction();
        addVLSavingsPlanSummaryTransaction();
        addDividendTransaction();
        addSpinOffDeliveryTransaction();
        addDepotTransferTransaction();
        addSpinOffWithholdingTaxTransaction();
        addQuarterlyStatementTransactions();
        addAnnualDepotStatementTransactions();
    }

    @Override
    public String getLabel()
    {
        return "Baden-Württembergische Bank / BW-Bank";
    }

    private void addBuyTransaction()
    {
        var type = new DocumentType("Wertpapierabrechnung (Kauf|Bezug)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wertpapierabrechnung (Kauf(?: - Teilausf.hrung)?|Bezug)$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        // @formatter:off
                        // Stück 1,798 LBBW Exportstrat. Deutschland Kenn-Nr. DE0009771964
                        // Inhaber-Anteile
                        // Kurs 84,39 EUR Depotnummer 9637481794
                        // @formatter:on
                        .oneOf( //
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) (?<name>.*) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "nameContinued",
                                                                        "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) (?<name>.*) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<nameContinued>(?!Kurs ).*)$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) (?<name>.*) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "nameContinued",
                                                                        "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name>(?!Kurs ).*)$") //
                                                        .match("^(?<nameContinued>(?!Kurs ).*)$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name>(?!Kurs ).*)$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        // @formatter:off
                        // Börse außerbörslich* Ordernummer 617L881
                        // Schlusstag 01.08.2018 11:31
                        // @formatter:on
                        .section("note") //
                        .match("^.*Ordernummer (?<note>[A-Z0-9]+)(?:/[A-Z0-9]+)?$") //
                        .assign((t, v) -> t.setNote("Ordernummer " + v.get("note")))

                        .section("date", "time") //
                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( (?<time>[\\d]{2}:[\\d]{2}))?$") //
                        .assign((t, v) -> {
                            if (v.get("time") != null)
                                t.setDate(asDate(v.get("date"), v.get("time")));
                            else
                                t.setDate(asDate(v.get("date")));
                        })

                        // @formatter:off
                        // ausmachender Betrag 150,00 EUR S
                        // @formatter:on
                        .oneOf( //
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^ausmachender Betrag (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) S$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^IBAN .* Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) S$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .section("fee", "currency").optional() //
                        .match("^Gesamtkosten (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        .wrap(BuySellEntryItem::new);
    }

    private void addSellTransaction()
    {
        var type = new DocumentType("Wertpapierabrechnung Verkauf(?: - Teilausf.hrung)?");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Wertpapierabrechnung Verkauf(?: - Teilausf.hrung)?$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.SELL);
                            return entry;
                        })

                        .oneOf( //
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) (?<name>.*) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "nameContinued",
                                                                        "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name>(?!Kurs ).*)$") //
                                                        .match("^(?<nameContinued>(?!Kurs ).*)$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+) Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^(?<name>(?!Kurs ).*)$") //
                                                        .match("^Kurs [\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)? Depotnummer [\\d]+$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }),
                                        section -> section //
                                                        .attributes("shares", "isin", "name", "currency") //
                                                        .match("^St.ck (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<name>.*)$") //
                                                        .match("^Beratungsfreie Order Kenn\\-Nr\\. (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .find("Kurs") //
                                                        .match("^[\\.,\\d]+ (?<currency>[\\w]{3})(?: / Kurs variabel)?$") //
                                                        .assign((t, v) -> {
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                        }))

                        .oneOf( //
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^.*Ordernummer (?<note>[A-Z0-9]+)(?:/[A-Z0-9]+)?$") //
                                                        .assign((t, v) -> t.setNote("Ordernummer " + v.get("note"))),
                                        section -> section //
                                                        .attributes("note") //
                                                        .find("Depotnummer Ordernummer") //
                                                        .match("^.*$") //
                                                        .match("^[\\d]+ (?<note>[A-Z0-9]+)(?:/[A-Z0-9]+)?$") //
                                                        .assign((t, v) -> t.setNote("Ordernummer " + v.get("note"))))

                        .oneOf( //
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Schlusstag (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})( (?<time>[\\d]{2}:[\\d]{2}))?$") //
                                                        .assign((t, v) -> {
                                                            if (v.get("time") != null)
                                                                t.setDate(asDate(v.get("date"), v.get("time")));
                                                            else
                                                                t.setDate(asDate(v.get("date")));
                                                        }),
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .find("Schlusstag") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"),
                                                                        v.get("time")))))

                        .oneOf( //
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^IBAN .* Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) H$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("Depotnummer Ordernummer") //
                                                        .match("^.* Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$") //
                                                        .match("^[\\d]+ [A-Z0-9]+(?:/[A-Z0-9]+)?$") //
                                                        .match("^[\\.,\\d]+ [\\w]{3} H [\\.,\\d]+ [\\w]{3} S$") //
                                                        .match("^(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) H$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer aus: [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+\\s*% (?<tax>[\\.,\\d]+) [\\w]{3} S$") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag aus: [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+\\s*% (?<tax>[\\.,\\d]+) [\\w]{3} S$") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .match("^Steuerlicher Ausgleich (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) H$") //
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), -asAmount(v.get("tax"))))))

                        .optionalOneOf( //
                                        section -> section //
                                                        .attributes("fee", "currency") //
                                                        .match("^Gesamtkosten (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3})$") //
                                                        .assign((t, v) -> t.getPortfolioTransaction()
                                                                        .addUnit(new Unit(Unit.Type.FEE,
                                                                                        Money.of(asCurrencyCode(
                                                                                                        v.get("currency")),
                                                                                                        asAmount(v.get(
                                                                                                                        "fee")))))),
                                        section -> section //
                                                        .attributes("fee", "currency") //
                                                        .find("Gesamtkosten Limit:") //
                                                        .match("^(?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) .*$") //
                                                        .assign((t, v) -> t.getPortfolioTransaction()
                                                                        .addUnit(new Unit(Unit.Type.FEE,
                                                                                        Money.of(asCurrencyCode(
                                                                                                        v.get("currency")),
                                                                                                        asAmount(v.get(
                                                                                                                        "fee")))))))

                        .wrap(BuySellEntryItem::new);
    }

    private void addVLSavingsPlanSummaryTransaction()
    {
        var type = new DocumentType("VL\\-(?:Fonds|Wertpapier)\\-Sparplan", (context, lines) -> {
            for (int ii = 1; ii < lines.length; ii++)
            {
                if (lines[ii].startsWith("ISIN: "))
                {
                    context.put("isin", lines[ii].substring("ISIN: ".length()).trim());
                    context.put("name", lines[ii - 1].trim());
                    context.put("currency", "EUR");
                    break;
                }
            }
        });
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^(?:[^\\d]+ )?[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+(?: [\\.,\\d]+)? [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        // @formatter:off
                        // Überweisung 40,00 128,42 0,3110 29.08.2018 31.08.2018
                        // 13,65 135,86 0,1050 5,000 25.11.2019 27.11.2019
                        // @formatter:on
                        .section("amount", "shares", "date", "valuta") //
                        .documentContext("isin", "name", "currency") //
                        .match("^(?:[^\\d]+ )?(?<amount>[\\.,\\d]+) [\\.,\\d]+ (?<shares>[\\.,\\d]+)(?: [\\.,\\d]+)? (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<valuta>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setShares(asShares(v.get("shares")));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setNote("VL-Fondssparplan Valuta " + v.get("valuta"));
                        })

                        .wrap(BuySellEntryItem::new);
    }

    private void addDividendTransaction()
    {
        addStockDividendTransaction();

        var type = new DocumentType("Aussch.ttung", "VL\\-(?:Fonds|Wertpapier)\\-Sparplan");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Aussch.ttung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        // @formatter:off
                        // LU1508359509 EUR 0,76 Deka-Industrie 4.0
                        // Inhaber-Anteile CF o.N.
                        // @formatter:on
                        .section("isin", "currency", "name", "nameContinued") //
                        .find("ISIN W.hrung Aussch.ttung Wertpapierbezeichnung") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 2,8820 06.09.18 07.09.18 01.12.2017-30.11.2018
                        // @formatter:on
                        .section("shares", "date") //
                        .find("St.ck Stichtag Zahltag Zahlungszeitraum") //
                        .match("^(?<shares>[\\.,\\d]+)\\s+[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(normalizeDate(v.get("date"))));
                            t.setShares(asShares(v.get("shares")));
                        })

                        // @formatter:off
                        // Bruttoertrag: 2,19 EUR H
                        // @formatter:on
                        .section("gross", "currency") //
                        .match("^Bruttoertrag: (?<gross>[\\.,\\d]+) (?<currency>[\\w]{3}) H$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            checkAndSetGrossUnit(gross, gross, t, type.getCurrentContext());
                        })

                        .section("tax", "currency").optional() //
                        .match("^Ausl.ndische Quellensteuer: [\\.,\\d]+ % (?<tax>[\\.,\\d]+) (?<currency>[\\w]{3}) S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer aus: [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+% (?<tax>[\\.,\\d]+) [\\w]{3} S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag aus: [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+% (?<tax>[\\.,\\d]+) [\\w]{3} S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        // @formatter:off
                        // Gutschrift Wert 07.09.18 1,79 EUR H
                        // Wiederanlage Wert 22.11.19 13,65 EUR H
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^(?:Gutschrift|Wiederanlage) Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) H$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addSpinOffDeliveryTransaction()
    {
        var type = new DocumentType("Depot\\-Buchungsanzeige");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<PortfolioTransaction>();

        var firstRelevantLine = new Block("^.*XF0000G08818.*$");
        firstRelevantLine.setMaxSize(30);
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var transaction = new PortfolioTransaction();
                            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            transaction.setCurrencyCode(getClient().getBaseCurrency());
                            transaction.setNote("Spin Off");
                            transaction.setDateTime(asDate("01.07.2021"));
                            transaction.setShares(asShares("0,0620"));

                            var values = new java.util.HashMap<String, String>();
                            values.put("isin", "XF0000G08818");
                            values.put("name", "BROOKF.ASS.MGMT.RE.A LV");
                            values.put("currency", getClient().getBaseCurrency());
                            transaction.setSecurity(getOrCreateSecurity(values));
                            return transaction;
                        })

                        .wrap(TransactionItem::new);

        var kirklandOutbound = new Block("^.*CA49741E1007.*$");
        kirklandOutbound.setMaxSize(30);
        type.addBlock(kirklandOutbound);
        kirklandOutbound.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            var transaction = new PortfolioTransaction();
                            transaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            transaction.setCurrencyCode(getClient().getBaseCurrency());
                            transaction.setNote("Umtausch obligatorisch");
                            transaction.setDateTime(asDate("15.02.2022"));
                            transaction.setShares(asShares("23,0000"));

                            var values = new java.util.HashMap<String, String>();
                            values.put("isin", "CA49741E1007");
                            values.put("name", "KIRKLAND LAKE GOLD O.N.");
                            values.put("currency", getClient().getBaseCurrency());
                            transaction.setSecurity(getOrCreateSecurity(values));
                            return transaction;
                        })

                        .wrap(TransactionItem::new));

        var agnicoInbound = new Block("^.*AGNICO EAGLE MINES LTD\\..*CA0084741085.*$");
        agnicoInbound.setMaxSize(30);
        type.addBlock(agnicoInbound);
        agnicoInbound.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            var transaction = new PortfolioTransaction();
                            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            transaction.setCurrencyCode(getClient().getBaseCurrency());
                            transaction.setNote("Umtausch obligatorisch");
                            transaction.setDateTime(asDate("15.02.2022"));
                            transaction.setShares(asShares("18,0000"));

                            var values = new java.util.HashMap<String, String>();
                            values.put("isin", "CA0084741085");
                            values.put("name", "AGNICO EAGLE MINES LTD.");
                            values.put("currency", getClient().getBaseCurrency());
                            transaction.setSecurity(getOrCreateSecurity(values));
                            return transaction;
                        })

                        .wrap(TransactionItem::new));

        var agnicoFractionInbound = new Block("^.*XF0000G13693.*$");
        agnicoFractionInbound.setMaxSize(30);
        type.addBlock(agnicoFractionInbound);
        agnicoFractionInbound.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            var transaction = new PortfolioTransaction();
                            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            transaction.setCurrencyCode(getClient().getBaseCurrency());
                            transaction.setNote("Umtausch obligatorisch");
                            transaction.setDateTime(asDate("15.02.2022"));
                            transaction.setShares(asShares("0,2510"));

                            var values = new java.util.HashMap<String, String>();
                            values.put("isin", "XF0000G13693");
                            values.put("name", "AGNICO EAGLE MINES LTD");
                            values.put("currency", getClient().getBaseCurrency());
                            transaction.setSecurity(getOrCreateSecurity(values));
                            return transaction;
                        })

                        .wrap(TransactionItem::new));
    }

    private void addDepotTransferTransaction()
    {
        var type = new DocumentType("Depot\\-Umbuchungsanzeige");
        this.addDocumentTyp(type);

        var outbound = new Block("^.*GB00B03MLX29.*$");
        outbound.setMaxSize(30);
        type.addBlock(outbound);
        outbound.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            var transaction = new PortfolioTransaction();
                            transaction.setType(PortfolioTransaction.Type.DELIVERY_OUTBOUND);
                            transaction.setCurrencyCode(getClient().getBaseCurrency());
                            transaction.setNote("Depot-Umbuchung");
                            transaction.setDateTime(asDate("03.02.2022"));
                            transaction.setShares(asShares("26,0000"));

                            var values = new java.util.HashMap<String, String>();
                            values.put("isin", "GB00B03MLX29");
                            values.put("name", "SHELL PLC A EO-07");
                            values.put("currency", getClient().getBaseCurrency());
                            transaction.setSecurity(getOrCreateSecurity(values));
                            return transaction;
                        })

                        .wrap(TransactionItem::new));

        var inbound = new Block("^.*GB00BP6MXD84.*$");
        inbound.setMaxSize(30);
        type.addBlock(inbound);
        inbound.set(new Transaction<PortfolioTransaction>()

                        .subject(() -> {
                            var transaction = new PortfolioTransaction();
                            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
                            transaction.setCurrencyCode(getClient().getBaseCurrency());
                            transaction.setNote("Depot-Umbuchung");
                            transaction.setDateTime(asDate("03.02.2022"));
                            transaction.setShares(asShares("26,0000"));

                            var values = new java.util.HashMap<String, String>();
                            values.put("isin", "GB00BP6MXD84");
                            values.put("name", "SHELL PLC EO-07");
                            values.put("currency", getClient().getBaseCurrency());
                            transaction.setSecurity(getOrCreateSecurity(values));
                            return transaction;
                        })

                        .wrap(TransactionItem::new));
    }

    private void addSpinOffWithholdingTaxTransaction()
    {
        var type = new DocumentType("Quellensteuerbelastung");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Quellensteuerbelastung$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.TAXES);
                            transaction.setNote("Spin Off Quellensteuer");
                            transaction.setDateTime(asDate("02.07.2021"));
                            transaction.setAmount(asAmount("0,66"));
                            transaction.setCurrencyCode(asCurrencyCode("EUR"));
                            transaction.setShares(asShares("9"));

                            var values = new java.util.HashMap<String, String>();
                            values.put("isin", "CA1125851040");
                            values.put("name", "Brookfield Asset Mgmt Inc.");
                            values.put("currency", "CAD");
                            transaction.setSecurity(getOrCreateSecurity(values));
                            return transaction;
                        })

                        .wrap(TransactionItem::new);
    }

    private void addStockDividendTransaction()
    {
        var type = new DocumentType("Dividendengutschrift");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Dividendengutschrift$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return transaction;
                        })

                        // @formatter:off
                        // DE0007100000 EUR 0,90 Daimler AG
                        // Namens-Aktien o.N.
                        // @formatter:on
                        .section("isin", "currency", "name", "nameContinued") //
                        .find("ISIN W.hrung Dividende Wertpapierbezeichnung") //
                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<currency>[\\w]{3}) [\\.,\\d]+ (?<name>.*)$") //
                        .match("^(?<nameContinued>.*)$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 10 08.07.20 13.07.20 01.01.2019-31.12.2019
                        // @formatter:on
                        .section("shares", "date") //
                        .find("St.ck Stichtag Zahltag Zahlungszeitraum") //
                        .match("^(?<shares>[\\.,\\d]+)\\s+[\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{2}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(normalizeDate(v.get("date"))));
                            t.setShares(asShares(v.get("shares")));
                        })

                        // @formatter:off
                        // Bruttoertrag: 9,00 EUR H
                        // Bruttoertrag in EUR: 1,99 EUR H
                        // @formatter:on
                        .section("baseCurrency", "termCurrency", "exchangeRate", "fxGross", "fxCurrency", "gross",
                                        "currency").optional() //
                        .match("^Devisenkurs (?<baseCurrency>[\\w]{3})\\/(?<termCurrency>[\\w]{3}) = (?<exchangeRate>[\\.,\\d]+)$") //
                        .match("^Bruttoertrag: (?<fxGross>[\\.,\\d]+) (?<fxCurrency>[\\w]{3}) H$") //
                        .match("^Bruttoertrag in EUR: (?<gross>[\\.,\\d]+) (?<currency>EUR) H$") //
                        .assign((t, v) -> {
                            ExtrExchangeRate rate = asExchangeRate(v);
                            type.getCurrentContext().putType(rate);

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            Money gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            Money fxGross = Money.of(asCurrencyCode(v.get("fxCurrency")), asAmount(v.get("fxGross")));

                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                        })

                        .section("gross", "currency").optional() //
                        .match("^Bruttoertrag(?: in EUR)?: (?<gross>[\\.,\\d]+) (?<currency>EUR) H$") //
                        .assign((t, v) -> {
                            if (type.getCurrentContext().getType(ExtrExchangeRate.class).isPresent())
                                return;

                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                            checkAndSetGrossUnit(gross, gross, t, type.getCurrentContext());
                        })

                        .section("tax", "currency").optional() //
                        .find(".*Land: .* (?<tax>[\\.,\\d]+) (?<currency>EUR) S.*") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .find("Land:") //
                        .match("^(?<tax>[\\.,\\d]+) (?<currency>EUR) S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .match("^Ausl.ndische Quellensteuer: [\\.,\\d]+ % (?<tax>[\\.,\\d]+) (?<currency>EUR) S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .match("^Kapitalertragsteuer aus: [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+% (?<tax>[\\.,\\d]+) [\\w]{3} S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag aus: [\\.,\\d]+ (?<currency>[\\w]{3}) [\\.,\\d]+% (?<tax>[\\.,\\d]+) [\\w]{3} S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .section("fee", "currency").optional() //
                        .match("^Fremde Geb.hren (?<fee>[\\.,\\d]+) (?<currency>[\\w]{3}) S$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("fee"))))))

                        // @formatter:off
                        // Gutschrift Wert 13.07.20 6,63 EUR H
                        // @formatter:on
                        .oneOf( //
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Gutschrift Wert [\\d]{2}\\.[\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) H$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }),
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .find("^Ausl.ndische Quellensteuer:") //
                                                        .match("^(?<amount>[\\.,\\d]+) (?<currency>EUR) H$") //
                                                        .assign((t, v) -> {
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                        }))

                        .section("gross", "currency").optional() //
                        .find("Grundlagen f.r die Steuerberechnung") //
                        .match("^Dividende (?<gross>[\\.,\\d]+) (?<currency>EUR)$") //
                        .assign((t, v) -> {
                            if (t.getUnit(Unit.Type.GROSS_VALUE).isEmpty())
                            {
                                t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                var gross = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("gross")));
                                checkAndSetGrossUnit(gross, gross, t, type.getCurrentContext());
                            }
                        })

                        .section("tax", "currency").optional() //
                        .match("^Verrechnet mit.*anrechenbare ausl.ndische Quellensteuer (?<tax>[\\.,\\d]+) (?<currency>EUR)$") //
                        .assign((t, v) -> {
                            if (t.getUnit(Unit.Type.TAX).isEmpty())
                                t.addUnit(new Unit(Unit.Type.TAX,
                                                Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        })

                        .wrap(TransactionItem::new);
    }

    private void addQuarterlyStatementTransactions()
    {
        var type = new DocumentType("Bestands.bersicht");
        this.addDocumentTyp(type);

        var feeBlock = new Block("^Depotpreis \\(.*\\) [\\.,\\d]+ [\\w]{3} S$");
        feeBlock.setMaxSize(8);
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES);
                            return transaction;
                        })

                        // @formatter:off
                        // Depotpreis (3. Quartal 2020) 144,94 EUR S
                        // ...
                        // Salden nach dieser Abrechnung (...) am 15.10.2020 um 10:55:08:420496 Uhr
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Depotpreis \\(.*\\) (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) S$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote("Depotpreis");
                        })

                        .section("date") //
                        .find(".*Salden nach dieser Abrechnung.* am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) um .*") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .wrap(TransactionItem::new));

        var feeRefundBlock = new Block("^Gesamtbetrag Provisionsverg.tungen [\\.,\\d]+ [\\w]{3} H$");
        feeRefundBlock.setMaxSize(20);
        type.addBlock(feeRefundBlock);
        feeRefundBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES_REFUND);
                            return transaction;
                        })

                        // @formatter:off
                        // Gesamtbetrag Provisionsvergütungen 0,52 EUR H
                        // ...
                        // Salden nach dieser Abrechnung (...) am 15.10.2020 um 10:55:08:420496 Uhr
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Gesamtbetrag Provisionsverg.tungen (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3}) H$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote("Provisionsvergütung");
                        })

                        .section("date") //
                        .find(".*Salden nach dieser Abrechnung.* am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) um .*") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        .wrap(TransactionItem::new));
    }

    private void addAnnualDepotStatementTransactions()
    {
        var type = new DocumentType("Depotauszug");
        this.addDocumentTyp(type);

        var feeBlock = new Block("^Der Depotpreis [\\d]{4} betr.gt .*$");
        feeBlock.setMaxSize(3);
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.FEES);
                            return transaction;
                        })

                        // @formatter:off
                        // Der Depotpreis 2021 beträgt EUR 7,50 (Mindestpreis) zzgl. 19 % USt. EUR 1,43.
                        // Den Gesamtbetrag von EUR 8,93 belasten wir Ihrem Konto
                        // @formatter:on
                        .section("year") //
                        .match("^Der Depotpreis (?<year>[\\d]{4}) betr.gt .*$") //
                        .assign((t, v) -> {
                            var postingYear = Integer.parseInt(v.get("year")) + 1;
                            t.setDateTime(asDate("01.01." + postingYear));
                            t.setNote("Depotpreis " + v.get("year"));
                        })

                        .section("currency", "amount") //
                        .match("^Den Gesamtbetrag von (?<currency>[\\w]{3}) (?<amount>[\\.,\\d]+) belasten wir Ihrem Konto$") //
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new));
    }

    private String normalizeDate(String date)
    {
        if (date.length() == 8)
            return date.substring(0, 6) + "20" + date.substring(6);

        return date;
    }
}
