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
        // 1234567891 0,2941% € 0,01
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

                        .section("note1", "note2", "note3", "currency", "amount") //
                        .documentContext("date") //
                        .match("^(?<note1>[\\d]+) (?<note3>\\p{Sc}) (?<note2>[\\.,\\d]+) (?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+) \\p{Sc} [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote("Konto: " + v.get("note1") + " (" + v.get("note2") + v.get("note3") + ")");
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 1234567891 0,2941% € 0,01
        // @formatter:on
        Block feesBlock02 = new Block("^[\\d]+ [\\.,\\d]+% \\p{Sc} [\\.,\\d]+$");
        type.addBlock(feesBlock02);
        feesBlock02.setMaxSize(1);
        feesBlock02.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.FEES);
                            return accountTransaction;
                        })

                        .section("note1", "note2", "currency", "amount") //
                        .documentContext("date") //
                        .match("^(?<note1>[\\d]+) (?<note2>[\\.,\\d]+%) (?<currency>\\p{Sc}) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote("Depot: " + v.get("note1") + " (" + v.get("note2") + ")");
                        })

                        .wrap(TransactionItem::new));
    }
}
