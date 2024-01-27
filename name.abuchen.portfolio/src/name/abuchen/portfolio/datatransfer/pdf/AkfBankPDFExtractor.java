package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Map;
import java.util.function.BiConsumer;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.ParsedData;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransaction.Type;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AkfBankPDFExtractor extends AbstractPDFExtractor
{

    private static final String DEPOSIT = "^[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (.*Gutschrift.*) (?<amount>[\\.\\d]+,[\\d]{2})";
    private static final String REMOVAL = "^[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (.*berweisung.*|Festgeld Anlage|Sparkonto K[ü|ue]ndigung) [-](?<amount>[\\.\\d]+,[\\d]{2})";
    private static final String INTEREST = "^[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ ([\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Kontoabschlu[ß|ss]) (?<amount>[\\.\\d]+,[\\d]{2})";
    private static final String FEES = "^(?<fee>.*)$";

    private static final String CONTEXT_KEY_CURRENCY = "currency";

    public AkfBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("akf bank");

        addTransactions();
    }

    @Override
    public String getLabel()
    {
        return "akf bank";
    }

    private void addTransactions()
    {
        final DocumentType type = new DocumentType("akf bank", //
                        documentContext -> documentContext //
                                        .section("currency") //
                                        .match("^Nr\\. Valuta \\/ Buchungstag Buchungstext (?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT);
        depositBlock.set(depositTransaction(type, DEPOSIT));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL);
        removalBlock.set(removalTransaction(type, REMOVAL, FEES));
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

    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex, String regexFee)
    {
        return new Transaction<AccountTransaction>().subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.REMOVAL);
            return entry;
        }).section("date", "amount", "fee").match(regex).match(regexFee).assign(assignmentsProvider(type))
                        .wrap(TransactionItem::new);
    }

    private BiConsumer<AccountTransaction, ParsedData> assignmentsProvider(DocumentType type)
    {
        return (transaction, matcherMap) -> {
            Map<String, String> context = type.getCurrentContext();

            transaction.setDateTime(asDate(matcherMap.get("date")));
            transaction.setAmount(asAmount(matcherMap.get("amount")));
            if ((matcherMap.get("fee") != null && matcherMap.get("fee").contains("Gebühren")))
            {
                transaction.setType(Type.FEES);
            }
            transaction.setCurrencyCode(context.get(AkfBankPDFExtractor.CONTEXT_KEY_CURRENCY));
        };
    }
}
