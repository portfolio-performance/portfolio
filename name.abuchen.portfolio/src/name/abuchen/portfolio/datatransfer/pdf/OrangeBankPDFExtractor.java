package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class OrangeBankPDFExtractor extends AbstractPDFExtractor
{
    public OrangeBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Orange Bank");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Orange Bank";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // 04248 YeJSlmR Währung: EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.* W.hrung: (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 10.03.2024 Zinsbuchung 150,83 5.150,83
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Zinsbuchung [\\.,\\d]+ [\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) Zinsbuchung (?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 12.03.2024 Zinsauszahlung 150,83 5.000,00
        // 12.03.2024 Rückzahlung des 5.000,00 0,00
        // @formatter:on
        var transferBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Zinsauszahlung|R.ckzahlung des) [\\.,\\d]+ [\\.,\\d]+$");
        type.addBlock(transferBlock);
        transferBlock.set(new Transaction<AccountTransferEntry>()

                        .subject(() -> {
                            var accountTransferEntry = new AccountTransferEntry();
                            return accountTransferEntry;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (Zinsauszahlung|R.ckzahlung des) (?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new AccountTransferItem(t, true)));
    }
}
