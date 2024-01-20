package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class JTDirektbankPDFExtractor extends AbstractPDFExtractor
{

    private static final String DEPOSIT_2023 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (.*gutschr\\.?)(\\s*)(?<amount>[\\.\\d]+,[\\d]{2}) [H]";
    private static final String REMOVAL_2023 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (Überweisungsauftrag)(\\s*)(?<amount>[\\.\\d]+,[\\d]{2}) [S]";
    private static final String INTEREST_2023 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (Abschluss lt\\. Anlage \\d) (?<amount>[\\.\\d]+,[\\d]{2}) [H]";
    private static final String INTEREST_CHARGE_2023 = "^(?<date>[\\d]{2}\\.[\\d]{2}\\.) ([\\d]{2}\\.[\\d]{2}\\.) (Storno Abschluss)(\\s*)(?<amount>[\\.\\d]+,[\\d]{2}) [S]";

    private static final String CONTEXT_KEY_YEAR = "year";
    private static final String CONTEXT_KEY_CURRENCY = "currency";
    private static final String CONTEXT_KEY_TRANSACTIONS_HAVE_FULL_DATE = "transactions_full_date";
    private static final String CONTEXT_VALUE_TRUE = "true";

    public JTDirektbankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("J&T Direktbank");

        addTransactionWith2023Format();
    }

    @Override
    public String getLabel()
    {
        return "J&T Direktbank";
    }

    private void addTransactionWith2023Format()
    {
        DocumentType type = new DocumentType("J&T Direktbank", contextProvider2023());
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT_2023);
        depositBlock.set(depositTransaction(type, DEPOSIT_2023));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL_2023);
        removalBlock.set(removalTransaction(type, REMOVAL_2023));
        type.addBlock(removalBlock);

        Block interestBlock = new Block(INTEREST_2023);
        interestBlock.set(interestTransaction(type, INTEREST_2023));
        type.addBlock(interestBlock);

        Block interestChargeBlock = new Block(INTEREST_CHARGE_2023);
        interestChargeBlock.set(interestChargeTransaction(type, INTEREST_CHARGE_2023));
        type.addBlock(interestChargeBlock);
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

    private Transaction<AccountTransaction> interestChargeTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST_CHARGE);
            return entry;
        }).section("date", "amount").match(regex).assign(assignmentsProvider(type)).wrap(TransactionItem::new);
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
            transaction.setCurrencyCode(context.get(JTDirektbankPDFExtractor.CONTEXT_KEY_CURRENCY));
        };
    }

    private BiConsumer<DocumentContext, String[]> contextProvider2023()
    {
        return (context, lines) -> {
            Pattern yearPattern = Pattern.compile("^.*Kontoauszug Nr\\.[\\s]{1,}[\\d]{1,2}\\/(?<year>[\\d]{4})$");
            Pattern currencyPattern = Pattern.compile("^(?<currency>[\\w]{3})\\-Konto Kontonummer.*$");
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
