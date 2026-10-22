package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

/**
 * @implNote J.P. Morgan SE operates the retail brand Chase in Germany.
 *           Currently only account statements of the Chase Tagesgeld
 *           (overnight money) account are supported.
 */
@SuppressWarnings("nls")
public class ChasePDFExtractor extends AbstractPDFExtractor
{
    public ChasePDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("J.P. Morgan SE");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "J.P. Morgan SE (Chase)";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug . Chase Tagesgeld");
        this.addDocumentTyp(type);

        // @formatter:off
        // 18. Aug. 2026 Von AAenlaJXh gtmAV +5,00 € 5,00 €
        // 18. Aug. 2026 Von zxVORVRSn SYnNM +36.427,04 € 36.432,04 €
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{1,2}\\. .{3,5} [\\d]{4} (Von|An) .* [\\-\\+][\\.,\\d]+ \\p{Sc} (\\-)?[\\.,\\d]+ \\p{Sc}$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        // @formatter:off
                        // 18. Aug. 2026 Von AAenlaJXh gtmAV +5,00 € 5,00 €
                        // @formatter:on
                        .section("day", "month", "year", "type", "amount", "currency") //
                        .match("^(?<day>[\\d]{1,2})\\. (?<month>.{3,5}) (?<year>[\\d]{4}) (Von|An) .* (?<type>[\\-\\+])(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) (\\-)?[\\.,\\d]+ \\p{Sc}$") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // IBAN: cY69 8303 4080 0909 5673 59
                        // Einzahlung Tagesgeld
                        // @formatter:on
                        .section("note").optional() //
                        .match("^IBAN: .*$") //
                        .match("^(?<note>.*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 01. Sept. 2026 Zinsen +41,16 € 36.473,20 €
        // Steuerabzüge:
        //  13,97 € Kapitalertragssteuer
        //  0,76 € Solidaritätszuschlag
        //  0,00 € Kirchensteuer
        // @formatter:on
        var interestBlock = new Block("^[\\d]{1,2}\\. .{3,5} [\\d]{4} Zinsen \\+[\\.,\\d]+ \\p{Sc} (\\-)?[\\.,\\d]+ \\p{Sc}$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        // @formatter:off
                        // 01. Sept. 2026 Zinsen +41,16 € 36.473,20 €
                        // @formatter:on
                        .section("day", "month", "year", "amount", "currency") //
                        .match("^(?<day>[\\d]{1,2})\\. (?<month>.{3,5}) (?<year>[\\d]{4}) Zinsen \\+(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}) (\\-)?[\\.,\\d]+ \\p{Sc}$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + v.get("year")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        //  13,97 € Kapitalertragssteuer
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^[\\s]*(?<tax>[\\.,\\d]+) (?<currency>\\p{Sc}) Kapitalertrags(s)?teuer$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        // @formatter:off
                        //  0,76 € Solidaritätszuschlag
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^[\\s]*(?<tax>[\\.,\\d]+) (?<currency>\\p{Sc}) Solidarit.tszuschlag$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        // @formatter:off
                        //  0,00 € Kirchensteuer
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^[\\s]*(?<tax>[\\.,\\d]+) (?<currency>\\p{Sc}) Kirchensteuer$") //
                        .assign((t, v) -> t.addUnit(new Unit(Unit.Type.TAX, //
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax"))))))

                        .wrap(TransactionItem::new));
    }
}
