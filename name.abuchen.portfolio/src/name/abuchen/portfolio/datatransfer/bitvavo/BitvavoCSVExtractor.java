package name.abuchen.portfolio.datatransfer.bitvavo;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

/**
 * @formatter:off
 * @implSpec Imports Bitvavo "Vollständige Transaktionshistorie" CSV exports.
 *
 *           Column layout:
 *           Timezone, Date, Time, Type, Currency, Amount,
 *           Quote Currency, Quote Price,
 *           Received / Paid Currency, Received / Paid Amount,
 *           Fee currency, Fee amount,
 *           Status, Transaction ID, Address
 *
 *           Handled transaction types:
 *           - buy / sell           → BuySellEntry
 *           - deposit              → AccountTransaction DEPOSIT
 *           - withdrawal (fiat)    → AccountTransaction REMOVAL
 *           - withdrawal (crypto)  → BuySellEntry SELL (network fee shares at EUR 0)
 *                                  + PortfolioTransferEntry TRANSFER_OUT (net shares at EUR 0)
 *           - rebate               → AccountTransaction FEES_REFUND
 *           - campaign_new_user_incentive → AccountTransaction DEPOSIT
 *
 *           Amount semantics for buy/sell:
 *           "Received / Paid Amount" already includes the fee
 *           (negative = paid for buy, positive = received for sell).
 *           PP amount = abs(Received/Paid Amount); FEE unit = Fee amount.
 *           PP derives gross value as: amount ∓ fee.
 *
 *           Rebate correlation:
 *           Bitvavo issues a rebate row for each trade to refund the charged fee.
 *           After all rows are parsed, each FEES_REFUND is matched to the nearest
 *           regular buy/sell within {@value #REBATE_CORRELATION_WINDOW_SECONDS}
 *           seconds so that PP can link the refund to the correct security.
 * @formatter:on
 */
@SuppressWarnings("nls")
public class BitvavoCSVExtractor implements Extractor
{
    private static final String TYPE_BUY = "buy";
    private static final String TYPE_SELL = "sell";
    private static final String TYPE_DEPOSIT = "deposit";
    private static final String TYPE_WITHDRAWAL = "withdrawal";
    private static final String TYPE_REBATE = "rebate";
    private static final String TYPE_CAMPAIGN = "campaign_new_user_incentive";

    /** Maximum seconds between a rebate and its corresponding trade. */
    private static final long REBATE_CORRELATION_WINDOW_SECONDS = 120;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Client client;

    public BitvavoCSVExtractor(Client client)
    {
        this.client = client;
    }

    @Override
    public String getLabel()
    {
        return Messages.BitvavoCSVLabel;
    }

    @Override
    public List<Item> extract(SecurityCache securityCache, InputFile inputFile, List<Exception> errors)
    {
        var items = new ArrayList<Item>();
        var source = inputFile.getName();

        try
        {
            var content = Files.readString(inputFile.getFile().toPath(), StandardCharsets.UTF_8);

            if (!tryParse(content, securityCache, source, items, errors))
            {
                errors.add(new IllegalArgumentException(
                                MessageFormat.format(Messages.BitvavoCSVMsgFileNotSupported, source)));
            }
        }
        catch (IOException e)
        {
            errors.add(e);
        }

        return items;
    }

    /**
     * Parses the CSV content and populates {@code items}. Returns {@code true} if
     * the file looks like a Bitvavo export (required headers present), {@code false}
     * if the file should be rejected as unsupported.
     */
    private boolean tryParse(String content, SecurityCache securityCache, String source, List<Item> items,
                    List<Exception> errors)
    {
        var format = CSVFormat.DEFAULT.builder() //
                        .setHeader() //
                        .setSkipHeaderRecord(true) //
                        .get();

        try (var reader = new StringReader(content); var parser = CSVParser.parse(reader, format))
        {
            var headers = parser.getHeaderMap();
            if (!headers.containsKey("Date") || !headers.containsKey("Type") //
                            || !headers.containsKey("Currency") || !headers.containsKey("Amount") //
                            || !headers.containsKey("Transaction ID"))
                return false;

            for (CSVRecord record : parser)
            {
                try
                {
                    processRecord(record, securityCache, source, items);
                }
                catch (Exception e)
                {
                    errors.add(e);
                }
            }

            correlateRebatesWithTrades(items);

            return true;
        }
        catch (IOException e)
        {
            errors.add(e);
            return true;
        }
    }

