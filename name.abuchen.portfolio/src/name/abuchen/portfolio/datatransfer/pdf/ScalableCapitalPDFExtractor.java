package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
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
public class ScalableCapitalPDFExtractor extends AbstractPDFExtractor
{
    public ScalableCapitalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Scalable Capital GmbH");

        addBuySellTransaction();
        addDividendeTransaction();
        addInterestTransaction();
        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Scalable Capital GmbH";
    }

    private void addBuySellTransaction()
    {
        final var type = new DocumentType("(Wertpapierabrechnung|Contract note)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^f.r (Kundenauftrag|Sparplanausf.hrung|savings plan order).*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "Verkauf" change from BUY to SELL
                        .section("type").optional() //
                        .match("^(?<type>(Kauf|Buy|Verkauf)) .* [\\.,\\d]+ Stk\\..*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type"))) //
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                                        // IE0008T6IUX0
                                        //
                                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                                        // LU2903252349
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^(Kauf|Verkauf) (?<name>.*) [\\.,\\d]+ Stk\\. [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // Buy Amundi Stoxx Europe 600 (Acc) 22.650225 pc. 919.45 EUR 2,788.00 EUR
                                        // LU0908500753
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "currency", "isin") //
                                                        .match("^Buy (?<name>.*) [\\.,\\d]+ pc\\. [\\.,\\d]+ (?<currency>[A-Z]{3}) [\\.,\\d]+ [A-Z]{3}$") //
                                                        .match("^(?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Kauf Vngrd Fds-ESG Dv.As-Pc Al ETF 3,00 Stk. 6,168 EUR 18,50 EUR
                                        // Verkauf Scalable MSCI AC World Xtrackers (Acc) 1,00 Stk. 9,585 EUR 9,59 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^(Kauf|Verkauf) .* (?<shares>[\\.,\\d]+) Stk\\. [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Buy Amundi Stoxx Europe 600 (Acc) 22.650225 pc. 919.45 EUR 2,788.00 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Buy .* (?<shares>[\\.,\\d]+) pc\\. [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [A-Z]{3}$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares"), "en", "US"))))

                        // @formatter:off
                        // Ausführung 12.12.2024 13:12:51 Geschäft 36581526
                        // Execution 05.05.2025 12:25:16 Exchange ID 86828W28lTD45195
                        // @formatter:on
                        .section("date", "time") //
                        .match("^(Ausf.hrung|Execution) (?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time"))))

                        // @formatter:off
                        // Total 19,49 EUR
                        // Gutschrift 472,00 EUR
                        // Belastung 200,00 EUR
                        // Debit 85.65 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^(Total|Gutschrift|Belastung|Debit) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Typ LIMIT Order SCALsin78vS5CYz
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Typ .* Order (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("Ord.-Nr.: " + trim(v.get("note")))),
                                        // @formatter:off
                                        // Type LIMIT Order ID BZzUZDlXBK5mNIc
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("note") //
                                                        .match("^Type .* Order ID (?<note>.*)$") //
                                                        .assign((t, v) -> t.setNote("Order ID: " + trim(v.get("note")))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        final var type = new DocumentType("Dividende");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^.*(Seite|Page) 1 \\/ [\\d]$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Berechtigtes Wertpapier AGNC Investment Corp.
                                        // ISIN US00123Q1040
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("name", "isin", "currency") //
                                                        .match("^Berechtigtes Wertpapier (?<name>.*)$") //
                                                        .match("^ISIN (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]).*$") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ (?<currency>[A-Z]{3}).*$") //)
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Berechtigte Anzahl 0,663129
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Berechtigte Anzahl (?<shares>[\\.,\\d]+).*$") //")
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))))

                        .oneOf( //
                                        // @formatter:off
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^(?<date>[\\d]{2}\\.[\\w]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ [\\.,\\d]+ [A-Z]{3}.*$") //"
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Gesamtbetrag 0,07 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount", "currency") //
                                                        .match("^Gesamtbetrag (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // 15.01.2025 15.01.2025 Gutschrift 0,12 USD 0,663129 0,08 EUR
                                        // USD / EUR 1,0269
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("termCurrency", "baseCurrency", "exchangeRate", "gross") //
                                                        .match("^[\\d]{2}\\.[\\w]{2}\\.[\\d]{4} [\\d]{2}\\.[\\w]{2}\\.[\\d]{4} Gutschrift [\\.,\\d]+ [A-Z]{3} [\\.,\\d]+ (?<gross>[\\.,\\d]+) [A-Z]{3}.*$") //")
                                                        .match("^(?<termCurrency>[A-Z]{3}) \\/ (?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var gross = Money.of(rate.getBaseCurrency(), asAmount(v.get("gross")));
                                                            var fxGross = rate.convert(rate.getTermCurrency(), gross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        .conclude(ExtractorUtils.fixGrossValueA())

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addInterestTransaction()
    {
        final var type = new DocumentType("Rechnungsabschluss");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Rechnungsabschluss$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Gesamt 13,69 EUR
                        // @formatter:on
                        .section("currency", "amount") //
                        .match("^Gesamt (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Kontostand am 31.03.2025 726,58 EUR (Soll- & Habenzinsen berücksichtigt)
                        // @formatter:on
                        .section("date") //
                        .match("^Kontostand am (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}).*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"))))

                        // @formatter:off
                        // Zeitraum 01.01.2025 - 31.03.2025
                        // @formatter:on
                        .section("note").optional() //
                        .match("^Zeitraum (?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);
    }

    private void addDepotStatementTransaction()
    {
        final var type = new DocumentType("Verrechnungskonto");
        this.addDocumentTyp(type);

        // @formatter:off
        // 07.04.2025 07.04.2025 Überweisung +29.715,63 EUR
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Überweisung) \\+[\\.,\\d]+ [A-Z]{3}.*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>(Überweisung)) \\+(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 03.04.2025 04.04.2025 Prime-Abonnementgebühr -4,99 EUR
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Prime\\-Abonnementgeb.hr) \\-[\\.,\\d]+ [A-Z]{3}.*$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "currency") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<note>(Prime\\-Abonnementgeb.hr)) \\-(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3}).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer 2,48 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Solidaritätszuschlag 0,14 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Solidarit.tszuschlag (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Kirchensteuer 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kirchensteuer (\\-)?(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Ausländische Quellensteuer -0,01 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Ausl.ndische Quellensteuer (\\-)?(?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "withHoldingTax", type))

                        // @formatter:off
                        // Verrechnung anrechenbarer Quellensteuer -0,12 EUR
                        // @formatter:on
                        .section("creditableWithHoldingTax", "currency").optional() //
                        .match("^Verrechnung anrechenbarer Quellensteuer (\\-)?(?<creditableWithHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processWithHoldingTaxEntries(t, v, "creditableWithHoldingTax", type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Ordergebühren +0,99 EUR
                        // Ordergebühren -0,99 EUR
                        // @formatter:on
                        .section("currency", "fee").optional() //
                        .match("^Ordergeb.hren [\\-|\\+](?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    @Override
    protected long asAmount(String value)
    {
        var language = "de";
        var country = "DE";

        var lastDot = value.lastIndexOf(".");
        var lastComma = value.lastIndexOf(",");

        // returns the greater of two int values
        if (Math.max(lastDot, lastComma) == lastDot)
        {
            language = "en";
            country = "US";
        }

        return ExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }
}
