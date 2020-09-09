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
    static String ACCOUNT_STATEMENT_TRANSACTION_REGEX = "^(?<date>\\d{2}.\\d{2}.\\d{4})\\s(?<kind>[^€\\d]*)(\\D[€]\\D|\\D)(?<amount>[\\d.]+(,\\d+)*)(\\D*)([\\d.]+(,\\d+)*)(.{2})?$"; //$NON-NLS-1$

    
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
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction

                        .section("date", "kind", "amount") //
                        .match(ACCOUNT_STATEMENT_TRANSACTION_REGEX) //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode("EUR");

                            String kind = v.get("kind");
                            if (kind != null)
                            {
                                switch (kind)
                                {
                                    case "Überweisen":
                                        t.setType(AccountTransaction.Type.DEPOSIT);
                                        break;
                                    case "Abheben":
                                        t.setType(AccountTransaction.Type.REMOVAL);
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
