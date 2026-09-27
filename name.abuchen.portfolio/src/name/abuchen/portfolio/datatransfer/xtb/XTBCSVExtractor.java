package name.abuchen.portfolio.datatransfer.xtb;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

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
public class XTBCSVExtractor implements Extractor
{
    private static final String[] HEADER = { "Type", "Instrument", "Ticker", "Category", "Time", "Amount", "ID",
                    "Comment", "Product", "Position ID" };
    private static final String[] ACCOUNT_HEADER = { "Type", "Ticker", "Instrument", "Time", "Amount", "ID",
                    "Comment", "Product" };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern CURRENCY = Pattern.compile("^([A-Z]{3})_.*\\.csv$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHARES = Pattern
                    .compile("^(?:OPEN BUY|CLOSE (?:BUY|SELL)) (\\d+(?:\\.\\d+)?)(?:/\\d+(?:\\.\\d+)?)? @ .*$");

    private record Columns(int instrument, int ticker, int time, int amount, int id, int comment)
    {
    }

    public XTBCSVExtractor(Client client)
    {
        // Keep the constructor consistent with other broker extractors.
    }

    @Override
    public String getLabel()
    {
        return "XTB (CSV)";
    }

    @Override
    public List<Item> extract(SecurityCache securityCache, InputFile inputFile, List<Exception> errors)
    {
        var items = new ArrayList<Item>();
        var currencyMatcher = CURRENCY.matcher(inputFile.getName());
        if (!currencyMatcher.matches() || CurrencyUnit.getInstance(currencyMatcher.group(1).toUpperCase()) == null)
        {
            errors.add(new IllegalArgumentException("XTB account currency is missing from the CSV filename"));
            return items;
        }
        var currency = currencyMatcher.group(1).toUpperCase();

        try (var reader = new StringReader(readContent(inputFile));
                        var parser = CSVFormat.DEFAULT.builder().setDelimiter(';').get().parse(reader))
        {
            var records = parser.getRecords();
            var columns = records.size() >= 5 ? columnsFor(records.get(4)) : null;
            if (records.size() < 5 || !"Account number".equals(value(records.get(0), 0))
                            || !"Cash Operations".equals(value(records.get(1), 0))
                            || columns == null)
            {
                errors.add(new IllegalArgumentException("File is not a supported XTB cash operations CSV export"));
                return items;
            }

            var interestTaxes = matchInterestTaxes(records, columns);
            var matchedTaxes = new HashSet<>(interestTaxes.values());
            var internalTransfers = matchInternalTransfers(records, columns);
            for (int index = 5; index < records.size(); index++)
            {
                var record = records.get(index);
                if ("Total".equals(value(record, 0)) || matchedTaxes.contains(record)
                                || internalTransfers.contains(record))
                    continue;
                try
                {
                    process(record, interestTaxes.get(record), columns, currency, securityCache, inputFile.getName(),
                                    items);
                }
                catch (RuntimeException e)
                {
                    errors.add(new IllegalArgumentException(
                                    "XTB CSV line " + record.getRecordNumber() + ": " + e.getMessage(), e));
                }
            }
        }
        catch (IOException e)
        {
            errors.add(e);
        }

        return items;
    }

    private Map<CSVRecord, CSVRecord> matchInterestTaxes(List<CSVRecord> records, Columns columns)
    {
        var matches = new HashMap<CSVRecord, CSVRecord>();
        var matchedTaxes = new HashSet<CSVRecord>();
        for (int index = 5; index < records.size(); index++)
        {
            var interest = records.get(index);
            if (!"Free funds interest".equals(value(interest, 0)))
                continue;

            var taxComment = value(interest, columns.comment()).replace("Interest ", "Interest Tax ");
            for (int taxIndex = 5; taxIndex < records.size(); taxIndex++)
            {
                var tax = records.get(taxIndex);
                if (matchedTaxes.contains(tax) || !"Free funds interest tax".equals(value(tax, 0))
                                || !taxComment.equals(value(tax, columns.comment())))
                    continue;

                try
                {
                    var interestDate = LocalDateTime.parse(value(interest, columns.time()), DATE_FORMAT);
                    var taxDate = LocalDateTime.parse(value(tax, columns.time()), DATE_FORMAT);
                    if (interestDate.toLocalDate().equals(taxDate.toLocalDate()))
                    {
                        matches.put(interest, tax);
                        matchedTaxes.add(tax);
                        break;
                    }
                }
                catch (RuntimeException e)
                {
                    // Malformed rows are reported while processing the individual record.
                }
            }
        }
        return matches;
    }

    private HashSet<CSVRecord> matchInternalTransfers(List<CSVRecord> records, Columns columns)
    {
        var paired = new HashSet<CSVRecord>();
        for (int index = 5; index < records.size(); index++)
        {
            var outgoing = records.get(index);
            var type = value(outgoing, 0);
            if (!"Transfer".equals(type) && !"Subaccount transfer".equals(type))
                continue;

            try
            {
                var amount = parseDecimal(value(outgoing, columns.amount()).replace(',', '.'), Values.Amount.factor());
                if (amount >= 0 || paired.contains(outgoing))
                    continue;

                for (int candidateIndex = 5; candidateIndex < records.size(); candidateIndex++)
                {
                    var incoming = records.get(candidateIndex);
                    if (!type.equals(value(incoming, 0)) || paired.contains(incoming)
                                    || !value(outgoing, columns.time()).equals(value(incoming, columns.time()))
                                    || !value(outgoing, columns.comment()).equals(value(incoming, columns.comment())))
                        continue;

                    var incomingAmount = parseDecimal(value(incoming, columns.amount()).replace(',', '.'),
                                    Values.Amount.factor());
                    if (incomingAmount == -amount)
                    {
                        paired.add(outgoing);
                        paired.add(incoming);
                        break;
                    }
                }
            }
            catch (RuntimeException e)
            {
                // Malformed or unmatched transfers are reported while processing the row.
            }
        }
        return paired;
    }

    private String readContent(InputFile inputFile) throws IOException
    {
        var bytes = Files.readAllBytes(inputFile.getFile().toPath());
        try
        {
            var content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            // Excel's "CSV UTF-8" starts with a byte order mark, which the
            // decoder keeps as a leading
            return content.startsWith("﻿") ? content.substring(1) : content;
        }
        catch (CharacterCodingException e)
        {
            return Charset.forName("windows-1250").decode(ByteBuffer.wrap(bytes)).toString();
        }
    }

    private Columns columnsFor(CSVRecord record)
    {
        if (matchesHeader(record, HEADER))
            return new Columns(1, 2, 4, 5, 6, 7);
        if (matchesHeader(record, ACCOUNT_HEADER))
            return new Columns(2, 1, 3, 4, 5, 6);
        return null;
    }

    private boolean matchesHeader(CSVRecord record, String[] header)
    {
        if (record.size() != header.length)
            return false;
        for (int index = 0; index < header.length; index++)
            if (!header[index].equals(value(record, index)))
                return false;
        return true;
    }

    private void process(CSVRecord record, CSVRecord interestTax, Columns columns, String currency,
                    SecurityCache securityCache, String source, List<Item> items)
    {
        var type = value(record, 0);
        var date = LocalDateTime.parse(value(record, columns.time()), DATE_FORMAT);
        var signedAmount = parseDecimal(value(record, columns.amount()).replace(',', '.'), Values.Amount.factor());
        var amount = Math.abs(signedAmount);
        var comment = value(record, columns.comment());
        var note = comment + " (XTB ID: " + value(record, columns.id()) + ")";

        switch (type)
        {
            case "Stock purchase", "Stock sale", "Stock sell":
                var security = lookupSecurity(record, columns, currency, securityCache);
                var sharesMatcher = SHARES.matcher(comment);
                if (!sharesMatcher.matches())
                    throw new IllegalArgumentException("Cannot read trade quantity from " + comment);
                var entry = new BuySellEntry();
                entry.setType("Stock purchase".equals(type) ? PortfolioTransaction.Type.BUY
                                : PortfolioTransaction.Type.SELL);
                entry.setSecurity(security);
                entry.setDate(date);
                entry.setCurrencyCode(currency);
                entry.setShares(parseDecimal(sharesMatcher.group(1), Values.Share.factor()));
                entry.setAmount(amount);
                entry.setNote(note);
                entry.setSource(source);
                items.add(new BuySellEntryItem(entry));
                break;
            case "Deposit", "Withdrawal", "Dividend", "Withholding tax", "Tax IFTT", "Swap",
                            "Free funds interest", "Free funds interest tax":
                var transaction = new AccountTransaction();
                transaction.setType(switch (type)
                {
                    case "Deposit" -> AccountTransaction.Type.DEPOSIT;
                    case "Withdrawal" -> AccountTransaction.Type.REMOVAL;
                    case "Dividend" -> AccountTransaction.Type.DIVIDENDS;
                    case "Free funds interest" -> AccountTransaction.Type.INTEREST;
                    case "Swap" -> signedAmount > 0 ? AccountTransaction.Type.INTEREST
                                    : AccountTransaction.Type.INTEREST_CHARGE;
                    default -> AccountTransaction.Type.TAXES;
                });
                if ("Dividend".equals(type) || "Withholding tax".equals(type) || "Tax IFTT".equals(type))
                    transaction.setSecurity(lookupSecurity(record, columns, currency, securityCache));
                transaction.setDateTime(date);
                transaction.setCurrencyCode(currency);
                transaction.setAmount(amount);
                if (interestTax != null)
                {
                    var tax = Math.abs(parseDecimal(value(interestTax, columns.amount()).replace(',', '.'),
                                    Values.Amount.factor()));
                    if (tax > amount)
                        throw new IllegalArgumentException("Interest tax exceeds interest");
                    transaction.setAmount(amount - tax);
                    transaction.addUnit(new Unit(Unit.Type.TAX, Money.of(currency, tax)));
                }
                transaction.setNote(note);
                transaction.setSource(source);
                items.add(new TransactionItem(transaction));
                break;
            default:
                throw new IllegalArgumentException("Unsupported transaction type: " + type);
        }
    }

    private Security lookupSecurity(CSVRecord record, Columns columns, String currency, SecurityCache securityCache)
    {
        var ticker = value(record, columns.ticker());
        var name = value(record, columns.instrument());
        if (ticker.isEmpty() || name.isEmpty())
            throw new IllegalArgumentException("Missing instrument or ticker");
        return securityCache.lookup(null, ticker, null, name, () -> new Security(name, currency));
    }

    private String value(CSVRecord record, int index)
    {
        return index < record.size() ? record.get(index).trim() : "";
    }

    private long parseDecimal(String value, long factor)
    {
        return new BigDecimal(value).multiply(BigDecimal.valueOf(factor)).setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();
    }
}
