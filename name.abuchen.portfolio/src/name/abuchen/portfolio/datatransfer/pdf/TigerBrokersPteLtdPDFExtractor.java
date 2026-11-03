package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetFee;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ExtractorUtils;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implNote Tiger Brokers (Singapore) PTE.LTD. is a US-based financial services company.
 *           The currency is USD.
 *
 *           All security currencies are USD.
 *           When there is no trade in progress, the securities currency is not issued.
 *           We then set the securities currency to USD.
 *           @see Test file --> AccountStatement06.txt
 *
 *           Late 2024 Format
 *           ================
 *           Apparently, the format has changed in late 2024.
 *           https://forum.portfolio-performance.info/t/pdf-import-from-tiger-brokers/20484/37
 *           https://forum.portfolio-performance.info/t/pdf-import-from-tiger-brokers/20484/41
 *           The PDF contains a table with each cell containing a multi-line text with different line wrapping (2-3 lines) which is centered.
 *           We do not use the currency from the context (anymore), but parse it as part of each transaction.
 *           Examples only contain purchases and dividend payments. Therefore we do not yet know if other transaction types (tax refund, deposit) still work.
 *
 * @implSpec In case of purchase and sale, the amount is given in gross.
 *           To get the correct net amount, we need to add the fees.
 * @formatter:on
 */

@SuppressWarnings("nls")
public class TigerBrokersPteLtdPDFExtractor extends AbstractPDFExtractor
{
    private static final DateTimeFormatter DATEFORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter TIMEFORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public TigerBrokersPteLtdPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Tiger Brokers (Singapore) PTE.LTD.");

