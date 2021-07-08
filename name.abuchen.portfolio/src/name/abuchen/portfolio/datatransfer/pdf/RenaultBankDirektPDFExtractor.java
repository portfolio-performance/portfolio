package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class RenaultBankDirektPDFExtractor extends AbstractPDFExtractor
{
    private static final String DEPOSIT_2019 = "^(?<date>\\d+.\\d+.)  (.*-Gutschrift)(\\s*)(?<amount>[\\d\\s,.]*)(\\+$)";
    private static final String REMOVAL_2019 = "^(?<date>\\d+.\\d+.)  (Internet-Euro-Überweisung)(\\s*)(?<amount>[\\d\\s,.]*)(\\-)(.*)";
    private static final String INTEREST_2019 = "^(?<date>\\d+.\\d+.)  (.*)(Zinsen\\/Kontoführung)(\\s*)(?<amount>[\\d\\s,.]*)(\\+$)";
    
    private static final String DEPOSIT_2021 = "^(?<date>\\d+.\\d+.) (\\d+.\\d+.) (.*gutschr\\.?)(\\s*)(?<amount>[\\d\\s,.]*) [H]";
    private static final String REMOVAL_2021 = "^(?<date>\\d+.\\d+.) (\\d+.\\d+.) (Umbuchung)(\\s*)(?<amount>[\\d\\s,.]*) [S]";
    private static final String INTEREST_2021 = "^(?<date>\\d+.\\d+.) (\\d+.\\d+.) (Abschluss)(\\s*)(?<amount>[\\d\\s,.]*) [H]";
    
    private static final String CONTEXT_KEY_YEAR = "year";
    private static final String CONTEXT_KEY_CURRENCY = "currency";

    public RenaultBankDirektPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("305 200 37"); //$NON-NLS-1$
        addBankIdentifier("Renault Bank direkt"); //$NON-NLS-1$
        
        addTransactionWith2019Format();
        addTransactionWith2021Format();
    }

    private void addTransactionWith2019Format()
    {
        DocumentType type = new DocumentType("305 200 37", contextProvider2019());
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT_2019);
        depositBlock.set(depositTransaction(type, DEPOSIT_2019));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL_2019);
        removalBlock.set(removalTransaction(type, REMOVAL_2019));
        type.addBlock(removalBlock);
        
        Block interestBlock = new Block(INTEREST_2019);
        interestBlock.set(interestTransaction(type, INTEREST_2019));
        type.addBlock(interestBlock);
    }
    
    private void addTransactionWith2021Format()
    {
        DocumentType type = new DocumentType("Renault Bank direkt", contextProvider2021());
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT_2021);
        depositBlock.set(depositTransaction(type, DEPOSIT_2021));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL_2021);
        removalBlock.set(removalTransaction(type, REMOVAL_2021));
        type.addBlock(removalBlock);
        
        Block interestBlock = new Block(INTEREST_2021);
        interestBlock.set(interestTransaction(type, INTEREST_2021));
        type.addBlock(interestBlock);
    }

    private Transaction<AccountTransaction> depositTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> interestTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.REMOVAL);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private BiConsumer<AccountTransaction, Map<String, String>> assignmentsProvider(DocumentType type)
    {
        return (transaction, matcherMap) -> {
            Map<String, String> context = type.getCurrentContext();

            transaction.setDateTime(asDate(matcherMap.get("date") + context.get(CONTEXT_KEY_YEAR)));
            transaction.setAmount(asAmount(matcherMap.get("amount")));
            transaction.setCurrencyCode(context.get(RenaultBankDirektPDFExtractor.CONTEXT_KEY_CURRENCY));
        };
    }

    private BiConsumer<Map<String, String>, String[]> contextProvider2019()
    {
        return (context, lines) -> {
            Pattern yearPattern = Pattern.compile("(.*)(KONTOAUSZUG  Nr. )(\\d+)\\/(?<year>\\d{4})");
            Pattern currencyPattern = Pattern
                            .compile("(.*)(Summe Zinsen\\/Kontoführung)(\\s*)                (?<currency>[\\w]{3})(.*)");
            
            contextProviderCommon(context, lines, yearPattern, currencyPattern);
        };
    }

    private BiConsumer<Map<String, String>, String[]> contextProvider2021()
    {
        return (context, lines) -> {
            Pattern yearPattern = Pattern.compile("(.*)(Kontoauszug Nr\\.)(\\s*)(\\d+)\\/(?<year>\\d{4})");
            Pattern currencyPattern = Pattern
                            .compile("(?<currency>[\\w]{3})(-Konto Kontonummer)(.*)");
            contextProviderCommon(context, lines, yearPattern, currencyPattern);
        };
    }
    
    private void contextProviderCommon(Map<String, String> context,
                    String[] lines, 
                    Pattern yearPattern,
                    Pattern currencyPattern)
    {
        for (String line : lines)
        {
            Matcher yearMatcher = yearPattern.matcher(line);
            if (yearMatcher.matches())
            {
                context.put(CONTEXT_KEY_YEAR, yearMatcher.group("year"));
            }

            Matcher currencyMatcher = currencyPattern.matcher(line);
            if (currencyMatcher.matches())
            {
                context.put(CONTEXT_KEY_CURRENCY, currencyMatcher.group("currency"));
            }
        }
    }

    @Override
    public String getLabel()
    {
        return "Renault Bank direkt"; //$NON-NLS-1$
    }
    
    @Override
    public String getPDFAuthor()
    {
        return ""; //$NON-NLS-1$
    }
}
