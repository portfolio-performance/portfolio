package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.snapshot.ClientPerformanceSnapshot;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;

public class DashboardData
{
    private static final class CacheKey
    {
        private Class<?> type;
        private ReportingPeriod period;

        public CacheKey(Class<?> type, ReportingPeriod period)
        {
            this.type = Objects.requireNonNull(type);
            this.period = Objects.requireNonNull(period);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(type, period);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            CacheKey other = (CacheKey) obj;
            if (!type.equals(other.type))
                return false;
            if (!period.equals(other.period))
                return false;
            return true;
        }

    }

    private final Client client;
    private final CurrencyConverter converter;

    private final Map<CacheKey, Object> cache = new HashMap<>();

    public DashboardData(Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;
    }

    public Client getClient()
    {
        return client;
    }

    public void clear()
    {
        cache.clear();
    }

    public <T> T calculate(Class<T> type, ReportingPeriod period)
    {
        CacheKey key = new CacheKey(type, period);
        return type.cast(cache.computeIfAbsent(key, k -> doCalculate(type, period)));
    }

    private Object doCalculate(Class<?> type, ReportingPeriod period)
    {
        if (type.equals(ClientPerformanceSnapshot.class))
        {
            return new ClientPerformanceSnapshot(client, converter, period);
        }
        else if (type.equals(PerformanceIndex.class))
        {
            return PerformanceIndex.forClient(client, converter, period, new ArrayList<Exception>());
        }
        else
        {
            return null;
        }
    }

}
