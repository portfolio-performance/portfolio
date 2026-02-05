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
        final var type = new DocumentType("[\\d]{2}\\. ACTIVITY DETAILS", //
                        documentContext -> documentContext //
                                        // @formatter:off
                                        // ¹Combined in CAD
                                        // @formatter:on
                                        .section("currency") //
                                        .match("^.Combined in (?<currency>[A-Z]{3})$") //
                                        .assign((ctx, v) -> ctx.put("currency", asCurrencyCode(v.get("currency")))));
        this.addDocumentTyp(type);

        var pdfTransaction = new Transaction<BuySellEntry>();

        var firstRelevantLine = new Block("^[\\d]{2}\\-[\\d]{2}\\-[\\d]{4} [\\d]{2}\\-[\\d]{2}\\-[\\d]{4} Buy \\..*$");
        type.addBlock(firstRelevantLine);
        firstRelevantLine.set(pdfTransaction);

        pdfTransaction //

                        .subject(() -> {
                            var portfolioTransaction = new BuySellEntry();
                            portfolioTransaction.setType(PortfolioTransaction.Type.BUY);
                            return portfolioTransaction;
                        })

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
                                                        .attributes( "tickerSymbol") //
                                                        .match("^.* Buy \\.(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) UNITS\\|WE ACTED AS AGENT\\|AVG PRICE - ASK.*$") //
                                                        .documentContext("currency") //
                                                        .assign((t, v) -> t.setSecurity(getOrCreateSecurity(v))),
                                        // @formatter:off
                                        // 04-10-2025 04-11-2025 Buy .VEQT VANGUARD ALL-EQUITY ETF  PORTFOLIO
                                        // 01-16-2023 01-18-2023 Buy .VEQT VANGUARD ALL-EQUITY ETF|PORTFOLIO ETF
                                        // 02-24-2023 02-28-2023 Buy .XEQT ISHARES CORE EQUITY ETF|PORTFOLIO 
                                        // @formatter:on
                                        section -> section //
                                                        .attributes( "tickerSymbol", "name") //
                                                        .match("^.* Buy \\.(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.+?)(?:\\|?[\\s]*PORTFOLIO.*|\\|PORTFOLIO ETF.*)$") //
                                                        .documentContext("currency") //
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
                                                        .documentContext("currency") //
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
                                                        .documentContext("currency") //
                                                        .match(".*WE ACTED AS AGENT [\\.,\\d]+ [\\.,\\d]+ \\([\\.,\\d]+\\) \\([\\.,\\d]+\\) \\((?<amount>[\\.,\\d]+)\\).*$") //
                                                        .assign((t, v) -> {
                                                            t.setCurrencyCode(v.get("currency"));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                        }))

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
                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DIVIDENDS);
                            return accountTransaction;
                        })

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

                        .subject(() -> {
                            var accountTransaction = new AccountTransaction();
                            accountTransaction.setType(AccountTransaction.Type.DEPOSIT);
                            return accountTransaction;
                        })

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
                        .documentContext("currency") //
                        .match("^.*WE ACTED AS AGENT [\\.,\\d]+ [\\.,\\d]+ \\([\\.,\\d]+\\) \\((?<fee>[\\.,\\d]+)\\).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
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
