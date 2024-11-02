package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class AudiBankPDFExtractor extends AbstractPDFExtractor
{
    public AudiBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Audi Bank");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Audi Bank";
    }


    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszug / Saldenmitteilung", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Alter Kontostand in EUR: 1.001,29
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Alter Kontostand in (?<currency>[\\w]{3}):.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 1 23.12.2021 Habenzinsen 25.12.2021 0,83
        // @formatter:on
        Block interestBlock = new Block("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Habenzinsen [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} [\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.INTEREST);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} Habenzinsen (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) (?<amount>[\\.,\\d]+)$")
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 2 23.12.2021 Solidaritätszuschlag 25.12.2021 -0,01
        // 3 23.12.2021 Kirchensteuer 25.12.2021 -0,01
        // 4 23.12.2021 Abgeltungsteuer 25.12.2021 -0,20
        // @formatter:on
        Block taxesBlock = new Block("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Solidarit.tszuschlag|Kirchensteuer|Abgeltungsteuer) [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} \\-[\\.,\\d]+$");
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.TAXES);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]+ [\\d]{2}\\.[\\d]{2}\\.[\\d]{4} (Solidarit.tszuschlag|Kirchensteuer|Abgeltungsteuer) (?<date>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) \\-(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                        })

                        .wrap(TransactionItem::new));
    }
}
