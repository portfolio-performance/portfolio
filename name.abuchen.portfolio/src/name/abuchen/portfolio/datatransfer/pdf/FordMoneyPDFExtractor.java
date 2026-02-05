package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class FordMoneyPDFExtractor extends AbstractPDFExtractor
{
    public FordMoneyPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Ford Bank GmbH");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Ford Bank GmbH / Ford Money";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Tagesgeld Kontoauszug", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match("^datum datum in (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 04.10.2024 04.10.2024 Gutschrift 1.000,00
        // 25.10.2024 25.10.2024 Überweisung -2.000,00
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Gutschrift|.berweisung) ([\\-])?[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date","note", "sign", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Gutschrift|.berweisung))(?<sign>[\\s|\\-]{1,})(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is sign --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(trim(v.get("sign"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        var interestBlock = new Block("^Abschluss für Konto.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Zinsen vom 30.09.2024 bis 31.10.2024 Steuern vom 30.09.2024 bis 31.10.2024
                        // @formatter:on
                        .section("note", "date") //
                        .match("^Zinsen vom (?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} bis (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})) Steuern vom [\\d]{2}\\.[\\d]{2}\\.[\\d]{4}.*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // Summe 148,34 Kirchensteuer 0,00
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^Summe (?<amount>[\\.,\\d]+) .* (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 2,65 % p.a. 85,61 Kapitalertragsteuer -37,09
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^.* [\\.,\\d]+ Kapitalertrags(s)?teuer \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        // @formatter:off
                        // 2,35 % p.a. 62,73 Solidaritätszuschlag -2,03
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^.* [\\.,\\d]+ Solidarit.tszuschlag \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        // @formatter:off
                        // Summe 148,34 Kirchensteuer 0,0
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^.* [\\.,\\d]+ Kirchensteuer \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .wrap(TransactionItem::new));
    }
}
