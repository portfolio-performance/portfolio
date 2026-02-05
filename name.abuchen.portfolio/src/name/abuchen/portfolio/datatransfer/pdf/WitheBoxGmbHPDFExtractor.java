package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class WitheBoxGmbHPDFExtractor extends AbstractPDFExtractor
{
    public WitheBoxGmbHPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Whitebox GmbH");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Whitebox GmbH";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Geb.hrenabrechnung f.r", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // 31. Oktober 2023
                                        // @formatter:on
                                        .section("date") //
                                        .match("^(?<date>[\\d]{1,2}\\. .* [\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("date", v.get("date"))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 1234567890 € 15.624,02 € 10,22 € 12,17
        // @formatter:on
        Block feesBlock01 = new Block("^[\\d]+ \\p{Sc} [\\.,\\d]+ \\p{Sc} [\\.,\\d]+ \\p{Sc} [\\.,\\d]+$");
        type.addBlock(feesBlock01);
        feesBlock01.setMaxSize(1);
        feesBlock01.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("currency", "amount") //
                        .documentContext("date") //
                        .match("^[\\d]+ \\p{Sc} [\\.,\\d]+ \\p{Sc} [\\.,\\d]+ (?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new));
    }
}
