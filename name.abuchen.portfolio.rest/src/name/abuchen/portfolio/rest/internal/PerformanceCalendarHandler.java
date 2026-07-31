package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.util.Interval;

public final class PerformanceCalendarHandler
{
    private PerformanceCalendarHandler()
    {
    }

    /**
     * Per-month returns and activity for the period.
     * <p>
     * The monthly return is the desktop's own figure: one {@link PerformanceIndex}
     * over the month-aligned span, then {@code getPerformance} per month - exactly
     * what the application's monthly-returns heat map does. Compounding the daily
     * series client-side would drift from it.
     * <p>
     * The money sums come from a single sweep over the transactions, each converted
     * at its own date. That matters: transactions carry their own currencies, so a
     * month's total in the reporting currency needs the rate on each transaction's
     * date, not one rate for the month.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String openingDateParam,
                    String closingDateParam, String currencyParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var range = PerformanceHandler.parseRange(client, openingDateParam, closingDateParam, currencyParam, errors);
        PerformanceHandler.checkRange(range, errors);

        var converter = new CurrencyConverterImpl(factory, range.currency());

        // Month buckets need whole months. The index therefore runs from the last day
        // of the month before the opening date (Interval excludes its start) to the
        // end of the closing month.
        var alignedStart = range.openingDate().withDayOfMonth(1).minusDays(1);
        var alignedEnd = range.closingDate().with(TemporalAdjusters.lastDayOfMonth());
        var aligned = Interval.of(alignedStart, alignedEnd);

        var index = PerformanceIndex.forClient(client, converter, aligned, new ArrayList<>());
        var actual = index.getActualInterval();

        var months = new LinkedHashMap<YearMonth, MonthBucket>();
        for (YearMonth month : aligned.getYearMonths())
            months.put(month, new MonthBucket());

        collectActivity(client, converter, months);

        var result = new ArrayList<MonthEntry>();
        for (Map.Entry<YearMonth, MonthBucket> entry : months.entrySet())
        {
            var month = entry.getKey();
            var monthInterval = monthInterval(month);

            // A month the index does not cover has no return; reporting 0 would read
            // as "flat" rather than "no data".
            Double ttwror = actual.intersects(monthInterval) ? index.getPerformance(monthInterval) : null;
            result.add(new MonthEntry(month, ttwror, entry.getValue()));
        }

        return EntityJson.performanceCalendar(range.currency(), result);
    }

    /** the month as the index wants it: start excluded, so the previous month's last day */
    private static Interval monthInterval(YearMonth month)
    {
        return Interval.of(month.atDay(1).minusDays(1), month.atEndOfMonth());
    }

    private static void collectActivity(Client client, CurrencyConverter converter,
                    Map<YearMonth, MonthBucket> months)
    {
        for (var account : client.getAccounts())
        {
            for (AccountTransaction transaction : account.getTransactions())
            {
                var bucket = months.get(YearMonth.from(transaction.getDateTime()));
                if (bucket == null)
                    continue;

                var date = transaction.getDateTime().toLocalDate();
                var amount = converted(converter, transaction, date);

                switch (transaction.getType())
                {
                    case DIVIDENDS -> bucket.dividends += amount;
                    case INTEREST -> bucket.interest += amount;
                    case INTEREST_CHARGE -> bucket.interest -= amount;
                    case FEES -> bucket.fees -= amount;
                    case FEES_REFUND -> bucket.fees += amount;
                    case TAXES -> bucket.taxes -= amount;
                    case TAX_REFUND -> bucket.taxes += amount;
                    case DEPOSIT -> bucket.netTransfers += amount;
                    case REMOVAL -> bucket.netTransfers -= amount;
                    // Buy/sell and transfers are the cash leg of a portfolio
                    // transaction, counted on that side instead.
                    default -> {
                        // nothing to accumulate
                    }
                }
            }
        }

        for (var portfolio : client.getPortfolios())
        {
            for (PortfolioTransaction transaction : portfolio.getTransactions())
            {
                var bucket = months.get(YearMonth.from(transaction.getDateTime()));
                if (bucket == null)
                    continue;

                var date = transaction.getDateTime().toLocalDate();
                bucket.fees -= convertedUnit(converter, transaction, Transaction.Unit.Type.FEE, date);
                bucket.taxes -= convertedUnit(converter, transaction, Transaction.Unit.Type.TAX, date);

                var amount = converted(converter, transaction, date);
                switch (transaction.getType())
                {
                    case BUY -> {
                        bucket.buys++;
                        bucket.buyVolume += amount;
                    }
                    case SELL -> {
                        bucket.sells++;
                        bucket.sellVolume += amount;
                    }
                    // A delivery moves assets in or out without a cash leg, so it
                    // belongs with deposits and removals rather than with trades.
                    case DELIVERY_INBOUND -> bucket.netTransfers += amount;
                    case DELIVERY_OUTBOUND -> bucket.netTransfers -= amount;
                    default -> {
                        // transfers between own portfolios are internal
                    }
                }
            }
        }
    }

    private static long converted(CurrencyConverter converter, Transaction transaction, LocalDate date)
    {
        return converter.convert(date, transaction.getMonetaryAmount()).getAmount();
    }

    private static long convertedUnit(CurrencyConverter converter, Transaction transaction,
                    Transaction.Unit.Type type, LocalDate date)
    {
        return converter.convert(date, transaction.getUnitSum(type)).getAmount();
    }

    /** unsigned magnitudes as the model stores them; signs are applied when accumulating */
    /* package */ static final class MonthBucket
    {
        long dividends;
        long interest;
        long fees;
        long taxes;
        long netTransfers;
        int buys;
        int sells;
        long buyVolume;
        long sellVolume;
    }

    /* package */ record MonthEntry(YearMonth month, Double ttwror, MonthBucket bucket)
    {
    }
}
