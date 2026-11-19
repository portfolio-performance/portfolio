package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.util.Interval;

public class LazySecurityPerformanceSnapshot
{
    /**
     * Creates the security performance for the given reporting interval. If
     * the interval extends into the future, it is capped at today: the
     * valuation at the end is dated today and transactions after today are not
     * included.
     */
    public static LazySecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval)
    {
        return createUncapped(client, converter, interval.capEnd(LocalDate.now()));
    }

    /**
     * Creates the security performance for the given interval without capping
     * it at today. Use it to calculate costs up to a transaction which might be
     * dated in the future, for example the moving average cost of a trade.
     */
    public static LazySecurityPerformanceSnapshot createUncapped(Client client, CurrencyConverter converter,
                    Interval interval)
    {
        var records = new SecurityPerformanceSnapshotBuilder<LazySecurityPerformanceRecord>(client, converter, interval)
                        .create(LazySecurityPerformanceRecord.class);

        return new LazySecurityPerformanceSnapshot(records);
    }

    /**
     * Creates the security performance based on the given valuations. The
     * interval is not capped; the caller must pass an interval that matches
     * the valuations (see ClientPerformanceSnapshot).
     */
    public static LazySecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval,
                    ClientSnapshot valuationAtStart, ClientSnapshot valuationAtEnd)
    {
        var records = new SecurityPerformanceSnapshotBuilder<LazySecurityPerformanceRecord>(client, converter, interval)
                        .create(LazySecurityPerformanceRecord.class, valuationAtStart, valuationAtEnd);

        return new LazySecurityPerformanceSnapshot(records);
    }

    private List<LazySecurityPerformanceRecord> records;

    private LazySecurityPerformanceSnapshot(List<LazySecurityPerformanceRecord> records)
    {
        this.records = records;
    }

    public List<LazySecurityPerformanceRecord> getRecords()
    {
        return records;
    }

    public Optional<LazySecurityPerformanceRecord> getRecord(Security security)
    {
        return records.stream().filter(r -> security.equals(r.getSecurity())).findAny();
    }
}
