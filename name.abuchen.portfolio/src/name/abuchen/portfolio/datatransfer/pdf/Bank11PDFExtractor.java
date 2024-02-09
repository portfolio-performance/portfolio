package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.function.BiConsumer;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class Bank11PDFExtractor extends AbstractPDFExtractor
{
    private static final String DEPOSIT = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (.*gutschr\\.?)(\\s+)(?<amount>[\\.,\\d]+) [H]";
    private static final String REMOVAL = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (Umbuchung|.*berweisungsauftrag)(\\s+)(?<amount>[\\.,\\d]+) [S]";
    private static final String INTEREST = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (Abschluss.*)(\\s+)(?<amount>[\\.,\\d]+) [H]";

    private static final String CONTEXT_KEY_YEAR = "year";
    private static final String CONTEXT_KEY_CURRENCY = "currency";

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
                                        .section("year").match("(.*)(Kontoauszug Nr\\.)(\\s*)(\\d+)\\/(?<year>\\d{4})")
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        .section("currency").match("(?<currency>[\\w]{3})(-Konto Kontonummer)(.*)")
                                        .assign((ctx, v) -> ctx.put(CONTEXT_KEY_CURRENCY,
                                                        asCurrencyCode(v.get("currency")))));
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
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;
        }).section("date", "amount").match(regex).assign(assignmentsProvider(type)).wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        }).section("date", "amount").match(regex).assign(assignmentsProvider(type)).wrap(TransactionItem::new);
    }


    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.REMOVAL);
            return entry;
        }).section("date", "amount").match(regex).assign(assignmentsProvider(type)).wrap(TransactionItem::new);
    }


    private BiConsumer<AccountTransaction, ParsedData> assignmentsProvider(DocumentType type)
    {
        return (transaction, matcherMap) -> {
            Map<String, String> context = type.getCurrentContext();

            String date = matcherMap.get("date");

            date += context.get(CONTEXT_KEY_YEAR);

            transaction.setDateTime(asDate(date));
            transaction.setAmount(asAmount(matcherMap.get("amount")));
            transaction.setCurrencyCode(asCurrencyCode(context.get(CONTEXT_KEY_CURRENCY)));
        };
    }
}