        addAccountStatementTransaction();
        addAccountStatementTransaction_late25();
    }

    @Override
    public String getLabel()
    {
        return "Tiger Brokers (Singapore) Pte. Ltd.";
    }

    private void addAccountStatementTransaction()
    {
        final var type = new DocumentType("Activity Statement", (context, lines) -> {
            var pCurrency = Pattern.compile("^.* (Base Currency :|Cash) (?<currency>[A-Z]{3})$");
            var pSecurity = Pattern.compile("^(?<tickerSymbol>(?!(GST|Net))[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*) [\\d]$");
            var pSecurityCurrency = Pattern.compile("^Stock Currency: (?<securityCurrency>[A-Z]{3})$");
            var pDividendTaxes = Pattern.compile("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) (?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) Cash Dividend .* \\-(?<tax>[\\.,\\d]+).*$");

            String securityCurrency = null;

            for (String line : lines)
            {
                var mCurrency = pCurrency.matcher(line);
                if (mCurrency.matches())
                    context.put("currency", mCurrency.group("currency"));

                var mSecurityCurrency = pSecurityCurrency.matcher(line);
                if (mSecurityCurrency.matches())
                    securityCurrency = mSecurityCurrency.group("securityCurrency");
            }

            // Create a helper to store the list of security items found in the document
            var securityListHelper = new SecurityListHelper();
            context.putType(securityListHelper);

            // Extract security information using pSecurity pattern and add
            // pSecurityCurrency pattern
            List<SecurityItem> securityItems = new ArrayList<>();

            for (String line : lines)
            {
                var mSecurity = pSecurity.matcher(line);
                if (mSecurity.matches())
                {
                    var securityItem = new SecurityItem();
                    securityItem.tickerSymbol = mSecurity.group("tickerSymbol");
                    securityItem.name = mSecurity.group("name");
                    securityItem.currency = (securityCurrency == null) ? "USD" : asCurrencyCode(securityCurrency);
                    securityItems.add(securityItem);
                    securityListHelper.items.add(securityItem);
                }
            }

            // Create a helper to store the list of dividend taxes items
            var dividendTaxesTransactionListHelper = new DividendTaxesTransactionListHelper();
            context.putType(dividendTaxesTransactionListHelper);

            // Extract dividend taxes using pDividendTaxes pattern
            List<DividendTaxesTransactionItem> dividendTaxesTransactionItems = new ArrayList<>();

            for (String line : lines)
            {
                var mDividendTaxes = pDividendTaxes.matcher(line);
                if (mDividendTaxes.matches())
                {
                    var dividendTaxesTransactionItem = new DividendTaxesTransactionItem();
                    dividendTaxesTransactionItem.tickerSymbol = mDividendTaxes.group("tickerSymbol");
                    dividendTaxesTransactionItem.date = LocalDate.parse(mDividendTaxes.group("date"), DATEFORMAT);
                    dividendTaxesTransactionItem.taxes = asAmount(mDividendTaxes.group("tax"));
                    dividendTaxesTransactionItems.add(dividendTaxesTransactionItem);
                    dividendTaxesTransactionListHelper.items.add(dividendTaxesTransactionItem);
                }
            }

            // @formatter:off
            // Collect the trade dates. The trade date is located somewhere between
            // the previous and the current trade data line and may be split into
            // two fragments. If no complete date is present, every pair of digit
            // fragments is combined in both directions until a valid date results.
            // @formatter:on
            var tradeDateListHelper = new TradeDateListHelper();
            context.putType(tradeDateListHelper);

            var pTradeData = Pattern.compile("^.*(SG|US) (SGX|NYSE|ARCA|NASDAQ) (Open|Close) "
                            + "\\-?[\\.,\\d]+ [\\.,\\d]+ \\-?[\\.,\\d]+ [\\.,\\d]+.*$");
            var pFullDate = Pattern.compile("^.*?(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}).*$");
            var pDateFragment = Pattern.compile("[\\d][\\d\\-]*");

            var previousTradeLine = 0;

            for (var ii = 0; ii < lines.length; ii++)
            {
                if (!pTradeData.matcher(lines[ii]).matches())
                    continue;

                String tradeDate = null;

                for (var jj = previousTradeLine; jj <= ii && tradeDate == null; jj++)
                {
                    var mFullDate = pFullDate.matcher(lines[jj]);
                    if (mFullDate.matches())
                        tradeDate = mFullDate.group("date");
                }

                if (tradeDate == null)
                    tradeDate = assembleDate(lines, previousTradeLine, ii, pDateFragment);

                if (tradeDate != null)
                {
                    var tradeDateItem = new TradeDateItem();
                    tradeDateItem.lineNo = ii;
                    tradeDateItem.date = tradeDate;
                    tradeDateItem.time = assembleTime(lines, previousTradeLine, ii);
                    tradeDateListHelper.items.add(tradeDateItem);
                }

                previousTradeLine = ii + 1;
            }
        });
        this.addDocumentTyp(type);

        var buyBlock_Format01 = new Transaction<BuySellEntry>();

        // @formatter:off
        // Settlement Fee: -0.14
        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
        // Platform Fee: -1.00
        // @formatter:on
        var firstRelevantLineForBuyBlock_Format01 = new Block("^Settlement Fee: \\-[\\.,\\d]+$", "^Platform Fee: \\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock_Format01);
        firstRelevantLineForBuyBlock_Format01.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format01.set(buyBlock_Format01);

        buyBlock_Format01 //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("tickerSymbol", "date", "time", "shares", "gross", "fee1", "fee2", "fee3") //
                        .find("Settlement Fee: \\-[\\.,\\d]+") //
                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) " //
                                        + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), " //
                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                        + "(?<gross>[\\.,\\d]+) " //
                                        + "Commission: \\-(?<fee1>[\\.,\\d]+) " //
                                        + "\\-(?<fee2>[\\.,\\d]+) " //
                                        + "(\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$") //
                        .match("^Platform Fee: \\-(?<fee3>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() == null)
                                return null;

                            if (t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        });

        addFeesSectionsTransaction(buyBlock_Format01, type);

        var buyBlock_Format02 = new Transaction<BuySellEntry>();

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        var firstRelevantLineForBuyBlock_Format02 = new Block("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format02);
        firstRelevantLineForBuyBlock_Format02.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format02.set(buyBlock_Format02);

        buyBlock_Format02 //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("tickerSymbol", "date", "time", "shares", "gross", "fee1", "fee2", "fee3").optional() //
                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) " //
                                        + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), " //
                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                        + "(?<gross>[\\.,\\d]+) " //
                                        + "Commission: \\-(?<fee1>[\\.,\\d]+)" //
                                        + "(\\s)?Platform Fee: \\-(?<fee2>[\\.,\\d]+) " //
                                        + "\\-(?<fee3>[\\.,\\d]+) " //
                                        + "(\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() == null)
                                return null;

                            if (t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        });

        addFeesSectionsTransaction(buyBlock_Format02, type);

        var buyBlock_Format03 = new Transaction<BuySellEntry>();

        // @formatter:off
        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
        // @formatter:on
        var firstRelevantLineForBuyBlock_Format03 = new Block("^[A-Z0-9]{2,4} [A-Z]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ .*$");
        type.addBlock(firstRelevantLineForBuyBlock_Format03);
        firstRelevantLineForBuyBlock_Format03.setMaxSize(1);
        firstRelevantLineForBuyBlock_Format03.set(buyBlock_Format03);

        buyBlock_Format03 //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("tickerSymbol", "shares", "gross", "fee1", "fee2", "date", "fee3", "time").optional() //
                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) [A-Z]+ " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                        + "(?<gross>[\\.,\\d]+) " //
                                        + "Commission: \\-(?<fee1>[\\.,\\d]+) " //
                                        + "\\-(?<fee2>[\\.,\\d]+) " //
                                        + "(\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ " //
                                        + "(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})" //
                                        + "(\\s)?Platform Fee: \\-(?<fee3>[\\.,\\d]+) " //
                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() == null)
                                return null;

                            if (t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        });

        addFeesSectionsTransaction(buyBlock_Format03, type);

        var buyBlock_Format04 = new Transaction<BuySellEntry>();

        var firstRelevantLineForBuyBlock_Format04 = new Block("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$");
        type.addBlock(firstRelevantLineForBuyBlock_Format04);
        firstRelevantLineForBuyBlock_Format04.setMaxSize(3);
        firstRelevantLineForBuyBlock_Format04.set(buyBlock_Format04);

        buyBlock_Format04 //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Commission: 2024-04-08
                                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                                        // -1.00 GMT+8 10
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "tickerSymbol", "shares", "gross", "fee1", "fee2", "time", "fee3") //
                                                        .match("^Commission: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) .* " //
                                                                        + "(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<gross>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "\\-(?<fee1>[\\.,\\d]+) .*Platform Fee: " //
                                                                        + "(\\-)?(?<fee2>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                                                        .match("^\\-(?<fee3>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            // The amount is stated in gross
                                                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                                                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                                        }),
                                        // @formatter:off
                                        // Commission: 2023-01-06
                                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                                        // -1.00 GMT+8
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("date", "tickerSymbol", "shares", "gross", "fee1", "fee2", "time", "fee3") //
                                                        .match("^Commission: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                                                        .match("^(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) .* " //
                                                                        + "(?<shares>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ [\\.,\\d]+ " //
                                                                        + "(?<gross>[\\.,\\d]+) " //
                                                                        + "\\-(?<fee1>[\\.,\\d]+)Platform Fee: " //
                                                                        + "(\\-)?(?<fee2>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ (\\-)[\\.,\\d]+ " //
                                                                        + "(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                                                        .match("^\\-(?<fee3>[\\.,\\d]+).*$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDate(asDate(v.get("date"), v.get("time")));
                                                            t.setShares(asShares(v.get("shares")));

                                                            // The amount is stated in gross
                                                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                                                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                                                        }))

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() == null)
                                return null;

                            if (t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        });

        addFeesSectionsTransaction(buyBlock_Format04, type);

        var buyBlock_Format05 = new Transaction<BuySellEntry>();

        // @formatter:off
        // Settlement Fee:
        // -0.01 2024-12-19
        // Vanguard Total World Stock ETF
        // US ARCA Open 3 118.29960 354.90 0.00 Commission: 2024-
        // -0.99 0.00 0.00 13:01:49, US USD
        // (VT) 12-20
        // Platform Fee: /Eastern
        // -1.00
        // @formatter:on
        var firstRelevantLineForBuyBlock_late24 = new Block("^Settlement Fee:\\s*$", "^\\-[\\.,\\d]+$");
        type.addBlock(firstRelevantLineForBuyBlock_late24);
        firstRelevantLineForBuyBlock_late24.setMaxSize(9);
        firstRelevantLineForBuyBlock_late24.set(buyBlock_Format05);

        buyBlock_Format05 //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))
                        .section("fee1", "date", "name", "shares", "gross", "fee2", "time", "tickerSymbol", "fee3") //
                        .find("Settlement Fee:[\\s]*") //
                        .match("^\\-(?<fee1>[\\.,\\d]+) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                        .match("^(?<name>.*)$") //
                        .match("^.*(?<shares>[\\.,\\d]+) [\\.,\\d]+ (?<gross>[\\.,\\d]+) [\\.,\\d]+ Commission: [\\d]{4}\\-$") //
                        .match("^\\-(?<fee2>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* [A-Z]{3}$") //
                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) [\\d]{2}\\-[\\d]{2}$") //
                        .find("Platform Fee:.*") //
                        .match("^\\-(?<fee3>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")) + asAmount(v.get("fee3")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() == null)
                                return null;

                            if (t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        });
        
        addFeesSectionsTransaction(buyBlock_Format05, type);

        var buyBlock_Format06 = new Transaction<BuySellEntry>();

        // @formatter:off
        // Vanguard Total World Stock Commission: 2025-04-08
        // ETF US ARCA Open 1 106.43000 106.43 0.00 -0.99 2025-
        // Platform Fee: 0.00 0.00 11:28:50, US USD
        // 04-09
        // (VT) -1.00 /Eastern
        // @formatter:on
        var firstRelevantLineForBuyBlock_Format06 = new Block("^.* Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$");
        type.addBlock(firstRelevantLineForBuyBlock_Format06);
        firstRelevantLineForBuyBlock_Format06.setMaxSize(5);
        firstRelevantLineForBuyBlock_Format06.set(buyBlock_Format06);

        buyBlock_Format06 //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        .section("date", "shares", "gross", "fee1", "time", "currency", "tickerSymbol", "fee2") //
                        .match("^.* Commission: (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                        .match("^.* Open " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ " //
                                        + "(?<gross>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ " //
                                        + "\\-(?<fee1>[\\.,\\d]+) [\\d]{4}\\-$") //
                        .match("^Platform Fee: [\\.,\\d]+ [\\.,\\d]+ (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .* (?<currency>[A-Z]{3})$") //
                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) \\-(?<fee2>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDate(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));

                            // The amount is stated in gross
                            t.setAmount(asAmount(v.get("gross")) + asAmount(v.get("fee1")) + asAmount(v.get("fee2")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        // @formatter:off
                        // ETF US ARCA Open 1 106.43000 106.43 0.00 -0.99 2025-
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.* Open [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-(?<fee>[\\.,\\d]+) [\\d]{4}\\-$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // (VT) -1.00 /Eastern
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^\\([A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?\\) \\-(?<fee>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getPortfolioTransaction().getCurrencyCode() == null)
                                return null;

                            if (t.getPortfolioTransaction().getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        });


        var dividendBlock = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2022-03-24 VT Cash Dividend 0.2572 USD per Share (Ordinary Dividend) 17.75
        // 2022-12-22 VT Cash Dividend 0.6381 USD per Share (Ordinary Dividend) 44.03 USD
        // @formatter:on
        var firstRelevantLineForDividendBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [A-Z0-9]{2,4} Cash Dividend .* [\\.,\\d]+.*$");
        type.addBlock(firstRelevantLineForDividendBlock);
        firstRelevantLineForDividendBlock.set(dividendBlock);

        dividendBlock //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .section("date", "tickerSymbol", "amountPerShare", "note", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "(?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) " //
                                        + "Cash Dividend " //
                                        + "(?<amountPerShare>[\\.,\\d]+) " //
                                        + "[A-Z]{3} per Share " //
                                        + "\\((?<note>.*)\\) " //
                                        + "(?<amount>[\\.,\\d]+).*$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                                v.put("currency", s.currency);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asDate(v.get("date")));

                            // Calculation of dividend shares and rounding to
                            // whole shares
                            var amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            var amount = BigDecimal.valueOf(asAmount(v.get("amount")));
                            t.setShares(amount.divide(amountPerShare, Values.Share.precision(), RoundingMode.HALF_UP) //
                                            .setScale(0, RoundingMode.HALF_UP) //
                                            .movePointRight(Values.Share.precision()) //
                                            .longValue());

                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));

                            // Set dividend taxes, if available
                            var divdendTaxesTransactionListHelper = context
                                            .getType(DividendTaxesTransactionListHelper.class)
                                            .orElseGet(DividendTaxesTransactionListHelper::new);
                            var divdendTaxesTransactionItem = divdendTaxesTransactionListHelper
                                            .findItem(v.get("tickerSymbol"), LocalDate.parse(v.get("date"), DATEFORMAT));

                            if (divdendTaxesTransactionItem.isPresent())
                            {
                                var tax = Money.of(asCurrencyCode(context.get("currency")), divdendTaxesTransactionItem.get().taxes);
                                checkAndSetTax(tax, t, type.getCurrentContext());

                                // Dividend are stated in gross.
                                // If taxes exist, then we subtract this amount.
                                t.setMonetaryAmount(t.getMonetaryAmount().subtract(tax));
                            }

                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);
                            type.getCurrentContext().removeType(DividendTaxesTransactionItem.class);

                            return new TransactionItem(t);
                        });

        var dividendBlock_late24 = new Transaction<AccountTransaction>();

        var firstRelevantLineForDividendBlock_late24 = new Block("^.*Quantity: [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDividendBlock_late24);
        firstRelevantLineForDividendBlock_late24.setMaxSize(6);
        firstRelevantLineForDividendBlock_late24.set(dividendBlock_late24);

        dividendBlock_late24 //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .optionalOneOf(
                                        // Pattern 1: three-line instrument
                                        //
                                        // @formatter:off
                                        // Vanguard Total World Stock Quantity: 72
                                        // 2024-12-
                                        // Stock ETF Gross Rate: 0.88 Paid 63.17 0 Dividend tax: 9.48 53.69 USD
                                        // 26
                                        // (VT) /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "taxes", "amount", "dateDay", "tickerSymbol", "currency") //
                                                        .match("^.* Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\-$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2})$") //
                                                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) \\/Share$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            var date = v.get("dateYear") + "-" + v.get("dateDay");
                                                            t.setDateTime(asDate(date));

                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxes")));
                                                            checkAndSetTax(tax, t, type.getCurrentContext());
                                                        }),
                                        // Pattern 2: two-line instrument
                                        //
                                        // @formatter:off
                                        // Quantity: 25
                                        // 2024-12- Vanguard S&P 500 ETF
                                        // Stock Gross Rate: 1.74 Paid 43.46 0 Dividend tax: 6.52 36.94 USD
                                        // 30 (VOO)
                                        // /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "taxes",
                                                                        "amount", "dateDay", "tickerSymbol", "currency") //
                                                        .match("^Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .*$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}) \\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\)$") //
                                                        .match("^\\/Share$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            var date = v.get("dateYear") + "-" + v.get("dateDay");
                                                            t.setDateTime(asDate(date));

                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxes")));
                                                            checkAndSetTax(tax, t, type.getCurrentContext());
                                                        }),

                                        // Pattern 3: two-line Quantity/Gross
                                        // Rate
                                        //
                                        // @formatter:off
                                        // Invesco QQQ Quantity: 54
                                        // 2025-01-02 Stock Paid 45.07 0 Dividend tax: 6.76 38.31 USD
                                        // (QQQ) Gross Rate: 0.83/Share
                                        // @formatter:on

                                        section -> section //
                                                        .attributes("shares", "date", "grossAmount", "taxes", "amount",
                                                                        "tickerSymbol", "currency") //
                                                        .match("^.* Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) .* Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) .*$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                                v.put("currency", s.currency);
                                                            });

                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            t.setDateTime(asDate(v.get("date")));

                                                            t.setShares(asShares(v.get("shares")));
                                                            t.setAmount(asAmount(v.get("amount")));
                                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                                            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxes")));
                                                            checkAndSetTax(tax, t, type.getCurrentContext());

                                                        })

                        )

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getDateTime() == null)
                                return null;
                            else
                                return new TransactionItem(t);
                        });

        addDividendTransaction_late25(type);
        addDividendAccrualsTransaction_late25(type);

        var taxRefundBlock = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2023-09-21 Cash Dividend USD per Share - Tax 12.29 USD
        // @formatter:on
        var firstRelevantLineForTaxRefundBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Cash Dividend [A-Z]{3} per Share \\- Tax [\\.,\\d]+ [A-Z]{3}$");
        type.addBlock(firstRelevantLineForTaxRefundBlock);
        firstRelevantLineForTaxRefundBlock.set(taxRefundBlock);

        taxRefundBlock //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.TAX_REFUND))

                        .section("date", "amount", "currency") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "Cash Dividend [A-Z]{3} per Share \\- Tax " //
                                        + "(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(TransactionItem::new);

        var depositBlock = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2022-03-02 Deposit DR-3649942 30,000.00
        // @formatter:on
        var firstRelevantLineForDepositBlock = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} Deposit .* [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDepositBlock);
        firstRelevantLineForDepositBlock.set(depositBlock);

        depositBlock //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "note", "amount") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) Deposit " //
                                        + "(?<note>.*) " //
                                        + "(?<amount>[\\.,\\d]+)$") //
                        .assign((t, v) -> {
                            Map<String, String> context = type.getCurrentContext();

                            t.setDateTime(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new);
    }


    /**
     * Combines two digit fragments into a valid date. Returns null if no
     * combination yields a valid date.
     */
    private String assembleDate(String[] lines, int from, int to, Pattern pDateFragment)
    {
        List<String> fragments = new ArrayList<>();

        for (var ii = from; ii <= to; ii++)
        {
            var m = pDateFragment.matcher(lines[ii]);
            while (m.find())
                fragments.add(m.group());
        }

        for (String first : fragments) // NOSONAR
        {
            for (String second : fragments) // NOSONAR
            {
                if (first.equals(second))
                    continue;

                var candidate = first + second;

                if (!candidate.matches("[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}"))
                    continue;

                try
                {
                    LocalDate.parse(candidate, DATEFORMAT);
                    return candidate;
                }
                catch (DateTimeParseException e)
                {
                    // no valid combination, try the next one
                }
            }
        }

        return null;
    }



    /**
     * @formatter:off
     * Re-assembles the trade time. The end of the time is always followed by a
     * comma and a blank ("12:58:58, US/Eastern"), which distinguishes it from a
     * thousands separator ("5,018.00"). If that part alone is not a valid time,
     * the beginning is the last digit group of the following line, because the
     * PDF to text conversion may split "16:06:34" into "1" and "6:06:34".
     * @formatter:on
     */
    private String assembleTime(String[] lines, int from, int to)
    {
        var pTail = Pattern.compile("(?<![\\d:\\.])(?<time>[\\d:]+),(?=\\s|$)");
        var pFragment = Pattern.compile("[\\d:]+");

        String tail = null;
        var tailLine = -1;

        for (var ii = from; ii <= to + 1 && ii < lines.length; ii++)
        {
            var mTail = pTail.matcher(lines[ii]);
            while (mTail.find())
            {
                tail = mTail.group("time");
                tailLine = ii;
            }
        }

        if (tail == null)
            return null;

        if (isValidTime(tail))
            return tail;

        if (tailLine + 1 >= lines.length)
            return null;

        String head = null;
        var mFragment = pFragment.matcher(lines[tailLine + 1]);
        while (mFragment.find())
            head = mFragment.group();

        if (head != null && isValidTime(head + tail))
            return head + tail;

        return null;
    }

    private boolean isValidTime(String value)
    {
        try
        {
            LocalTime.parse(value, TIMEFORMAT);
            return true;
        }
        catch (DateTimeParseException e)
        {
            return false;
        }
    }

    private void processTradeFee(BuySellEntry t, Map<String, String> v, DocumentType type)
    {
        var fee = Money.of(t.getPortfolioTransaction().getCurrencyCode(), asAmount(v.get("fee")));
        checkAndSetFee(fee, t, type.getCurrentContext());
    }

    private void assignDividend(AccountTransaction t, Map<String, String> v, DocumentType type)
    {
        var context = type.getCurrentContext();

        var securityItem = context.getType(SecurityListHelper.class).get().findItem(v.get("tickerSymbol"));

        securityItem.ifPresent(s -> {
            v.put("name", s.name);
            v.put("tickerSymbol", s.tickerSymbol);
        });

        t.setSecurity(getOrCreateSecurity(v));

        t.setDateTime(asDate(v.get("dateYear") + "-" + v.get("dateDay")));
        t.setShares(asShares(v.get("shares")));
        t.setCurrencyCode(asCurrencyCode(v.get("currency")));
        t.setAmount(asAmount(v.get("amount")));

        // Some layouts state the withholding tax within the amount line
        if (v.get("taxes") != null)
        {
            var tax = Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("taxes")));
            checkAndSetTax(tax, t, context);
        }
    }

    /**
     * Dividends in the late 2025 layout. Used by both document types, the
     * layout appears in the "Activity Statement" as well as in the
     * "Statement Period" documents.
     */
    private void addDividendTransaction_late25(DocumentType type)
    {
        var dividendBlock_late25 = new Transaction<AccountTransaction>();

        var firstRelevantLineForDividendBlock_late25 = new Block("^.*Quantity: [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDividendBlock_late25);
        firstRelevantLineForDividendBlock_late25.setMaxSize(9);
        firstRelevantLineForDividendBlock_late25.set(dividendBlock_late25);

        dividendBlock_late25 //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        .optionalOneOf( //
                                        // @formatter:off
                                        // Quantity: 2200
                                        // 2025-09- CapLand Ascendas REIT
                                        // Stock Gross Rate: 0.000930 Paid 2.05 0 2.05 SGD
                                        // 04 (A17U.SI) Reinvestment
                                        // /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "amount",
                                                                        "currency", "dateDay", "tickerSymbol") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .*$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}) \\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\).*$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)),
                                        // @formatter:off
                                        // Blackstone Secured Lending Quantity: 367
                                        // 2025-10- Dividend tax:
                                        // Stock Fund Gross Rate: 0.770000 Paid 282.59 0 254.33 USD
                                        // 24 Reinvestment 28.26
                                        // (BXSL) /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "amount",
                                                                        "currency", "dateDay", "tickerSymbol") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .*$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}).*$") //
                                                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) \\/Share$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)),
                                        // @formatter:off
                                        // Quantity: 200
                                        // Gross Rate: 0.600000
                                        // 2025-11- Stock DBS /Share Paid 120.00 0 120.00 SGD
                                        // 24 (D05.SI) Reinvestment
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "amount",
                                                                        "currency", "dateDay", "tickerSymbol") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .* Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}) \\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\).*$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)),
                                        // @formatter:off
                                        // Quantity: 2200
                                        // 2025-09- CapLand Ascendas REIT
                                        // 04 Stock (A17U.SI) Reinvestment Gross Rate: 0.009050 Paid 19.91 0 19.91 SGD
                                        // /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "dateDay", "tickerSymbol",
                                                                        "grossAmount", "amount", "currency") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .*$") //
                                                        .match("^(?<dateDay>[\\d]{2}) .*\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\).* Gross Rate: [\\.,\\d]+ Paid (?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)))


                        // @formatter:off
                        // The withholding tax is stated at the end of the day line and is
                        // present for USD dividends only.
                        //
                        // 15 (O) 17.60
                        // 24 Reinvestment 28.26
                        // @formatter:on
                        .section("taxes").optional() //
                        .match("^[\\d]{2} (.* )?(?<taxes>[\\d]+\\.[\\d]{2})$") //
                        .assign((t, v) -> {
                            if (t.getCurrencyCode() == null)
                                return;

                            var tax = Money.of(t.getCurrencyCode(), asAmount(v.get("taxes")));
                            checkAndSetTax(tax, t, type.getCurrentContext());
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getDateTime() == null)
                                return null;

                            return new TransactionItem(t);
                        });
    }

    /**
     * "Dividend Accruals" are announced but not paid out yet, there is no
     * cash flow. They are skipped with a message instead of being dropped
     * silently.
     */
    private void addDividendAccrualsTransaction_late25(DocumentType type)
    {
        var dividendAccrualsBlock_late25 = new Transaction<AccountTransaction>();

        var firstRelevantLineForDividendAccrualsBlock_late25 = new Block("^.*Quantity: [\\.,\\d]+$");
        type.addBlock(firstRelevantLineForDividendAccrualsBlock_late25);
        firstRelevantLineForDividendAccrualsBlock_late25.setMaxSize(9);
        firstRelevantLineForDividendAccrualsBlock_late25.set(dividendAccrualsBlock_late25);

        dividendAccrualsBlock_late25 //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DIVIDENDS))

                        // @formatter:off
                        // "Dividend Accruals" are announced but not paid out yet, there is
                        // no cash flow. They are skipped with a message instead of being
                        // dropped silently.
                        // @formatter:on
                        .optionalOneOf( //
                                        // @formatter:off
                                        // Quantity: 3000
                                        // 2025-10- Frasers Cpt Tr Dividend Accruals
                                        // Stock Gross Rate: 0.055850 167.55 0 167.55 SGD
                                        // 31 (J69U.SI) Reinvestment Increase
                                        // /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "amount",
                                                                        "currency", "dateDay", "tickerSymbol") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .*Dividend Accruals.*$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ (\\-)?(?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}) \\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\).*$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)),
                                        // @formatter:off
                                        // Blackstone Secured Lending Quantity: 1033
                                        // 2025-12- Dividend Accruals Dividend tax:
                                        // Stock Fund Gross Rate: 0.770000 795.41 0 715.87 USD
                                        // 31 Increase 79.54
                                        // (BXSL) /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "amount",
                                                                        "currency", "dateDay", "tickerSymbol") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\- .*Dividend Accruals.*$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ (\\-)?(?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ (\\-)?(?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}).*$") //
                                                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) \\/Share$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)),
                                        // @formatter:off
                                        // Vanguard Total World Stock Quantity: 72
                                        // 2024-12-
                                        // Stock ETF Gross Rate: 0.88 Dividend Accruals Increase 63.17 0 Dividend tax: 9.48 53.69 USD
                                        // 20
                                        // (VT) /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "taxes",
                                                                        "amount", "currency", "dateDay", "tickerSymbol") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\-( .*)?$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Dividend Accruals (Increase|Reduction) (\\-)?(?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2})$") //
                                                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\) \\/Share$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)),
                                        // @formatter:off
                                        // Quantity: 25
                                        // 2024-12- Vanguard S&P 500 ETF
                                        // Stock Gross Rate: 1.74 Dividend Accruals Increase 43.46 0 Dividend tax: 6.52 36.94 USD
                                        // 23 (VOO)
                                        // /Share
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("shares", "dateYear", "grossAmount", "taxes",
                                                                        "amount", "currency", "dateDay", "tickerSymbol") //
                                                        .match("^.*Quantity: (?<shares>[\\.,\\d]+)$") //
                                                        .match("^(?<dateYear>[\\d]{4}\\-[\\d]{2})\\-( .*)?$") //
                                                        .match("^.* Gross Rate: [\\.,\\d]+ Dividend Accruals (Increase|Reduction) (\\-)?(?<grossAmount>[\\.,\\d]+) [\\.,\\d]+ Dividend tax: (?<taxes>[\\.,\\d]+) (?<amount>[\\.,\\d]+) (?<currency>[A-Z]{3})$") //
                                                        .match("^(?<dateDay>[\\d]{2}) \\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\)$") //
                                                        .match("^\\/Share$") //
                                                        .assign((t, v) -> assignDividend(t, v, type)))


                        // @formatter:off
                        // The withholding tax is stated at the end of the day line and is
                        // present for USD dividends only.
                        //
                        // 15 (O) 17.60
                        // 24 Reinvestment 28.26
                        // @formatter:on
                        .section("taxes").optional() //
                        .match("^[\\d]{2} (.* )?(?<taxes>[\\d]+\\.[\\d]{2})$") //
                        .assign((t, v) -> {
                            if (t.getCurrencyCode() == null)
                                return;

                            var tax = Money.of(t.getCurrencyCode(), asAmount(v.get("taxes")));
                            checkAndSetTax(tax, t, type.getCurrentContext());
                        })

                        // @formatter:off
                        // Set a note so that the reason for skipping is visible in the
                        // import dialog and in the note column.
                        //
                        // 2025-10- Frasers Cpt Tr Dividend Accruals
                        // @formatter:on
                        .section("note").optional() //
                        .match("^.*(?<note>Dividend Accruals).*$") //
                        .assign((t, v) -> t.setNote(trim(v.get("note"))))

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getDateTime() == null)
                                return null;

                            return new SkippedItem(new TransactionItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);
                        });
    }

    /**
     * @formatter:off
     * Late 2025 layout. These documents carry "Statement Period" in the header
     * instead of "Activity Statement" and contain bookings in more than one
     * currency. They need their own document type because single fee lines
     * like "Settlement Fee: -2.70" are indistinguishable from the old layout.
     * @formatter:on
     */
    private void addAccountStatementTransaction_late25()
    {
        final var type = new DocumentType("Statement Period", (context, lines) -> {
            var pSecurity = Pattern.compile("^(?<tickerSymbol>(?!(GST|Net))[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?) (?<name>.*) [\\d]$");

            // @formatter:off
            // Create a helper to store the list of security items found in the document.
            //
            // These documents do not contain a "Stock Currency:" line and hold bookings
            // in more than one currency, therefore no currency is stored here. It is
            // taken from the respective transaction instead.
            // @formatter:on
            var securityListHelper = new SecurityListHelper();
            context.putType(securityListHelper);

            for (String line : lines)
            {
                var mSecurity = pSecurity.matcher(line);
                if (mSecurity.matches())
                {
                    var securityItem = new SecurityItem();
                    securityItem.tickerSymbol = mSecurity.group("tickerSymbol");
                    securityItem.name = mSecurity.group("name");
                    securityListHelper.items.add(securityItem);
                }
            }

            // @formatter:off
            // Collect the trade dates. The trade date is located somewhere between
            // the previous and the current trade data line and may be split into
            // two fragments. If no complete date is present, every pair of digit
            // fragments is combined in both directions until a valid date results.
            // @formatter:on
            var tradeDateListHelper = new TradeDateListHelper();
            context.putType(tradeDateListHelper);

            var pTradeData = Pattern.compile("^.*(SG|US) (SGX|NYSE|ARCA|NASDAQ) (Open|Close) "
                            + "\\-?[\\.,\\d]+ [\\.,\\d]+ \\-?[\\.,\\d]+ [\\.,\\d]+.*$");
            var pFullDate = Pattern.compile("^.*?(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}).*$");
            var pDateFragment = Pattern.compile("[\\d][\\d\\-]*");

            var previousTradeLine = 0;

            for (var ii = 0; ii < lines.length; ii++)
            {
                if (!pTradeData.matcher(lines[ii]).matches())
                    continue;

                String tradeDate = null;

                for (var jj = previousTradeLine; jj <= ii && tradeDate == null; jj++)
                {
                    var mFullDate = pFullDate.matcher(lines[jj]);
                    if (mFullDate.matches())
                        tradeDate = mFullDate.group("date");
                }

                if (tradeDate == null)
                    tradeDate = assembleDate(lines, previousTradeLine, ii, pDateFragment);

                if (tradeDate != null)
                {
                    var tradeDateItem = new TradeDateItem();
                    tradeDateItem.lineNo = ii;
                    tradeDateItem.date = tradeDate;
                    tradeDateItem.time = assembleTime(lines, previousTradeLine, ii);
                    tradeDateListHelper.items.add(tradeDateItem);
                }

                previousTradeLine = ii + 1;
            }
        });
        this.addDocumentTyp(type);

        var buySellBlock_late25 = new Transaction<BuySellEntry>();

        // @formatter:off
        // Every trade starts with exactly one fee line, therefore this is the only
        // reliable anchor: the trade date is torn apart by the PDF to text conversion
        // and is taken from the TradeDateListHelper instead.
        //
        // CapLand Ascendas REIT Exchange Fee: -1.98 2025-09-03
        // 2025-
        // SG SGX Open 1800 2.75000 4,950.00 0.00 6:06:34, SGD
        // (A17U.SI) Commission: -1.49 0.00 0.00 1
        // Platform Fee: -1.49 09-05
        // GMT+8
        // @formatter:on
        var firstRelevantLineForBuySellBlock_late25 = new Block("^.*(Exchange Fee|Settlement Fee): \\-[\\.,\\d]+.*$");
        type.addBlock(firstRelevantLineForBuySellBlock_late25);
        firstRelevantLineForBuySellBlock_late25.setMaxSize(10);
        firstRelevantLineForBuySellBlock_late25.set(buySellBlock_late25);

        buySellBlock_late25 //

                        .subject(() -> new BuySellEntry(PortfolioTransaction.Type.BUY))

                        // @formatter:off
                        // The document contains SGD and USD bookings, therefore the
                        // currency is parsed per transaction. It must be set before the
                        // fee sections are processed.
                        // @formatter:on
                        .section("currency") //
                        .match("^.* (?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> t.setCurrencyCode(asCurrencyCode(v.get("currency"))))

                        .oneOf( //
                                        // @formatter:off
                                        // (N2IU.SI) SG SGX Open 100 1.32000 132.00 0.00 Exchange Fee: -0.05 0.00 0.00 GMT+8 2025- SGD
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("tickerSymbol", "type", "shares", "gross") //
                                                        .match("^.*\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\).*" //
                                                                        + "(SG|US) (SGX|NYSE|ARCA|NASDAQ) " //
                                                                        + "(?<type>Open|Close) " //
                                                                        + "(?<shares>\\-?[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "\\-?(?<gross>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+.*$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                            });

                                                            v.put("currency", t.getPortfolioTransaction().getCurrencyCode());
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            // Is type --> "Close" change from BUY to SELL
                                                            if ("Close".equals(v.get("type")))
                                                                t.setType(PortfolioTransaction.Type.SELL);

                                                            var tradeDate = context.getType(TradeDateListHelper.class).get()
                                                                            .findItem(v.getStartLineNumber());

                                                            if (tradeDate.isPresent())
                                                                t.setDate(asDate(tradeDate.get().date, tradeDate.get().time));
                                                            else
                                                                v.skipTransaction(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                                                            t.setShares(asShares(v.get("shares")));

                                                            // The amount is stated in gross, the fees are added in wrap()
                                                            t.setAmount(asAmount(v.get("gross")));
                                                        }),
                                        // @formatter:off
                                        // SG SGX Open 1800 2.75000 4,950.00 0.00 6:06:34, SGD
                                        // (A17U.SI) Commission: -1.49 0.00 0.00 1
                                        // @formatter:on
                                        section -> section //
                                                        .attributes("type", "shares", "gross", "tickerSymbol") //
                                                        .match("^.*(SG|US) (SGX|NYSE|ARCA|NASDAQ) " //
                                                                        + "(?<type>Open|Close) " //
                                                                        + "(?<shares>\\-?[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+ " //
                                                                        + "\\-?(?<gross>[\\.,\\d]+) " //
                                                                        + "[\\.,\\d]+.*$") //
                                                        .match("^.*\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\).*$") //
                                                        .assign((t, v) -> {
                                                            var context = type.getCurrentContext();

                                                            var securityItem = context.getType(SecurityListHelper.class).get()
                                                                            .findItem(v.get("tickerSymbol"));

                                                            securityItem.ifPresent(s -> {
                                                                v.put("name", s.name);
                                                                v.put("tickerSymbol", s.tickerSymbol);
                                                            });

                                                            v.put("currency", t.getPortfolioTransaction().getCurrencyCode());
                                                            t.setSecurity(getOrCreateSecurity(v));

                                                            // Is type --> "Close" change from BUY to SELL
                                                            if ("Close".equals(v.get("type")))
                                                                t.setType(PortfolioTransaction.Type.SELL);

                                                            var tradeDate = context.getType(TradeDateListHelper.class).get()
                                                                            .findItem(v.getStartLineNumber());

                                                            if (tradeDate.isPresent())
                                                                t.setDate(asDate(tradeDate.get().date, tradeDate.get().time));
                                                            else
                                                                v.skipTransaction(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                                                            t.setShares(asShares(v.get("shares")));

                                                            // The amount is stated in gross, the fees are added in wrap()
                                                            t.setAmount(asAmount(v.get("gross")));
                                                        }))

                        // @formatter:off
                        // CapLand Ascendas REIT Exchange Fee: -1.98 2025-09-03
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Exchange Fee: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // Blackstone Secured Settlement Fee: -1.09 2025-09-29
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Settlement Fee: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // (A17U.SI) Commission: -1.49 0.00 0.00 1
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Commission: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // Platform Fee: -1.49 09-05
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Platform Fee: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // (ARE) Consolidated Audit Trail 10-30
                        // /Eastern
                        // Fee: -0.01
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Consolidated Audit Trail.*$") //
                        .match("^Fee: \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // Trading Activity Fee:
                        // -0.04 2025-10-23
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Trading Activity Fee:.*$") //
                        .match("^\\-(?<fee>[\\.,\\d]+) .*$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // Realty Income Trading Activity Fee: 2025-10-24
                        // (O) -0.04
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*Trading Activity Fee:.*$") //
                        .match("^\\([A-Z0-9\\.]+\\) \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        // @formatter:off
                        // Equities US NYSE Close -133 48.42647 -6,440.72 0.00 -0.05 0.00 -1,596.96 22:02:07, US USD
                        // @formatter:on
                        .section("fee").optional() //
                        .match("^.*(Open|Close) \\-?[\\.,\\d]+ [\\.,\\d]+ \\-?[\\.,\\d]+ [\\.,\\d]+ " //
                                        + "\\-(?<fee>[\\.,\\d]+) [\\.,\\d]+ \\-?[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processTradeFee(t, v, type))

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            var tx = t.getPortfolioTransaction();

                            if (tx.getCurrencyCode() == null || tx.getDateTime() == null)
                                return null;

                            // @formatter:off
                            // The amount is stated in gross. Add the fees for a purchase
                            // and subtract them for a sale to get the settlement amount.
                            // @formatter:on
                            var fees = tx.getUnitSum(Unit.Type.FEE).getAmount();

                            if (tx.getType() == PortfolioTransaction.Type.BUY)
                                t.setAmount(tx.getAmount() + fees);
                            else
                                t.setAmount(tx.getAmount() - fees);

                            if (tx.getAmount() == 0)
                                return new SkippedItem(new BuySellEntryItem(t), Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

                            return new BuySellEntryItem(t);
                        });

        var depositRemovalBlock_late25 = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2025-08-06 Deposit 432.18 SGD
        // 2025-08-07 Withdrawal -10.00 SGD
        // @formatter:on
        var firstRelevantLineForDepositRemovalBlock_late25 = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (Deposit|Withdrawal) (\\-)?[\\.,\\d]+ [A-Z]{3}$");
        type.addBlock(firstRelevantLineForDepositRemovalBlock_late25);
        firstRelevantLineForDepositRemovalBlock_late25.set(depositRemovalBlock_late25);

        depositRemovalBlock_late25 //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.DEPOSIT))

                        .section("date", "type", "amount", "currency") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "(?<type>Deposit|Withdrawal) " //
                                        + "(\\-)?(?<amount>[\\.,\\d]+) " //
                                        + "(?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            // Is type --> "Withdrawal" change from DEPOSIT to REMOVAL
                            if ("Withdrawal".equals(v.get("type")))
                                t.setType(AccountTransaction.Type.REMOVAL);

                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                        })

                        .wrap(TransactionItem::new);

        var feesRefundBlock_late25 = new Transaction<AccountTransaction>();

        // @formatter:off
        // 2025-10-06 Order Rebate 50.00 SGD
        // 2025-09-09 Coupon Rebate 0.99 USD
        // @formatter:on
        var firstRelevantLineForFeesRefundBlock_late25 = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} (Order|Coupon) Rebate [\\.,\\d]+ [A-Z]{3}$");
        type.addBlock(firstRelevantLineForFeesRefundBlock_late25);
        firstRelevantLineForFeesRefundBlock_late25.set(feesRefundBlock_late25);

        feesRefundBlock_late25 //

                        .subject(() -> new AccountTransaction(AccountTransaction.Type.FEES_REFUND))

                        .section("date", "note", "amount", "currency") //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}) " //
                                        + "(?<note>(Order|Coupon) Rebate) " //
                                        + "(?<amount>[\\.,\\d]+) " //
                                        + "(?<currency>[A-Z]{3})$") //
                        .assign((t, v) -> {
                            t.setDateTime(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setNote(trim(v.get("note")));
                        })

                        .wrap(TransactionItem::new);

        var accountTransferBlock_late25 = new Transaction<AccountTransferEntry>();

        // @formatter:off
        // 2025-09-09
        // USD.SGD Buy 3,885.3600 1.28688 -5,000.00 2025-09-10 SGD
        // 13:52:45, US/Eastern
        // @formatter:on
        var firstRelevantLineForAccountTransferBlock_late25 = new Block("^[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$");
        type.addBlock(firstRelevantLineForAccountTransferBlock_late25);
        firstRelevantLineForAccountTransferBlock_late25.setMaxSize(3);
        firstRelevantLineForAccountTransferBlock_late25.set(accountTransferBlock_late25);

        accountTransferBlock_late25 //

                        .subject(AccountTransferEntry::new)

                        .section("date", "baseCurrency", "quoteCurrency", "type", "baseAmount", "exchangeRate",
                                        "quoteAmount", "time").optional() //
                        .match("^(?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2})$") //
                        .match("^(?<baseCurrency>[A-Z]{3})\\.(?<quoteCurrency>[A-Z]{3}) " //
                                        + "(?<type>Buy|Sell) " //
                                        + "(\\-)?(?<baseAmount>[\\.,\\d]+) " //
                                        + "(?<exchangeRate>[\\.,\\d]+) " //
                                        + "(\\-)?(?<quoteAmount>[\\.,\\d]+) " //
                                        + "[\\d]{4}\\-[\\d]{2}\\-[\\d]{2} [A-Z]{3}$") //
                        .match("^(?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), .*$") //
                        .assign((t, v) -> {
                            // @formatter:off
                            // Buy  --> the quote currency is sold, the base currency is bought
                            // Sell --> the base currency is sold, the quote currency is bought
                            // @formatter:on
                            var sourceCurrency = "Buy".equals(v.get("type")) ? v.get("quoteCurrency") : v.get("baseCurrency");
                            var targetCurrency = "Buy".equals(v.get("type")) ? v.get("baseCurrency") : v.get("quoteCurrency");
                            var sourceAmount = "Buy".equals(v.get("type")) ? v.get("quoteAmount") : v.get("baseAmount");
                            var targetAmount = "Buy".equals(v.get("type")) ? v.get("baseAmount") : v.get("quoteAmount");

                            t.setDate(asDate(v.get("date"), v.get("time")));

                            t.getSourceTransaction().setCurrencyCode(asCurrencyCode(sourceCurrency));
                            t.getSourceTransaction().setAmount(asAmount(sourceAmount));

                            t.getTargetTransaction().setCurrencyCode(asCurrencyCode(targetCurrency));
                            t.getTargetTransaction().setAmount(asAmount(targetAmount));

                            var gross = Money.of(asCurrencyCode(sourceCurrency), asAmount(sourceAmount));
                            var forex = Money.of(asCurrencyCode(targetCurrency), asAmount(targetAmount));
                            // @formatter:off
                            // The unit requires forex x exchangeRate == amount. Depending
                            // on the direction that is either the printed trade price or
                            // its reciprocal, therefore it is derived from both amounts.
                            // @formatter:on
                            var exchangeRate = BigDecimal.valueOf(gross.getAmount())
                                            .divide(BigDecimal.valueOf(forex.getAmount()), 10, RoundingMode.HALF_UP);

                            t.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE, gross, forex, exchangeRate));

                            t.setNote(v.get("baseCurrency") + "/" + v.get("quoteCurrency") + " " + v.get("exchangeRate"));
                        })

                        .wrap(t -> {
                            if (t.getSourceTransaction().getCurrencyCode() == null)
                                return null;

                            return new AccountTransferItem(t, true);
                        });

        var deliveryInboundBlock_late25 = new Transaction<PortfolioTransaction>();

        // @formatter:off
        // DBS
        // Stock 2025-09-01, 15:38:02, GMT+8 EXTERNAL IN 9999 200 0 34.2100 10,040.00 SGD
        // (D05.SI)
        // @formatter:on
        var firstRelevantLineForDeliveryInboundBlock_late25 = new Block("^(Stock|Fund) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* EXTERNAL IN .*$");
        type.addBlock(firstRelevantLineForDeliveryInboundBlock_late25);
        firstRelevantLineForDeliveryInboundBlock_late25.setMaxSize(2);
        firstRelevantLineForDeliveryInboundBlock_late25.set(deliveryInboundBlock_late25);

        deliveryInboundBlock_late25 //

                        .subject(() -> new PortfolioTransaction(PortfolioTransaction.Type.DELIVERY_INBOUND))

                        // @formatter:off
                        // An inbound delivery from another custodian. The book value is
                        // the cost price times the number of shares, the market value
                        // column states the value at transfer date instead.
                        // @formatter:on
                        .section("date", "time", "note", "shares", "amountPerShare", "currency", "tickerSymbol") //
                        .match("^(Stock|Fund) (?<date>[\\d]{4}\\-[\\d]{2}\\-[\\d]{2}), (?<time>[\\d]{2}:[\\d]{2}:[\\d]{2}), " //
                                        + "[^\\s]+ (?<note>EXTERNAL IN) [\\d]+ " //
                                        + "(?<shares>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ " //
                                        + "(?<amountPerShare>[\\.,\\d]+) " //
                                        + "[\\.,\\d]+ " //
                                        + "(?<currency>[A-Z]{3})$") //
                        .match("^\\((?<tickerSymbol>[A-Z0-9]{1,6}(?:\\.[A-Z]{1,4})?)\\)$") //
                        .assign((t, v) -> {
                            var context = type.getCurrentContext();

                            var securityItem = context.getType(SecurityListHelper.class).get()
                                            .findItem(v.get("tickerSymbol"));

                            securityItem.ifPresent(s -> {
                                v.put("name", s.name);
                                v.put("tickerSymbol", s.tickerSymbol);
                            });

                            t.setSecurity(getOrCreateSecurity(v));

                            t.setDateTime(asDate(v.get("date"), v.get("time")));
                            t.setShares(asShares(v.get("shares")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                            t.setNote(trim(v.get("note")));

                            // @formatter:off
                            // The market value is stated at transfer date, the book value
                            // is the cost price times the number of shares.
                            // @formatter:on
                            var amountPerShare = BigDecimal.valueOf(asAmount(v.get("amountPerShare")));
                            var shares = BigDecimal.valueOf(asShares(v.get("shares")));

                            t.setAmount(amountPerShare.multiply(shares)
                                            .divide(BigDecimal.valueOf(Values.Share.factor()), 0, RoundingMode.HALF_UP)
                                            .longValue());
                        })

                        .wrap(t -> {
                            type.getCurrentContext().removeType(SecurityItem.class);

                            if (t.getCurrencyCode() == null)
                                return null;

                            return new TransactionItem(t);
                        });

        addDividendTransaction_late25(type);
        addDividendAccrualsTransaction_late25(type);
    }

    private <T extends Transaction<?>> void addFeesSectionsTransaction(T transaction, DocumentType type)
    {
        transaction //

                        // @formatter:off
                        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ 2022-03-10, 01:52:40, GMT+8 48 334.80000 334.99000 16,070.40 Commission: -0.99 -0.15 0.00 9.12
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ 2023-01-06, 03:33:08, GMT+8 1 262.78870 261.58000 262.79 Commission: -0.99Platform Fee: -1.00 -0.16 0.00 -1.21
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}, [\\d]{2}:[\\d]{2}:[\\d]{2}, .* Commission: \\-[\\.,\\d]+([\\s])?Platform Fee: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: (?<fee>\\-[\\.,\\d]+) \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: \\-[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: \\-[\\.,\\d]+ \\-(?<fee>[\\.,\\d]+) (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: \\-[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // QQQ US 1 262.78870 261.58000 262.79 Commission: -0.99 -0.16 0.00 -1.21 2023-01-06Platform Fee: -1.00 03:33:08, GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^[A-Z0-9]{2,4} [\\w]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: \\-[\\.,\\d]+ \\-[\\.,\\d]+ (\\-)?[\\.,\\d]+ (\\-)?[\\.,\\d]+ [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}(\\s)?Platform Fee: (?<fee>\\-[\\.,\\d]+) [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2024-04-08
                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                        // -1.00 GMT+8 10
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ (?<gross>[\\.,\\d]+) [\\.,\\d]+ \\-[\\.,\\d]+ .*Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2024-04-08
                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                        // -1.00 GMT+8 10
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-(?<fee>[\\.,\\d]+) .*Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2024-04-08
                        // QQQ US NASDAQ Open 1 440.50000 440.60000 440.50 0.00 -0.99 2024-04-Platform Fee: 0.00 0.00 0.10 23:45:38,
                        // -1.00 GMT+8 10
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-[\\.,\\d]+ .*Platform Fee: (\\-)?(?<fee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2023-01-06
                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                        // -1.00 GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-[\\.,\\d]+Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ (\\-)[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-(?<fee>[\\.,\\d]+).*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2023-01-06
                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                        // -1.00 GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-(?<fee>[\\.,\\d]+)Platform Fee: (\\-)?[\\.,\\d]+ [\\.,\\d]+ (\\-)[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Commission: 2023-01-06
                        // QQQ US 1 262.78870 261.58000 262.79 -0.99Platform Fee: -0.16 0.00 -1.21 03:33:08,
                        // -1.00 GMT+8
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Commission: [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .match("^[A-Z0-9]{2,4} .* [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ \\-[\\.,\\d]+Platform Fee: (\\-)?(?<fee>[\\.,\\d]+) [\\.,\\d]+ (\\-)[\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .match("^\\-[\\.,\\d]+.*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Settlement Fee:
                        // -0.01 2024-12-19
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("Platform Fee:.*") //
                        .match("^\\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // US ARCA Open 3 118.29960 354.90 0.00 Commission: 2024-
                        // -0.99 0.00 0.00 13:01:49, US USD
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("^.*[\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ [\\.,\\d]+ Commission: [\\d]{4}\\-$") //
                        .match("^\\-(?<fee>[\\.,\\d]+) [\\.,\\d]+ [\\.,\\d]+ [\\d]{2}:[\\d]{2}:[\\d]{2}, .*$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Settlement Fee:
                        // -0.01 2024-12-19
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .find("Settlement Fee:[\\s]*") //
                        .match("^\\-(?<fee>[\\.,\\d]+) [\\d]{4}\\-[\\d]{2}\\-[\\d]{2}$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))
                        
                        // @formatter:off
                        // Settlement Fee: -0.14
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Settlement Fee: \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type))

                        // @formatter:off
                        // Platform Fee: -1.00
                        // @formatter:on
                        .section("fee").optional() //
                        .documentContext("currency") //
                        .match("^Platform Fee: \\-(?<fee>[\\.,\\d]+)$") //
                        .assign((t, v) -> processFeeEntries(t, v, type));
    }

    private static class SecurityItem
    {
        String tickerSymbol;
        String name;
        String currency;

        @Override
        public String toString()
        {
            return "SecurityItem [tickerSymbol=" + tickerSymbol + ", name=" + name + ", currency=" + currency + "]";
        }
    }

    private static class SecurityListHelper
    {
        private final List<SecurityItem> items = new ArrayList<>();

        // Finds a SecurityItem in the list
        public Optional<SecurityItem> findItem(String tickerSymbol)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (SecurityItem item : items) // NOSONAR
            {
                if (!item.tickerSymbol.equals(tickerSymbol))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    /**
     * @formatter:off
     * Holds the trade date of every trade found in the document.
     *
     * In the late 2025 format the trade date is torn apart by the PDF to text
     * conversion ("20" + "25-08-25", "2025-08" + "-07"). Re-assembling it with
     * regular expressions is not possible, therefore the whole trades section is
     * scanned once and the date is stored per line number of the trade data line.
     * @formatter:on
     */
    private static class TradeDateItem
    {
        int lineNo;
        String date;
        String time;
    }

    private static class TradeDateListHelper
    {
        private final List<TradeDateItem> items = new ArrayList<>();

        /**
         * Returns the first trade at or after the given line number. Every trade
         * starts with exactly one fee line, therefore the block start is never
         * behind its own data line.
         */
        public Optional<TradeDateItem> findItem(int startLineNo)
        {
            for (TradeDateItem item : items) // NOSONAR
            {
                if (item.lineNo >= startLineNo)
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class DividendTaxesTransactionItem
    {
        String tickerSymbol;
        LocalDate date;
        Long taxes;

        @Override
        public String toString()
        {
            return "DividendTaxesTransactionItem [tickerSymbol=" + tickerSymbol + ", date=" + date + ", taxes=" + taxes
                            + "]";
        }
    }

    private static class DividendTaxesTransactionListHelper
    {
        private final List<DividendTaxesTransactionItem> items = new ArrayList<>();

        public Optional<DividendTaxesTransactionItem> findItem(String tickerSymbol, LocalDate date)
        {
            if (items.isEmpty())
                return Optional.empty();

            for (DividendTaxesTransactionItem item : items) // NOSONAR
            {
                if (!item.tickerSymbol.equals(tickerSymbol))
                    continue;

                if (!item.date.equals(date))
                    continue;

                return Optional.of(item);
            }

            return Optional.empty();
        }
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

    @Override
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "US");
    }
}
