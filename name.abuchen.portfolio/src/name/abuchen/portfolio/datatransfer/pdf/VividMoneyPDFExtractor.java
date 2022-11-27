package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class VividMoneyPDFExtractor extends AbstractPDFExtractor
{
    
    private static final String DEPOSIT = "^(?<date>\\d+.\\d+.\\d+) (?<valuta>\\d+.\\d+.\\d+) (SEPA\\-.berweisung|.berweisung).* (?<amount>\\d*[,.]\\d{2}) (?<currency>.{3})$";
    private static final String REMOVAL = "^(?<date>\\d+.\\d+.\\d+) (?<valuta>\\d+.\\d+.\\d+) (Kartenzahlung|Überweisung) (?<note>.*) (?<amount>\\-\\d*[,.]\\d{2}) (?<currency>.{3})$";
    
    private static final String CONTEXT_KEY_DATE = "date";
    private static final String CONTEXT_KEY_AMOUNT = "amount";
    private static final String CONTEXT_KEY_CURRENCY = "currency";
    private static final String CONTEXT_KEY_NOTE = "note";

    public VividMoneyPDFExtractor(Client client)
    {
        super(client);
        
        addBankIdentifier("SOBKDEB2XXX"); //$NON-NLS-1$
        addBankIdentifier("Vivid"); //$NON-NLS-1$

        addAccountStatement();
    }

    @Override
    public String getLabel()
    {
        return "Vivid"; //$NON-NLS-1$
    }
    
    private void addAccountStatement()
    {
        DocumentType type = new DocumentType("Rechnungsabschluss", (context, lines) -> {
            Pattern accountDetailPattern = Pattern.compile("^.*IBAN: (?<iban>.*)\\W+BIC: (?<bic>.*)$");
            Arrays.stream(lines).forEach(line -> {
                Matcher m = accountDetailPattern.matcher(line);
                if (m.matches())
                {
                    context.put("account", m.group("iban"));
                    context.put("bic", m.group("bic"));
                }
            });
        });
        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT);
        depositBlock.set(depositTransaction(DEPOSIT));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL);
        removalBlock.set(removalTransaction(REMOVAL));
        type.addBlock(removalBlock);
    }

    
    private Transaction<AccountTransaction> depositTransaction(String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;})
                        .section(CONTEXT_KEY_DATE, CONTEXT_KEY_AMOUNT, CONTEXT_KEY_CURRENCY)
                        .match(regex)
                        .assign(assignmentsProvider())
                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> removalTransaction(String regex)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.REMOVAL);
            return entry;})
                        .section(CONTEXT_KEY_DATE, CONTEXT_KEY_AMOUNT, CONTEXT_KEY_CURRENCY, CONTEXT_KEY_NOTE)
                        .match(regex)
                        .assign(assignmentsProvider())
                        .wrap(TransactionItem::new);
    }

    private BiConsumer<AccountTransaction, ParsedData> assignmentsProvider()
    {
        return (transaction, matcherMap) -> {
            transaction.setDateTime(asDate(matcherMap.get(CONTEXT_KEY_DATE)));
            transaction.setAmount(asAmount(matcherMap.get(CONTEXT_KEY_AMOUNT)));
            transaction.setCurrencyCode(matcherMap.get(CONTEXT_KEY_CURRENCY));
            transaction.setNote(matcherMap.get(CONTEXT_KEY_NOTE));
        };
    }

}
