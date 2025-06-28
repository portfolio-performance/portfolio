package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;

public class PriceUpdateSnapshot
{
    private final long timestamp;
    private final Map<Security, SecurityUpdateStatus> statuses;

    private final int count;
    private final int completed;

    public PriceUpdateSnapshot(long timestamp, Map<Security, SecurityUpdateStatus> statuses)
    {
        this.timestamp = timestamp;
        this.statuses = statuses;

        this.count = statuses.size();
        this.completed = (int) statuses.values().stream()
                        .filter(status -> status.getHistoricStatus().getStatus().isTerminal
                                        && status.getLatestStatus().getStatus().isTerminal)
                        .count();
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public Optional<FeedUpdateStatus> getHistoricStatus(Security security)
    {
        var status = statuses.get(security);
        if (status == null)
            return Optional.empty();

        return Optional.of(status.getHistoricStatus());
    }

    public Optional<FeedUpdateStatus> getLatestStatus(Security security)
    {
        var status = statuses.get(security);
        if (status == null)
            return Optional.empty();

        return Optional.of(status.getLatestStatus());
    }

    public int getCount()
    {
        return count;
    }

    public int getCompleted()
    {
        return completed;
    }

    public Collection<Security> getSecurities()
    {
        return statuses.keySet();
    }
}
