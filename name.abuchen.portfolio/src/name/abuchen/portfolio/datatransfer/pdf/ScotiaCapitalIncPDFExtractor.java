package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Scotia iTRADE is the order-execution only brokerage division of Scotia Capital Inc. (Canada).
 *
 * @implSpec The monthly investment account statement contains one section per account currency (CAD and USD).
 *           The currency is taken from the line "Portfolio Overview Account Currency: XXX" of each section.
 *
 *           The activity lines only contain month and day (e.g. "Mar. 14"). The year is taken from the
 *           statement period "For the Period: March 1 to 29, 2019". The month is reduced to its first
 *           three letters, because Canadian abbreviations like "June", "July" or "Sept." are not
 *           supported by the date formatter.
 *
 *           The statement does not show the commission of a trade. It is calculated as the difference
 *           between the debit amount and quantity times price.
 *
 *           Dividend reinvestments (DRIP) are shown as dividend, two internal "REINVEST" bookings
 *           (funds held and released) which cancel each other out, and a "PURCHASE" of the new shares.
 *           The "REINVEST" bookings are not imported.
 *
 *           Currency conversions ("CONVERT") are imported as removal in the source currency
 *           and as deposit in the target currency.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class ScotiaCapitalIncPDFExtractor extends AbstractPDFExtractor
{
    /**
     * The security name can be continued in the line following the activity
     * line. These lines do not belong to the security name.
     */
    private static final Pattern NO_NAME_CONTINUATION = Pattern.compile("^(" //
                    + "(## )?[A-Z][a-z]{2,3}\\.? [\\d]{1,2} .*" //
                    + "|(STP|UNSOLICITED|CASH DIV|DIST|REINV(EST)?|FUNDS HELD|DIVIDEND|REC|BROKERAGE|Page|Closing|SWSTM)\\b.*" //
                    + ")$");

    public ScotiaCapitalIncPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Scotia iTRADE");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Scotia iTRADE";
    }

    private void addAccountStatementTransaction()
    {
        var currencyRange = new Block("^Portfolio Overview Account Currency: [A-Z]{3}$") //
                        .asRange(section -> section //
                                        // @formatter:off
                                        // Portfolio Overview Account Currency: CAD
                                        // @formatter:on
                                        .attributes("currency") //
                                        .match("^Portfolio Overview Account Currency: (?<currency>[A-Z]{3})$"));

        var yearRange = new Block("^For the Period: .* [\\d]{4}$") //
                        .asRange(section -> section //
                                        // @formatter:off
                                        // For the Period: March 1 to 29, 2019
                                        // @formatter:on
                                        .attributes("year") //
                                        .match("^For the Period: .* (?<year>[\\d]{4})$"));

        final var type = new DocumentType("Monthly Activity", currencyRange, yearRange);
        this.addDocumentTyp(type);

        // @formatter:off
        // Mar. 14 BUY TWILIO INC 31 123.2999 -3,832.29
        // Mar. 20 PURCHASE CHORUS AVIATION INC 2 -13.90
        // @formatter:on
        var buyBlock = new Block("^(## )?[A-Z][a-z]{2,3}\\.? [\\d]{1,2} (BUY|PURCHASE) .* \\-[\\.,\\d]+$");
        type.addBlock(buyBlock);
        buyBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .oneOf( //
                                        // @formatter:off
                                        // Mar. 14 BUY TWILIO INC 31 123.2999 -3,832.29
                                        // CL A
                                        // STP PET
                                        // UNSOLICITED VIA INTERNET
                                        //
                                        // Mar. 19 BUY MARKEL CORP HOLDING CO 4 1019.0500 -4,086.19
                                        // STP PET
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("month", "day", "name", "name1", "shares", "price", "amount") //
                                                        .documentRange("currency", "year") //
                                                        .match("^(## )?(?<month>[A-Z][a-z]{2,3})\\.? (?<day>[\\d]{1,2}) BUY (?<name>.*) (?<shares>[\\.,\\d]+) (?<price>[\\.,\\d]+) \\-(?<amount>[\\.,\\d]+)$") //
                                                        .match("^(## )?(?<name1>.*)$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", buildName(v));

                                                            t.setDate(buildDate(v));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));

                                                            // The commission is not shown in the statement
                                                            var grossValue = asBigDecimal(v.get("price")) //
                                                                            .multiply(asBigDecimal(v.get("shares"))) //
                                                                            .setScale(2, RoundingMode.HALF_UP) //
                                                                            .multiply(BigDecimal.valueOf(Values.Amount.factor())) //
                                                                            .longValue();

                                                            var fee = Money.of(v.get("currency"), t.getPortfolioTransaction().getAmount() - grossValue);
                                                            if (fee.isPositive())
                                                                checkAndSetFee(fee, t, type.getCurrentContext());
                                                        }),
                                        // @formatter:off
                                        // Mar. 20 PURCHASE CHORUS AVIATION INC 2 -13.90
                                        // VTG & VARIABLE VTG SHS
                                        // REINVEST @6.9549 03/18/19
                                        //
                                        // Mar. 29 PURCHASE BROOKFIELD INFRASTRUCTURE 1 -55.29
                                        // PARTNERS L P UNITS
                                        // REINV @ 55.2942 C$ 03/29/19
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("month", "day", "name", "name1", "shares", "amount") //
                                                        .documentRange("currency", "year") //
                                                        .match("^(## )?(?<month>[A-Z][a-z]{2,3})\\.? (?<day>[\\d]{1,2}) PURCHASE (?<name>.*) (?<shares>[\\.,\\d]+) \\-(?<amount>[\\.,\\d]+)$") //
                                                        .match("^(## )?(?<name1>.*)$") //
                                                        .match("^REINV(EST)? @[\\s]?[\\.,\\d]+ .*$") //
                                                        .assign((t, v) -> {
                                                            v.put("name", buildName(v));

                                                            t.setDate(buildDate(v));
                                                            t.setSecurity(getOrCreateSecurity(v));
                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .wrap(BuySellEntryItem::new));

        // @formatter:off
        // Mar. 14 RICHARDS PACKAGING INCOME 0.1100 16.50
        // Mar. 18 DIVIDEND CHORUS AVIATION INC 0.0400 15.20
        // @formatter:on
        var dividendBlock = new Block("^(## )?[A-Z][a-z]{2,3}\\.? [\\d]{1,2} (?!(BUY|SELL|PURCHASE|REINVEST|CONVERT) ).* [\\.\\d]+ [\\.,\\d]+$", //
                        "^REC [\\d]{2}\\/[\\d]{2}\\/[\\d]{2} PAY [\\d]{2}\\/[\\d]{2}\\/[\\d]{2}$");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Mar. 14 RICHARDS PACKAGING INCOME 0.1100 16.50
                        // FUND TRUST UTS
                        //
                        // Mar. 29 DIVIDEND UNION PACIFIC CORP 0.8800 44.00
                        // CASH DIV ON 50 SHS
                        // @formatter:on
                        .section("month", "day", "name", "name1", "amount") //
                        .documentRange("currency", "year") //
                        .match("^(## )?(?<month>[A-Z][a-z]{2,3})\\.? (?<day>[\\d]{1,2}) (DIVIDEND )?(?<name>.*) [\\.\\d]+ (?<amount>[\\.,\\d]+)$") //
                        .match("^(## )?(?<name1>.*)$") //
                        .assign((t, v) -> {
                            v.put("name", buildName(v));

                            t.setDateTime(buildDate(v));
                            t.setSecurity(getOrCreateSecurity(v));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // DIST ON 150 SHS
                        // CASH DIV ON 380 SHS
                        // @formatter:on
                        .section("shares") //
                        .match("^(DIST|CASH DIV) ON (?<shares>[\\.,\\d]+) SHS$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .wrap(TransactionItem::new));

        // @formatter:off
        // Mar. 15 CONVERT conf#34Q7K Scotia Online -13,505.00
        // Mar. 15 CONVERT conf#34Q7K Scotia Online 10,000.00
        // @formatter:on
        var depositRemovalBlock = new Block("^(## )?[A-Z][a-z]{2,3}\\.? [\\d]{1,2} CONVERT .* (\\-)?[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("month", "day", "type", "amount") //
                        .documentRange("currency", "year") //
                        .match("^(## )?(?<month>[A-Z][a-z]{2,3})\\.? (?<day>[\\d]{1,2}) CONVERT .*(?<type>[\\s](\\-)?)(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            // Is type --> "-" change from DEPOSIT to REMOVAL
                            if ("-".equals(trim(v.get("type"))))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(buildDate(v));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // Mar. 15 CONVERT conf#34Q7K Scotia Online -13,505.00
                        // BROKERAGE C$ - U$ @ 1.3505
                        // @formatter:on
                        .section("note1", "note2").optional() //
                        .match("^(## )?[A-Z][a-z]{2,3}\\.? [\\d]{1,2} CONVERT (?<note1>conf#[\\w]+) .*$") //
                        .match("^BROKERAGE (?<note2>.* @ [\\.,\\d]+)$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note1")) + " | " + trim(v.get("note2"))))

                        .wrap(TransactionItem::new));
    }

    private String buildName(Map<String, String> v)
    {
        var name = trim(v.get("name"));
        var name1 = trim(v.get("name1"));

        if (name1.isEmpty() || NO_NAME_CONTINUATION.matcher(name1).matches())
            return name;

        return name + " " + name1;
    }

    private LocalDateTime buildDate(Map<String, String> v)
    {
        // @formatter:off
        // Formatting: Mar. 14 | June 14 | Sept. 14 --> Mar 14, 2019
        // @formatter:on
        return asDate(v.get("month").substring(0, 3) + " " + v.get("day") + ", " + v.get("year"), Locale.US);
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "CA");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "CA");
    }

    @Override
    protected BigDecimal asBigDecimal(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "CA");
    }
}
