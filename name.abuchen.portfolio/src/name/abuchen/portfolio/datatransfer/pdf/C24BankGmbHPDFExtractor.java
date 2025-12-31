package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class C24BankGmbHPDFExtractor extends AbstractPDFExtractor
{
    public C24BankGmbHPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("C24 Bank GmbH");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "C24 Bank GmbH";
    }


    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug [\\d]{2}\\/[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Kontoauszug 05/2024 Kontostand 0,00 €
                                        // Vorläufiger Kontoauszug 08/2024 Kontostand 0,00 €
                                        // @formatter:on
                                        .section("year") //
                                        .match("^(Vorl.ufiger )?Kontoauszug [\\d]{2}\\/(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))


                                        .optionalOneOf( //
                                                        // @formatter:off
                                                        // 31.05. 31.05. Steuern - 2,29 €
                                                        // @formatter:on
                                                        section -> section //
                                                                        .attributes("year", "taxDate", "tax", "taxCurrency") //
                                                                        .match("^(Vorl.ufiger )?Kontoauszug [\\d]{2}\\/(?<year>[\\d]{4}).*$") //
                                                                        .match("^(?<taxDate>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Steuern [\\-|\\+] (?<tax>[\\.,\\d]+) (?<taxCurrency>\\p{Sc}).*$") //
                                                                        .assign((ctx, v) -> {
                                                                            ctx.put("taxDate", v.get("taxDate") + v.get("year"));
                                                                            ctx.put("tax", v.get("tax"));
                                                                            ctx.put("taxCurrency", v.get("taxCurrency"));
                                                                        })));

        this.addDocumentTyp(type);

        // @formatter:off
        // 17.05. 17.05. Überweisung - 1.508,42 €
        // 17.05. 17.05. Überweisung + 1.115,22 €
        // 05.08. 05.08. Echtzeitüberweisung - 2.800,00 €
        // 31.01. 31.01. Lastschrift - 3.800,00 €
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(?!(Zinsen|Steuern))" //
                        + ".* " //
                        + "[\\-|\\+] [\\.,\\d]+ \\p{Sc}.*$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?!(Zinsen|Steuern))" //
                                        + "(?<note>.*) " //
                                        + "(?<type>[\\-|\\+]) " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.05. 31.05. Zinsen + 1,93 €
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Zinsen [\\-|\\+] [\\.,\\d]+ \\p{Sc}.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .documentContextOptionally("taxDate", "tax", "taxCurrency") //)
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<note>Zinsen) " //
                                        + "(?<type>[\\-|\\+]) " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));

                            if (v.containsKey("tax") && v.containsKey("taxCurrency") && t.getDateTime().equals(asDate(v.get("taxDate"))))
                            {
                                var tax = Money.of(asCurrencyCode(v.get("taxCurrency")), asAmount(v.get("tax")));
                                t.addUnit(new Unit(Unit.Type.TAX, tax));
                            }
                        })

                        .wrap(TransactionItem::new));
    }
}
