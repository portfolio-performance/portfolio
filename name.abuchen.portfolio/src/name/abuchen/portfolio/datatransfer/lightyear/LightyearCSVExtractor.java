package name.abuchen.portfolio.datatransfer.lightyear;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
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
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
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

    public LightyearCSVExtractor(Client client)
    {
        // Keep the constructor consistent with other broker extractors.
    }

    @Override
    public String getLabel()
    {
        return Messages.LightyearCSVLabel;
    }

    @Override
    public List<Item> extract(SecurityCache securityCache, InputFile inputFile, List<Exception> errors)
    {
        var items = new ArrayList<Item>();
        var source = inputFile.getName();

        try
        {
            var content = readContent(inputFile);

            // try comma first, fall back to semicolon
            if (!tryParse(content, ',', securityCache, source, items, errors)
                            && !tryParse(content, ';', securityCache, source, items, errors))
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

    private String readContent(InputFile inputFile) throws IOException
    {
        var bytes = Files.readAllBytes(inputFile.getFile().toPath());
        try
        {
            var content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            // Strip UTF-8 Byte Order Mark (BOM) if present
            return content.startsWith("\uFEFF") ? content.substring(1) : content;
        }
        catch (CharacterCodingException e)
        {
            return Charset.forName("windows-1252").decode(ByteBuffer.wrap(bytes)).toString();
        }
    }

    private boolean tryParse(String content, char delimiter, SecurityCache securityCache, String source,
                    List<Item> items, List<Exception> errors)
    {
        var format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
                        .setIgnoreHeaderCase(true).setTrim(true).get();

        try (var reader = new StringReader(content); var parser = CSVParser.parse(reader, format))
        {
            var headerMap = parser.getHeaderMap();
            if (headerMap == null || !hasHeader(headerMap, "Date") || !hasHeader(headerMap, "Type")
                            || !hasHeader(headerMap, "Net Amt.") || !hasHeader(headerMap, "CCY")
                            || !hasHeader(headerMap, "Reference"))
            { return false; }

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
        catch (IOException e)
        {
            errors.add(e);
            return true;
        }
    }

    private boolean hasHeader(Map<String, Integer> headerMap, String expectedName)
    {
        for (String header : headerMap.keySet())
        {
            if (header != null && header.trim().equalsIgnoreCase(expectedName))
                return true;
        }
        return false;
    }

    private void processConversions(List<CSVRecord> records, Set<CSVRecord> processedRecords, String source,
                    List<Item> items, List<Exception> errors)
    {
        // Group "Conversion" records by Reference
        var conversionGroups = new HashMap<String, List<CSVRecord>>();
        for (CSVRecord record : records)
        {
            var type = getField(record, "Type");
            if ("Conversion".equalsIgnoreCase(type))
            {
                var ref = getField(record, "Reference");
                conversionGroups.computeIfAbsent(ref, k -> new ArrayList<>()).add(record);
            }
        }

        for (var entryGroup : conversionGroups.entrySet())
        {
            var group = entryGroup.getValue();
            if (group.size() == 2)
            {
                CSVRecord outRecord = null;
                CSVRecord inRecord = null;

                for (CSVRecord record : group)
                {
                    var grossAmt = parseSignedDecimal(record, "Gross Amount", Values.Amount.factor());
                    var netAmt = parseSignedDecimal(record, "Net Amt.", Values.Amount.factor());
                    if (grossAmt < 0 || netAmt < 0)
                        outRecord = record;
                    else
                        inRecord = record;
                }

                if (outRecord != null && inRecord != null)
                {
                    try
                    {
                        var transferEntry = new AccountTransferEntry();
                        var date = parseDate(getField(outRecord, "Date"));
                        var reference = entryGroup.getKey();

                        var outCcy = getField(outRecord, "CCY");
                        var outAmount = Math.abs(parseSignedDecimal(outRecord, "Net Amt.", Values.Amount.factor()));
                        transferEntry.getSourceTransaction().setCurrencyCode(outCcy);
                        transferEntry.getSourceTransaction().setAmount(outAmount);
                        transferEntry.getSourceTransaction().setDateTime(date);
                        transferEntry.getSourceTransaction().setSource(source);

                        var inCcy = getField(inRecord, "CCY");
                        var inAmount = Math.abs(parseSignedDecimal(inRecord, "Net Amt.", Values.Amount.factor()));
                        var exchangeRate = getField(inRecord, "FX Rate");
                        if (!exchangeRate.isEmpty())
                        {
                            transferEntry.getSourceTransaction()
                                            .addUnit(new Unit(Unit.Type.GROSS_VALUE, Money.of(outCcy, outAmount),
                                                            Money.of(inCcy, inAmount),
                                                            new BigDecimal(exchangeRate.replace(',', '.'))));
                        }
                        transferEntry.getTargetTransaction().setCurrencyCode(inCcy);
                        transferEntry.getTargetTransaction().setAmount(inAmount);
                        transferEntry.getTargetTransaction().setDateTime(parseDate(getField(inRecord, "Date")));
                        transferEntry.getTargetTransaction().setSource(source);

                        transferEntry.setDate(date);
                        var note = "Currency conversion Ref: " + reference;
                        transferEntry.setNote(note);

                        items.add(new AccountTransferItem(transferEntry, true));

                        // Fee on conversion if present
                        var fee = Math.abs(parseSignedDecimal(outRecord, "Fee", Values.Amount.factor()));
                        if (fee > 0)
                        {
                            var feeTx = new AccountTransaction();
                            feeTx.setType(AccountTransaction.Type.FEES);
                            feeTx.setDateTime(date);
                            feeTx.setCurrencyCode(outCcy);
                            feeTx.setAmount(fee);
                            feeTx.setNote("Currency conversion fee Ref: " + reference);
                            feeTx.setSource(source);
                            items.add(new TransactionItem(feeTx));
                        }

                        processedRecords.add(outRecord);
                        processedRecords.add(inRecord);
                    }
                    catch (Exception e)
                    {
                        errors.add(new IllegalArgumentException("Failed to pair conversion: " + e.getMessage(), e));
                    }
                }
            }
        }
    }

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

    private void processBuySell(CSVRecord record, String type, SecurityCache securityCache, String source,
                    List<Item> items)
    {
        var isBuy = "buy".equalsIgnoreCase(type);
        var security = lookupSecurity(record, securityCache);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        var shares = Math.abs(parseSignedDecimal(record, "Quantity", Values.Share.factor()));
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

        items.add(new BuySellEntryItem(entry));
    }

    private void processDividendOrDistribution(CSVRecord record, String type, SecurityCache securityCache,
                    String source, List<Item> items)
    {
        var isDistribution = "distribution".equalsIgnoreCase(type);
        var security = lookupSecurity(record, securityCache);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        var netAmount = Math.abs(parseSignedDecimal(record, "Net Amt.", Values.Amount.factor()));
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

        items.add(new TransactionItem(t));
    }

    private void processDepositWithdrawal(CSVRecord record, String type, String source, List<Item> items)
    {
        var isDeposit = "deposit".equalsIgnoreCase(type);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        var amount = Math.abs(parseSignedDecimal(record, "Net Amt.", Values.Amount.factor()));

        var t = new AccountTransaction();
        t.setType(isDeposit ? AccountTransaction.Type.DEPOSIT : AccountTransaction.Type.REMOVAL);
        t.setDateTime(date);
        t.setCurrencyCode(ccy);
        t.setAmount(amount);
        t.setNote(getField(record, "Reference"));
        t.setSource(source);

        items.add(new TransactionItem(t));
    }

    private void processInterestOrReward(CSVRecord record, String type, String source, List<Item> items)
    {
        var isInterest = "interest".equalsIgnoreCase(type);
        var date = parseDate(getField(record, "Date"));
        var ccy = getField(record, "CCY");
        var netAmount = Math.abs(parseSignedDecimal(record, "Net Amt.", Values.Amount.factor()));
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

        var ccy = getField(record, "CCY");
        if (ccy.isEmpty())
            ccy = CurrencyUnit.EUR;

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

    private String getField(CSVRecord record, String column)
    {
        for (String key : record.getParser().getHeaderMap().keySet())
        {
            if (key != null && key.trim().equalsIgnoreCase(column))
            {
                var val = record.get(key);
                return val != null ? val.trim() : "";
            }
        }
        return "";
    }

    private long parseSignedDecimal(CSVRecord record, String column, long factor)
    {
        var value = getField(record, column);
        if (value.isEmpty())
            return 0;
        return new BigDecimal(value.replace(',', '.')).multiply(BigDecimal.valueOf(factor))
                        .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
