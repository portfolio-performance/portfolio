package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class Bank11PDFExtractor extends AbstractPDFExtractor
{
    private static final String DEPOSIT = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. (?<note>.*gutschr\\.?)[\\s]{1,}(?<amount>[\\.,\\d]+) [H]";
    private static final String REMOVAL = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. (?<note>(Umbuchung|.*berweisungsauftrag))[\\s]{1,}(?<amount>[\\.,\\d]+) [S]";
    private static final String INTEREST = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Abschluss.*[\\s]{1,}(?<amount>[\\.,\\d]+) [H]";

    public Bank11PDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Bank11 für Privatkunden und Handel GmbH");

        addTransactions();
    }

    @Override
    public String getLabel()
    {
        return "Bank11 für Privatkunden und Handel GmbH";
    }

    private void addTransactions()
    {
        final DocumentType type = new DocumentType(".*-Konto Kontonummer", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Hammer Landstr. 91, 41460 Neuss Kontoauszug Nr.  1/2022
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.*Kontoauszug Nr\\.[\\s]{1,}[\\d]{1,2}\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        // EUR-Konto Kontonummer 764783800
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[\\w]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT);
        depositBlock.set(depositTransaction(type, DEPOSIT));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL);
        removalBlock.set(removalTransaction(type, REMOVAL));
        type.addBlock(removalBlock);

        Block interestBlock = new Block(INTEREST);
        interestBlock.set(interestTransaction(type, INTEREST));
        type.addBlock(interestBlock);
    }

    private Transaction<AccountTransaction> depositTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "note", "amount") //
                        .documentContext("currency", "year") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>()

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

    private void assignmentsProvider(AccountTransaction t, ParsedData v)
    {
        t.setDateTime(asDate(v.get("date") + v.get("year")));
        t.setAmount(asAmount(v.get("amount")));
        t.setCurrencyCode(asCurrencyCode(v.get("currency")));

        // Formatting some notes
        if ("Überweisungsgutschr.".equals(v.get("note")))
            v.put("note", "Überweisungsgutschrift");

        t.setNote(v.get("note"));
    }
}
