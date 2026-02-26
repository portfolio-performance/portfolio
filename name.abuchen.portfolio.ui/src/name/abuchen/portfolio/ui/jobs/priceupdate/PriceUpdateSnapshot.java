package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.model.Security;

public class PriceUpdateSnapshot
{
    private final long timestamp;
    private final Map<Security, SecurityUpdateStatus> statuses;

    private final int taskCount;
    private final int completedTaskCount;

    public PriceUpdateSnapshot(long timestamp, Map<Security, SecurityUpdateStatus> statuses)
    {
        this.timestamp = timestamp;
        this.statuses = statuses;

        var count = 0;
        var completed = 0;

        for (var status : statuses.values())
        {
            var statusesToCheck = new FeedUpdateStatus[] { status.getHistoricStatus(), status.getLatestStatus() };

            for (var feedStatus : statusesToCheck)
            {
                if (feedStatus.getStatus() != UpdateStatus.SKIPPED)
                {
                    count++;
                    if (feedStatus.getStatus().isTerminal)
                        completed++;
                }
            }
        }

        this.taskCount = count;
        this.completedTaskCount = completed;
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

    public int getTaskCount()
    {
        return taskCount;
    }

    public int getCompletedTaskCount()
    {
        return completedTaskCount;
    }

    public Collection<Security> getSecurities()
    {
        return statuses.keySet();
    }
}
