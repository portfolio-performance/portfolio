package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentContext;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
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
    
    private static final String DEPOSIT_2022 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Zahlungseingang) (\\w+) (?<amount>[\\d\\s,.]*?) (.*)";
    private static final String REMOVAL_2022 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (.berweisung) (\\w+) (\\-)(?<amount>[\\d\\s,.]*?) (.*)";
    private static final String INTEREST_2022 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (\\w+zinsen) (\\w+) (?<amount>[\\d\\s,.]*?) (.*)";
    private static final String TAXES_2022 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Kapitalertragsteuer) (\\w+) (\\-)(?<amount>[\\d\\s,.]*?) (.*)";
    
    private static final String CONTEXT_KEY_YEAR = "year";
    private static final String CONTEXT_KEY_CURRENCY = "currency";
    private static final String CONTEXT_KEY_TRANSACTIONS_HAVE_FULL_DATE = "transactions_full_date";
    private static final String CONTEXT_VALUE_TRUE = "true";

    public RenaultBankDirektPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("305 200 37"); //$NON-NLS-1$
        addBankIdentifier("Renault Bank direkt"); //$NON-NLS-1$
        
        addTransactionWith2019Format();
        addTransactionWith2021Format();
        addTransactionWith2022Format();
    }

    @Override
    public String getLabel()
    {
        return "Renault Bank direkt"; //$NON-NLS-1$
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
    
    private void addTransactionWith2022Format()
    {
        DocumentType type = new DocumentType("Renault Bank direkt", contextProvider2022());
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT_2022);
        depositBlock.set(depositTransaction(type, DEPOSIT_2022));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL_2022);
        removalBlock.set(removalTransaction(type, REMOVAL_2022));
        type.addBlock(removalBlock);
        
        Block interestBlock = new Block(INTEREST_2022);
        interestBlock.set(interestTransaction(type, INTEREST_2022));
        type.addBlock(interestBlock);
        
        Block taxesBlock = new Block(TAXES_2022);
        taxesBlock.set(taxesTransaction(type, TAXES_2022));
        type.addBlock(taxesBlock);
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
    
    private Transaction<AccountTransaction> taxesTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.TAXES);
            return entry;})
                        .section("date", "amount")
                        .match(regex)
                        .assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private BiConsumer<AccountTransaction, ParsedData> assignmentsProvider(DocumentType type)
    {
        return (transaction, matcherMap) -> {
            Map<String, String> context = type.getCurrentContext();

            boolean transactionsHaveFullDate = CONTEXT_VALUE_TRUE.equals(context.get(CONTEXT_KEY_TRANSACTIONS_HAVE_FULL_DATE));
            
            String date = matcherMap.get("date");
            
            if (!transactionsHaveFullDate)
            {
                date += context.get(CONTEXT_KEY_YEAR);
            }
            
            transaction.setDateTime(asDate(date));
            transaction.setAmount(asAmount(matcherMap.get("amount")));
            transaction.setCurrencyCode(context.get(RenaultBankDirektPDFExtractor.CONTEXT_KEY_CURRENCY));
        };
    }

    private BiConsumer<DocumentContext, String[]> contextProvider2019()
    {
        return (context, lines) -> {
            Pattern yearPattern = Pattern.compile("(.*)(KONTOAUSZUG  Nr. )(\\d+)\\/(?<year>\\d{4})");
            Pattern currencyPattern = Pattern
                            .compile("(.*)(Summe Zinsen\\/Kontoführung)(\\s*)                (?<currency>[\\w]{3})(.*)");
            
            contextProviderCommon(context, lines, yearPattern, currencyPattern);
        };
    }

    private BiConsumer<DocumentContext, String[]> contextProvider2021()
    {
        return (context, lines) -> {
            Pattern yearPattern = Pattern.compile("(.*)(Kontoauszug Nr\\.)(\\s*)(\\d+)\\/(?<year>\\d{4})");
            Pattern currencyPattern = Pattern
                            .compile("(?<currency>[\\w]{3})(-Konto Kontonummer)(.*)");
            contextProviderCommon(context, lines, yearPattern, currencyPattern);
        };
    }

    private BiConsumer<DocumentContext, String[]> contextProvider2022()
    {
        return (context, lines) -> {
            Pattern currencyPattern = Pattern
                            .compile("(.*) Betrag in (?<currency>[\\w]{3}) (.*)");
            context.put(CONTEXT_KEY_TRANSACTIONS_HAVE_FULL_DATE, CONTEXT_VALUE_TRUE);
            contextProviderCommon(context, lines, null, currencyPattern);
        };
    }
    
    private void contextProviderCommon(Map<String, String> context,
                    String[] lines, 
                    Pattern yearPattern,
                    Pattern currencyPattern)
    {
        for (String line : lines)
        {
            if (yearPattern != null)
            {
                Matcher yearMatcher = yearPattern.matcher(line);
                if (yearMatcher.matches())
                {
                    context.put(CONTEXT_KEY_YEAR, yearMatcher.group("year"));
                }
            }

            Matcher currencyMatcher = currencyPattern.matcher(line);
            if (currencyMatcher.matches())
            {
                context.put(CONTEXT_KEY_CURRENCY, currencyMatcher.group("currency"));
            }
        }
    }
}
