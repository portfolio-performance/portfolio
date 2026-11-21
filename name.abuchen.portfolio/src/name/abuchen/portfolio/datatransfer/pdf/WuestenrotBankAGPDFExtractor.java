package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class WuestenrotBankAGPDFExtractor extends AbstractPDFExtractor
{
    public WuestenrotBankAGPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("WUSNATWW");
        addBankIdentifier("bankkonto@wuestenrot.at");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Wüstenrot Bank AG";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug [\\d]+\\/[\\d]{4}", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Alter Kontostand 0,00 €
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^Alter Kontostand [\\.,\\d]+ (?<currency>\\p{Sc})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // für den Zeitraum von 3.8.2026 bis 31.8.2026 Wüstenrot FLEX - das Online Sparkonto
                                        // @formatter:on
                                        .section("year") //
                                        .match("^f.r den Zeitraum von [\\d]{1,2}\\.[\\d]{1,2}\\.[\\d]{4} bis [\\d]{1,2}\\.[\\d]{1,2}\\.(?<year>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 5.8. Eingehende Echtzeitzahlung 4.8. 40.098,19
        // Auftraggeber XUNpZAt jBgstiwZl
        // Kontonummer nG983901627636499462
        //
        // 7.8. Überweisung 7.8. -1.000,00
        // Begünstigter udlMUfL ovMHPcFNq
        // Kontonummer Xr565365627668109828
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{1,2}\\.[\\d]{1,2}\\. (?!Habenzinsen|Bonuszinsen|Kapitalertragsteuer).* [\\d]{1,2}\\.[\\d]{1,2}\\. (\\-)?[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "type", "amount") //
                        .documentContext("currency", "year") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.) (?<note>(?!Habenzinsen|Bonuszinsen|Kapitalertragsteuer).*) [\\d]{1,2}\\.[\\d]{1,2}\\.(?<type>\\s(\\-)?)(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));
    }
}
