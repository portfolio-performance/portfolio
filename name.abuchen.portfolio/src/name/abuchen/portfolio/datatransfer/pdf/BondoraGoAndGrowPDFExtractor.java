package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class BondoraGoAndGrowPDFExtractor extends AbstractPDFExtractor
{
    static String ACCOUNT_STATEMENT_DOCUMENT_TYPE = "(Zusammenfassung|Summary)"; //$NON-NLS-1$
    static String ACCOUNT_STATEMENT_TRANSACTION_REGEX = "^(?<date>\\d{2}.\\d{2}.\\d{4}|\\d{4}.\\d{2}.\\d{2})\\s(?<kind>[^€\\d]*)(\\s[€]\\D|\\D)(?<amount>[\\d\\s.']+(,\\d+)?)(\\D*)([\\d\\s.']+(,\\d+)*)(.{2})?$"; //$NON-NLS-1$

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
                            t.setAmount(asAmount(v.get("amount").replace(' ', '.')));
                            t.setCurrencyCode("EUR");

                            String kind = v.get("kind");
                            // remove space at the end. could be done using kind.stripTrailing() as well
                            if (kind.endsWith(" "))
                                kind = kind.substring(0, kind.length() - 1);
                            if (kind != null)
                            {
                                switch (kind)
                                {
                                    case "Überweisen":
                                    case "Transfer":
                                        t.setType(AccountTransaction.Type.DEPOSIT);
                                        break;
                                    case "Abheben":
                                    case "Withdrawal":
                                        t.setType(AccountTransaction.Type.REMOVAL);
                                        break;
                                    case "Go & Grow Zinsen":
                                    case "Go & Grow returns":
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
