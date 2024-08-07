package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

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
        final DocumentType type = new DocumentType("Kontoauszug [\\d]{2}\\/[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Kontoauszug 05/2024 Kontostand 0,00 €
                                        // Vorläufiger Kontoauszug 08/2024 Kontostand 0,00 €
                                        // @formatter:on
                                        .section("year") //
                                        .match("^(Vorl.ufiger )?Kontoauszug [\\d]{2}\\/(?<year>[\\d]{4}) .*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 17.05. 17.05. Überweisung - 1.508,42 €
        // 17.05. 17.05. Überweisung + 1.115,22 €
        // 05.08. 05.08. Echtzeitüberweisung - 2.800,00 €
        // @formatter:on
        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. "
                        + "(.berweisung"
                        + "|Echtzeit.berweisung) "
                        + "[\\-|\\+] [\\.,\\d]+ \\p{Sc}.*$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<note>.berweisung"
                                        + "|Echtzeit.berweisung) " //
                                        + "(?<type>[\\-|\\+]) " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // Is sign --> "-" change from DEPOSIT to REMOVAL
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
        Block interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Zinsen [\\-|\\+] [\\.,\\d]+ \\p{Sc}.*$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<note>Zinsen) " //
                                        + "(?<type>[\\-|\\+]) " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // Is sign --> "-" change from INTEREST to INTEREST_CHARGE
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 30.06. 30.06. Steuern - 4,04 €
        // @formatter:on
        Block taxesBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Steuern [\\-|\\+] [\\.,\\d]+ \\p{Sc}.*$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAX_REFUND);
                            return accountTransaction;
                        })

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<note>Steuern) " //
                                        + "(?<type>[\\-|\\+]) " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // Is sign --> "-" change from TAXES to TAX_REFUND
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAXES);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }
}
