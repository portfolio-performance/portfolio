package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
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
        final var type = new DocumentType("FLEX STATEMENT", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Date Goal Type Amount (€) Balance (€)
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Date Goal Type Amount \\((?<currency>\\p{Sc})\\) Balance \\(\\p{Sc}\\).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{2}.[\\d]{2}.[\\d]{4} .* (Deposit|Profit) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.setMaxSize(1);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        // 14.07.2022 CRF-6-4911 Deposit +50.00 50.00
                        // 16.07.2022 CRF-6-4911 Profit +0.03 50.03
                        .section("date", "type", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}.[\\d]{2}.[\\d]{4}) .* (?<type>(Deposit|Profit)) \\+(?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                            // Switch transactions if ...
                            switch (v.get("type"))
                            {
                                case "Deposit":
                                    t.setType(AccountTransaction.Type.DEPOSIT);
                                    break;
                                case "Profit":
                                    t.setType(AccountTransaction.Type.INTEREST);
                                    break;
                                default:
                                    break;
                            }
                        })

                        .wrap(TransactionItem::new);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }
}
