package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.util.TextUtil.concatenate;
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
public class VDKBankNVPDFExtractor extends AbstractPDFExtractor
{
    public VDKBankNVPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("vdk bank nv");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "VDK Bank NV / VDK Spaarbank";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Uitvoering op");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Overzicht transactie voor rekening .* op [\\d]{1,2}/[\\d]{1,2}/[\\d]{4}$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

                        // Is type --> "verkoop" change from BUY to SELL
                        .section("type").optional() //
                        .match("^Je (?<type>(verkoop|aankoop)) van: .*") //
                        .assign((t, v) -> {
                            if ("verkoop".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // ISIN: US0138721065
                                        // Je aankoop van: Alcoa Corp
                                        // Koers: 22,9856 USD
                                        //
                                        // ISIN: BE0974320526
                                        // Je verkoop van: Umicore NV
                                        // Koers: 8,7 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "currency") //
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Je (verkoop|aankoop) van: (?<name>.*)$") //
                                                        .match("^Koers: [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // ISIN: NO0010786288
                                        // Je verkoop van: Norwegian Govt 1,750% 17/02/2027
                                        // Nominaal bedrag: 80.000,00 NOK
                                        // Koers: 95,554 %
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "currency") //
                                                        .match("^ISIN: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Je (verkoop|aankoop) van: (?<name>.*)$") //
                                                        .match("^Nominaal bedrag: [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .match("^Koers: [\\.,\\d]+ %$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        .oneOf( //
                                        // @formatter:off
                                        // Aantal delen: 100
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Aantal delen: (?<shares>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> t.setShares(asShares(v.get("shares")))),
                                        // @formatter:off
                                        // Nominaal bedrag: 80.000,00 NOK
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares") //
                                                        .match("^Nominaal bedrag: (?<shares>[\\.,\\d]+) [A-Z]{3}$") //
                                                        .assign((t, v) -> {
                                                            // Percentage quotation, workaround for bonds
                                                            var shares = asBigDecimal(v.get("shares"));
                                                            t.setShares(Values.Share.factorize(shares.doubleValue() / 100));
                                                        }))

                        .oneOf( //
                                        // @formatter:off
                                        // Uitvoering op: 21/04/2025 om 19:44:00
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Uitvoering op: (?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{4}) om (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2})$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))))

                        // @formatter:off
                        // Netto afrekening op BE12 7740 1745 3286 door afhaling van: -2.047,36 EUR
                        // Netto afrekening op BE17 1131 0380 5536 door storting van: 852,07 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Netto afrekening op .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Totaal bedrag: 2.298,56 USD
                                        // Wisselkoers USD/EUR 1,146626
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^Totaal bedrag: (?<fxGross>[\\.,\\d]+) [A-Z]{3}$") //
                                                        .match("^Wisselkoers (?<termCurrency>[A-Z]{3})\\/(?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Borderelreferentie: 2025.000123456789 Orderreferentie: 395693
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Borderelreferentie:(?<note>.*) Orderreferentie.*$") //
                        .assign((t, v) -> t.setNote("Borderel-Ref.: " + trim(v.get("note"))))

                        // @formatter:off
                        // Borderelreferentie: 2025.000123456789 Orderreferentie: 395693
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Orderreferentie:(?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), "Ord.-Ref.: " + trim(v.get("note")), " | ")))

                        .conclude(ExtractorUtils.fixGrossValueBuySell())

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("Uitbetaling dividend");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Overzicht transactie voor rekening .* op [\\d]{1,2}/[\\d]{1,2}/[\\d]{4}$");
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
                                        // Uitbetaling dividend in contanten van BE0003470755
                                        // Naam: Solvay NV
                                        // Aan: 1,62 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "currency") //
                                                        .match("^Uitbetaling dividend in contanten van (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9])$") //
                                                        .match("^Naam: (?<name>.*)$") //
                                                        .match("^Aan: [\\.,\\d]+ (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Positie op ex-datum: 100
                        // @formatter:on
                        .section("shares") //
                        .match("^Positie op ex\\-datum: (?<shares>[\\.,\\d]+)$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Betaaldatum: 17/01/2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Betaaldatum: (?<date>[\\d]{1,2}/[\\d]{1,2}/[\\d]{4})$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        // @formatter:off
                        // Netto afrekening op BE12 7740 1745 3286 door afhaling van: -2.047,36 EUR
                        // Netto afrekening op BE17 1131 0380 5536 door storting van: 852,07 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Netto afrekening op .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Totaal bedrag: 2.298,56 USD
                                        // Wisselkoers USD/EUR 1,146626
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("fxGross", "termCurrency", "baseCurrency", "exchangeRate") //
                                                        .match("^Totaal bedrag: (?<fxGross>[\\.,\\d]+) [A-Z]{3}$") //
                                                        .match("^Wisselkoers (?<termCurrency>[A-Z]{3})\\/(?<baseCurrency>[A-Z]{3}) (?<exchangeRate>[\\.,\\d]+)$") //
                                                        .assign((t, v) -> {
                                                            var rate = asExchangeRate(v);
                                                            type.getCurrentContext().putType(rate);

                                                            var fxGross = Money.of(rate.getTermCurrency(), asAmount(v.get("fxGross")));
                                                            var gross = rate.convert(rate.getBaseCurrency(), fxGross);

                                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                                        }))

                        // @formatter:off
                        // Borderelreferentie: 2024.000123456789 Referentie: D046336-758
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Borderelreferentie:(?<note>.*) Referentie.*$") //
                        .assign((t, v) -> t.setNote("Borderel-Ref.: " + trim(v.get("note"))))

                        // @formatter:off
                        // Borderelreferentie: 2024.000123456789 Referentie: D046336-758
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*Referentie:(?<note>.*)$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), "Ord.-Ref.: " + trim(v.get("note")), " | ")))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Beurstaks (0,35%): 8,04 USD
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Beurstaks .*: (?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Belgische roerende voorheffing (30,00%): 48,60 EUR
                        // @formatter:on
                        .section("withHoldingTax", "currency").optional() //
                        .match("^Belgische roerende voorheffing .*: (?<withHoldingTax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            if (!type.getCurrentContext().getBoolean("noTax"))
                                processWithHoldingTaxEntries(t, v, "withHoldingTax", type);
                        });
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Makelaarsloon: 35,31 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Makelaarsloon: (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Vaste kosten: 5,65 USD
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Vaste kosten: (?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
