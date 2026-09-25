package name.abuchen.portfolio.datatransfer.pdf;

import java.util.Locale;

import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Questrade Financial Group is a Canadian online brokerage firm
 *
 * @implSpec The date is given in US format MM-DD-YYYY.
 *           Currently, all transactions are processed in CAD.
 *
 *           However, the broker reflects the exchange rate.
 *           Current month FX rate: $1.00 USD = $1.4539 CAD Previous month FX rate: $1.00 USD = $1.4383 CAD Account #: 62639842 Current month: January 31, 2025
 *
 *           Trade confirmation reports use the date format DD-MM-YY and
 *           the currency of the respective account (CAD or USD).
 * @formatter:on
 */

@SuppressWarnings("nls")
public class QuestradeGroupPDFExtractor extends AbstractPDFExtractor
{
    public QuestradeGroupPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Questrade, Inc.");

        addBuyTransaction();
        addBuySellTransaction();
        addDividendTransaction();
        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Questrade Financial Group";
    }

    private void addBuyTransaction()
    {
        var sectionRange = new Block("^[\\d]{2}\\. ACTIVITY DETAILS$", "^Closing balance \\-.*$") //
                        .asRange(section -> section //
                                        // @formatter:off
                                        // Combined in Combined in ¹Combined in CAD
                                        // @formatter:on
                                        .attributes("currency") //
                                        .match("^.* .Combined in (?<currency>[A-Z]{3})$"));

        final var type = new DocumentType("[\\d]{2}\\. ACTIVITY DETAILS", sectionRange);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Buy \\..*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // 04-10-2025 04-11-2025 Buy .VEQT VANGUARD ALL-EQUITY ETF  PORTFOLIO
                        // 01-16-2023 01-18-2023 Buy .VEQT VANGUARD ALL-EQUITY ETF|PORTFOLIO ETF
                        // 01-17-2023 01-19-2023 Buy .XEQT UNITS|WE ACTED AS AGENT|AVG PRICE - ASK 19 25.320 (481.08) - (481.08) - - - -
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Buy \\..*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("date"), Locale.US)))

                        .oneOf( //
                                        // @formatter:off
                                        // 01-17-2023 01-19-2023 Buy .XEQT UNITS|WE ACTED AS AGENT|AVG PRICE - ASK 19 25.320 (481.08) - (481.08) - - - -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol") //
                                                        .match("^.* Buy \\.(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) UNITS\\|WE ACTED AS AGENT\\|AVG PRICE - ASK.*$") //
                                                        .documentRange("currency") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 04-10-2025 04-11-2025 Buy .VEQT VANGUARD ALL-EQUITY ETF  PORTFOLIO
                                        // 01-16-2023 01-18-2023 Buy .VEQT VANGUARD ALL-EQUITY ETF|PORTFOLIO ETF
                                        // 02-24-2023 02-28-2023 Buy .XEQT ISHARES CORE EQUITY ETF|PORTFOLIO
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "name") //
                                                        .match("^.* Buy \\.(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.+?)(?:\\|?[\\s]*PORTFOLIO.*|\\|PORTFOLIO ETF.*)$") //
                                                        .documentRange("currency") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))))

                        // @formatter:off
                        // ETF UNIT  WE ACTED AS AGENT 50.0000 40.930 (2,046.50) - (2,046.50) - - - -
                        // UNIT|WE ACTED AS AGENT 50.0000 40.930 (2,046.50) (0.10) (2,046.60) - - - -
                        // UNITS|WE ACTED AS AGENT|AVG PRICE - ASK 19 25.320 (481.08) - (481.08) - - - -
                        // @formatter:on
                        .section("shares") //
                        .match("^.*WE ACTED AS AGENT(?:\\|AVG PRICE \\- ASK)? (?<shares>[\\.,\\d]+).*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        .oneOf( //
                                        // @formatter:off
                                        // ETF UNIT  WE ACTED AS AGENT 50.0000 40.930 (2,046.50) - (2,046.50) - - - -
                                        // UNITS|WE ACTED AS AGENT|AVG PRICE - ASK 19 25.320 (481.08) - (481.08) - - - -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentRange("currency") //
                                                        .match("^.*WE ACTED AS AGENT(?:\\|AVG PRICE \\- ASK)? [\\.,\\d]+ [\\.,\\d]+ \\([\\.,\\d]+\\) \\- \\((?<amount>[\\.,\\d]+)\\).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }),
                                        // @formatter:off
                                        // UNIT|WE ACTED AS AGENT 50.0000 40.930 (2,046.50) (0.10) (2,046.60) - - - -
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("amount") //
                                                        .documentRange("currency") //
                                                        .match(".*WE ACTED AS AGENT [\\.,\\d]+ [\\.,\\d]+ \\([\\.,\\d]+\\) \\([\\.,\\d]+\\) \\((?<amount>[\\.,\\d]+)\\).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addBuySellTransaction()
    {
        var sectionRange = new Block("^TRADE CONFIRMATION REPORT$", "^Report generated on .*$") //
                        .asRange(section -> section //
                                        // @formatter:off
                                        // date (USD) Comm (USD) (USD) amount Net amount (USD)
                                        // date (CAD) amount Net amount (CAD)
                                        // @formatter:on
                                        .attributes("currency") //
                                        .optional() //
                                        .match("^.*Net amount \\((?<currency>[A-Z]{3})\\).*$"));

        final var type = new DocumentType("TRADE CONFIRMATION REPORT", sectionRange);
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (Buy|Sell) .*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // Is type --> "Sell" change from BUY to SELL
                        // @formatter:off
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // 06-05-26 07-05-26 A3DB70 Sell 160 .CASH  A T 50.02 8,003.20 0.00 0.00 0.00 8,003.20
                        // @formatter:on
                        .section("type") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (?<type>(Buy|Sell)) .*$") //
                        .assign((t, v) -> {
                            if ("Sell".equals(v.get("type")))
                                t.setType(PortfolioTransaction.Type.SELL);
                        })

                        // @formatter:off
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // Description ISHARES TRUST, ISHARES MSCI INTL VALUE FACTOR, ETF
                        //
                        // 06-05-26 07-05-26 A3DB70 Sell 160 .CASH  A T 50.02 8,003.20 0.00 0.00 0.00 8,003.20
                        // Description GLOBAL X HIGH INT SVGS ETF, CL A UNIT, AVG PRICE - ASK US FOR DETAILS
                        // @formatter:on
                        .section("tickerSymbol", "name") //
                        .documentRange("currency") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (Buy|Sell) [\\.,\\d]+[\\s]+(\\.)?(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)[\\s]+.*$") //
                        .match("^Description (?<name>.*?)[\\s]*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // Formatting: DD-MM-YY
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // 06-05-26 07-05-26 A3DB70 Sell 160 .CASH  A T 50.02 8,003.20 0.00 0.00 0.00 8,003.20
                        // @formatter:on
                        .section("day", "month", "year") //
                        .match("^(?<day>[\\d]{2})\\-(?<month>[\\d]{2})\\-(?<year>[\\d]{2}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (Buy|Sell) .*$") //
                        .assign((t, v) -> t.setDate(asDate(v.get("day") + "." + v.get("month") + "." + v.get("year"))))

                        // @formatter:off
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // 06-05-26 07-05-26 A3DB70 Sell 160 .CASH  A T 50.02 8,003.20 0.00 0.00 0.00 8,003.20
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (Buy|Sell) (?<shares>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // 06-05-26 07-05-26 A3DB70 Sell 160 .CASH  A T 50.02 8,003.20 0.00 0.00 0.00 8,003.20
                        // @formatter:on
                        .section("amount") //
                        .documentRange("currency") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (Buy|Sell) .* \\(?(?<amount>[\\.,\\d]+)\\)?[\\s]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // 06-05-26 07-05-26 A3DB70 Sell 160 .CASH  A T 50.02 8,003.20 0.00 0.00 0.00 8,003.20
                        // @formatter:on
                        .section("note") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} (?<note>[A-Z0-9]+) (Buy|Sell) .*$") //
                        .assign((t, v) -> t.setNote("Trade #: " + v.get("note")))

                        .wrap(BuySellEntryItem::new);

        addFeesSectionsTransaction(pdfTransaction, type);
    }

    private void addDividendTransaction()
    {
        final var type = new DocumentType("[\\d]{2}\\. ACTIVITY DETAILS", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // ¹Combined in CAD
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.Combined in (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<AccountTransaction>();

        var firstRelevantLine = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4}[\\s]*\\.[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? UNITS?([\\s]|\\|)DIST.*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //
                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // 01-07-2025 01-07-2025    .VEQT UNIT DIST      ON      29 SHS REC 12/30/24 PAY - - - - 20.69 - - - -
                        // 09-29-2023 09-29-2023    .XEQT UNITS DIST      ON     95 SHS REC 09/26/23 - - - - 23.55 - - - -
                        // 03-31-2023 03-31-2023    .XEQT UNITS|DIST      ON     19 SHS|REC 03/23/23 - - - - 1.67 - - - -
                        // @formatter:on
                        .section("tickerSymbol") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4}[\\s]*\\.(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) UNITS?([\\s]|\\|)DIST.*$") //
                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v)))

                        // @formatter:off
                        // 01-07-2025 01-07-2025    .VEQT UNIT DIST      ON      29 SHS REC 12/30/24 PAY - - - - 20.69 - - - -
                        // 09-29-2023 09-29-2023    .XEQT UNITS DIST      ON     95 SHS REC 09/26/23 - - - - 23.55 - - - -
                        // 03-31-2023 03-31-2023    .XEQT UNITS|DIST      ON     19 SHS|REC 03/23/23 - - - - 1.67 - - - -
                        // @formatter:on
                        .section("date") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{4}[\\s]*\\.(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) UNITS?([\\s]|\\|)DIST.*$") //
                        .assign((t, v) -> t.setDateTime(asDate(v.get("date"), Locale.US)))

                        // @formatter:off
                        // 01-07-2025 01-07-2025    .VEQT UNIT DIST      ON      29 SHS REC 12/30/24 PAY - - - - 20.69 - - - -
                        // 09-29-2023 09-29-2023    .XEQT UNITS DIST      ON     95 SHS REC 09/26/23 - - - - 23.55 - - - -
                        // 03-31-2023 03-31-2023    .XEQT UNITS|DIST      ON     19 SHS|REC 03/23/23 - - - - 1.67 - - - -
                        // @formatter:on
                        .section("shares") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4}[\\s]*\\.[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? UNITS?([\\s]|\\|)DIST.* (?<shares>[\\.,\\d]+) SHS([\\s]|\\|)REC [\\d]{2}\\/[\\d]{2}\\/[\\d]{2}.*$") //
                        .assign((t, v) -> t.setShares(asShares(v.get("shares"))))

                        // @formatter:off
                        // 01-07-2025 01-07-2025    .VEQT UNIT DIST      ON      29 SHS REC 12/30/24 PAY - - - - 20.69 - - - -
                        // 09-29-2023 09-29-2023    .XEQT UNITS DIST      ON     95 SHS REC 09/26/23 - - - - 23.55 - - - -
                        // 03-31-2023 03-31-2023    .XEQT UNITS|DIST      ON     19 SHS|REC 03/23/23 - - - - 1.67 - - - -
                        // @formatter:on
                        .section("amount") //
                        .documentContext("currency") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4}[\\s]*\\.[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? UNITS?([\\s]|\\|)DIST.* [\\.,\\d]+ SHS([\\s]|\\|)REC [\\d]{2}\\/[\\d]{2}\\/[\\d]{2}( PAY)? [\\s\\-]* (?<amount>[\\.,\\d]+) [\\s\\-]*$") //
                        .assign((t, v) -> {
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        // @formatter:off
                        // 01-07-2025 01-07-2025    .VEQT UNIT DIST      ON      29 SHS REC 12/30/24 PAY - - - - 20.69 - - - -
                        // 09-29-2023 09-29-2023    .XEQT UNITS DIST      ON     95 SHS REC 09/26/23 - - - - 23.55 - - - -
                        // 03-31-2023 03-31-2023    .XEQT UNITS|DIST      ON     19 SHS|REC 03/23/23 - - - - 1.67 - - - -
                        // @formatter:on
                        .section("note") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4}[\\s]*\\.[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? UNITS?([\\s]|\\|)DIST.* [\\.,\\d]+ SHS([\\s]|\\|)(?<note>REC [\\d]{2}\\/[\\d]{2}\\/[\\d]{2})( PAY)? .*$") //
                        .assign((t, v) -> t.setNote(v.get("note")))

                        .wrap(TransactionItem::new);
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("[\\d]{2}\\. ACTIVITY DETAILS", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // ¹Combined in CAD
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.Combined in (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        // @formatter:off
        // 04-09-2025 04-09-2025 Contribution CONT 6263984218 - - - - 10,000.00 - - - -
        // @formatter:on
        var depositBlock = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Contribution .*$");
        type.addBlock(depositBlock);
        depositBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "amount") //
                        .documentContext("currency") //
                        .match("^(?<date>[\\d]{2}\\-[\\d]{2}\\-[\\d]{4}) [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Contribution .* (?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date"), Locale.US));
                            t.setCurrencyCode(v.get("currency"));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new));
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // UNIT|WE ACTED AS AGENT 50.0000 40.930 (2,046.50) (0.10) (2,046.60) - - - -
                        // UNIT|WE ACTED AS AGENT 29 33.600 (974.40) (0.10) (974.50) - - - -
                        // @formatter:on
                        .section("fee").optional() //
                        .documentRange("currency") //
                        .match("^.*WE ACTED AS AGENT [\\.,\\d]+ [\\.,\\d]+ \\([\\.,\\d]+\\) \\((?<fee>[\\.,\\d]+)\\).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // Commission
                        // @formatter:off
                        // Formatting:
                        // Trade date | Settlement date | Trade # | Action | Quantity | Symbol | TB | EX | Price | Gross amount | Comm | SEC fees | Interest amount | Net amount
                        // -------------------------------------
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // @formatter:on
                        .section("fee").optional() //
                        .documentRange("currency") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (Buy|Sell) .* \\(?(?<fee>[\\.,\\d]+)\\)? \\(?[\\.,\\d]+\\)? \\(?[\\.,\\d]+\\)? \\(?[\\.,\\d]+\\)?[\\s]*$") //
                        .assign((t, v) -> {
                            if (asAmount(v.get("fee")) != 0)
                                processFeeEntries(t, v, type);
                        })

                        // SEC fees
                        // @formatter:off
                        // Formatting:
                        // Trade date | Settlement date | Trade # | Action | Quantity | Symbol | TB | EX | Price | Gross amount | Comm | SEC fees | Interest amount | Net amount
                        // -------------------------------------
                        // 15-07-25 16-07-25 30F9C7 Buy 96 IVLU A NY 32.71 (3,140.14) 0.00 0.00 0.00 (3,140.14)
                        // @formatter:on
                        .section("fee").optional() //
                        .documentRange("currency") //
                        .match("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [\\d]{2}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]+ (Buy|Sell) .* \\(?(?<fee>[\\.,\\d]+)\\)? \\(?[\\.,\\d]+\\)? \\(?[\\.,\\d]+\\)?[\\s]*$") //
                        .assign((t, v) -> {
                            if (asAmount(v.get("fee")) != 0)
                                processFeeEntries(t, v, type);
                        });
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
}
