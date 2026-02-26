package name.abuchen.portfolio.ui.jobs.priceupdate;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.WebAccess.WebAccessException;

abstract class Task
{
    static class HistoricalTask extends Task
    {
        public HistoricalTask(String groupingCriterion, QuoteFeed feed, FeedUpdateStatus status, Security security)
        {
            super(groupingCriterion, feed, status, security);
        }

        @Override
        public UpdateStatus update() throws QuoteFeedException
        {
            var data = feed.getHistoricalQuotes(security, false);
            var isDirty = security.addAllPrices(data.getPrices());

            if (!data.getErrors().isEmpty())
                PortfolioPlugin.log(createErrorStatus(security.getName(), data.getErrors()));

            return isDirty ? UpdateStatus.MODIFIED : UpdateStatus.UNMODIFIED;
        }

        private IStatus createErrorStatus(String label, List<Exception> exceptions)
        {
            MultiStatus status = new MultiStatus(PortfolioPlugin.PLUGIN_ID, IStatus.ERROR, label, null);
            for (Exception exception : exceptions)
                status.add(new Status(IStatus.ERROR, PortfolioPlugin.PLUGIN_ID, exception.getMessage(),
                                exception instanceof WebAccessException ? null : exception));

            return status;
        }
    }

    static class LatestTask extends Task
    {
        public LatestTask(String groupingCriterion, QuoteFeed feed, FeedUpdateStatus status, Security security)
        {
            super(groupingCriterion, feed, status, security);
        }

        @Override
        public UpdateStatus update() throws QuoteFeedException
        {
            var latest = feed.getLatestQuote(security);
            if (latest.isPresent())
            {
                return security.setLatest(latest.get()) ? UpdateStatus.MODIFIED : UpdateStatus.UNMODIFIED;
            }
            else
            {
                return UpdateStatus.UNMODIFIED;
            }
        }
    }

    protected final String groupingCriterion;
    protected final QuoteFeed feed;
    protected final FeedUpdateStatus status;
    protected final Security security;

    protected Task(String groupingCriterion, QuoteFeed feed, FeedUpdateStatus status, Security security)
    {
        this.groupingCriterion = groupingCriterion;
        this.feed = feed;
        this.status = status;
        this.security = security;
    }

    public abstract UpdateStatus update() throws QuoteFeedException;

    public QuoteFeed getFeed()
    {
        return feed;
    }
}
