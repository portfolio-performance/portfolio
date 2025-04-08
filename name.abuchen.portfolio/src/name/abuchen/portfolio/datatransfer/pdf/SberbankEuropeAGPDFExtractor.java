package name.abuchen.portfolio.datatransfer.pdf;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class SberbankEuropeAGPDFExtractor extends AbstractPDFExtractor
{
    public SberbankEuropeAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Sberbank Europe AG");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Sberbank Europe AG";
    }

    private void addAccountStatementTransaction()
    {
        final DocumentType type = new DocumentType("Tagesgeld", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // EUR-Konto Kontonummer 0123456789
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[\\w]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Postfach 6 20, 45956 Gladbeck Kontoauszug Nr.  1/2021
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.* Kontoauszug Nr\\.[\\s]{1,}[\\d]+\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 01.07. 01.07. Überweisungsgutschr. 123.456,78 H
        // @formatter:on
        Block depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(.berweisungsgutschr\\.)" //
                        + " .* [S|H]$");
        type.addBlock(depositRemovalBlock);

        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> {
                            AccountTransaction transaction = new AccountTransaction();
                            transaction.setType(AccountTransaction.Type.DEPOSIT);
                            return transaction;
                        })

                        .section("date", "note", "amount", "sign") //
                        .documentContext("currency", "year") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.) " //
                                        + "(?<note>.berweisungsgutschr\\.) " //
                                        + "(?<amount>[\\.,\\d]+) " //
                                        + "(?<sign>[S|H])$")
                        .assign((t, v) -> {
                            // Is sign --> "S" change from DEPOSIT to REMOVAL
                            if ("S".equals(v.get("sign")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            // create a long date from the year in the context
                            t.setDateTime(asDate(v.get("date") + v.get("year")));

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if ("Überweisungsgutschr.".equals(v.get("note")))
                                v.put("note", "Überweisungsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }
}