    private void processRecord(CSVRecord record, SecurityCache securityCache, String source, List<Item> items)
    {
        var type = getField(record, "Type");

        switch (type)
        {
            case TYPE_BUY:
                processBuySell(record, PortfolioTransaction.Type.BUY, securityCache, source, items);
                break;
            case TYPE_SELL:
                processBuySell(record, PortfolioTransaction.Type.SELL, securityCache, source, items);
                break;
            case TYPE_DEPOSIT:
                processDeposit(record, source, items);
                break;
            case TYPE_WITHDRAWAL:
                processWithdrawal(record, securityCache, source, items);
                break;
            case TYPE_REBATE:
                processRebate(record, source, items);
                break;
            case TYPE_CAMPAIGN:
                processCampaign(record, source, items);
                break;
            default:
            {
                // Unknown type: placeholder deposit with failure message for user review
                var tx = new AccountTransaction();
                tx.setType(AccountTransaction.Type.DEPOSIT);
                tx.setDateTime(parseDateTime(record));
                tx.setCurrencyCode(client.getBaseCurrency());
                tx.setAmount(0);
                tx.setNote(noteWithAddress(record));
                tx.setSource(source);
                var item = new TransactionItem(tx);
                item.setFailureMessage(MessageFormat.format(Messages.BitvavoCSVMsgUnsupportedTransactionType, type));
                items.add(item);
                break;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Buy / Sell
    // -----------------------------------------------------------------------

    private void processBuySell(CSVRecord record, PortfolioTransaction.Type type, SecurityCache securityCache,
                    String source, List<Item> items)
    {
        var ticker = getField(record, "Currency");
        var accountCurrency = getField(record, "Quote Currency");
        if (accountCurrency.isEmpty())
            accountCurrency = CurrencyUnit.EUR;

        // Share count from the crypto "Amount" column
        var shares = Math.abs(Math.round(getDouble(record, "Amount") * Values.Share.factor()));

        // "Received / Paid Amount" is the total including fee (negative=buy, positive=sell)
        var totalAmount = Math.abs(
                        Math.round(getDouble(record, "Received / Paid Amount") * Values.Amount.factor()));
        var feeAmount = Math.abs(Math.round(getDouble(record, "Fee amount") * Values.Amount.factor()));

        var security = lookupCryptoSecurity(ticker, accountCurrency, securityCache);

        var entry = new BuySellEntry();
        entry.setType(type);
        entry.setSecurity(security);
        entry.setDate(parseDateTime(record));
        entry.setCurrencyCode(accountCurrency);
        entry.setShares(shares);
        entry.setAmount(totalAmount);
        entry.setNote(noteWithAddress(record));
        entry.setSource(source);

        if (feeAmount > 0)
            entry.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE, Money.of(accountCurrency, feeAmount)));

        items.add(new BuySellEntryItem(entry));
    }

    // -----------------------------------------------------------------------
    // Deposit
    // -----------------------------------------------------------------------

    private void processDeposit(CSVRecord record, String source, List<Item> items)
    {
        var currency = getField(record, "Currency");
        var amount = Math.abs(Math.round(getDouble(record, "Amount") * Values.Amount.factor()));

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DEPOSIT);
        tx.setDateTime(parseDateTime(record));
        tx.setCurrencyCode(currency);
        tx.setAmount(amount);
        tx.setNote(noteWithAddress(record));
        tx.setSource(source);

        items.add(new TransactionItem(tx));
    }

