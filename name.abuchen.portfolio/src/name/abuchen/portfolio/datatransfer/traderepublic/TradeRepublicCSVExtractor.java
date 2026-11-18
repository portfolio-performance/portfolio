package name.abuchen.portfolio.datatransfer.traderepublic;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class TradeRepublicCSVExtractor implements Extractor
{
    private final Client client;

    private static final String CATEGORY_TRADING = "TRADING";
    private static final String CATEGORY_CASH = "CASH";
    private static final String CATEGORY_CORPORATE_ACTION = "CORPORATE_ACTION";

    public TradeRepublicCSVExtractor(Client client)
    {
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }

    @Override
    public String getLabel()
    {
        return Messages.TradeRepublicCSVLabel;
    }

    @Override
    public void postProcessing(List<Item> items)
    {
        CashDividendCorrectionMatcher.reconcile(items);
    }

    @Override
    public List<Item> extract(SecurityCache securityCache, InputFile inputFile, List<Exception> errors)
    {
        var items = new ArrayList<Item>();
        var source = inputFile.getName();

        try
        {
            var content = Files.readString(inputFile.getFile().toPath(), StandardCharsets.UTF_8);

            // a byte order mark (e.g. after saving the file with an editor)
            // would otherwise become part of the first column name
            if (content.startsWith("\uFEFF"))
                content = content.substring(1);

            // try comma first, fall back to semicolon
            if (!tryParse(content, ',', securityCache, source, items, errors)
                            && !tryParse(content, ';', securityCache, source, items, errors))
            {
                errors.add(new IllegalArgumentException(
                                MessageFormat.format(Messages.TradeRepublicCSVMsgFileNotSupported, source)));
            }
        }
        catch (IOException e)
        {
            errors.add(e);
        }

        return items;
    }

    private boolean tryParse(String content, char delimiter, SecurityCache securityCache, String source,
                    List<Item> items, List<Exception> errors)
    {
        var format = CSVFormat.DEFAULT.builder() //
                        .setDelimiter(delimiter) //
                        .setHeader() //
                        .setSkipHeaderRecord(true) //
                        .get();

        try (var reader = new StringReader(content); var parser = CSVParser.parse(reader, format))
        {
            var headerMap = parser.getHeaderMap();
            if (!headerMap.containsKey("datetime") || !headerMap.containsKey("category")
                            || !headerMap.containsKey("type") || !headerMap.containsKey("amount")
                            || !headerMap.containsKey("currency"))
            {
                return false;
            }

            for (CSVRecord csvRecord : parser)
            {
                try
                {
                    processRecord(csvRecord, securityCache, source, items);
                }
                catch (Exception e)
                {
                    errors.add(e);
                }
            }
            return true;
        }
        catch (IOException e)
        {
            errors.add(e);
            return true;
        }
        catch (UncheckedIOException e)
        {
            // commons-csv reports malformed content (e.g. an unclosed quote)
            // while iterating over the records
            errors.add(e.getCause());
            return true;
        }
    }

    private void processRecord(CSVRecord csvRecord, SecurityCache securityCache, String source, List<Item> items)
    {
        var category = getField(csvRecord, "category");
        var type = getField(csvRecord, "type");

        switch (category)
        {
            case CATEGORY_TRADING:
                processTrading(csvRecord, type, securityCache, source, items);
                break;
            case CATEGORY_CASH:
                processCash(csvRecord, type, securityCache, source, items);
                break;
            case CATEGORY_CORPORATE_ACTION:
                processCorporateAction(csvRecord, type, securityCache, source, items);
                break;
            default:
                var item = createFailureItem(csvRecord, securityCache, source);
                item.setFailureMessage(MessageFormat.format(Messages.TradeRepublicCSVMsgUnsupportedTransactionType,
                                category, type));
                items.add(item);
                break;
        }
    }

    // -------------------------------------------------------
    // TRADING
    // -------------------------------------------------------

    private void processTrading(CSVRecord csvRecord, String type, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        switch (type)
        {
            case "BUY":
                processTradingBuySell(csvRecord, PortfolioTransaction.Type.BUY, securityCache, source, items);
                break;
            case "SELL":
                processTradingBuySell(csvRecord, PortfolioTransaction.Type.SELL, securityCache, source, items);
                break;
            default:
                var item = createFailureItem(csvRecord, securityCache, source);
                item.setFailureMessage(MessageFormat.format(Messages.TradeRepublicCSVMsgUnsupportedTransactionType,
                                CATEGORY_TRADING, type));
                items.add(item);
                break;
        }
    }

    private void processTradingBuySell(CSVRecord csvRecord, PortfolioTransaction.Type txType,
                    SecurityCache securityCache, String source, List<Item> items)
    {
        var security = lookupSecurity(csvRecord, securityCache);
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var shares = parseShares(csvRecord);
        var fee = parseFee(csvRecord);
        var tax = parseTax(csvRecord);
        var proceeds = Math.abs(parseAmount(csvRecord));

        // PP cannot represent a SELL where fees and taxes reach or exceed the
        // proceeds. Split into a SELL without fees/taxes and separate
        // FEES and TAXES account transactions.
        if (txType == PortfolioTransaction.Type.SELL && fee + tax > 0 && fee + tax >= proceeds)
        {
            var entry = new BuySellEntry();
            entry.setType(txType);
            entry.setSecurity(security);
            entry.setDate(date);
            entry.setCurrencyCode(currency);
            entry.setShares(shares);
            entry.setAmount(proceeds);
            entry.setNote(getField(csvRecord, "description"));
            entry.setSource(source);

            addFxUnitsIfNeeded(csvRecord, entry.getPortfolioTransaction(), security, currency);

            items.add(new BuySellEntryItem(entry));

            if (fee > 0)
                items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.FEES, fee, security, shares,
                                source));

            if (tax > 0)
                items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.TAXES, tax, security, shares,
                                source));

            addRefunds(csvRecord, security, shares, source, items);
            return;
        }

        var totalAmount = computeTotalAmount(csvRecord);

        var entry = new BuySellEntry();
        entry.setType(txType);
        entry.setSecurity(security);
        entry.setDate(date);
        entry.setCurrencyCode(currency);
        entry.setShares(shares);
        entry.setAmount(totalAmount);
        entry.setNote(getField(csvRecord, "description"));
        entry.setSource(source);

        if (fee > 0)
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(currency, fee)));

        if (tax > 0)
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX, Money.of(currency, tax)));

        addFxUnitsIfNeeded(csvRecord, entry.getPortfolioTransaction(), security, currency);

        var item = new BuySellEntryItem(entry);

        // PP does not allow purchases with a zero amount
        if (txType == PortfolioTransaction.Type.BUY && totalAmount == 0)
            item.setFailureMessage(Messages.MsgErrorTransactionTypeNotSupportedOrRequired);

        items.add(item);

        addRefunds(csvRecord, security, shares, source, items);
    }

    // -------------------------------------------------------
    // CASH
    // -------------------------------------------------------

    private void processCash(CSVRecord csvRecord, String type, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        switch (type)
        {
            case "DIVIDEND":
            case "DISTRIBUTION":
            case "DIVIDEND_EQUIVALENT_PAYMENT":
                processCashDividend(csvRecord, securityCache, source, items);
                break;
            case "INTEREST_PAYMENT":
                processCashInterest(csvRecord, securityCache, source, items);
                break;
            case "CUSTOMER_INBOUND":
            case "CUSTOMER_INPAYMENT":
            case "TRANSFER_INBOUND":
            case "TRANSFER_INSTANT_INBOUND":
                processCashDeposit(csvRecord, securityCache, source, items);
                break;
            case "CUSTOMER_OUTBOUND_REQUEST":
                processCashRemoval(csvRecord, securityCache, source, items);
                break;
            case "FINAL_MATURITY":
            case "TILG":
                processTradingBuySell(csvRecord, PortfolioTransaction.Type.SELL, securityCache, source, items);
                break;
            case "EARNINGS":
                processCashEarnings(csvRecord, securityCache, source, items);
                break;
            case "TAX_OPTIMIZATION":
                processCashTaxOptimization(csvRecord, securityCache, source, items);
                break;
            case "PRIVATE_MARKET_BUY":
                processTradingBuySell(csvRecord, PortfolioTransaction.Type.BUY, securityCache, source, items);
                break;
            case "PRIVATE_MARKET_SELL":
                processTradingBuySell(csvRecord, PortfolioTransaction.Type.SELL, securityCache, source, items);
                break;
            case "PRE_DETERMINED_TAX_BASE":
            case "SEC_ACCOUNT":
                processCashTaxes(csvRecord, securityCache, source, items);
                break;
            case "ADDITIONAL_INCOME":
            case "BENEFITS_SAVEBACK":
            case "BOND_ACCRUED_INTEREST":
            case "BONUS":
            case "CAPITAL_INC_CASH":
            case "CARD_TRANSACTION":
            case "CARD_TRANSACTION_INTERNATIONAL":
            case "CARD_TRANSACTION_REFUND":
            case "CARD_TRANSACTION_REFUND_REVERSAL":
            case "CFD_ORDER":
            case "CITI_TRADING":
            case "COMPENSATION":
            case "CRYPTO_TRADING":
            case "CUSTOMER_INPAYMENT_REVERSAL":
            case "EARLY_REDEMPTION":
            case "EXCHANGE":
            case "GENERAL_INBOUND":
            case "INSTRUCTION":
            case "KINDERGELD_BONUS":
            case "LIQUIDATION_PROCEEDS":
            case "MANUAL":
            case "MANUAL_CASH_TRANSFER":
            case "MERGER":
            case "OFFER":
            case "OTHER_EXECUTION":
            case "PAYMENT":
            case "PEA_MARKETING":
            case "REFERRAL":
            case "STOCKPERK":
            case "TRADING_CITI":
            case "TRANSFER_DIRECT_DEBIT_INBOUND":
            case "TRANSFER_IN":
            case "TRANSFER_INSTANT_OUTBOUND":
            case "TRANSFER_OUT":
            case "TRANSFER_OUTBOUND":
            case "VIBAN_TRANSFER_INBOUND":
            case "VIBAN_TRANSFER_OUTBOUND":
            case "GIFT":
                processCashBySign(csvRecord, AccountTransaction.Type.DEPOSIT, AccountTransaction.Type.REMOVAL,
                                securityCache, source, items);
                break;
            case "FEE":
            case "CARD_ORDERING_FEE":
                processCashBySign(csvRecord, AccountTransaction.Type.FEES_REFUND, AccountTransaction.Type.FEES,
                                securityCache, source, items);
                break;
            case "TAX":
                processCashBySign(csvRecord, AccountTransaction.Type.TAX_REFUND, AccountTransaction.Type.TAXES,
                                securityCache, source, items);
                break;
            default:
                var item = createFailureItem(csvRecord, securityCache, source);
                item.setFailureMessage(MessageFormat.format(
                                Messages.TradeRepublicCSVMsgUnsupportedTransactionType, CATEGORY_CASH, type));
                items.add(item);
                break;
        }
    }

    private void processCashDividend(CSVRecord csvRecord, SecurityCache securityCache, String source, List<Item> items)
    {
        if (parseAmount(csvRecord) < 0)
        {
            processCashDividendCorrection(csvRecord, securityCache, source, items);
            return;
        }

        if (isZero(csvRecord))
        {
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        var security = lookupSecurity(csvRecord, securityCache);
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var shares = parseShares(csvRecord);
        var totalAmount = computeTotalAmount(csvRecord);
        var tax = parseTax(csvRecord);
        var fee = parseFee(csvRecord);

        var t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setShares(shares);
        t.setAmount(totalAmount);
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        if (tax > 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, tax)));

        if (fee > 0)
            t.addUnit(new Unit(Unit.Type.FEE, Money.of(currency, fee)));

        addFxUnitsIfNeeded(csvRecord, t, security, currency);

        items.add(new TransactionItem(t));

        addRefunds(csvRecord, security, shares, source, items);
    }

    // Emits a failure DIVIDENDS item for a cash-dividend correction row
    // (negative amount). Values are stored with their CSV signs preserved
    // on the transaction (amount signed, tax/fee Units signed, GROSS_VALUE
    // Unit signed) so that CashDividendCorrectionMatcher can locate the
    // matching original — if any — by inverting every sign. If no match is
    // found, the failure message asks the user to delete the original
    // dividend they imported previously.
    private void processCashDividendCorrection(CSVRecord csvRecord, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        var security = lookupSecurity(csvRecord, securityCache);
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var shares = parseShares(csvRecord);

        long amount = parseAmount(csvRecord);
        long feeRaw = parseFeeRaw(csvRecord);
        long taxRaw = parseTaxRaw(csvRecord);
        long signedNet = amount + feeRaw + taxRaw;

        var t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setShares(shares);
        t.setAmount(signedNet);
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        if (taxRaw != 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, -taxRaw)));

        if (feeRaw != 0)
            t.addUnit(new Unit(Unit.Type.FEE, Money.of(currency, -feeRaw)));

        addSignedFxUnitsIfNeeded(csvRecord, t, security, currency);

        var item = new TransactionItem(t);
        item.setFailureMessage(Messages.MsgErrorCashDividendCorrectionUnmatched);
        items.add(item);
    }

    private void processCashInterest(CSVRecord csvRecord, SecurityCache securityCache, String source, List<Item> items)
    {
        if (isZero(csvRecord))
        {
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var tax = parseTax(csvRecord);
        var fee = parseFee(csvRecord);

        // bond interest (has security) → DIVIDENDS; cash interest → INTEREST
        var name = getField(csvRecord, "name");
        var hasSecurity = !name.isEmpty();

        var t = new AccountTransaction();
        t.setType(hasSecurity ? AccountTransaction.Type.DIVIDENDS : AccountTransaction.Type.INTEREST);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        if (tax > 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, tax)));

        Security security = null;
        long shares = 0;

        if (hasSecurity)
        {
            security = lookupSecurity(csvRecord, securityCache);
            shares = parseShares(csvRecord);

            t.setSecurity(security);
            t.setShares(shares);
            t.setAmount(computeTotalAmount(csvRecord));

            if (fee > 0)
                t.addUnit(new Unit(Unit.Type.FEE, Money.of(currency, fee)));

            addFxUnitsIfNeeded(csvRecord, t, security, currency);
        }
        else
        {
            // PP does not allow fees on interest. Book the interest without
            // the fee and the fee as separate FEES transaction.
            t.setAmount(computeTotalAmount(csvRecord) + fee);
        }

        items.add(new TransactionItem(t));

        if (!hasSecurity && fee > 0)
            items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.FEES, fee, null, 0, source));

        addRefunds(csvRecord, security, shares, source, items);
    }

    private void processCashDeposit(CSVRecord csvRecord, SecurityCache securityCache, String source, List<Item> items)
    {
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var totalAmount = computeTotalAmount(csvRecord);
        var fee = parseFee(csvRecord);

        if (totalAmount + fee == 0)
        {
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        // PP cannot represent a DEPOSIT with fees (e.g. card top-ups). Book
        // the full deposit and a separate FEES account transaction.
        var t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DEPOSIT);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setAmount(totalAmount + fee);
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        items.add(new TransactionItem(t));

        if (fee > 0)
            items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.FEES, fee, null, 0, source));

        addRefunds(csvRecord, null, 0, source, items);
    }

    private void processCashRemoval(CSVRecord csvRecord, SecurityCache securityCache, String source, List<Item> items)
    {
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var totalAmount = computeTotalAmount(csvRecord);
        var fee = parseFee(csvRecord);

        if (totalAmount == 0)
        {
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        // same as for deposits: book the removal without the fee and the fee
        // as separate FEES account transaction
        var t = new AccountTransaction();
        t.setType(AccountTransaction.Type.REMOVAL);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setAmount(totalAmount - fee);
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        if (t.getAmount() > 0)
            items.add(new TransactionItem(t));

        if (fee > 0)
            items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.FEES, fee, null, 0, source));

        addRefunds(csvRecord, null, 0, source, items);
    }

    private void processCashEarnings(CSVRecord csvRecord, SecurityCache securityCache, String source, List<Item> items)
    {
        if (parseAmount(csvRecord) == 0 && parseTaxRaw(csvRecord) == 0)
        {
            // tax-free allowance covers it
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        var security = lookupSecurity(csvRecord, securityCache);
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var shares = parseShares(csvRecord);

        var amount = parseAmount(csvRecord);
        var tax = parseTaxRaw(csvRecord);
        long total = amount + tax;

        if (amount == 0 || total < 0)
        {
            // Vorabpauschale: tax charge (or its refund)
            var t = new AccountTransaction();
            t.setType(total > 0 ? AccountTransaction.Type.TAX_REFUND : AccountTransaction.Type.TAXES);
            t.setSecurity(security);
            t.setDateTime(date);
            t.setCurrencyCode(currency);
            t.setShares(shares);
            t.setAmount(Math.abs(total));
            t.setNote(getField(csvRecord, "description"));
            t.setSource(source);

            items.add(new TransactionItem(t));
        }
        else
        {
            // actual earnings
            var t = new AccountTransaction();
            t.setType(AccountTransaction.Type.DIVIDENDS);
            t.setSecurity(security);
            t.setDateTime(date);
            t.setCurrencyCode(currency);
            t.setShares(shares);
            t.setAmount(computeTotalAmount(csvRecord));
            t.setNote(getField(csvRecord, "description"));
            t.setSource(source);

            if (parseTax(csvRecord) > 0)
                t.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, parseTax(csvRecord))));

            if (parseFee(csvRecord) > 0)
                t.addUnit(new Unit(Unit.Type.FEE, Money.of(currency, parseFee(csvRecord))));

            addFxUnitsIfNeeded(csvRecord, t, security, currency);

            items.add(new TransactionItem(t));

            addRefunds(csvRecord, security, shares, source, items);
        }
    }

    private void processCashTaxOptimization(CSVRecord csvRecord, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var rawTax = parseTaxRaw(csvRecord);

        if (rawTax == 0)
        {
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        var t = new AccountTransaction();
        t.setType(rawTax > 0 ? AccountTransaction.Type.TAX_REFUND : AccountTransaction.Type.TAXES);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setAmount(Math.abs(rawTax));
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        items.add(new TransactionItem(t));
    }

    private void processCashTaxes(CSVRecord csvRecord, SecurityCache securityCache, String source, List<Item> items)
    {
        var rawTax = parseTaxRaw(csvRecord);

        if (rawTax == 0)
        {
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        var security = lookupSecurity(csvRecord, securityCache);
        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var shares = parseShares(csvRecord);

        var t = new AccountTransaction();
        t.setType(rawTax > 0 ? AccountTransaction.Type.TAX_REFUND : AccountTransaction.Type.TAXES);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setShares(shares);
        t.setAmount(Math.abs(rawTax));
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        items.add(new TransactionItem(t));
    }

    /**
     * Books the amount by its sign as positive or negative type. Fees and taxes
     * in their own columns become separate transactions, as PP does not allow
     * them as units of these types.
     */
    private void processCashBySign(CSVRecord csvRecord, AccountTransaction.Type positiveType,
                    AccountTransaction.Type negativeType, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        if (isZero(csvRecord))
        {
            items.add(createSkippedItem(csvRecord, securityCache, source,
                            Messages.MsgErrorTransactionTypeNotSupportedOrRequired));
            return;
        }

        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        var amount = parseAmount(csvRecord);

        if (amount != 0)
        {
            var t = new AccountTransaction();
            t.setType(amount > 0 ? positiveType : negativeType);
            t.setDateTime(date);
            t.setCurrencyCode(currency);
            t.setAmount(Math.abs(amount));
            t.setNote(getField(csvRecord, "description"));
            t.setSource(source);

            items.add(new TransactionItem(t));
        }

        var fee = parseFee(csvRecord);
        if (fee > 0)
            items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.FEES, fee, null, 0, source));

        var tax = parseTax(csvRecord);
        if (tax > 0)
            items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.TAXES, tax, null, 0, source));

        addRefunds(csvRecord, null, 0, source, items);
    }

    /**
     * Positive values in the fee or tax column are credits, e.g. a tax refund
     * settled together with the transaction. PP cannot store them as units,
     * therefore they are booked as separate FEES_REFUND and TAX_REFUND
     * transactions.
     */
    private void addRefunds(CSVRecord csvRecord, Security security, long shares, String source, List<Item> items)
    {
        var feeRaw = parseFeeRaw(csvRecord);
        if (feeRaw > 0)
            items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.FEES_REFUND, feeRaw, security,
                            shares, source));

        var taxRaw = parseTaxRaw(csvRecord);
        if (taxRaw > 0)
            items.add(createSeparateTransaction(csvRecord, AccountTransaction.Type.TAX_REFUND, taxRaw, security,
                            shares, source));
    }

    private Item createSeparateTransaction(CSVRecord csvRecord, AccountTransaction.Type type, long amount,
                    Security security, long shares, String source)
    {
        var currency = getField(csvRecord, "currency");

        var t = new AccountTransaction();
        t.setType(type);
        t.setDateTime(parseDate(csvRecord));
        t.setCurrencyCode(currency);
        t.setAmount(amount);
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        if (security != null)
        {
            var securityCurrency = security.getCurrencyCode();
            var fxRateStr = getField(csvRecord, "fx_rate");

            if (securityCurrency == null || securityCurrency.equals(currency))
            {
                t.setSecurity(security);
                t.setShares(shares);
            }
            else if (!fxRateStr.isEmpty())
            {
                // a security in a foreign currency requires a gross value unit
                var rate = new BigDecimal(fxRateStr);
                var forex = BigDecimal.valueOf(amount).divide(rate, 0, RoundingMode.HALF_UP).longValue();

                t.setSecurity(security);
                t.setShares(shares);
                t.addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(currency, amount),
                                Money.of(securityCurrency, forex), rate));
            }
        }

        return new TransactionItem(t);
    }

    // -------------------------------------------------------
    // CORPORATE_ACTION
    // -------------------------------------------------------

    private void processCorporateAction(CSVRecord csvRecord, String type, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        switch (type)
        {
            case "REDEMPTION":
            case "FINAL_MATURITY":
            case "WARRANT_EXERCISE":
                // informational — the financial transaction is captured by the
                // corresponding CASH entry
                var skippedItem = createSkippedItem(csvRecord, securityCache, source);
                items.add(skippedItem);
                break;
            case "SPLIT":
                var splitItem = createFailureItem(csvRecord, securityCache, source);
                splitItem.setFailureMessage(Messages.MsgErrorTransactionSplitUnsupported);
                items.add(splitItem);
                break;
            default:
                var failureItem = createFailureItem(csvRecord, securityCache, source);
                failureItem.setFailureMessage(MessageFormat.format(
                                Messages.TradeRepublicCSVMsgUnsupportedTransactionType, CATEGORY_CORPORATE_ACTION,
                                type));
                items.add(failureItem);
                break;
        }
    }

    // -------------------------------------------------------
    // Security lookup
    // -------------------------------------------------------

    private Security lookupSecurity(CSVRecord csvRecord, SecurityCache securityCache)
    {
        var assetClass = getField(csvRecord, "asset_class");
        var symbol = getField(csvRecord, "symbol");
        var name = getField(csvRecord, "name");
        var originalCurrency = getField(csvRecord, "original_currency");
        var currency = getField(csvRecord, "currency");

        String isin;
        String ticker;

        if ("CRYPTO".equals(assetClass))
        {
            isin = null;
            ticker = symbol.isEmpty() ? null : symbol;
        }
        else
        {
            isin = symbol.isEmpty() ? null : symbol;
            ticker = null;
        }

        var securityCurrency = !originalCurrency.isEmpty() ? originalCurrency
                        : (!currency.isEmpty() ? currency : CurrencyUnit.EUR);

        return securityCache.lookup(isin, ticker, null, name, () -> {
            var s = new Security(name, securityCurrency);
            if (isin != null)
                s.setIsin(isin);
            if (ticker != null)
                s.setTickerSymbol(ticker);
            return s;
        });
    }

    // -------------------------------------------------------
    // FX handling
    // -------------------------------------------------------

    private void addFxUnitsIfNeeded(CSVRecord csvRecord, name.abuchen.portfolio.model.Transaction t, Security security,
                    String accountCurrency)
    {
        var securityCurrency = security.getCurrencyCode();
        if (securityCurrency == null || securityCurrency.equals(accountCurrency))
            return;

        var originalAmountStr = getField(csvRecord, "original_amount");
        var originalCurrency = getField(csvRecord, "original_currency");
        var fxRateStr = getField(csvRecord, "fx_rate");

        if (originalAmountStr.isEmpty() || originalCurrency.isEmpty() || fxRateStr.isEmpty())
            return;

        long fxAmount = Math.abs(Math.round(Double.parseDouble(originalAmountStr) * Values.Amount.factor()));
        long amount = Math.abs(parseAmount(csvRecord));
        var rate = new BigDecimal(fxRateStr);

        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of(accountCurrency, amount), //
                        Money.of(originalCurrency, fxAmount), //
                        rate));
    }

    private void addSignedFxUnitsIfNeeded(CSVRecord csvRecord, name.abuchen.portfolio.model.Transaction t,
                    Security security, String accountCurrency)
    {
        var securityCurrency = security.getCurrencyCode();
        if (securityCurrency == null || securityCurrency.equals(accountCurrency))
            return;

        var originalAmountStr = getField(csvRecord, "original_amount");
        var originalCurrency = getField(csvRecord, "original_currency");
        var fxRateStr = getField(csvRecord, "fx_rate");

        if (originalAmountStr.isEmpty() || originalCurrency.isEmpty() || fxRateStr.isEmpty())
            return;

        long fxAmount = Math.round(Double.parseDouble(originalAmountStr) * Values.Amount.factor());
        long amount = parseAmount(csvRecord);
        var rate = new BigDecimal(fxRateStr);

        t.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                        Money.of(accountCurrency, amount), //
                        Money.of(originalCurrency, fxAmount), //
                        rate));
    }

    // -------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------

    private LocalDateTime parseDate(CSVRecord csvRecord)
    {
        var datetimeStr = getField(csvRecord, "datetime");
        return Instant.parse(datetimeStr).atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    private long parseShares(CSVRecord csvRecord)
    {
        var sharesStr = getField(csvRecord, "shares");
        if (sharesStr.isEmpty())
            return 0;
        return Math.abs(Math.round(Double.parseDouble(sharesStr) * Values.Share.factor()));
    }

    private long parseAmount(CSVRecord csvRecord)
    {
        var amountStr = getField(csvRecord, "amount");
        if (amountStr.isEmpty())
            return 0;
        return Math.round(Double.parseDouble(amountStr) * Values.Amount.factor());
    }

    /**
     * Returns the fee charged (negative values in the fee column). Credits are
     * booked separately, see {@link #addRefunds}.
     */
    private long parseFee(CSVRecord csvRecord)
    {
        return Math.max(-parseFeeRaw(csvRecord), 0);
    }

    /**
     * Returns the tax withheld (negative values in the tax column). Credits are
     * booked separately, see {@link #addRefunds}.
     */
    private long parseTax(CSVRecord csvRecord)
    {
        return Math.max(-parseTaxRaw(csvRecord), 0);
    }

    private long parseTaxRaw(CSVRecord csvRecord)
    {
        var taxStr = getField(csvRecord, "tax");
        if (taxStr.isEmpty())
            return 0;
        return Math.round(Double.parseDouble(taxStr) * Values.Amount.factor());
    }

    /**
     * Computes the total amount as abs(amount + fee + tax). All three columns
     * are separate additive components of the net cash effect. Positive fees
     * and taxes (credits) are not included because they are booked as separate
     * transactions.
     */
    private long computeTotalAmount(CSVRecord csvRecord)
    {
        long amount = parseAmount(csvRecord);
        long fee = Math.min(parseFeeRaw(csvRecord), 0);
        long tax = Math.min(parseTaxRaw(csvRecord), 0);
        return Math.abs(amount + fee + tax);
    }

    private boolean isZero(CSVRecord csvRecord)
    {
        return parseAmount(csvRecord) == 0 && parseFeeRaw(csvRecord) == 0 && parseTaxRaw(csvRecord) == 0;
    }

    private long parseFeeRaw(CSVRecord csvRecord)
    {
        var feeStr = getField(csvRecord, "fee");
        if (feeStr.isEmpty())
            return 0;
        return Math.round(Double.parseDouble(feeStr) * Values.Amount.factor());
    }

    private String getField(CSVRecord csvRecord, String column)
    {
        if (!csvRecord.isSet(column))
            return "";
        var value = csvRecord.get(column);
        return value == null ? "" : value.trim();
    }

    // -------------------------------------------------------
    // Item creation helpers
    // -------------------------------------------------------

    private Item createFailureItem(CSVRecord csvRecord, SecurityCache securityCache, String source)
    {
        var category = getField(csvRecord, "category");
        var type = getField(csvRecord, "type");
        var csvTypeInfo = category + " / " + type;

        var date = parseDate(csvRecord);
        var currency = getField(csvRecord, "currency");
        if (currency.isEmpty())
            currency = CurrencyUnit.EUR;
        var amount = parseAmount(csvRecord);

        var t = new AccountTransaction();
        t.setType(amount >= 0 ? AccountTransaction.Type.DEPOSIT : AccountTransaction.Type.REMOVAL);
        t.setDateTime(date);
        t.setCurrencyCode(currency);
        t.setAmount(Math.abs(amount));
        t.setNote(getField(csvRecord, "description"));
        t.setSource(source);

        var name = getField(csvRecord, "name");
        if (!name.isEmpty())
        {
            var security = lookupSecurity(csvRecord, securityCache);
            t.setSecurity(security);
            t.setShares(parseShares(csvRecord));
        }

        return new TransactionItem(t)
        {
            @Override
            public String getTypeInformation()
            {
                return csvTypeInfo;
            }
        };
    }

    private Item createSkippedItem(CSVRecord csvRecord, SecurityCache securityCache, String source)
    {
        return createSkippedItem(csvRecord, securityCache, source,
                        Messages.TradeRepublicCSVMsgSkippedCoveredByOtherLines);
    }

    private Item createSkippedItem(CSVRecord csvRecord, SecurityCache securityCache, String source, String reason)
    {
        var item = createFailureItem(csvRecord, securityCache, source);
        return new SkippedItem(item, reason);
    }
}
