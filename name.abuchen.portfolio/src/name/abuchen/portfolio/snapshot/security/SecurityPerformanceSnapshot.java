package name.abuchen.portfolio.snapshot.security;

import java.util.List;
import java.util.Optional;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.snapshot.filter.PortfolioClientFilter;
import name.abuchen.portfolio.util.Interval;

public class SecurityPerformanceSnapshot
{
    @SafeVarargs
    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval,
                    Class<? extends SecurityPerformanceIndicator>... indicators)
    {
        var records = new SecurityPerformanceSnapshotBuilder<SecurityPerformanceRecord>(client, converter, interval)
                        .create(SecurityPerformanceRecord.class);

        return doCreateSnapshot(records, indicators);
    }

    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Portfolio portfolio,
                    Interval interval)
    {
        return create(new PortfolioClientFilter(portfolio).filter(client), converter, interval);
    }

    @SafeVarargs
    public static SecurityPerformanceSnapshot create(Client client, CurrencyConverter converter, Interval interval,
                    ClientSnapshot valuationAtStart, ClientSnapshot valuationAtEnd,
                    Class<? extends SecurityPerformanceIndicator>... indicators)
    {
        var records = new SecurityPerformanceSnapshotBuilder<SecurityPerformanceRecord>(client, converter, interval)
                        .create(SecurityPerformanceRecord.class, valuationAtStart, valuationAtEnd);
        return doCreateSnapshot(records, indicators);
    }

    @SafeVarargs
    private static SecurityPerformanceSnapshot doCreateSnapshot(List<SecurityPerformanceRecord> records,
                    Class<? extends SecurityPerformanceIndicator>... indicators)
    {
        records.forEach(r -> r.calculate(indicators));
        return new SecurityPerformanceSnapshot(records);
    }

    private List<SecurityPerformanceRecord> records;

    private SecurityPerformanceSnapshot(List<SecurityPerformanceRecord> records)
    {
        this.records = records;
    }

    public List<SecurityPerformanceRecord> getRecords()
    {
        return records;
    }

    public Optional<SecurityPerformanceRecord> getRecord(Security security)
    {
        return records.stream().filter(r -> security.equals(r.getSecurity())).findAny();
    }
}
