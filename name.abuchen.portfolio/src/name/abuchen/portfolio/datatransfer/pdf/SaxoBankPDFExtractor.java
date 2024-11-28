package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SaxoBankPDFExtractor extends AbstractPDFExtractor
{
    public SaxoBankPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Saxo Bank");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Saxo Bank A/S";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Kontoauszugsbericht", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Währung : CHF
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^W.hrung : (?<currency>[\\w]{3}).*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 26-Nov-2024 26-Nov-2024 DEPOSIT (6980803089, 6083903733) 700,00 700,00
        // @formatter:on
        Block depositBlock = new Block("^[\\d]{2}\\-[\\w]+\\-[\\d]{4} [\\d]{2}\\-[\\w]+\\-[\\d]{4} (DEPOSIT) .* [\\.,\\d]+ [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\-[\\w]+\\-[\\d]{4} " //
                                        + "(?<date>[\\d]{2}\\-[\\w]+\\-[\\d]{4}) " //
                                        + "DEPOSIT .* "
                                        + "(?<amount>[\\.,\\d]+)"
                                        + "[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }
}