    // -----------------------------------------------------------------------
    // Withdrawal (fiat → REMOVAL, crypto → SELL fee + TRANSFER_OUT net)
    // -----------------------------------------------------------------------

    /**
     * Fiat withdrawals become a simple REMOVAL. Crypto withdrawals are split into
     * two items: a SELL at EUR 0 for the network fee shares (to remove them from
     * the portfolio without proceeds) and an outgoing portfolio transfer
     * (Depotumbuchung ausgehend) for the remaining net shares.
     */
    private void processWithdrawal(CSVRecord record, SecurityCache securityCache, String source, List<Item> items)
    {
        var currency = getField(record, "Currency");

        // ISO 4217 currencies are fiat; crypto tickers like BTC return null
        if (CurrencyUnit.getInstance(currency) != null)
        {
            var amount = Math.abs(Math.round(getDouble(record, "Amount") * Values.Amount.factor()));

            var tx = new AccountTransaction();
            tx.setType(AccountTransaction.Type.REMOVAL);
            tx.setDateTime(parseDateTime(record));
            tx.setCurrencyCode(currency);
            tx.setAmount(amount);
            tx.setNote(noteWithAddress(record));
            tx.setSource(source);

            items.add(new TransactionItem(tx));
        }
        else
        {
            var totalShares = Math.abs(Math.round(getDouble(record, "Amount") * Values.Share.factor()));
            var feeShares = Math.abs(Math.round(getDouble(record, "Fee amount") * Values.Share.factor()));
            var netShares = totalShares - feeShares;
            var security = lookupCryptoSecurity(currency, CurrencyUnit.EUR, securityCache);
            var note = noteWithAddress(record);
            var dateTime = parseDateTime(record);

            // Network fee: sell fee shares at EUR 0 to write them off without proceeds
            if (feeShares > 0)
            {
                var feeSell = new BuySellEntry();
                feeSell.setType(PortfolioTransaction.Type.SELL);
                feeSell.setSecurity(security);
                feeSell.setDate(dateTime);
                feeSell.setCurrencyCode(CurrencyUnit.EUR);
                feeSell.setShares(feeShares);
                feeSell.setAmount(0);
                feeSell.setNote(note);
                feeSell.setSource(source);
                items.add(new BuySellEntryItem(feeSell));
            }

            // Net withdrawal: outgoing portfolio transfer (Depotumbuchung ausgehend)
            var transfer = new PortfolioTransferEntry();
            transfer.setSecurity(security);
            transfer.setDate(dateTime);
            transfer.setCurrencyCode(CurrencyUnit.EUR);
            transfer.setAmount(0);
            transfer.setShares(netShares > 0 ? netShares : totalShares);
            transfer.setNote(note);

            items.add(new PortfolioTransferItem(transfer));
        }
    }

    // -----------------------------------------------------------------------
    // Rebate → FEES_REFUND
    // -----------------------------------------------------------------------

    private void processRebate(CSVRecord record, String source, List<Item> items)
    {
        var currency = getField(record, "Currency");
        var amount = Math.abs(Math.round(getDouble(record, "Amount") * Values.Amount.factor()));

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.FEES_REFUND);
        tx.setDateTime(parseDateTime(record));
        tx.setCurrencyCode(currency);
        tx.setAmount(amount);
        tx.setNote(noteWithAddress(record));
        tx.setSource(source);

