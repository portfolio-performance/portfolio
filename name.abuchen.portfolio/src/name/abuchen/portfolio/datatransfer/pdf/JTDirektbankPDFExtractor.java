package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class JTDirektbankPDFExtractor extends AbstractPDFExtractor
{
    public JTDirektbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("J&T Direktbank");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "J&T Direktbank";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("J&T Direktbank", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // 45952 Gladbeck Kontoauszug Nr.  1/2023
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.*Kontoauszug Nr\\.[\\s]{1,}[\\d]{1,2}\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // EUR-Konto Kontonummer 6480010
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[A-Z]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 31.03. 31.03. Überweisungsgutschr. 3.000,00 H
        // 01.06. 01.06. Überweisungsauftrag 400,00 S
        // 18.12. 18.12. Dauerauftragsgutschr 900,00 H
        // 29.01. 29.01. Spar/Fest/Termingeld 5.000,00 S
        // 08.03. 08.03. Umbuchung  1.100,00 S
        // @formatter:on
        var depositRemovalBlock = new Block(
                        "^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. (.*gutschr\\.?|.berweisungsauftrag|Spar/Fest/Termingeld|Umbuchung)[\s]{1,}[\\.,\\d]+ [S|H]$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. (?<note>(.*gutschr\\.?|.berweisungsauftrag|Spar/Fest/Termingeld|Umbuchung))[\s]{1,}(?<amount>[\\.,\\d]+) (?<type>[S|H])$") //
                        .assign((t, v) -> {
                        // @formatter:off
                            // Is type is "S" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("S".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            if ("Überweisungsgutschr.".equals(v.get("note")))
                                v.put("note", "Überweisungsgutschrift");

                            if ("Dauerauftragsgutschr".equals(v.get("note")))
                                v.put("note", "Dauerauftragsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));


        // @formatter:off
        // 04.05. 30.04. Abschluss lt. Anlage 1 11,69 H
        // @formatter:on
        var interestBlock = new Block(
                        "^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Abschluss lt\\. Anlage [\\d][\\s]{1,}[\\.,\\d]+ [H]$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Abschluss lt\\. Anlage [\\d][\\s]{1,}(?<amount>[\\.,\\d]+) [H]$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new));
        

        // @formatter:off
        // 03.05. 30.04. Storno Abschluss  13,44 S
        // @formatter:on
        var interestCancellation = new Block(
                        "^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Storno .*[\\s]{1,}[\\.,\\d]+ [S]$");
        type.addBlock(interestCancellation);
        interestCancellation.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Storno .*[\\s]{1,}(?<amount>[\\.,\\d]+) [S]$") //
                        .assign((t, v) -> {
                            v.getTransactionContext().put(FAILURE,
                                            Messages.MsgErrorTransactionOrderCancellationUnsupported);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        }));
    }

}
