package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.trim;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

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
                                        .section("startMonth", "startYear", "endYear") //
                                        .match("^f.r den Zeitraum von [\\d]{1,2}\\.(?<startMonth>[\\d]{1,2})\\.(?<startYear>[\\d]{4}) bis [\\d]{1,2}\\.[\\d]{1,2}\\.(?<endYear>[\\d]{4}).*$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("startMonth", v.get("startMonth"));
                                            ctx.put("startYear", v.get("startYear"));
                                            ctx.put("endYear", v.get("endYear"));
                                        })

                                        // @formatter:off
                                        // 31.8. Habenzinsen 31.8. 0,28
                                        // 31.8. Bonuszinsen 31.8. 70,99
                                        // @formatter:on
                                        .section("interest").optional().multipleTimes() //
                                        .match("^[\\d]{1,2}\\.[\\d]{1,2}\\. (?<interest>Habenzinsen|Bonuszinsen) [\\d]{1,2}\\.[\\d]{1,2}\\. [\\.,\\d]+$") //
                                        .assign((ctx, v) -> ctx.put("interestCount",
                                                        String.valueOf(asInt(ctx.get("interestCount")) + 1)))

                                        // @formatter:off
                                        // 31.8. Kapitalertragsteuer 31.8. -0,07
                                        // 31.8. Kapitalertragsteuer 31.8. -17,75
                                        // @formatter:on
                                        .section("tax").optional().multipleTimes() //
                                        .match("^[\\d]{1,2}\\.[\\d]{1,2}\\. (?<tax>Kapitalertragsteuer) [\\d]{1,2}\\.[\\d]{1,2}\\. \\-[\\.,\\d]+$") //
                                        .assign((ctx, v) -> ctx.put("taxCount",
                                                        String.valueOf(asInt(ctx.get("taxCount")) + 1))));

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
                        .documentContext("currency", "startMonth", "startYear", "endYear") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.) (?<note>(?!Habenzinsen|Bonuszinsen|Kapitalertragsteuer).*) [\\d]{1,2}\\.[\\d]{1,2}\\.(?<type>\\s(\\-)?)(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + getYearOfBooking(v.get("date"), v.get("startMonth"),
                                            v.get("startYear"), v.get("endYear"))));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.8. Habenzinsen 31.8. 0,28
        // 31.8. Bonuszinsen 31.8. 70,99
        // 31.8. Kapitalertragsteuer 31.8. -0,07
        // 31.8. Kapitalertragsteuer 31.8. -17,75
        // @formatter:on
        var interestBlock_Format01 = new Block("^[\\d]{1,2}\\.[\\d]{1,2}\\. Habenzinsen [\\d]{1,2}\\.[\\d]{1,2}\\. [\\.,\\d]+$");
        type.addBlock(interestBlock_Format01);
        interestBlock_Format01.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "note", "amount") //
                        .documentContext("currency", "startMonth", "startYear", "endYear") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.) (?<note>Habenzinsen) [\\d]{1,2}\\.[\\d]{1,2}\\. (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + getYearOfBooking(v.get("date"), v.get("startMonth"),
                                            v.get("startYear"), v.get("endYear"))));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        // The statement does not link a capital gains tax
                        // booking to the interest it belongs to, the bank only
                        // books them in the same order. They are therefore
                        // paired by position, but solely if the statement holds
                        // exactly one tax booking per interest booking. With
                        // any other combination no tax is assigned at all
                        // rather than assigning it to the wrong interest. The
                        // credit interest is booked first, so its tax is the
                        // first capital gains tax booking.
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .documentContextOptionally("interestCount", "taxCount") //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2}\\. Kapitalertragsteuer [\\d]{1,2}\\.[\\d]{1,2}\\. \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (asInt(v.get("interestCount")) != asInt(v.get("taxCount")))
                                return;

                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.8. Bonuszinsen 31.8. 70,99
        // 31.8. Kapitalertragsteuer 31.8. -0,07
        // 31.8. Kapitalertragsteuer 31.8. -17,75
        // @formatter:on
        var interestBlock_Format02 = new Block("^[\\d]{1,2}\\.[\\d]{1,2}\\. Bonuszinsen [\\d]{1,2}\\.[\\d]{1,2}\\. [\\.,\\d]+$");
        type.addBlock(interestBlock_Format02);
        interestBlock_Format02.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "note", "amount") //
                        .documentContext("currency", "startMonth", "startYear", "endYear") //
                        .match("^(?<date>[\\d]{1,2}\\.[\\d]{1,2}\\.) (?<note>Bonuszinsen) [\\d]{1,2}\\.[\\d]{1,2}\\. (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date") + getYearOfBooking(v.get("date"), v.get("startMonth"),
                                            v.get("startYear"), v.get("endYear"))));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        // Two interest bookings and two tax bookings: the first
                        // tax booking belongs to the credit interest, so the
                        // one of the bonus interest is the second.
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .documentContextOptionally("interestCount", "taxCount") //
                        .find("[\\d]{1,2}\\.[\\d]{1,2}\\. Kapitalertragsteuer [\\d]{1,2}\\.[\\d]{1,2}\\. \\-[\\.,\\d]+") //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2}\\. Kapitalertragsteuer [\\d]{1,2}\\.[\\d]{1,2}\\. \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (asInt(v.get("interestCount")) != asInt(v.get("taxCount")))
                                return;

                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        // The bonus interest is the only interest booking of
                        // the statement, hence the single tax booking belongs
                        // to it and the section above has found nothing.
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .documentContextOptionally("interestCount", "taxCount") //
                        .match("^[\\d]{1,2}\\.[\\d]{1,2}\\. Kapitalertragsteuer [\\d]{1,2}\\.[\\d]{1,2}\\. \\-(?<tax>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            if (asInt(v.get("interestCount")) != 1 || asInt(v.get("taxCount")) != 1)
                                return;

                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));

                            t.addUnit(new Unit(Unit.Type.TAX, tax));

                            t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                        })

                        .wrap(TransactionItem::new));
    }

    /**
     * The booking lines contain the day and the month only, the year has to be
     * taken from the period of the statement. If that period crosses a year
     * boundary, every booking from the first month of the period onwards
     * belongs to the first year and all others to the second one.
     */
    private String getYearOfBooking(String date, String startMonth, String startYear, String endYear)
    {
        if (startYear.equals(endYear))
            return endYear;

        var month = Integer.parseInt(date.split("\\.")[1]);

        return month >= Integer.parseInt(startMonth) ? startYear : endYear;
    }

    /**
     * Returns the counter held in the document context, zero if the counted
     * booking does not occur in the statement at all.
     */
    private int asInt(String value)
    {
        return value == null ? 0 : Integer.parseInt(value);
    }
}
