package name.abuchen.portfolio.datatransfer.pdf;

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
    private static final String DEPOSIT = "^[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Gutschrift|SEPA Gutschrift( Bank)?)) (?<amount>[\\.,\\d]+)$";
    private static final String REMOVAL = "^[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (?<note>(Einzel)?(.*berweisung.*|Festgeld Anlage|Sparkonto K.ndigung)) \\-(?<amount>[\\.,\\d]+)$";
    private static final String REMOVAL_LINE_2 = "^(?<type>[^\\s]+).*$";
    private static final String INTEREST = "^[\\d]{2} (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\/ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Kontoabschluß (?<amount>[\\.,\\d]+)$";

    public AkfBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("akf bank GmbH & Co KG");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "akf bank GmbH & Co KG";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("akf bank", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Nr. Valuta / Buchungstag Buchungstext EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Nr\\. Valuta \\/ Buchungstag Buchungstext (?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        Block depositBlock = new Block(DEPOSIT);
        depositBlock.set(depositTransaction(type, DEPOSIT));
        type.addBlock(depositBlock);

        Block removalBlock = new Block(REMOVAL);
        removalBlock.set(removalTransaction(type, REMOVAL, REMOVAL_LINE_2));
        type.addBlock(removalBlock);

        Block interestBlock = new Block(INTEREST);
        interestBlock.set(interestTransaction(type, INTEREST));
        type.addBlock(interestBlock);
    }

    private Transaction<AccountTransaction> depositTransaction(DocumentType type, String regex)
    {
        return new Transaction<AccountTransaction>() //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note") //
                        .documentContext("currency") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private Transaction<AccountTransaction> removalTransaction(DocumentType type, String regex, String regexLine2)
    {
        return new Transaction<AccountTransaction>() //

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.REMOVAL);
                            return accountTransaction;
                        })

                        .section("date", "amount", "note", "type") //
                        .documentContext("currency") //
                        .match(regex) //
                        .match(regexLine2) //
                        .assign((t, v) -> {
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
                        .documentContext("currency") //
                        .match(regex) //
                        .assign((t, v) -> {
                            assignmentsProvider(t, v);
                        })

                        .wrap(TransactionItem::new);
    }

    private void assignmentsProvider(AccountTransaction t, ParsedData v)
    {
        t.setDateTime(asDate(v.get("date")));
        t.setAmount(asAmount(v.get("amount")));
        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
        t.setNote(v.get("note"));
        if (v.get("type") != null && v.get("type").matches("Geb.hren"))
        {
            t.setType(Type.FEES);
        }
    }
}
