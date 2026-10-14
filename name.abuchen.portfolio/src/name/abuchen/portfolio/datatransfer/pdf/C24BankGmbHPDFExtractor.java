package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.util.TextUtil.concatenate;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.LineSpan;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.SplittingStrategy;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class C24BankGmbHPDFExtractor extends AbstractPDFExtractor
{
    private static final String NOTE_HAS_NAME = "noteHasName";
    private static final String NOTE_HAS_PURPOSE = "noteHasPurpose";
    private static final String TAX_DEDUCTED_FROM_INTEREST = "taxDeductedFromInterest";

    // @formatter:off
    // 17.05. 17.05. Überweisung - 1.508,42 €
    // 29.05. 29.05. Lastschrift -2.469,00 €
    // 31.05. 31.05. Zinsen + 1,93 €
    // @formatter:on
    private static final Pattern BOOKING_LINE = Pattern
                    .compile("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. .* [\\-\\+][\\s]?[\\.,\\d]+ \\p{Sc}.*$");

    // @formatter:off
    // IBAN: DE00000000000000000000 / BIC: DEFFDEFFXXX
    // Zusammenfassung
    // 05/2026 Neue Mainzer Straße 14 - 18, 60311 Frankfurt am Main Seite 1 von 4
    // @formatter:on
    private static final Pattern BOOKING_END = Pattern
                    .compile("^(IBAN: .*|Zusammenfassung.*|[\\d]{2}\\/[\\d]{4} .* Seite [\\d]+ von [\\d]+.*)$");

    // @formatter:off
    // 05/2026 Neue Mainzer Straße 14 - 18, 60311 Frankfurt am Main Seite 1 von 4
    // @formatter:on
    private static final Pattern PAGE_FOOTER = Pattern.compile("^[\\d]{2}\\/[\\d]{4} .* Seite [\\d]+ von [\\d]+.*$");

    public C24BankGmbHPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("C24 Bank GmbH");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "C24 Bank GmbH";
    }


    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Kontoauszug [\\d]{2}\\/[\\d]{4}", (context, lines) -> {
            // @formatter:off
            // Kontoauszug 05/2024 Kontostand 0,00 €
            // Vorläufiger Kontoauszug 08/2024 Kontostand 0,00 €
            // @formatter:on
            var pYear = Pattern.compile("^(Vorl.ufiger )?Kontoauszug [\\d]{2}\\/(?<year>[\\d]{4}).*$");

            // @formatter:off
            // 31.05. 31.05. Zinsen + 1,93 €
            // 31.05. 31.05. Zinsen +1,93 €
            // @formatter:on
            var pInterest = Pattern.compile("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Zinsen \\+[\\s]?(?<amount>[\\.,\\d]+) \\p{Sc}.*$");

            // @formatter:off
            // 31.05. 31.05. Steuern - 2,29 €
            // 31.05. 31.05. Steuern -2,29 €
            // @formatter:on
            var pTax = Pattern.compile("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. Steuern \\-[\\s]?(?<amount>[\\.,\\d]+) \\p{Sc}.*$");

            var interestItems = new ArrayList<BookingItem>();
            var taxItems = new ArrayList<BookingItem>();

            for (var ii = 0; ii < lines.length; ii++)
            {
                var mYear = pYear.matcher(lines[ii]);
                if (!context.containsKey("year") && mYear.matches())
                    context.put("year", mYear.group("year"));

                var mInterest = pInterest.matcher(lines[ii]);
                if (mInterest.matches())
                    interestItems.add(new BookingItem(ii, mInterest.group("date"), asAmount(mInterest.group("amount"))));

                var mTax = pTax.matcher(lines[ii]);
                if (mTax.matches())
                    taxItems.add(new BookingItem(ii, mTax.group("date"), asAmount(mTax.group("amount"))));
            }

            // Each tax booking is paired with exactly one interest booking of
            // the same day, as long as the taxes do not exceed the interest.
            var helper = new InterestTaxHelper();

            for (var tax : taxItems)
            {
                for (var interest : interestItems)
                {
                    if (helper.taxByInterestLine.containsKey(interest.line))
                        continue;

                    if (interest.date.equals(tax.date) && tax.amount <= interest.amount)
                    {
                        helper.taxByInterestLine.put(interest.line, tax.amount);
                        helper.deductedTaxLines.add(tax.line);
                        break;
                    }
                }
            }

            context.putType(helper);
        });

        this.addDocumentTyp(type);

        // @formatter:off
        // 17.05. 17.05. Überweisung - 1.508,42 €
        // 17.05. 17.05. Überweisung + 1.115,22 €
        // 05.08. 05.08. Echtzeitüberweisung - 2.800,00 €
        // 31.01. 31.01. Lastschrift - 3.800,00 €
        // 29.05. 29.05. Lastschrift -2.469,00 €
        // 28.05. 28.05. Überweisung +7.522,77 €
        // 29.05. 29.05. Online-Kartenzahlung -91,27 €
        // 20.05. 20.05. Rückerstattung +20,99 €
        // @formatter:on
        var depositRemovalBlock = new Block(
                        createBookingSplittingStrategy(Pattern.compile("^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. " //
                        + "(?!(Zinsen|Steuern))" //
                        + ".* " //
                                        + "[\\-\\+][\\s]?[\\.,\\d]+ \\p{Sc}.*$")));
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?!(Zinsen|Steuern))" //
                                        + "(?<note>.*) " //
                                        + "(?<type>[\\-\\+])[\\s]?" //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(trim(v.get("note")));
                        })

                        // @formatter:off
                        // 28.05. 28.05. Überweisung +7.522,77 €
                        // HSS pDoZtnjv MGFv
                        // aIFH - tBncOq yUgelmgQUK 33/9487
                        // IBAN: iq92248166539838096252 / BIC: nqHImmh9wMC
                        //
                        // 05.05. 05.05. Überweisung +39,00 €
                        // QD ciqneEzK eYiG
                        // 53o33382334 Saq sxwqrMIdjLIAmbUCdI / qHBrysEZW jP 24.91.3008/ KxmDQAIJN:
                        // UkZVtc, cZUhJc
                        // IBAN: kr83003788587407363642 / BIC: RFPHIEug
                        // @formatter:on
                        .section("note").optional().multipleTimes() //
                        .match("^(?!([\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. |IBAN: ))(?<note>[^\\s].*)$") //
                        .assign((t, v) -> {
                            // The first line after the booking line contains
                            // the name, all following lines contain the
                            // purpose which may be wrapped over several lines
                            var txContext = v.getTransactionContext();

                            if (txContext.getBoolean(NOTE_HAS_PURPOSE))
                            {
                                t.setNote(concatenate(t.getNote(), trim(v.get("note")), " "));
                            }
                            else
                            {
                                t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | "));

                                if (txContext.getBoolean(NOTE_HAS_NAME))
                                    txContext.putBoolean(NOTE_HAS_PURPOSE, true);
                                else
                                    txContext.putBoolean(NOTE_HAS_NAME, true);
                            }
                        })

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.05. 31.05. Zinsen + 1,93 €
        // 31.05. 31.05. Zinsen +1,93 €
        // @formatter:on
        var interestBlock = new Block(createBookingSplittingStrategy(Pattern.compile(
                        "^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Zinsen [\\-\\+][\\s]?[\\.,\\d]+ \\p{Sc}.*$")));
        type.addBlock(interestBlock);
        interestBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.INTEREST))

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<note>Zinsen) " //
                                        + "(?<type>[\\-\\+])[\\s]?" //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "-" change from INTEREST to INTEREST_CHARGE
                            // @formatter:on
                            if ("-".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.INTEREST_CHARGE);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));

                            // The interest is credited gross; the taxes are
                            // debited in a separate line. Therefore the taxes
                            // are deducted from the credited amount. If the
                            // taxes exceed the interest, they are imported as
                            // a separate tax transaction.
                            if (t.getType() == AccountTransaction.Type.INTEREST)
                            {
                                var helper = type.getCurrentContext().getType(InterestTaxHelper.class);
                                var taxAmount = helper.map(h -> h.taxByInterestLine.get(v.getStartLineNumber()));

                                if (taxAmount.isPresent())
                                {
                                    var tax = Money.of(t.getCurrencyCode(), taxAmount.get());
                                    t.addUnit(new Unit(Unit.Type.TAX, tax));
                                    t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                                }
                            }
                        })

                        // @formatter:off
                        // 31.05. 31.05. Zinsen + 1,93 €
                        // C24 Bank
                        // 16.05.2024-30.05.2024
                        // @formatter:on
                        .section("note").optional() //
                        .match("^(?<note>[\\d]{2}\\.[\\d]{2}\\.[\\d]{4}[\\s]?\\-[\\s]?[\\d]{2}\\.[\\d]{2}\\.[\\d]{4})[\\s]*$") //
                        .assign((t, v) -> t.setNote(concatenate(t.getNote(), trim(v.get("note")), " | ")))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // 31.05. 31.05. Steuern - 2,29 €
        // 31.05. 31.05. Steuern -2,29 €
        // @formatter:on
        var taxesBlock = new Block(createBookingSplittingStrategy(Pattern.compile(
                        "^[\\d]{2}\\.[\\d]{2}\\. [\\d]{2}\\.[\\d]{2}\\. Steuern [\\-\\+][\\s]?[\\.,\\d]+ \\p{Sc}.*$")));
        type.addBlock(taxesBlock);
        taxesBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAXES))

                        .section("date", "note", "type", "amount", "currency") //
                        .documentContext("year") //
                        .match("^(?<date>[\\d]{2}\\.[\\d]{2}\\.) [\\d]{2}\\.[\\d]{2}\\. " //
                                        + "(?<note>Steuern) " //
                                        + "(?<type>[\\-\\+])[\\s]?" //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>\\p{Sc}).*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Is type --> "+" change from TAXES to TAX_REFUND
                            // @formatter:on
                            if ("+".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.TAX_REFUND);

                            t.setDateTime(asDate(v.get("date") + v.get("year")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(v.get("note"));

                            // The taxes are already deducted from the paired
                            // interest of the same day
                            var helper = type.getCurrentContext().getType(InterestTaxHelper.class);
                            if (t.getType() == AccountTransaction.Type.TAXES && helper.isPresent()
                                            && helper.get().deductedTaxLines.contains(v.getStartLineNumber()))
                                v.getTransactionContext().putBoolean(TAX_DEDUCTED_FROM_INTEREST, true);
                        })

                        .wrap((t, ctx) -> {
                            if (ctx.getBoolean(TAX_DEDUCTED_FROM_INTEREST))
                                return null;
                            return new TransactionItem(t);
                        }));
    }

    private record BookingItem(int line, String date, long amount)
    {
    }

    private static class InterestTaxHelper
    {
        private final Map<Integer, Long> taxByInterestLine = new HashMap<>();
        private final Set<Integer> deductedTaxLines = new HashSet<>();
    }

    /**
     * Creates a splitting strategy for the bookings of the account statement.
     * Each block starts with a booking line and ends before the next booking
     * line, the IBAN/BIC line, the summary or the page footer (including the
     * bank name printed above the footer). This way the lines of the name and
     * the purpose can be assigned to the correct booking.
     */
    private static SplittingStrategy createBookingSplittingStrategy(Pattern startsWith)
    {
        return lines -> {
            var spans = new ArrayList<LineSpan>();

            for (var ii = 0; ii < lines.length; ii++)
            {
                if (!startsWith.matcher(lines[ii]).matches())
                    continue;

                var endLine = lines.length - 1;

                for (var jj = ii + 1; jj < lines.length; jj++)
                {
                    if (isEndOfBooking(lines, jj))
                    {
                        endLine = jj - 1;
                        break;
                    }
                }

                spans.add(new LineSpan(ii, endLine));
            }

            return spans;
        };
    }

    private static boolean isEndOfBooking(String[] lines, int lineNo)
    {
        var line = lines[lineNo];

        if (BOOKING_LINE.matcher(line).matches() || BOOKING_END.matcher(line).matches())
            return true;

        if (line.isBlank())
            return false;

        // The line above the page footer contains the bank name
        for (var ii = lineNo + 1; ii < lines.length; ii++)
        {
            if (!lines[ii].isBlank())
                return PAGE_FOOTER.matcher(lines[ii]).matches();
        }

        return false;
    }
}
