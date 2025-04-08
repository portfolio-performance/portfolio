package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class CrowdestorPDFExtractor extends AbstractPDFExtractor
{
    public CrowdestorPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("FLEX STATEMENT");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Crowdestor OÜ";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("FLEX STATEMENT");
        this.addDocumentTyp(type);

        Block block = new Block("^[\\d]{2}.[\\d]{2}.[\\d]{4} .* (Deposit|Profit) .*$");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<>();
        pdfTransaction.subject(() -> {
            AccountTransaction entry = new AccountTransaction();
            entry.setType(AccountTransaction.Type.INTEREST);
            return entry;
        });

        pdfTransaction
                // 14.07.2022 CRF-6-4911 Deposit +50.00 50.00
                // 16.07.2022 CRF-6-4911 Profit +0.03 50.03
                .section("date", "type", "amount")
                .match("^(?<date>[\\d]{2}.[\\d]{2}.[\\d]{4}) .* (?<type>(Deposit|Profit)) \\+(?<amount>[\\.,\\d]+) [\\.,\\d]+$")
                .assign((t, v) -> {
                    t.setDateTime(asDate(v.get("date")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(CurrencyUnit.EUR));

                    // Switch transactions if ...
                    switch (v.get("type"))
                    {
                        case "Deposit":
                            t.setType(AccountTransaction.Type.DEPOSIT);
                            v.put("note", "Einzahlung");
                            break;
                        case "Profit":
                            t.setType(AccountTransaction.Type.INTEREST);
                            v.put("note", "Zinsen");
                            break;
                        default:
                            break;
                    }

                    t.setNote(v.get("note"));
                })

                .wrap(TransactionItem::new);

        block.set(pdfTransaction);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }
}
