package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class JTDirektbankPDFExtractor extends AbstractPDFExtractor
{
    private static final String DEPOSIT_REMOVAL = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. (?<note>(.*gutschr\\.?|.berweisungsauftrag|Spar/Fest/Termingeld|Umbuchung))[\s]{1,}(?<amount>[\\.,\\d]+) (?<type>[S|H])$";
    private static final String INTEREST = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Abschluss lt\\. Anlage [\\d][\\s]{1,}(?<amount>[\\.,\\d]+) [H]$";
    private static final String INTEREST_CANCELLATION = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Storno .*[\\s]{1,}(?<amount>[\\.,\\d]+) [S]$";

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
        final DocumentType type = new DocumentType("J&T Direktbank", //
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
                                        .match("^(?<currency>[\\w]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        Block depositRemovalBlock = new Block(DEPOSIT_REMOVAL);
        depositRemovalBlock.set(depositRemovalTransaction(type, DEPOSIT_REMOVAL));
        type.addBlock(depositRemovalBlock);

        Block interestBlock = new Block(INTEREST);
        interestBlock.set(interestTransaction(type, INTEREST));
        type.addBlock(interestBlock);

        Block interestCanellation = new Block(INTEREST_CANCELLATION);
        interestCanellation.set(interestCanellationTransaction(type, INTEREST_CANCELLATION));
        type.addBlock(interestCanellation);
    }

    private Transaction<AccountTransaction> depositRemovalTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>() //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note", "type") //
                        .documentContext("currency", "year") //
                        .match(regex) //
                        .assign((t, v) -> {

                            // @formatter:off
                            // Is type is "S" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("S".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>() //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestCanellationTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>() //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST_CHARGE);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency", "year") //
                        .match(regex) //
                        .assign((t, v) -> {
                            v.getTransactionContext().put(FAILURE, Messages.MsgErrorOrderCancellationUnsupported);

                            assignmentsProvider(t, v);
                        })

                        .wrap((t, ctx) -> {
                            TransactionItem item = new TransactionItem(t);

                            if (ctx.getString(FAILURE) != null)
                                item.setFailureMessage(ctx.getString(FAILURE));

                            return item;
                        });
    }

    private void assignmentsProvider(AccountTransaction t, ParsedData v)
    {
        t.setDateTime(asDate(v.get("date") + v.get("year")));
        t.setAmount(asAmount(v.get("amount")));
        t.setCurrencyCode(asCurrencyCode(v.get("currency")));

        // Formatting some notes
        if ("Überweisungsgutschr.".equals(v.get("note")))
            v.put("note", "Überweisungsgutschrift");

        if ("Dauerauftragsgutschr".equals(v.get("note")))
            v.put("note", "Dauerauftragsgutschrift");

        t.setNote(v.get("note"));
    }
}
