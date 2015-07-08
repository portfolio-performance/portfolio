package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.QuotesTableViewer;

import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class HistoricalQuoteProviderPage extends AbstractQuoteProviderPage
{
    private QuotesTableViewer tableSampleData;

    private Map<Object, List<LatestSecurityPrice>> cacheQuotes = new HashMap<Object, List<LatestSecurityPrice>>();

    // read and write 'currentJob' only from the UI thread; used to check
    // whether a more recent job has already been started
    private LoadHistoricalQuotes currentJob;

    public HistoricalQuoteProviderPage(final EditSecurityModel model, BindingHelper bindings)
    {
        super(model);

        setTitle(Messages.EditWizardQuoteFeedTitle);

        // validate that quote provider message is null -> no errors
        bindings.getBindingContext().addValidationStatusProvider(new MultiValidator()
        {
            IObservableValue observable = BeansObservables.observeValue(model, "statusHistoricalQuotesProvider"); //$NON-NLS-1$

            @Override
            protected IStatus validate()
            {
                return observable.getValue() == null ? ValidationStatus.ok()
                                : ValidationStatus.error(observable.getValue().toString());
            }
        });

    }

    @Override
    protected String getFeed()
    {
        return getModel().getFeed();
    }

    @Override
    protected void setFeed(String feed)
    {
        getModel().setFeed(feed);
    }

    @Override
    protected String getFeedURL()
    {
        return getModel().getFeedURL();
    }

    @Override
    protected void setFeedURL(String feedURL)
    {
        getModel().setFeedURL(feedURL);
    }

    @Override
    protected void setStatus(String status)
    {
        getModel().setStatusHistoricalQuotesProvider(status);
    }

    @Override
    protected List<QuoteFeed> getAvailableFeeds()
    {
        return Factory.getQuoteFeedProvider();
    }

    @Override
    protected QuoteFeed getQuoteFeedProvider(String feedId)
    {
        return Factory.getQuoteFeedProvider(feedId);
    }

    @Override
    protected void createSampleArea(Composite container)
    {
        Composite composite = new Composite(container, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        composite.setLayout(layout);

        tableSampleData = new QuotesTableViewer(composite);
    }

    @Override
    protected void reinitCaches()
    {
        cacheQuotes = new HashMap<Object, List<LatestSecurityPrice>>();
    }

    @Override
    protected void clearSampleQuotes()
    {
        currentJob = null;
        tableSampleData.setInput(null);
        tableSampleData.refresh();
    }

    @Override
    protected void showSampleQuotes(QuoteFeed feed, Exchange exchange, String feedURL)
    {
        Object cacheKey = exchange != null ? exchange : feedURL;

        List<LatestSecurityPrice> quotes = cacheQuotes.get(cacheKey);

        if (quotes != null)
        {
            tableSampleData.setInput(quotes);
            tableSampleData.refresh();
        }
        else
        {
            tableSampleData.setMessage(Messages.EditWizardQuoteFeedMsgLoading);
            tableSampleData.refresh();

            Job job = new LoadHistoricalQuotes(feed, exchange);
            job.setUser(true);
            job.schedule(150);
        }
    }

    private class LoadHistoricalQuotes extends Job
    {
        private QuoteFeed feed;
        private Exchange exchange;

        public LoadHistoricalQuotes(QuoteFeed feed, Exchange exchange)
        {
            super(MessageFormat.format(Messages.JobMsgSamplingHistoricalQuotes,
                            exchange != null ? exchange.getName() : "")); //$NON-NLS-1$
            this.feed = feed;
            this.exchange = exchange;

            HistoricalQuoteProviderPage.this.currentJob = this;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            try
            {
                Security s = buildTemporarySecurity();
                if (exchange != null)
                    s.setTickerSymbol(exchange.getId());
                s.setFeed(feed.getId());

                // last 2 months as sample
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, -2);

                final List<LatestSecurityPrice> quotes = feed.getHistoricalQuotes(s, cal.getTime(),
                                new ArrayList<Exception>());

                Display.getDefault().asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (LoadHistoricalQuotes.this.equals(HistoricalQuoteProviderPage.this.currentJob)
                                        && !tableSampleData.getControl().isDisposed())
                        {
                            HistoricalQuoteProviderPage.this.currentJob = null;
                            cacheQuotes.put(exchange, quotes);
                            if (!tableSampleData.getControl().isDisposed())
                            {
                                tableSampleData.setInput(quotes);
                                tableSampleData.refresh();
                            }
                        }
                    }

                });
            }
            catch (Exception e)
            {
                Display.getDefault().asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (LoadHistoricalQuotes.this.equals(HistoricalQuoteProviderPage.this.currentJob)
                                        && !tableSampleData.getControl().isDisposed())
                        {
                            currentJob = null;
                            tableSampleData.setMessage(Messages.EditWizardQuoteFeedMsgErrorOrNoData);
                            tableSampleData.refresh();
                        }
                    }

                });

                PortfolioPlugin.log(e);
            }

            return Status.OK_STATUS;
        }

    }
}
