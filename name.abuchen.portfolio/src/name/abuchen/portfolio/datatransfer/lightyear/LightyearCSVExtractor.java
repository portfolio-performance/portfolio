package name.abuchen.portfolio.datatransfer.lightyear;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.SecurityCache;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class LightyearCSVExtractor implements Extractor
{
    private static final DateTimeFormatter DATE_FORMAT_SLASH = new DateTimeFormatterBuilder()
                    .appendPattern("dd/MM/yyyy").optionalStart().appendPattern("[' ']['T']HH:mm[:ss]").optionalEnd()
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0).parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0).toFormatter(Locale.ROOT);

    private static final DateTimeFormatter DATE_FORMAT_ISO = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd")
                    .optionalStart().appendPattern("[' ']['T']HH:mm[:ss]").optionalEnd()
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0).parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0).toFormatter(Locale.ROOT);

    /**
     * Lightyear money market funds (e.g. BlackRock ICS liquidity funds) often
     * omit the ISIN in distribution entries. This mapping provides known
     * fallback ISINs for these tickers.
     */
    private static final Map<String, String> TICKER_TO_ISIN = Map.of("BRICEKSP", "IE000GWTNRJ7", "ICSUSSDP",
                    "IE00B44BQ083");

    private static final Set<String> REQUIRED_HEADERS = Set.of("date", "type", "net amt.", "ccy", "reference");

    private Map<String, Integer> headerMap;

    /** Creates a new Lightyear CSV extractor for the given client. */
    public LightyearCSVExtractor(Client client)
    {
        // Keep the constructor consistent with other broker extractors.
    }

    @Override
    public String getLabel()
    {
        return Messages.LightyearCSVLabel;
    }

    /**
     * Extracts transactions from a Lightyear CSV file.
     *
     * @param securityCache
     *                          cache used to reuse and create securities
     * @param inputFile
     *                          CSV file to import
     * @param errors
     *                          receives file-level and row-level parsing errors
     * @return the transactions successfully extracted from the file
     */
    @Override
    public List<Item> extract(SecurityCache securityCache, InputFile inputFile, List<Exception> errors)
    {
        var items = new ArrayList<Item>();
        var source = inputFile.getName();

        try
        {
            var content = Files.readString(inputFile.getFile().toPath(), StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF"))
                content = content.substring(1);

            if (!tryParse(content, securityCache, source, items, errors))
            {
                errors.add(new IllegalArgumentException(
                                MessageFormat.format(Messages.LightyearCSVMsgFileNotSupported, source)));
            }
        }
        catch (IOException e)
        {
            errors.add(e);
        }

        return items;
    }

    /** Parses records using comma delimiter and processes them if headers match. */
    private boolean tryParse(String content, SecurityCache securityCache, String source, List<Item> items,
                    List<Exception> errors)
    {
        var format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true)
                        .setTrim(true).get();

        try (var reader = new StringReader(content); var parser = CSVParser.parse(reader, format))
        {
            var rawHeaders = parser.getHeaderMap();
            if (rawHeaders == null)
                return false;

            var normalizedHeaders = new HashMap<String, Integer>();
            rawHeaders.forEach((k, v) -> {
                if (k != null)
                    normalizedHeaders.put(k.trim().toLowerCase(Locale.ROOT), v);
            });

            if (!normalizedHeaders.keySet().containsAll(REQUIRED_HEADERS))
                return false;

            this.headerMap = normalizedHeaders;

            var records = parser.getRecords();
            var processedRecords = new HashSet<CSVRecord>();

            // Phase 1: Reconcile paired multi-currency conversions
            processConversions(records, processedRecords, source, items, errors);

            // Phase 2: Process individual records
            for (CSVRecord record : records)
            {
                if (processedRecords.contains(record))
                    continue;

                try
                {
                    processRecord(record, securityCache, source, items);
                }
                catch (Exception e)
                {
                    errors.add(new IllegalArgumentException(
                                    "Lightyear CSV line " + record.getRecordNumber() + ": " + e.getMessage(), e));
                }
            }
            return true;
        }
        catch (Exception e)
        {
            errors.add(e);
            return false;
        }
    }

    /**
     * Pairs conversion rows by reference and imports each complete pair
     * atomically. Invalid, incomplete, or unpaired conversion rows are reported
     * and marked as processed.
     *
     * @param records
     *                             all parsed CSV records
     * @param processedRecords
     *                             records handled here so they are not
     *                             processed as standalone transactions
     * @param source
     *                             source filename attached to imported items
     * @param items
     *                             receives complete transfer and fee items
     * @param errors
     *                             receives conversion-specific errors
     */
    private void processConversions(List<CSVRecord> records, Set<CSVRecord> processedRecords, String source,
                    List<Item> items, List<Exception> errors)
    {
        // Group "Conversion" records by Reference
        var conversionGroups = new HashMap<String, List<CSVRecord>>();
        for (CSVRecord record : records)
        {
            try
            {
                var type = getField(record, "Type");
                if ("Conversion".equalsIgnoreCase(type))
                {
                    var reference = getField(record, "Reference");
                    if (reference.isBlank())
                        throw new IllegalArgumentException("Missing conversion reference");
                    conversionGroups.computeIfAbsent(reference, key -> new ArrayList<>()).add(record);
                }
            }
            catch (Exception exception)
            {
                processedRecords.add(record);
                errors.add(new IllegalArgumentException(
                                "Lightyear CSV line " + record.getRecordNumber() + ": " + exception.getMessage(),
                                exception));
            }
        }

        for (var entryGroup : conversionGroups.entrySet())
        {
            var group = entryGroup.getValue();
            if (group.size() != 2)
            {
                for (CSVRecord record : group)
                {
                    processedRecords.add(record);
                    errors.add(new IllegalArgumentException("Lightyear CSV line " + record.getRecordNumber()
                                    + ": Expected two conversion rows for reference " + entryGroup.getKey()));
                }
                continue;
            }

            CSVRecord outRecord = null;
            CSVRecord inRecord = null;
            try
            {
                for (CSVRecord record : group)
                {
                    var grossAmountText = getField(record, "Gross Amount");
                    var netAmountText = getField(record, "Net Amt.");
                    if (grossAmountText.isBlank() || netAmountText.isBlank())
                        throw new IllegalArgumentException("Missing conversion amount");

                    var grossAmount = parseSignedDecimal(record, "Gross Amount", Values.Amount.factor());
                    var netAmount = parseSignedDecimal(record, "Net Amt.", Values.Amount.factor());
                    if (grossAmount == 0 || netAmount == 0)
                        throw new IllegalArgumentException("Zero conversion amount");

                    if (grossAmount < 0 || netAmount < 0)
                        outRecord = record;
                    else
                        inRecord = record;
                }

                if (outRecord == null || inRecord == null)
                    throw new IllegalArgumentException(
                                    "Conversion rows must contain one outgoing and one incoming leg");

                var outCurrency = getField(outRecord, "CCY");
                var inCurrency = getField(inRecord, "CCY");
                if (outCurrency.isBlank() || inCurrency.isBlank())
                    throw new IllegalArgumentException("Missing currency for conversion");

                var date = parseDate(getField(outRecord, "Date"));
                var outAmount = Math.abs(parseSignedDecimal(outRecord, "Net Amt.", Values.Amount.factor()));
                var inAmount = Math.abs(parseSignedDecimal(inRecord, "Net Amt.", Values.Amount.factor()));
                var exchangeRate = getField(inRecord, "FX Rate");
                if (!outCurrency.equals(inCurrency) && exchangeRate.isBlank())
                    throw new IllegalArgumentException(
                                    "Missing FX Rate for conversion from " + outCurrency + " to " + inCurrency);

                BigDecimal rate = null;
                if (!exchangeRate.isBlank())
                {
                    rate = new BigDecimal(exchangeRate.replace(',', '.'));
                    if (rate.compareTo(BigDecimal.ZERO) <= 0)
                        throw new IllegalArgumentException("Invalid FX Rate: " + exchangeRate);
                }

                var transferEntry = new AccountTransferEntry();
                transferEntry.getSourceTransaction().setCurrencyCode(outCurrency);
                transferEntry.getSourceTransaction().setAmount(outAmount);
                transferEntry.getSourceTransaction().setDateTime(date);
                transferEntry.getSourceTransaction().setSource(source);

                if (rate != null)
                {
                    transferEntry.getSourceTransaction().addUnit(new Unit(Unit.Type.GROSS_VALUE,
                                    Money.of(outCurrency, outAmount), Money.of(inCurrency, inAmount), rate));
                }

                transferEntry.getTargetTransaction().setCurrencyCode(inCurrency);
                transferEntry.getTargetTransaction().setAmount(inAmount);
                transferEntry.getTargetTransaction().setDateTime(parseDate(getField(inRecord, "Date")));
                transferEntry.getTargetTransaction().setSource(source);

                transferEntry.setDate(date);
                var reference = entryGroup.getKey();
                transferEntry.setNote("Currency conversion Ref: " + reference);

                var transferItem = new AccountTransferItem(transferEntry, true);

                // Validate and stage the fee before adding either conversion
                // item.
                var fee = Math.abs(parseSignedDecimal(outRecord, "Fee", Values.Amount.factor()));
                TransactionItem feeItem = null;
                if (fee > 0)
                {
                    var feeTransaction = new AccountTransaction();
                    feeTransaction.setType(AccountTransaction.Type.FEES);
                    feeTransaction.setDateTime(date);
                    feeTransaction.setCurrencyCode(outCurrency);
                    feeTransaction.setAmount(fee);
                    feeTransaction.setNote("Currency conversion fee Ref: " + reference);
                    feeTransaction.setSource(source);
                    feeItem = new TransactionItem(feeTransaction);
                }

                items.add(transferItem);
                if (feeItem != null)
                    items.add(feeItem);
            }
            catch (Exception exception)
            {
                errors.add(new IllegalArgumentException("Lightyear CSV lines " + group.get(0).getRecordNumber()
                                + " and " + group.get(1).getRecordNumber() + ": Failed to pair conversion for ref "
                                + entryGroup.getKey() + ": " + exception.getMessage(), exception));
            }
            finally
            {
                processedRecords.addAll(group);
            }
        }
    }

    /** Dispatches a record to the transaction processor for its Lightyear type. */
    private void processRecord(CSVRecord record, SecurityCache securityCache, String source, List<Item> items)
    {
        var type = getField(record, "Type");
        switch (type.toLowerCase(Locale.ROOT))
        {
            case "buy":
            case "sell":
                processBuySell(record, type, securityCache, source, items);
                break;
            case "dividend":
            case "distribution":
                processDividendOrDistribution(record, type, securityCache, source, items);
                break;
            case "deposit":
            case "withdrawal":
                processDepositWithdrawal(record, type, source, items);
                break;
            case "interest":
            case "reward":
                processInterestOrReward(record, type, source, items);
                break;
            default:
                throw new IllegalArgumentException(
                                MessageFormat.format(Messages.LightyearCSVMsgUnsupportedTransactionType, type));
        }
    }

    /** Validates and converts a buy or sell record, including fees and FX units. */
    private void processBuySell(CSVRecord record, String type, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        var isBuy = "buy".equalsIgnoreCase(type);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        if (ccy.isBlank())
            throw new IllegalArgumentException("Missing currency for " + type);

        var quantityStr = getField(record, "Quantity");
        if (quantityStr.isEmpty())
            throw new IllegalArgumentException("Missing quantity for " + type);
        var shares = Math.abs(parseSignedDecimal(record, "Quantity", Values.Share.factor()));
        if (shares == 0)
            throw new IllegalArgumentException("Zero quantity for " + type);

        var netAmtStr = getField(record, "Net Amt.");
        var grossAmtStr = getField(record, "Gross Amount");
        if (netAmtStr.isEmpty() && grossAmtStr.isEmpty())
            throw new IllegalArgumentException("Missing transaction amount for " + type);

        var fee = Math.abs(parseSignedDecimal(record, "Fee", Values.Amount.factor()));
        var netAmt = Math.abs(parseSignedDecimal(record, "Net Amt.", Values.Amount.factor()));
        var grossAmt = Math.abs(parseSignedDecimal(record, "Gross Amount", Values.Amount.factor()));

        long totalAmount;
        if (isBuy)
        {
            if (grossAmt > 0 && Math.abs(grossAmt - netAmt) == fee)
                totalAmount = Math.max(grossAmt, netAmt);
            else if (grossAmt == netAmt || grossAmt == 0)
                totalAmount = netAmt + fee;
            else
                totalAmount = Math.max(netAmt, grossAmt);
        }
        else
        {
            totalAmount = netAmt;
        }

        if (totalAmount == 0)
            throw new IllegalArgumentException("Zero total amount for " + type);

        // Lookup security only after validation succeeds
        var security = lookupSecurity(record, securityCache);

        var entry = new BuySellEntry();
        entry.setType(isBuy ? PortfolioTransaction.Type.BUY : PortfolioTransaction.Type.SELL);
        entry.setSecurity(security);
        entry.setDate(date);
        entry.setCurrencyCode(ccy);
        entry.setShares(shares);
        entry.setAmount(totalAmount);
        entry.setNote(getField(record, "Reference"));
        entry.setSource(source);

        if (fee > 0)
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(ccy, fee)));

        addFxUnitsIfNeeded(record, entry.getPortfolioTransaction(), security, ccy,
                        entry.getPortfolioTransaction().getGrossValue().getAmount());

        items.add(new BuySellEntryItem(entry));
    }

    /**
     * Validates and converts a dividend or fund distribution, including its
     * deductions and forex gross value.
     *
     * @param record
     *                          dividend or distribution CSV record
     * @param type
     *                          transaction type from the record
     * @param securityCache
     *                          cache used to resolve the underlying security
     *                          after validation
     * @param source
     *                          source filename attached to the transaction
     * @param items
     *                          receives the extracted dividend
     */
    private void processDividendOrDistribution(CSVRecord record, String type, SecurityCache securityCache,
                    String source, List<Item> items)
    {
        var isDistribution = "distribution".equalsIgnoreCase(type);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        if (ccy.isBlank())
            throw new IllegalArgumentException("Missing currency for " + type);

        var netAmtStr = getField(record, "Net Amt.");
        var grossAmtStr = getField(record, "Gross Amount");
        if (netAmtStr.isEmpty() && grossAmtStr.isEmpty())
            throw new IllegalArgumentException("Missing amount for " + type);

        var netAmount = Math.abs(parseSignedDecimal(record, "Net Amt.", Values.Amount.factor()));
        var grossAmount = Math.abs(parseSignedDecimal(record, "Gross Amount", Values.Amount.factor()));
        var shares = Math.abs(parseSignedDecimal(record, "Quantity", Values.Share.factor()));
        var reference = getField(record, "Reference");

        long tax;
        long fee;

        if (isDistribution)
        {
            // Broker quirk: Lightyear distribution exports invert taxes into
            // Fee column
            tax = Math.abs(parseSignedDecimal(record, "Fee", Values.Amount.factor()));
            fee = Math.abs(parseSignedDecimal(record, "Tax Amt.", Values.Amount.factor()));
        }
        else
        {
            tax = Math.abs(parseSignedDecimal(record, "Tax Amt.", Values.Amount.factor()));
            fee = Math.abs(parseSignedDecimal(record, "Fee", Values.Amount.factor()));
        }

        // Allow zero net amount only when deductions account for the full gross
        if (netAmount == 0 && grossAmount > 0 && (tax + fee) != grossAmount)
            throw new IllegalArgumentException("Zero net amount does not reconcile with gross for " + type);
        if (netAmount == 0 && grossAmount == 0)
            throw new IllegalArgumentException("Zero amount for " + type);

        // Lookup security only after validation succeeds
        var security = lookupSecurity(record, securityCache);

        var t = new AccountTransaction();
        t.setType(AccountTransaction.Type.DIVIDENDS);
        t.setSecurity(security);
        t.setDateTime(date);
        t.setCurrencyCode(ccy);
        t.setShares(shares);
        t.setAmount(netAmount);
        t.setNote(isDistribution ? "Distribution " + getField(record, "Ticker") + " REF: " + reference : reference);
        t.setSource(source);

        if (tax > 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(ccy, tax)));
        if (fee > 0)
            t.addUnit(new Unit(Unit.Type.FEE, Money.of(ccy, fee)));

        addFxUnitsIfNeeded(record, t, security, ccy, t.getGrossValue().getAmount());

        items.add(new TransactionItem(t));
    }

    /** Validates and converts a deposit or withdrawal record. */
    private void processDepositWithdrawal(CSVRecord record, String type, String source, List<Item> items)
    {
        var isDeposit = "deposit".equalsIgnoreCase(type);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        if (ccy.isBlank())
            throw new IllegalArgumentException("Missing currency for " + type);

        var netAmtStr = getField(record, "Net Amt.");
        if (netAmtStr.isEmpty())
            throw new IllegalArgumentException("Missing amount for " + type);
        var amount = Math.abs(parseSignedDecimal(record, "Net Amt.", Values.Amount.factor()));
        if (amount == 0)
            throw new IllegalArgumentException("Zero amount for " + type);

        var t = new AccountTransaction();
        t.setType(isDeposit ? AccountTransaction.Type.DEPOSIT : AccountTransaction.Type.REMOVAL);
        t.setDateTime(date);
        t.setCurrencyCode(ccy);
        t.setAmount(amount);
        t.setNote(getField(record, "Reference"));
        t.setSource(source);

        items.add(new TransactionItem(t));
    }

    /** Validates and converts an interest or reward record, including tax withholding. */
    private void processInterestOrReward(CSVRecord record, String type, String source, List<Item> items)
    {
        var isInterest = "interest".equalsIgnoreCase(type);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        if (ccy.isBlank())
            throw new IllegalArgumentException("Missing currency for " + type);

        var netAmtStr = getField(record, "Net Amt.");
        if (netAmtStr.isEmpty())
            throw new IllegalArgumentException("Missing amount for " + type);
        var netAmount = Math.abs(parseSignedDecimal(record, "Net Amt.", Values.Amount.factor()));
        if (netAmount == 0)
            throw new IllegalArgumentException("Zero amount for " + type);

        var tax = Math.abs(parseSignedDecimal(record, "Tax Amt.", Values.Amount.factor()));
        var reference = getField(record, "Reference");

        var t = new AccountTransaction();
        t.setType(AccountTransaction.Type.INTEREST);
        t.setDateTime(date);
        t.setCurrencyCode(ccy);
        t.setAmount(netAmount);
        t.setNote((isInterest ? "Interest " : "Reward ") + reference);
        t.setSource(source);

        if (tax > 0)
            t.addUnit(new Unit(Unit.Type.TAX, Money.of(ccy, tax)));

        items.add(new TransactionItem(t));
    }

    /**
     * Adds the forex gross-value unit required when a transaction and its
     * security use different currencies.
     *
     * @param record
     *                                CSV record containing the exchange rate
     * @param t
     *                                transaction receiving the unit
     * @param security
     *                                traded security whose currency is the
     *                                forex currency
     * @param transactionCurrency
     *                                currency in which the transaction is
     *                                recorded
     * @param grossAmount
     *                                gross amount before fees or taxes
     */
    private void addFxUnitsIfNeeded(CSVRecord record, Transaction t, Security security, String transactionCurrency,
                    long grossAmount)
    {
        var securityCurrency = security.getCurrencyCode();
        if (securityCurrency == null || securityCurrency.equals(transactionCurrency))
            return;

        var fxRateStr = getField(record, "FX Rate");
        if (fxRateStr.isEmpty())
            throw new IllegalArgumentException("Missing FX Rate for transaction currency " + transactionCurrency
                            + " and security currency " + securityCurrency
                            + "; provide an FX Rate or correct the security currency");

        try
        {
            var rate = new BigDecimal(fxRateStr.replace(',', '.'));
            if (rate.compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Invalid FX Rate: " + fxRateStr);

            var fxGrossAmount = new BigDecimal(grossAmount).multiply(rate).setScale(0, RoundingMode.HALF_UP)
                            .longValue();
            var invertedRate = BigDecimal.ONE.divide(rate, 10, RoundingMode.HALF_DOWN);
            t.addUnit(new Unit(Unit.Type.GROSS_VALUE, //
                            Money.of(transactionCurrency, grossAmount), //
                            Money.of(securityCurrency, fxGrossAmount), //
                            invertedRate));
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid FX Rate: " + fxRateStr, e);
        }
    }

    /** Resolves a security by ISIN/ticker and creates it using row currency if necessary. */
    private Security lookupSecurity(CSVRecord record, SecurityCache securityCache)
    {
        var isin = getField(record, "ISIN");
        var ticker = getField(record, "Ticker");

        if (isin.isEmpty() && !ticker.isEmpty() && TICKER_TO_ISIN.containsKey(ticker))
            isin = TICKER_TO_ISIN.get(ticker);

        var name = getField(record, "Instrument");
        if (name.isEmpty())
            name = getField(record, "Name");
        if (name.isEmpty())
            name = !ticker.isEmpty() ? ticker : isin;

        if (name.isEmpty())
            throw new IllegalArgumentException("Missing security name, ISIN, and ticker");

        var ccy = getField(record, "CCY");

        final var finalIsin = isin.isEmpty() ? null : isin;
        final var finalTicker = ticker.isEmpty() ? null : ticker;
        final var finalName = name;
        final var finalCcy = ccy;

        return securityCache.lookup(finalIsin, finalTicker, null, finalName, () -> {
            var s = new Security(finalName, finalCcy);
            if (finalIsin != null)
                s.setIsin(finalIsin);
            if (finalTicker != null)
                s.setTickerSymbol(finalTicker);
            return s;
        });
    }

    /** Parses a date/timestamp in slash-separated, ISO, or offset date-time format. */
    private LocalDateTime parseDate(String dateStr)
    {
        if (dateStr == null || dateStr.isBlank())
            throw new IllegalArgumentException("Missing date");

        var trimmed = dateStr.trim();
        if (trimmed.endsWith("Z") || trimmed.contains("+") || (trimmed.lastIndexOf('-') > 10))
        {
            try
            {
                return java.time.OffsetDateTime.parse(trimmed).toLocalDateTime();
            }
            catch (Exception ignored)
            {
                // Fall back to pattern parsing
            }
        }

        if (trimmed.contains("/"))
            return LocalDateTime.parse(trimmed, DATE_FORMAT_SLASH);

        if (trimmed.length() > 19 && (trimmed.charAt(10) == ' ' || trimmed.charAt(10) == 'T'))
        {
            try
            {
                return LocalDateTime.parse(trimmed.substring(0, 19).replace('T', ' '),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
            catch (Exception ignored)
            {
                // Fall back
            }
        }

        return LocalDateTime.parse(trimmed, DATE_FORMAT_ISO);
    }

    /** Retrieves trimmed CSV value by case-insensitive column name. */
    private String getField(CSVRecord record, String column)
    {
        var index = headerMap != null ? headerMap.get(column.toLowerCase(Locale.ROOT)) : null;
        if (index == null || index.intValue() >= record.size())
            return "";
        var val = record.get(index.intValue());
        return val != null ? val.trim() : "";
    }

    /** Parses signed decimal field and converts it to the model's integer scale. */
    private long parseSignedDecimal(CSVRecord record, String column, long factor)
    {
        var value = getField(record, column);
        if (value.isEmpty())
            return 0;
        return new BigDecimal(value.replace(',', '.')).multiply(BigDecimal.valueOf(factor))
                        .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
