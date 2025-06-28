package name.abuchen.portfolio.ui.jobs.priceupdate;

import name.abuchen.portfolio.model.Security;

/* package */ class SecurityUpdateStatus
{
    private final Security security;
    private final FeedUpdateStatus historic;
    private final FeedUpdateStatus latest;

    public SecurityUpdateStatus(Security security, FeedUpdateStatus historic, FeedUpdateStatus latest)
    {
        this.security = security;
        this.historic = historic;
        this.latest = latest;
    }

    public Security getSecurity()
    {
        return security;
    }

    public FeedUpdateStatus getHistoricStatus()
    {
        return historic;
    }

    public FeedUpdateStatus getLatestStatus()
    {
        return latest;
    }
}
