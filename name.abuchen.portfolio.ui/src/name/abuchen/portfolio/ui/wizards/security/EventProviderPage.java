package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.SecurityElement;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.Feed;
import name.abuchen.portfolio.online.EventFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.EventsTableViewer;

public class EventProviderPage extends AbstractEventProviderPage
{
    private EventsTableViewer tableSampleData;

    private Map<Object, List<SecurityEvent>> cacheEvents = new HashMap<Object, List<SecurityEvent>>();

    // read and write 'currentJob' only from the UI thread; used to check
    // whether a more recent job has already been started
    private LoadEvents currentJob;

    public EventProviderPage(final EditSecurityModel model, BindingHelper bindings)
    {
        super(model);

        setTitle(Messages.EditWizardEventFeedTitle);

        // validate that quote provider message is null -> no errors
        bindings.getBindingContext().addValidationStatusProvider(new MultiValidator()
        {
            IObservableValue observable = BeanProperties.value("statusEventProvider").observe(model); //$NON-NLS-1$

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
        return getModel().getEventFeed();
    }

    @Override
    protected void setFeed(String feed)
    {
        getModel().setEventFeed(feed);
    }

    @Override
    protected String getFeedURL()
    {
        return getModel().getEventFeedURL();
    }

    @Override
    protected void setFeedURL(String feedURL)
    {
        getModel().setEventFeedURL(feedURL);
    }

    @Override
    protected void setStatus(String status)
    {
        getModel().setStatusLatestQuotesProvider(status);
    }

    @Override
    protected List<Feed> getAvailableFeeds()
    {
        return Factory.cast2FeedList(Factory.getEventFeedProvider());
    }

    @Override
    protected Feed getFeedProvider(String feedId)
    {
        return Factory.getEventFeedProvider(feedId);
    }

    @Override
    protected void createSampleArea(Composite container)
    {
        Composite composite = new Composite(container, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        composite.setLayout(layout);

        tableSampleData = new EventsTableViewer(composite);
    }

    @Override
    protected void reinitCaches()
    {
        cacheEvents = new HashMap<Object, List<SecurityEvent>>();
    }

    @Override
    protected void clearSamples()
    {
        currentJob = null;
        tableSampleData.setInput(null);
        tableSampleData.refresh();
    }

    @Override
    protected void showSamples(Feed feed, Exchange exchange)
    {
        if (feed != null) 
        {
            if (feed instanceof EventFeed) 
            {
                Object cacheKey = exchange != null ? exchange : getModel().getFeedURL();
        
                List<SecurityEvent> events = cacheEvents.get(cacheKey);
        
                if (events != null)
                {
                    tableSampleData.setInput(events);
                    tableSampleData.refresh();
                }
                else
                {
                    tableSampleData.setMessage(Messages.EditWizardEventFeedMsgLoading);
                    tableSampleData.refresh();
        
                    Job job = new LoadEvents((EventFeed)feed, exchange);
                    job.setUser(true);
                    job.schedule(150);
                }
    
            }
        }
    }

    private class LoadEvents extends Job
    {
        private EventFeed feed;
        private Exchange exchange;

        public LoadEvents(EventFeed feed, Exchange exchange)
        {
            super(MessageFormat.format(Messages.JobMsgSamplingHistoricalEvents,
                            exchange != null ? exchange.getName() : "")); //$NON-NLS-1$
            this.feed = feed;
            this.exchange = exchange;

            EventProviderPage.this.currentJob = this;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            try
            {
                final Security s = buildTemporarySecurity();

                // exchange is not bound to model (only set in #afterPage)
                // therefore we must set it explicitly here
                if (exchange != null)
                    s.setTickerSymbol(exchange.getId());
                s.setFeed(feed.getId());

                // last 2 months as sample
                LocalDate t = LocalDate.now().minusMonths(2);

                final List<SecurityElement> events = feed.get(s, t, new ArrayList<Exception>());

                Display.getDefault().asyncExec(() -> {
                    if (LoadEvents.this.equals(EventProviderPage.this.currentJob)
                                    && !tableSampleData.getControl().isDisposed())
                    {
                        EventProviderPage.this.currentJob = null;
                        cacheEvents.put(exchange, SecurityEvent.castElement2EventList(events));
                        if (!tableSampleData.getControl().isDisposed())
                        {
                            tableSampleData.setInput(SecurityEvent.castElement2EventList(events));
                            tableSampleData.refresh();
                        }
                    }
                });
            }
            catch (Exception e)
            {
                Display.getDefault().asyncExec(() -> {
                    if (LoadEvents.this.equals(EventProviderPage.this.currentJob)
                                    && !tableSampleData.getControl().isDisposed())
                    {
                        currentJob = null;
                        tableSampleData.setMessage(Messages.EditWizardEventFeedMsgErrorOrNoData);
                        tableSampleData.refresh();
                    }
                });

                PortfolioPlugin.log(e);
            }

            return Status.OK_STATUS;
        }
    }

}
