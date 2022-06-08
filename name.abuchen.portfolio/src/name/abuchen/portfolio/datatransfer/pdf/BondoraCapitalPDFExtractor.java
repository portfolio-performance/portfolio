package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class BondoraCapitalPDFExtractor extends AbstractPDFExtractor
{
    public BondoraCapitalPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Zusammenfassung"); // $NON-NLS-1$
        addBankIdentifier("Summary"); // $NON-NLS-1$

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Bondora Capital"; //$NON-NLS-1$
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("(Zusammenfassung|Summary)");
        this.addDocumentTyp(type);

        Block block = new Block("^([\\d]{2}.[\\d]{2}.[\\d]{4}|[\\d]{4}.[\\d]{2}.[\\d]{2}) .*$");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction
                .section("date", "note", "amount")
                .match("^(?<date>([\\d]{2}.[\\d]{2}.[\\d]{4}|[\\d]{4}.[\\d]{2}.[\\d]{2})) "
                                + "(?<note>(.berweisen|"
                                + "Transfer|"
                                + "Abheben|"
                                + "Go & Grow Zinsen|"
                                + "Go & Grow returns|"
                                + "Withdrawal)) "
                                + "(\\p{Sc})?(\\W)?"
                                + "(?<amount>[\\.,'\\d\\s]+)"
                                + "(\\W)?(\\p{Sc})(\\W)?[\\.,'\\d\\s]+(\\W)?(\\p{Sc})?$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));
                    t.setNote(trim(v.get("note")));

                    // Switch transactions if ...
                    if (t.getNote() != null)
                    {
                        switch (t.getNote())
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
    protected long asAmount(String value)
    {
        value = value.trim().replaceAll("\\s", ""); //$NON-NLS-1$//$NON-NLS-2$

        String language = "de"; //$NON-NLS-1$
        String country = "DE"; //$NON-NLS-1$

        int apostrophe = value.indexOf("\'"); //$NON-NLS-1$
        if (apostrophe >= 0)
        {
            language = "de"; //$NON-NLS-1$
            country = "CH"; //$NON-NLS-1$
        }

        return PDFExtractorUtils.convertToNumberLong(value, Values.Amount, language, country);
    }
}