        // Security is assigned later by correlateRebatesWithTrades()
        items.add(new TransactionItem(tx));
    }

    // -----------------------------------------------------------------------
    // Campaign / new-user incentive → DEPOSIT
    // -----------------------------------------------------------------------

    private void processCampaign(CSVRecord record, String source, List<Item> items)
    {
        var currency = getField(record, "Currency");
        var amount = Math.abs(Math.round(getDouble(record, "Amount") * Values.Amount.factor()));

        var tx = new AccountTransaction();
        tx.setType(AccountTransaction.Type.DEPOSIT);
        tx.setDateTime(parseDateTime(record));
        tx.setCurrencyCode(currency);
        tx.setAmount(amount);
        tx.setNote(noteWithAddress(record));
        tx.setSource(source);

        items.add(new TransactionItem(tx));
    }

    // -----------------------------------------------------------------------
    // Rebate ↔ Trade correlation
    // -----------------------------------------------------------------------

    /**
     * Bitvavo issues a rebate row for every fee charged on a trade. This method
     * links each FEES_REFUND transaction to the security of the temporally nearest
     * regular buy/sell within {@value #REBATE_CORRELATION_WINDOW_SECONDS} seconds.
     * Fee-sell entries created for crypto withdrawal network fees (amount = 0) are
     * excluded from matching.
     */
    private void correlateRebatesWithTrades(List<Item> items)
    {
        for (var item : items)
        {
            if (!(item instanceof TransactionItem ti))
                continue;
            if (!(ti.getSubject() instanceof AccountTransaction tx))
                continue;
            if (tx.getType() != AccountTransaction.Type.FEES_REFUND)
                continue;

            Security best = null;
            long bestDiff = Long.MAX_VALUE;

            for (var other : items)
            {
                if (!(other instanceof BuySellEntryItem bsei))
                    continue;

                var ptx = ((BuySellEntry) bsei.getSubject()).getPortfolioTransaction();

                // Skip the synthetic fee-sell entries from crypto withdrawals (amount = 0)
                if (ptx.getAmount() == 0)
                    continue;

                long diff = Math.abs(Duration.between(tx.getDateTime(), ptx.getDateTime()).toSeconds());

                if (diff <= REBATE_CORRELATION_WINDOW_SECONDS && diff < bestDiff)
                {
                    bestDiff = diff;
                    best = ptx.getSecurity();
                }
            }

            if (best != null)
                tx.setSecurity(best);
        }
    }

    // -----------------------------------------------------------------------
    // Security lookup
    // -----------------------------------------------------------------------

    /**
     * Finds or creates a crypto security by ticker symbol. The ticker is used as
     * both the symbol and the display name since Bitvavo does not provide ISINs.
     * Both parameters are passed to {@link SecurityCache#lookup} so it does not
     * overwrite the name with {@code null} when registering a newly created security.
     */
    private Security lookupCryptoSecurity(String ticker, String currency, SecurityCache securityCache)
    {
        return securityCache.lookup(null, ticker, null, ticker, () -> {
            var s = new Security();
            s.setCurrencyCode(currency);
            return s;
        });
    }

    // -----------------------------------------------------------------------
    // Parsing helpers
    // -----------------------------------------------------------------------

    private LocalDateTime parseDateTime(CSVRecord record)
    {
        var dateStr = getField(record, "Date");
        var timeStr = getField(record, "Time");

        var date = LocalDate.parse(dateStr, DATE_FORMAT);

        // Strip optional milliseconds: "HH:mm:ss.SSS" → "HH:mm:ss"
        if (timeStr.contains("."))
            timeStr = timeStr.substring(0, timeStr.indexOf('.'));

        var time = LocalTime.parse(timeStr);
        return LocalDateTime.of(date, time);
    }

    private double getDouble(CSVRecord record, String column)
    {
        var value = getField(record, column);
        if (value.isEmpty())
            return 0.0;
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e)
        {
            return 0.0;
        }
    }

    /** Returns "txId | address" if an on-chain address is present, otherwise just the txId. */
    private String noteWithAddress(CSVRecord record)
    {
        var txId = getField(record, "Transaction ID");
        var address = getField(record, "Address");
        if (!address.isEmpty())
            return txId + " | " + address;
        return txId;
    }

    private String getField(CSVRecord record, String column)
    {
        if (!record.isSet(column))
            return "";
        var value = record.get(column);
        return value == null ? "" : value.trim();
    }
}
