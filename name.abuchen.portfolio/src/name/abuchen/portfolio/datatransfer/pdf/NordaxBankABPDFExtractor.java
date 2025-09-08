package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class NordaxBankABPDFExtractor extends AbstractPDFExtractor
{
    public NordaxBankABPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Nordax Bank AB");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Nordax Bank AB";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Account Statement", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // 11286 SBNIRZM Währung: EUR
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.* W.hrung: (?<currency>[\\w]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 24.08.2024 Zinsbuchung 403,39 10.403,39
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
        // 24.08.2024 Übertrag des verlängerten 403,39 10.000,00
        // 24.08.2024 Übertrag des verlängerten 10.000,00 0,00
        // @formatter:on
        var transferBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\.[\\d]{4} .bertrag des verl.ngerten [\\.,\\d]+ [\\.,\\d]+$");
        type.addBlock(transferBlock);
        transferBlock.set(new Transaction<AccountTransferEntry>()

                        .subject(() -> {
                            var accountTransferEntry = new AccountTransferEntry();
                            return accountTransferEntry;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .bertrag des verl.ngerten (?<amount>[\\.,\\d]+) [\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(t -> new AccountTransferItem(t, true)));
    }
}
