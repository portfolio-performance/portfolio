package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.time.LocalDateTime;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class Bank99PDFExtractor extends AbstractPDFExtractor
{
    public Bank99PDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("bank99 AG");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "bank99 AG";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("KONTOAUSZUG", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // Neuer Saldo zu Ihren Gunsten
                                        // MKrVEjU dWjvlpdOY EUR 40.854,96
                                        // @formatter:on
                                        .section("currency") //
                                        .find("Neuer Saldo zu Ihren Gunsten") //
                                        .match("^.* (?<currency>[A-Z]{3}) [\\.,\\d]+(\\-)?$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency"))))

                                        // @formatter:off
                                        // Handelsgericht Wien FN 76198g vom 31.03.2026
                                        // @formatter:on
                                        .section("month", "year") //
                                        .match("^Handelsgericht .* vom [\\d]{2}\\.(?<month>[\\d]{2})\\.(?<year>[\\d]{4})$") //
                                        .assign((ctx, v) -> {
                                            ctx.put("month", v.get("month"));
                                            ctx.put("year", v.get("year"));
                                        }));

        this.addDocumentTyp(type);

        // @formatter:off
        // 19.03 zRqdzxM lkcSiCYHv 19.03 38.852,86
        // IBAN: gI33 3931 7920 0382 3470
        // Auftraggeberreferenz: 382824934750XM10608172739392
        // BrY WzTuqKxLM
        // REF: 307814928358cz23349072053964
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\.[\\d]{2} (?!Realisat).* [\\d]{2}\\.[\\d]{2} [\\.,\\d]+$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "amount") //
                        .documentContext("currency", "month", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}) .* [\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            t.setDateTime(asBookingDate(v.get("date"), v.get("month"), v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // IBAN: gI33 3931 7920 0382 3470
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>IBAN: .*)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 09.04 IBAN: Wv63 5616 6408 8648 6313 09.04 1.000,00-
        // XhrnQzW DBTlvdywW
        // YjTeg nUSzRNM
        // REF: 01860095525Mwj48605232609669
        // @formatter:on
        var removalBlock = new Block("^[\\d]{2}\\.[\\d]{2} .* [\\d]{2}\\.[\\d]{2} [\\.,\\d]+\\-$");
        type.addBlock(removalBlock);
        removalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.REMOVAL))

                        .section("date", "amount") //
                        .documentContext("currency", "month", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}) .* [\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            t.setDateTime(asBookingDate(v.get("date"), v.get("month"), v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 09.04 IBAN: Wv63 5616 6408 8648 6313 09.04 1.000,00-
                        // @formatter:on
                        .section("note").optional() //
                        .match("^[\\d]{2}\\.[\\d]{2} (?<note>IBAN: .*) [\\d]{2}\\.[\\d]{2} [\\.,\\d]+\\-$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 19.06 Realisat 19.06 155,65
        // Habenzinsen                          206,94
        // Bonus                                  0,60
        // KESt                                  51,89-
        // @formatter:on
        var interestBlock = new Block("^[\\d]{2}\\.[\\d]{2} Realisat [\\d]{2}\\.[\\d]{2} [\\.,\\d]+$");
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "amount") //
                        .documentContext("currency", "month", "year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}) Realisat [\\d]{2}\\.[\\d]{2} (?<amount>[\\.,\\d]+)$") //
                        .match("^Habenzinsen[\\s]{1,}[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            t.setDateTime(asBookingDate(v.get("date"), v.get("month"), v.get("year")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // KESt                                  51,89-
                        // @formatter:on
                        .section("tax").optional() //
                        .documentContext("currency") //
                        .match("^KESt[\\s]{1,}(?<tax>[\\.,\\d]+)\\-$") //
                        .assign((t, v) -> {
                            var tax = Money.of(v.get("currency"), asAmount(v.get("tax")));
                            checkAndSetTax(tax, t, type.getCurrentContext());
                        })

                        .wrap(TransactionItem::new));
    }

    /**
     * The booking lines of the account statement only contain day and month.
     * The year is taken from the statement date. If the month of the booking
     * is after the month of the statement date, the booking belongs to the
     * previous year (statement across the turn of the year).
     *
     * @param dayMonth
     *            booking date in the format dd.MM
     * @param statementMonth
     *            month of the statement date
     * @param statementYear
     *            year of the statement date
     * @return the booking date including the year
     */
    private LocalDateTime asBookingDate(String dayMonth, String statementMonth, String statementYear)
    {
        var bookingMonth = Integer.parseInt(dayMonth.substring(dayMonth.indexOf('.') + 1));
        var year = Integer.parseInt(statementYear);

        if (bookingMonth > Integer.parseInt(statementMonth))
            year--;

        return asDate(dayMonth + "." + year);
    }
}
