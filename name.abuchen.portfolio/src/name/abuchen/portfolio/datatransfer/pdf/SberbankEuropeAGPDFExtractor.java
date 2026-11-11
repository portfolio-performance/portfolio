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
        final var type = new DocumentType("Tagesgeld", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // EUR-Konto Kontonummer 0123456789
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^(?<currency>[A-Z]{3})\\-Konto Kontonummer.*$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Postfach 6 20, 45956 Gladbeck Kontoauszug Nr.  1/2021
                                        // @formatter:on
                                        .section("year") //
                                        .match("^.* Kontoauszug Nr\\.[\\s]+[\\d]+\\/(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> ctx.put("year", v.get("year")))

                                        // @formatter:off
                                        //  neuer Kontostand vom 30.07.2021 123.456,78 H
                                        // @formatter:on
                                        .section("referenceDate").optional() //
                                        .match("^.*neuer Kontostand vom (?<referenceDate>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}) .*$") //
                                        .assign((ctx, v) -> ctx.put("referenceDate", v.get("referenceDate"))));

        this.addDocumentTyp(type);

        // @formatter:off
        // 01.07. 01.07. Überweisungsgutschr. 123.456,78 H
        // @formatter:on
        var depositRemovalBlock = new Block("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(.berweisungsgutschr\\.)" //
                        + " .* [SH]$");
        type.addBlock(depositRemovalBlock);

        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "amount", "type") //
                        .documentContext("currency", "year") //
                        .documentContextOptionally("referenceDate") //
                        .match("^[\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<date>[\\d]{2}\\.[\\d]{2}\\.) " //
                                        + "(?<note>.berweisungsgutschr\\.) " //
                                        + "(?<amount>[\\.,\\d]+) " //
                                        + "(?<type>[SH])$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "S" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("S".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + getYear(v.get("date"), v.get("referenceDate"), v.get("year"))));

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));

                            // Formatting some notes
                            if (v.get("note").matches(".berweisungsgutschr\\."))
                                v.put("note", "Überweisungsgutschrift");

                            t.setNote(v.get("note"));
                        })

                        .wrap(TransactionItem::new));
    }

    /**
     * Returns the year of a booking date without year (dd.MM.). The year is
     * taken from the date of the new balance. If the booking month is after
     * the month of the new balance, the booking belongs to the previous year
     * (statement across the turn of the year). Without the date of the new
     * balance, the year of the statement number is used.
     */
    private String getYear(String date, String referenceDate, String year)
    {
        if (referenceDate == null)
            return year;

        var reference = asDate(referenceDate);
        var month = Integer.parseInt(date.substring(3, 5));

        if (month > reference.getMonthValue())
            return String.valueOf(reference.getYear() - 1);

        return String.valueOf(reference.getYear());
    }
}
