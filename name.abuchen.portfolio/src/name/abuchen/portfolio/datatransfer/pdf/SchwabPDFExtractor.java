package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.LineSpan;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.SplittingStrategy;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Charles Schwab & Co., Inc. is a US-based financial services company.
 *           The currency is USD --> $.
 *
 * @implSpec All security currencies are USD --> $.
 *           The monthly account statement contains neither ISIN nor CUSIP.
 *           The securities are identified by ticker symbol and name.
 *
 * @implSpec Account statement:
 *           The date column contains only month and day (MM/DD), the year is
 *           taken from the statement period. Rows without a date belong to the
 *           date of the previous dated row.
 *
 * @implSpec Purchase transactions:
 *           Purchases with a value in the column "Charges/Interest" are not
 *           supported and are reported as failure.
 *
 * @implSpec Dividend transactions:
 *           The amount of dividends is reported in gross. ADR pass-through fees
 *           of the same security on the same date are booked as fees of the
 *           dividend.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class SchwabPDFExtractor extends AbstractPDFExtractor
{
    private static final String USD = "USD";

    public SchwabPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Charles Schwab & Co., Inc.");

        addAccountStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Charles Schwab & Co., Inc.";
    }

    private void addAccountStatementTransaction()
    {
        // @formatter:off
        // Account Number Statement Period
        // pKRvF ZcVDzXZ 6359-8029 April 1-30, 2026
        // @formatter:on
        var yearRange = new Block("^Account Number Statement Period$") //
                        .asRange(section -> section //
                                        .attributes("year") //
                                        .match("^Account Number Statement Period$") //
                                        .match("^.* [A-Z][a-z]+ [\\d]{1,2}\\-[\\d]{1,2}, (?<year>[\\d]{4})$"));

        // @formatter:off
        // 04/15 Purchase NEM NEWMONT CORP 5.0000 118.5620 (592.81)
        // Dividend Qual. Dividend NLCP NEWLAKE CAP PARTNERS INC 3.44
        // @formatter:on
        var dateRange = new Block("^[\\d]{2}\\/[\\d]{2} [A-Z][a-z]+ .*$") //
                        .asRange(section -> section //
                                        .attributes("date") //
                                        .match("^(?<date>[\\d]{2}\\/[\\d]{2}) .*$"));

        final var type = new DocumentType("Account Number Statement Period", yearRange, dateRange);
        this.addDocumentTyp(type);

        // @formatter:off
        // Formatting:
        // Date | Category | Action | Symbol/CUSIP | Description | Quantity | Price/Rate per Share($) | Charges/Interest($) | Amount($) | Realized Gain/(Loss)($)
        // -------------------------------------
        // 04/01 Purchase Reinvested Shares NVDA NVIDIA CORP 0.0002 175.9095 (0.04)
        // 04/15 Purchase NEM NEWMONT CORP 5.0000 118.5620 (592.81)
        // 04/22 Purchase SGOV ISHARES 0-3 MONTH TREASURY 100.0000 100.5850 (10,058.50)
        // BOND ETF
        // @formatter:on
        var buyBlock = new Block("^([\\d]{2}\\/[\\d]{2} )?Purchase (Reinvested Shares )?[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})? .* [\\.,\\d]+ [\\.,\\d]+ \\([\\.,\\d]+\\)$");
        type.addBlock(buyBlock);
        buyBlock.setMaxSize(1);
        buyBlock.set(new Transaction<BuySellEntry>()

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("tickerSymbol", "name", "shares", "price", "amount") //
                        .documentRange("date", "year") //
                        .match("^([\\d]{2}\\/[\\d]{2} )?Purchase (Reinvested Shares )?(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*) (?<shares>[\\.,\\d]+) (?<price>[\\.,\\d]+) \\((?<amount>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> {
                            var shares = ExtractorUtils.convertToNumberBigDecimal(v.get("shares"), Values.Share, "en", "US");
                            var price = ExtractorUtils.convertToNumberBigDecimal(v.get("price"), Values.Share, "en", "US");
                            var amount = ExtractorUtils.convertToNumberBigDecimal(v.get("amount"), Values.Amount, "en", "US");

                            // Problem: there is a "charges/interest" column
                            // where we have no example for yet. Such a row
                            // could shift the values into the wrong columns.
                            // Only rows that add up arithmetically are
                            // imported; the others are reported as failures.
                            var tolerance = new BigDecimal("0.01").add(price.add(shares).multiply(new BigDecimal("0.0001")));

                            if (shares.multiply(price).subtract(amount).abs().compareTo(tolerance) > 0)
                                v.markAsFailure(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            v.put("currency", asCurrencyCode(USD));

                            t.setDate(asDate(v.get("date") + "/" + v.get("year"), Locale.US));
                            t.setShares(asShares(v.get("shares")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .wrap(BuySellEntryItem::new));

        // A dividend block starts with the dividend row and ends before the
        // next dated row (date change), before the next dividend row of the
        // same security or at the end of the transaction details. Thereby the
        // block contains all ADR pass-through fees which belong to the
        // dividend.
        var dividendStart = Pattern.compile("^([\\d]{2}\\/[\\d]{2} )?Dividend (Qual\\. Dividend|Qual Div Reinvest) (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) .* [\\.,\\d]+$");
        var datedRow = Pattern.compile("^[\\d]{2}\\/[\\d]{2} .*$");
        var endOfTransactionDetails = Pattern.compile("^Total Transactions .*$");

        var splittingStrategy = (SplittingStrategy) lines -> {
            var spans = new ArrayList<LineSpan>();

            for (var ii = 0; ii < lines.length; ii++)
            {
                var matcher = dividendStart.matcher(lines[ii]);
                if (!matcher.matches())
                    continue;

                var tickerSymbol = matcher.group("tickerSymbol");
                var endLine = lines.length - 1;

                for (var jj = ii + 1; jj < lines.length; jj++)
                {
                    var nextDividend = dividendStart.matcher(lines[jj]);

                    if (datedRow.matcher(lines[jj]).matches() //
                                    || endOfTransactionDetails.matcher(lines[jj]).matches() //
                                    || (nextDividend.matches() && tickerSymbol.equals(nextDividend.group("tickerSymbol"))))
                    {
                        endLine = jj - 1;
                        break;
                    }
                }

                spans.add(new LineSpan(ii, endLine));
            }

            return spans;
        };

        var dividendBlock = new Block(splittingStrategy);
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // Formatting:
                        // Date | Category | Action | Symbol/CUSIP | Description | Quantity | Price/Rate per Share($) | Charges/Interest($) | Amount($) | Realized Gain/(Loss)($)
                        // -------------------------------------
                        // Dividend Qual Div Reinvest NVDA NVIDIA CORP 0.04
                        // Dividend Qual. Dividend NLCP NEWLAKE CAP PARTNERS INC 3.44
                        // 04/27 Dividend Qual. Dividend QABSY QANTAS AIRWAYS LTD 1.41
                        // FSPONSORED ADR 1 ADR
                        // REPS
                        // @formatter:on
                        .section("tickerSymbol", "name", "amount") //
                        .documentRange("date", "year") //
                        .match("^([\\d]{2}\\/[\\d]{2} )?Dividend (Qual\\. Dividend|Qual Div Reinvest) (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*) (?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            v.put("currency", asCurrencyCode(USD));
                            v.getTransactionContext().putString("tickerSymbol", v.get("tickerSymbol"));

                            t.setDateTime(asDate(v.get("date") + "/" + v.get("year"), Locale.US));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(v.get("currency"));
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        // @formatter:off
                        // Expense ADR Pass Thru QABSY QANTAS AIRWAYS LTD (0.10)
                        // Fee FSPONSORED ADR 1 ADR
                        // REPS
                        // @formatter:on
                        .section("feeTickerSymbol", "fee").multipleTimes().optional() //
                        .match("^Expense ADR Pass Thru (?<feeTickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) .* \\((?<fee>[\\.,\\d]+)\\)$") //
                        .assign((t, v) -> {
                            // Only fees of the same security belong to the dividend
                            if (v.get("feeTickerSymbol").equals(v.getTransactionContext().getString("tickerSymbol")))
                            {
                                var fee = Money.of(asCurrencyCode(USD), asAmount(v.get("fee")));

                                checkAndSetFee(fee, t, type.getCurrentContext());
                                t.setAmount(t.getAmount() - fee.getAmount());
                            }
                        })

                        .wrap(TransactionItem::new));
    }

    @Override
    protected long asAmount(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Amount, "en", "US");
    }

    @Override
    protected long asShares(String value)
    {
        return ExtractorUtils.convertToNumberLong(value, Values.Share, "en", "US");
    }
}
