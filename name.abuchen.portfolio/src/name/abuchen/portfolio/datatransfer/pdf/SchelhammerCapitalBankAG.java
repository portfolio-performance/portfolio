package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;

@SuppressWarnings("nls")
public class SchelhammerCapitalBankAG extends AbstractPDFExtractor
{
    public SchelhammerCapitalBankAG(Client client)
    {
        super(client);

        addBankIdentifier("Schelhammer Capital Bank AG");

        addBuySellTransaction();
        addDividendeTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Schelhammer Capital Bank AG";
    }

    private void addBuySellTransaction()
    {
        var type = new DocumentType("Gesch.ftsart: (Kauf|Verkauf|Ausgabe Fonds aus Dauerauftrag)");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^Abrechnung (Handel|Dauerauftrag)$");
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
                        .match("^Gesch.ftsart: (?<type>(Kauf|Verkauf)).*$") //
                        .assign((t, v) -> {
                            if ("Verkauf".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        .oneOf( //
                                        // @formatter:off
                                        // Titel: IE00BYPHT736  X t r . ( I E ) - i B oxx EUR Cor.Bd Y.P.
                                        // Registered Shares 1D o.N.
                                        // Kurs: 15,185916 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Kurs: [\\.,\\d]+ (?<currency>[\\w]{3})[\\s]*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // Zugang: 5,319 Stk
                        // Abgang: 282,2 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^(Zugang|Abgang): (?<shares>[\\.,\\d]+) Stk[\\s]*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Handelszeit: 8.1.2025 um 17:30:00 Uhr
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "time") //
                                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}) .* (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date"), v.get("time")))),
                                        // @formatter:off
                                        // Handelszeit: 18.12.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Handelszeit: (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4}).*$") //
                                                        .assign((t, v) -> t.setDate(asDate(v.get("date")))))

                        // @formatter:off
                        // Zu Lasten IBAN AT18 4419 3463 9750 1470 -80,77 EUR
                        // Zu Gunsten IBAN AT18 3222 3274 4871 1501 9.711,19 EUR
                        // @formatter:on
                        .section("amount", "currency") //
                        .match("^Zu (Lasten|Gunsten) IBAN .* (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Auftrags-Nr.: 12345678-8.1.2025
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftrags\\-Nr.: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(BuySellEntryItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendeTransaction()
    {
        var type = new DocumentType("Geschäftsart: Ertrag");
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^Wir haben f.r Sie am.*$");
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
                                        // Titel: IE00BSKRJX20  i S h s I V - E O  G o.Bd 20yr T.D.U.ETF
                                        // egistered Shares EUR (Dist)oN
                                        // Ertrag: 0,0549 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("isin", "name", "nameContinued", "currency") //
                                                        .match("^Titel: (?<isin>[A-Z]{2}[A-Z0-9]{9}[0-9]) (?<name>.*)$") //
                                                        .match("^(?<nameContinued>.*)$") //
                                                        .match("^Ertrag: [\\.,\\d]+ (?<currency>[\\w]{3})[\\s]*$") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // 778,05 Stk
                        // @formatter:on
                        .section("shares") //
                        .match("^(?<shares>[\\.,\\d]+) Stk[\\s]*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // Valuta 27.12.2024
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date") //
                                                        .match("^Valuta (?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4})[\\s]*$") //
                                                        .assign((t, v) -> t.setDateTime(asDate(v.get("date")))))

                        .oneOf( //
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT18 8415 8694 0263 7385 38,59 EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Zu Gunsten IBAN .* (?<amount>[\\.,\\d]+) (?<currency>[\\w]{3})[\\s]*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // Zu Gunsten IBAN AT48 2048 5050 9000 9796 0,-- EUR
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("currency", "amount") //
                                                        .match("^Zu Gunsten IBAN .* (?<amount>[\\-\\.,\\d]+) (?<currency>[\\w]{3})[\\s]*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        // @formatter:off
                        // Auftragsnummer: 330401346 Kontonummer: 1234567.031
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>Auftragsnummer: [\\d]+) .*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new);

        addTaxesSectionsTransaction(pdfTransaction, type);
        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private <T extends Transaction<?>> void addTaxesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // Kapitalertragsteuer: -130,54 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Kapitalertrags(s)?teuer: \\-(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type))

                        // @formatter:off
                        // Auslands-KESt: -19,77 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Auslands\\-KESt: \\-(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processTaxEntries(t, v, type));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //


                        // @formatter:off
                        // Effektenprovision: -0,40 EUR
                        // @formatter:on
                        .section("fee", "currency").optional() //
                        .match("^Effektenprovision: \\-(?<fee>[\\.,\\d]+) (?<currency>[A-Z]{3})[\\s]*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }
}
