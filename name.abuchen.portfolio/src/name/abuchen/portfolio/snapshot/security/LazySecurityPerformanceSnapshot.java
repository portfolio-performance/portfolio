package name.abuchen.portfolio.snapshot.security;

import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.util.Interval;

public class LazySecurityPerformanceSnapshot
{
    public static LazySecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval)
    {
        var records = new SecurityPerformanceSnapshotBuilder<LazySecurityPerformanceRecord>(client, converter, interval)
                        .create(LazySecurityPerformanceRecord.class);

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
