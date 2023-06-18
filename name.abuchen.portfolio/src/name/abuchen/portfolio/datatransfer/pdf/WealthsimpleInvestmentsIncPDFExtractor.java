package name.abuchen.portfolio.datatransfer.pdf;

import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetGrossUnit;
import static name.abuchen.portfolio.datatransfer.ExtractorUtils.checkAndSetTax;
import static name.abuchen.portfolio.util.TextUtil.trim;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.DocumentContext;
import name.abuchen.portfolio.datatransfer.ExtrExchangeRate;
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

@SuppressWarnings("nls")
public class WealthsimpleInvestmentsIncPDFExtractor extends AbstractPDFExtractor
{
    /**
     * Information:
     * Wealthsimple Investments Inc. is a CAD-based financial
     * services company. The currency is $CAD.
     *
     * All securities are specified in $CAD.
     * However, there is an exchange rate in $USD.
     */

    public WealthsimpleInvestmentsIncPDFExtractor(Client client)
    {
        super(client);

        addBankIdentifier("Wealthsimple Inc.");

        addDepotStatementTransaction();
    }

    @Override
    public String getLabel()
    {
        return "Wealthsimple Investments Inc.";
    }

    private void addDepotStatementTransaction()
    {
        final DocumentType type = new DocumentType("Portfolio Performance Report", (context, lines) -> {
            Pattern pYear = Pattern.compile("^.*(?<year>[\\d]{4}) Performance Report .*$");
            Pattern pCurrency = Pattern.compile("^.* Canada Conversion rate \\p{Sc}[\\.,\\d]+ [\\w]{3} = \\p{Sc}[\\.,\\d]+ (?<currency>[\\w]{3})$");
            Pattern pDividendTaxTransactions = Pattern.compile("^(?<month>[\\w]{3,4}) "
                            + "(?<day>[\\d]{2}) "
                            + "(?<tickerSymbol>[A-Z]{3,4})"
                            + "[\\W]{1,3}.*: .* tax .* \\([\\.,\\d]+ [\\w]{3}, .* "
                            + "(?<currency>[\\w]{3})"
                            + " .([\\.,\\d]+\\))? .* "
                            + "\\-\\p{Sc}(?<tax>[\\.,\\d]+)$");
            Pattern pFeeRefundTransactions = Pattern.compile("^(?<month>[\\w]{3,4}) "
                            + "(?<day>[\\d]{2}) "
                            + "Promotions and discounts applied to Wealthsimple fee [\\W]{1,3} "
                            + "\\p{Sc}(?<feeRefund>[\\.,\\d]+)$");
            Pattern pFeeTaxTransactions = Pattern.compile("^(?<month>[\\w]{3,4}) "
                            + "(?<day>[\\d]{2}) "
                            + "Sales tax on management fee to Wealthsimple [\\W]{1,3} "
                            + "\\-\\p{Sc}(?<tax>[\\.,\\d]+)$");

            DividendTaxTransactionHelper dividendTaxTransactionHelper = new DividendTaxTransactionHelper();
            context.putType(dividendTaxTransactionHelper);

            FeeRefundTransactionHelper feeRefundTransactionHelper = new FeeRefundTransactionHelper();
            context.putType(feeRefundTransactionHelper);

            FeeTaxTransactionHelper feeTaxTransactionHelper = new FeeTaxTransactionHelper();
            context.putType(feeTaxTransactionHelper);

            for (String line : lines)
            {
                Matcher m = pYear.matcher(line);
                if (m.matches())
                    context.put("year", m.group("year"));

                m = pCurrency.matcher(line);
                if (m.matches())
                    context.put("currency", m.group("currency"));

                m = pDividendTaxTransactions.matcher(line);
                if (m.matches())
                {
                    DividendTaxTransactionsItem item = new DividendTaxTransactionsItem();
                    item.dateTime = asDate(m.group("day") + " " + m.group("month") + " " + context.get("year"));
                    item.tickerSymbol = m.group("tickerSymbol");
                    item.currency = m.group("currency");
                    item.tax = asAmount(m.group("tax"));

                    dividendTaxTransactionHelper.items.add(item);
                }

                m = pFeeRefundTransactions.matcher(line);
                if (m.matches())
                {
                    FeeRefundTransactionsItem item = new FeeRefundTransactionsItem();
                    item.dateTime = asDate(m.group("day") + " " + m.group("month") + " " + context.get("year"));
                    item.feeRefund = asAmount(m.group("feeRefund"));

                    feeRefundTransactionHelper.items.add(item);
                }

                m = pFeeTaxTransactions.matcher(line);
                if (m.matches())
                {
                    FeeTaxTransactionsItem item = new FeeTaxTransactionsItem();
                    item.dateTime = asDate(m.group("day") + " " + m.group("month") + " " + context.get("year"));
                    item.tax = asAmount(m.group("tax"));

                    feeTaxTransactionHelper.items.add(item);
                }
            }
        });
        this.addDocumentTyp(type);

        // @formatter:off
        // Jul 02 Electronic Funds Transfer Out: 1,000.00 CAD – – -$1,000.00
        // Jun 01 Electronic Funds Transfer In: $100.00 CAD – – $100.00
        // May 02 settled May 04 Electronic Funds Transfer In: $100.00 CAD – – $100.00
        // @formatter:on
        Block depositRemovalBlock = new Block("^[\\w]{3,4} [\\d]{2} .* Transfer (In|Out): (\\p{Sc})?[\\.,\\d]+ [\\w]{3} .* (\\-)?\\p{Sc}[\\.,\\d]+$");
        type.addBlock(depositRemovalBlock);
        depositRemovalBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DEPOSIT);
                    return entry;
                })

                .section("month", "day", "type", "amount", "currency")
                .match("^(?<month>[\\w]{3,4}) "
                                + "(?<day>[\\d]{2}) "
                                + ".* Transfer "
                                + "(?<type>(In|Out)): "
                                + "(\\p{Sc})?(?<amount>[\\.,\\d]+) "
                                + "(?<currency>[\\w]{3}) .* "
                                + "(\\-)?\\p{Sc}[\\.,\\d]+$")
                .assign((t, v) -> {
                    Map<String, String> context = type.getCurrentContext();

                    // Is type --> "Out" change from DEPOSIT to REMOVAL
                    if ("Out".equals(v.get("type")))
                        t.setType(AccountTransaction.Type.REMOVAL);

                    t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(t -> {
                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        // @formatter:off
        // Jul 03 settled Jul 07 Bought 1.0702 of ZFL - BMO Long Federal Bond ETF for 22.32 CAD 1.0702 $20.86 $22.32
        // Jun 29 settled Jul 01 Sold 1.7993 of EEMV - iShares MSCI Emerg Min Vol ETF for 128.87 CAD -1.7993 $71.62 -$128.87
        // @formatter:om
        Block buySellBlock = new Block("^[\\w]{3} [\\d]{2} .* (Bought|Sold) [\\.,\\d]+ of [A-Z]{3,4} [\\W]{1,3} .* for [\\.,\\d]+ [\\w]{3} (\\-)?[\\.,\\d]+ \\p{Sc}[\\.,\\d]+ (\\-)?\\p{Sc}[\\.,\\d]+$");
        type.addBlock(buySellBlock);
        buySellBlock.set(new Transaction<BuySellEntry>()

                .subject(() -> {
                    BuySellEntry entry = new BuySellEntry();
                    entry.setType(PortfolioTransaction.Type.BUY);
                    return entry;
                })

                .section("month", "day", "type", "tickerSymbol", "name", "amount", "currency", "shares")
                .match("^(?<month>[\\w]{3,4}) "
                                + "(?<day>[\\d]{2}) "
                                + ".* "
                                + "(?<type>(Bought|Sold)) "
                                + "[\\.,\\d]+ of "
                                + "(?<tickerSymbol>[A-Z]{3,4}) "
                                + "[\\W]{1,3} "
                                + "(?<name>.*) "
                                + "for "
                                + "(?<amount>[\\.,\\d]+) "
                                + "(?<currency>[\\w]{3}) "
                                + "(\\-)?(?<shares>[\\.,\\d]+) "
                                + "\\p{Sc}[\\.,\\d]+ (\\-)?\\p{Sc}[\\.,\\d]+$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    // Is type --> "Out" change from BUY to SELL
                    if ("Sold".equals(v.get("type")))
                        t.setType(PortfolioTransaction.Type.SELL);

                    t.setSecurity(getOrCreateSecurity(v));
                    t.setDate(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                    t.setShares(asShares(v.get("shares")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap(BuySellEntryItem::new));

        // @formatter:off
        // Dec 22 ACWV-iShares Edge MSCI Min Vol Global ETF: 15-DEC-20 (record date) 8.3610 shares, gross 6.61 USD, – – $8.55
        // convert to CAD @ 1.2927
        // Dec 22 EEMV-iShares MSCI Emerg Min Vol ETF: 15-DEC-20 (record date) 20.8583 shares, gross 19.65 USD, – – $25.40
        // convert to CAD @ 1.2927
        //
        // Dec 03 ZFL-BMO Long Federal Bond ETF: 30-NOV-20 (record date) 148.3070 shares – – $6.38
        //
        // Oct 02 VTI - Vanguard Index STK MKT ETF: 28-SEP-20 (record date) 4.7706 shares, gross 3.22 USD, convert to CAD @ 1.3319 – – $4.29
        //
        // Jun 23 ACWV-iShares Edge MSCI Min Vol Global ETF: 16-JUN-20 (record date) 8.8424 shares, gross 8.14 USD, convert to CAD – – $11.02
        // @ 1.3535
        //
        // Jun 23 EEMV-iShares MSCI Emerg Min Vol ETF: 16-JUN-20 (record date) 22.6576 shares, gross 12.51 USD, convert to CAD @ – – $16.93
        // 1.3535
        //
        // Jun 23 ACWV-iShares Edge MSCI Min Vol Global ETF: 16-JUN-20 (record date) 8.8424 shares, gross 8.14 USD, convert to – – $11.02
        // CAD @ 1.3535
        // @formatter:on
        Block dividendBlock = new Block("^[\\w]{3,4} [\\d]{2} [A-Z]{3,4}[\\W]{1,3}.*: .* \\(record date\\) .*$");
        type.addBlock(dividendBlock);
        dividendBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DIVIDENDS);
                    return entry;
                })

                .oneOf(
                                section -> section
                                        .attributes("month", "day", "tickerSymbol", "name", "shares", "fxCurrency", "amount", "currency", "exchangeRate")
                                        .match("^(?<month>[\\w]{3,4}) "
                                                        + "(?<day>[\\d]{2}) "
                                                        + "(?<tickerSymbol>[A-Z]{3,4})"
                                                        + "[\\W]{1,3}(?<name>.*): .* "
                                                        + "(?<shares>[\\.,\\d]+) "
                                                        + "shares, .* [\\.,\\d]+ "
                                                        + "(?<fxCurrency>[\\w]{3}), .*[\\s\\–]{2,3} "
                                                        + "\\p{Sc}(?<amount>[\\.,\\d]+)$")
                                        .match("^(?![\\w]{3} [\\d]{2})([\\D]+)?(?<currency>[\\w]{3})(.* )?(?<exchangeRate>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            DocumentContext context = type.getCurrentContext();

                                            v.put("termCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                                            t.setShares(asShares(v.get("shares")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                            DividendTaxTransactionHelper dividendTaxTransactionHelper = context.getType(DividendTaxTransactionHelper.class).orElseGet(DividendTaxTransactionHelper::new);
                                            Optional<DividendTaxTransactionsItem> dividendTaxTransaction = dividendTaxTransactionHelper.findItem(t.getDateTime(), t.getSecurity().getTickerSymbol());
                                            if (dividendTaxTransaction.isPresent() && v.get("tickerSymbol").equalsIgnoreCase(t.getSecurity().getTickerSymbol()))
                                            {
                                                Money tax = Money.of(asCurrencyCode(dividendTaxTransaction.get().currency), dividendTaxTransaction.get().tax);
                                                checkAndSetTax(tax, t, type.getCurrentContext());
                                            }

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), t.getGrossValueAmount());
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                section -> section
                                        .attributes("month", "day", "tickerSymbol", "name", "shares", "fxCurrency", "currency", "amount", "exchangeRate")
                                        .match("^(?<month>[\\w]{3,4}) "
                                                        + "(?<day>[\\d]{2}) "
                                                        + "(?<tickerSymbol>[A-Z]{3,4})"
                                                        + "[\\W]{1,3}"
                                                        + "(?<name>.*)"
                                                        + ": .* "
                                                        + "(?<shares>[\\.,\\d]+) "
                                                        + "shares, .* [\\.,\\d]+ "
                                                        + "(?<fxCurrency>[\\w]{3})"
                                                        + ", convert to "
                                                        + "(?<currency>[\\w]{3}) "
                                                        + ".* "
                                                        + "\\p{Sc}(?<amount>[\\.,\\d]+)$")
                                        .match("^(?![\\w]{3} [\\d]{2})([\\D]+)?(?<exchangeRate>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            DocumentContext context = type.getCurrentContext();

                                            v.put("termCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                                            t.setShares(asShares(v.get("shares")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            DividendTaxTransactionHelper dividendTaxTransactionHelper = context.getType(DividendTaxTransactionHelper.class).orElseGet(DividendTaxTransactionHelper::new);
                                            Optional<DividendTaxTransactionsItem> dividendTaxTransaction = dividendTaxTransactionHelper.findItem(t.getDateTime(), t.getSecurity().getTickerSymbol());
                                            if (dividendTaxTransaction.isPresent() && v.get("tickerSymbol").equalsIgnoreCase(t.getSecurity().getTickerSymbol()))
                                            {
                                                Money tax = Money.of(asCurrencyCode(dividendTaxTransaction.get().currency), dividendTaxTransaction.get().tax);
                                                checkAndSetTax(tax, t, type.getCurrentContext());
                                            }

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), t.getGrossValueAmount());
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                section -> section
                                        .attributes("month", "day", "tickerSymbol", "name", "shares", "fxCurrency", "currency", "exchangeRate", "amount")
                                        .match("^(?<month>[\\w]{3,4}) "
                                                        + "(?<day>[\\d]{2}) "
                                                        + "(?<tickerSymbol>[A-Z]{3,4})"
                                                        + "[\\W]{1,3}"
                                                        + "(?<name>.*)"
                                                        + ": .* "
                                                        + "(?<shares>[\\.,\\d]+) "
                                                        + "shares, .* [\\.,\\d]+ "
                                                        + "(?<fxCurrency>[\\w]{3})"
                                                        + ", convert to "
                                                        + "(?<currency>[\\w]{3})"
                                                        + " . "
                                                        + "(?<exchangeRate>[\\.,\\d]+) "
                                                        + "[\\s\\–]{2,3} "
                                                        + "\\p{Sc}(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            DocumentContext context = type.getCurrentContext();

                                            v.put("termCurrency", asCurrencyCode(v.get("currency")));
                                            v.put("baseCurrency", asCurrencyCode(v.get("fxCurrency")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                                            t.setShares(asShares(v.get("shares")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                            ExtrExchangeRate rate = asExchangeRate(v);
                                            type.getCurrentContext().putType(rate);

                                            DividendTaxTransactionHelper dividendTaxTransactionHelper = context.getType(DividendTaxTransactionHelper.class).orElseGet(DividendTaxTransactionHelper::new);
                                            Optional<DividendTaxTransactionsItem> dividendTaxTransaction = dividendTaxTransactionHelper.findItem(t.getDateTime(), t.getSecurity().getTickerSymbol());
                                            if (dividendTaxTransaction.isPresent() && v.get("tickerSymbol").equalsIgnoreCase(t.getSecurity().getTickerSymbol()))
                                            {
                                                Money tax = Money.of(asCurrencyCode(dividendTaxTransaction.get().currency), dividendTaxTransaction.get().tax);
                                                checkAndSetTax(tax, t, type.getCurrentContext());
                                            }

                                            Money gross = Money.of(asCurrencyCode(v.get("currency")), t.getGrossValueAmount());
                                            Money fxGross = rate.convert(asCurrencyCode(v.get("fxCurrency")), gross);

                                            checkAndSetGrossUnit(gross, fxGross, t, type.getCurrentContext());
                                        })
                                ,
                                section -> section
                                        .attributes("month", "day", "tickerSymbol", "name", "shares", "amount")
                                        .match("^(?<month>[\\w]{3,4}) "
                                                        + "(?<day>[\\d]{2}) "
                                                        + "(?<tickerSymbol>[A-Z]{3,4})"
                                                        + "[\\W]{1,3}(?<name>.*)"
                                                        + ": .* "
                                                        + "(?<shares>[\\.,\\d]+) "
                                                        + "shares [\\s\\–]{2,3} "
                                                        + "\\p{Sc}(?<amount>[\\.,\\d]+)$")
                                        .assign((t, v) -> {
                                            DocumentContext context = type.getCurrentContext();

                                            v.put("currency", asCurrencyCode(context.get("currency")));

                                            t.setSecurity(getOrCreateSecurity(v));
                                            t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                                            t.setShares(asShares(v.get("shares")));

                                            t.setAmount(asAmount(v.get("amount")));
                                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));

                                            DividendTaxTransactionHelper dividendTaxTransactionHelper = context.getType(DividendTaxTransactionHelper.class).orElseGet(DividendTaxTransactionHelper::new);
                                            Optional<DividendTaxTransactionsItem> dividendTaxTransaction = dividendTaxTransactionHelper.findItem(t.getDateTime(), t.getSecurity().getTickerSymbol());
                                            if (dividendTaxTransaction.isPresent() && v.get("tickerSymbol").equalsIgnoreCase(t.getSecurity().getTickerSymbol()))
                                            {
                                                Money tax = Money.of(asCurrencyCode(dividendTaxTransaction.get().currency), dividendTaxTransaction.get().tax);
                                                checkAndSetTax(tax, t, type.getCurrentContext());
                                            }
                                        })
                        )

                .wrap(t -> {
                    type.getCurrentContext().removeType(DividendTaxTransactionsItem.class);

                    if (t.getCurrencyCode() != null && t.getAmount() != 0)
                        return new TransactionItem(t);
                    return null;
                }));

        // @formatter:off
        // It may happen that no ticker symbol is available for
        // dividend transaction and the security name is different.
        // In this case, we are unable to assign it and
        // therefore deny the transaction.
        //
        // Detected Security:
        // -----------------
        // Sep 25 settled Sep 29 Sold 14.0853 of QTIP - Mackenzie Financial Corp for 1521.49 CAD -14.0853 $108.02 -$1,521.49
        //
        // Transaction:
        // ------------
        // Jul 10 Mackenzie US TIPS Index ETF (CAD-Hedged): 03-JUL-20 (record date) 14.0853 shares – – $1.36
        // Jun 09 Mackenzie US TIPS Index ETF (CAD-Hedged): 02-JUN-20 (record date) 14.3972 shares – – $0.88
        // May 11 Mackenzie US TIPS Index ETF (CAD-Hedged): 04-MAY-20 (record date) 14.3972 shares – – $1.27
        // @formatter:on
        Block missingTickerSymbolforDividendeBlock = new Block("^[\\w]{3,4} [\\d]{2} [\\w]{5,}[\\W]{1,3}.*: .* \\(record date\\) .*$");
        type.addBlock(missingTickerSymbolforDividendeBlock);
        missingTickerSymbolforDividendeBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.DIVIDENDS);
                    return entry;
                })

                .section("month", "day", "name", "shares", "amount")
                .match("^(?<month>[\\w]{3,4}) "
                                + "(?<day>[\\d]{2}) "
                                + "(?<name>[\\w]{5,}[\\W]{1,3}.*): .* \\(record date\\) "
                                + "(?<shares>[\\.,\\d]+) "
                                + "shares [\\s\\–]{2,3} "
                                + "\\p{Sc}(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    v.put("currency", asCurrencyCode(context.get("currency")));

                    v.getTransactionContext().put(FAILURE, MessageFormat.format(Messages.MsgMissingTickerSymbol, trim(v.get("name"))));

                    t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                    t.setShares(asShares(v.get("shares")));

                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                })

                .wrap((t, ctx) -> {
                    TransactionItem item = new TransactionItem(t);
                    item.setFailureMessage(ctx.getString(FAILURE));
                    return item;
                }));

        // Dec 31 Gross management fee to Wealthsimple – – -$4.42
        // Dec 31 Promotions and discounts applied to Wealthsimple fee – – $0.26
        // Dec 31 Sales tax on management fee to Wealthsimple – – -$0.21
        Block feeBlock = new Block("^[\\w]{3,4} [\\d]{2} Gross management fee to Wealthsimple [\\W]{1,3} \\-\\p{Sc}[\\.,\\d]+$");
        type.addBlock(feeBlock);
        feeBlock.set(new Transaction<AccountTransaction>()

                .subject(() -> {
                    AccountTransaction entry = new AccountTransaction();
                    entry.setType(AccountTransaction.Type.FEES);
                    return entry;
                })

                .section("month", "day", "amount")
                .match("^(?<month>[\\w]{3,4}) (?<day>[\\d]{2}) Gross management fee to Wealthsimple [\\W]{1,3} \\-\\p{Sc}(?<amount>[\\.,\\d]+)$")
                .assign((t, v) -> {
                    DocumentContext context = type.getCurrentContext();

                    t.setDateTime(asDate(v.get("day") + " " + v.get("month") + " " + context.get("year")));
                    t.setAmount(asAmount(v.get("amount")));
                    t.setCurrencyCode(asCurrencyCode(context.get("currency")));
                    t.setNote("Management fee to Wealthsimple");

                    // Calculation of total fees:
                    // Fee = gross + taxes - fee refund
                    FeeRefundTransactionHelper feeRefundTransactionHelper = context.getType(FeeRefundTransactionHelper.class).orElseGet(FeeRefundTransactionHelper::new);
                    Optional<FeeRefundTransactionsItem> feeRefundTransaction = feeRefundTransactionHelper.findItem(t.getDateTime());
                    if (feeRefundTransaction.isPresent())
                    {
                        Money feeRefund = Money.of(t.getCurrencyCode(), feeRefundTransaction.get().feeRefund);
                        t.setMonetaryAmount(t.getMonetaryAmount().subtract(feeRefund));
                    }

                    FeeTaxTransactionHelper feeTaxTransactionHelper = context.getType(FeeTaxTransactionHelper.class).orElseGet(FeeTaxTransactionHelper::new);
                    Optional<FeeTaxTransactionsItem> feeTaxTransaction = feeTaxTransactionHelper.findItem(t.getDateTime());
                    if (feeTaxTransaction.isPresent())
                    {
                        Money tax = Money.of(t.getCurrencyCode(), feeTaxTransaction.get().tax);
                        t.setMonetaryAmount(t.getMonetaryAmount().add(tax));
                    }
                })

                .wrap(t -> {
                    type.getCurrentContext().removeType(FeeRefundTransactionsItem.class);
                    type.getCurrentContext().removeType(FeeTaxTransactionsItem.class);

                    return new TransactionItem(t);
                }));
    }

    private static class DividendTaxTransactionHelper
    {
        private List<DividendTaxTransactionsItem> items = new ArrayList<>();

        public Optional<DividendTaxTransactionsItem> findItem(LocalDateTime dateTime, String tickerSymbol)
        {
            // Search date of dividend transaction using date and tickerSymbol.

            for (DividendTaxTransactionsItem item : items)
            {
                if (item.dateTime.equals(dateTime) && item.tickerSymbol.equals(tickerSymbol))
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class DividendTaxTransactionsItem
    {
        LocalDateTime dateTime;
        String tickerSymbol;

        String currency;
        long tax;

        @Override
        public String toString()
        {
            return "DividendTaxTransactionsItem [dateTime=" + dateTime + ", tickerSymbol=" + tickerSymbol + ", currency="
                            + currency + ", tax=" + tax + "]";
        }
    }

    private static class FeeTaxTransactionHelper
    {
        private List<FeeTaxTransactionsItem> items = new ArrayList<>();

        public Optional<FeeTaxTransactionsItem> findItem(LocalDateTime dateTime)
        {
            // Search for date of fee tax by date.

            for (FeeTaxTransactionsItem item : items)
            {
                if (item.dateTime.equals(dateTime))
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class FeeTaxTransactionsItem
    {
        LocalDateTime dateTime;
        long tax;

        @Override
        public String toString()
        {
            return "FeeTaxTransactionsItem [dateTime=" + dateTime + ", tax=" + tax + "]";
        }
    }

    private static class FeeRefundTransactionHelper
    {
        private List<FeeRefundTransactionsItem> items = new ArrayList<>();

        public Optional<FeeRefundTransactionsItem> findItem(LocalDateTime dateTime)
        {
            // Search by date of fee refund by date.

            for (FeeRefundTransactionsItem item : items)
            {
                if (item.dateTime.equals(dateTime))
                    return Optional.of(item);
            }

            return Optional.empty();
        }
    }

    private static class FeeRefundTransactionsItem
    {
        LocalDateTime dateTime;
        long feeRefund;

        @Override
        public String toString()
        {
            return "FeeRefundTransactionsItem [dateTime=" + dateTime + ", feeRefund=" + feeRefund + "]";
        }
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
    protected BigDecimal asExchangeRate(String value)
    {
        return ExtractorUtils.convertToNumberBigDecimal(value, Values.Share, "en", "CA");
    }
}
