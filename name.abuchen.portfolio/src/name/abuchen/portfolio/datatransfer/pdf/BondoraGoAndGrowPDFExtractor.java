package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BondoraGoAndGrowPDFExtractor extends AbstractPDFExtractor
{
    static String ACCOUNT_STATEMENT_DOCUMENT_TYPE = "Zusammenfassung"; //$NON-NLS-1$
    static String ACCOUNT_STATEMENT_TRANSACTION_REGEX = "(?<date>\\d{2}.\\d{2}.\\d{4})\\s(?<kind>[\\D]+)\\s(?<sign>-?)(?<amount>(\\d+\\.)?\\d+,?\\d{0,2}).+(?<currency>[\\S]+)"; //$NON-NLS-1$
    static String BANK_IDENTIFIER = "Go & Grow";

    public BondoraGoAndGrowPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier(BANK_IDENTIFIER); // $NON-NLS-1$
        addAccountStatementTransaction();
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType(ACCOUNT_STATEMENT_DOCUMENT_TYPE);
        this.addDocumentTyp(type);

        Block block = new Block(ACCOUNT_STATEMENT_TRANSACTION_REGEX);
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.DEPOSIT);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction

                        .section("date", "kind", "amount") //
                        .match(ACCOUNT_STATEMENT_TRANSACTION_REGEX) //
                        .assign((t, v) -> {
                            String date = v.get("date");
                            t.setDateTime(asDate(date));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode("EUR");

                            String sign = v.get("sign");
                            if ("-".equals(sign))
                            {
                                t.setType(AccountTransaction.Type.REMOVAL);
                            }
                            String kind = v.get("kind");
                            if (kind != null)
                            {
                                switch (kind)
                                {
                                    case "Überweisen":
                                        t.setType(AccountTransaction.Type.DEPOSIT);
                                        break;
                                    case "Go & Grow Zinsen":
                                        t.setType(AccountTransaction.Type.INTEREST);
                                        break;
                                    default:
                                        break;
                                }
                            }
                        })

                        .wrap(t -> {
                            return new TransactionItem(t);
                        });
    }

    @Override
    public String getLabel()
    {
        return "Bondora"; //$NON-NLS-1$
    }
}
