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
public class SuresseDirektBankPDFExtractor extends AbstractPDFExtractor
{
    public SuresseDirektBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Suresse");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Suresse Direkt Bank";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Auszug [\\d]+([\\s]+)\\/[\\d]+", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo am 31-01-2023 : 5.011,05 EUR
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.* [\\d]{2}\\-[\\d]{2}\\-(?<year>[\\d]{4}) :[\\s|\\-]{1,}[\\.,\\d]+ [A-Z]{3}$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 1 19-12 19-12 Uberweisung zu Ihren Gunsten 1,00 EUR
        // 2 20-12 20-12 Ihre U¨berweisung -1,00 EUR
        // @formatter:on
        var depositBlock = new Block("^[\\d]+ [\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2} .*.berweisung.*[\\s|\\-]{1,}[\\.,\\d]+ [A-Z]{3}$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "sign", "amount", "currency", "note") //
                        .documentContext("year") //
                        .match("^[\\d]+ (?<date>[\\d]{2}\\-[\\d]{2}) [\\d]{2}\\-[\\d]{2} .*.berweisung.*(?<sign>[\\s|\\-]{1,})(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .match("^(?<note>Referenz : .*)$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is sign --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(trim(v.get("sign"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + "-" + v.get("year")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        var interestBlock = new Block("^Z I N S A B S C H L U S S$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // @formatter:off
                        // Periode von: 01-01-2023 bis 31-01-2023 Referenz: C3A31IN111111
                        // Zu erhalten Nettozinsbetrag : 8,25 EUR
                        // @formatter:on
                        .section("note", "date", "amount", "currency") //
                        .match("^Periode von: (?<note>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} .* (?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4})).*$") //
                        .match("^Zu erhalten Nettozinsbetrag : (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(v.get("note"));
                        })

                        // @formatter:off
                        // Summe verschiedene Steuern 0,00 EUR
                        // @formatter:on
                        .section("tax", "currency").optional() //
                        .match("^Summe verschiedene Steuern \\-(?<tax>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            t.addUnit(new Unit(Unit.Type.TAX, tax));
                        })

                        .wrap(TransactionItem::new));
    }
}
