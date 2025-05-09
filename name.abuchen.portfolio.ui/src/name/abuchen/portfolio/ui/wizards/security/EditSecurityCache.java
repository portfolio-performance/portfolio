package name.abuchen.portfolio.ui.wizards.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Shared cache between pages of the EditSecurityDialog.
 */
public class EditSecurityCache
{
    private class LoadExchangesJob extends Job
    {
        private final EditSecurityCache cache;
        private final QuoteFeed feed;
        private final Security security;

        public LoadExchangesJob(EditSecurityCache cache, QuoteFeed feed, Security security)
        {
            super(Messages.JobMsgLoadingExchanges);
            this.cache = cache;
            this.feed = feed;
            this.security = security;

            setSystem(true);
        }

        @Override
        public IStatus run(IProgressMonitor monitor)
        {
            monitor.beginTask(Messages.JobMsgLoadingExchanges, 1);

            List<Exception> errors = new ArrayList<>();
            var exchanges = feed.getExchanges(security, errors);
            PortfolioPlugin.log(errors);

            Display.getDefault().asyncExec(() -> {
                cache.cacheExchanges.put(feed, exchanges);
                for (BiConsumer<QuoteFeed, List<Exchange>> listener : listeners)
                    listener.accept(feed, exchanges);
            });

            monitor.done();
            return Status.OK_STATUS;
        }
    }

    private final List<BiConsumer<QuoteFeed, List<Exchange>>> listeners = new ArrayList<>();

    private final Map<QuoteFeed, List<Exchange>> cacheExchanges = new HashMap<>();

    EditSecurityCache()
    {
    }

    public void clearExchanges()
    {
        cacheExchanges.clear();
    }

    public void addListener(BiConsumer<QuoteFeed, List<Exchange>> listener)
    {
        listeners.add(listener);
    }

    public Optional<List<Exchange>> getOrLoadExchanges(QuoteFeed feed, Security security)
    {
        var list = cacheExchanges.get(feed);
        if (list != null)
            return Optional.of(list);

        new LoadExchangesJob(this, feed, security).schedule();

        return Optional.empty();
    }
}
