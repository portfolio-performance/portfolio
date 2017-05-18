package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Feed;
import name.abuchen.portfolio.online.EventFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public abstract class AbstractEventProviderPage extends AbstractProviderPage
{
    private class LoadExchangesJob extends Job
    {
        public LoadExchangesJob()
        {
            super(Messages.JobMsgLoadingExchanges);
            setSystem(true);
        }

        @Override
        public IStatus run(IProgressMonitor monitor)
        {
            List<Feed> provider = getAvailableFeeds();
            monitor.beginTask(Messages.JobMsgLoadingExchanges, provider.size());
            for (Feed feed : provider)
            {
                Security s = buildTemporarySecurity();

                List<Exception> errors = new ArrayList<Exception>();
                cacheExchanges.put(feed, feed.getExchanges(s, errors));

                PortfolioPlugin.log(errors);

                monitor.worked(1);
            }

            Display.getDefault().asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    Feed feed = (Feed) ((IStructuredSelection) comboProvider.getSelection())
                                    .getFirstElement();

                    if (feed != null && feed.getId() != null)
                    {
                        List<Exchange> exchanges = cacheExchanges.get(feed);
                        if (comboExchange != null)
                        {
                            comboExchange.setSelection(StructuredSelection.EMPTY);

                            if (exchanges != null)
                            {
                                comboExchange.setInput(exchanges);

                                // if ticker symbol matches any of the
                                // exchanges, select this exchange in the
                                // combo list
                                exchanges.stream() //
                                                .filter(e -> e.getId().equals(model.getTickerSymbol())) //
                                                .findAny() //
                                                .ifPresent(e -> comboExchange.setSelection(new StructuredSelection(e)));
                            }

                            if (comboExchange.getSelection().isEmpty())
                                clearSamples();
                            else
                                showSamples(feed, (Exchange) ((StructuredSelection) comboExchange.getSelection())
                                                .getFirstElement());
                        }
                        else
                        {
                            if (exchanges == null || exchanges.isEmpty())
                            {
                                showSamples(feed, null);
                            }
                        }
                    }

                }
            });

            monitor.done();
            return Status.OK_STATUS;
        }
    }

    //private Map<EventFeed, List<Exchange>> cacheExchanges = new HashMap<EventFeed, List<Exchange>>();

    protected AbstractEventProviderPage(EditSecurityModel model)
    {
        super(model);
    }
    
    @Override
    public void beforePage()
    {
        if (!Objects.equals(tickerSymbol, model.getTickerSymbol()))
        {
            this.tickerSymbol = model.getTickerSymbol();

            // clear caches
            cacheExchanges = new HashMap<Feed, List<Exchange>>();
            reinitCaches();

            new LoadExchangesJob().schedule();

            EventFeed feed = (EventFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();

            if (feed.getId() != null && feed.getId().indexOf(HTML) >= 0)
            {
                if (getFeedURL() == null || getFeedURL().length() == 0)
                    clearSamples();
                else
                    showSamples(feed, null);
            }
        }
    }

    @Override
    public void afterPage()
    {
        EventFeed feed = (EventFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        setFeed(feed.getId());

        if (comboExchange != null && feed.getId() != null && feed.getId().startsWith(YAHOO))
        {
            Exchange exchange = (Exchange) ((IStructuredSelection) comboExchange.getSelection()).getFirstElement();
            if (exchange != null)
            {
                model.setTickerSymbol(exchange.getId());
                tickerSymbol = exchange.getId();
                setFeedURL(null);
            }
        }
        else if (textFeedURL != null)
        {
            setFeedURL(textFeedURL.getText());
        }
    }

    @Override
    public final void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);
        container.setLayout(new FormLayout());

        createProviderGroup(container);

        Composite sampleArea = new Composite(container, SWT.NONE);
        sampleArea.setLayout(new FillLayout());
        createSampleArea(sampleArea);

        FormData data = new FormData();
        data.top = new FormAttachment(grpFeed, 5);
        data.left = new FormAttachment(0, 10);
        data.right = new FormAttachment(100, -10);
        data.bottom = new FormAttachment(100, -10);
        sampleArea.setLayoutData(data);

        setupInitialData();

        comboProvider.addSelectionChangedListener(new ISelectionChangedListener()
        {
            @Override
            public void selectionChanged(SelectionChangedEvent event)
            {
                onFeedProviderChanged(event);
            }
        });
    }

    protected void createProviderGroup(Composite container)
    {
        grpFeed = new Group(container, SWT.NONE);
        grpFeed.setText(Messages.LabelEventFeed);
        FormData fd_grpFeed = new FormData();
        fd_grpFeed.top = new FormAttachment(0, 5);
        fd_grpFeed.left = new FormAttachment(0, 10);
        grpFeed.setLayoutData(fd_grpFeed);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(grpFeed);

        Label lblProvider = new Label(grpFeed, SWT.NONE);
        lblProvider.setText(Messages.LabelFeedProvider);

        comboProvider = new ComboViewer(grpFeed, SWT.READ_ONLY);
        comboProvider.setContentProvider(ArrayContentProvider.getInstance());
        comboProvider.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((EventFeed) element).getName();
            }
        });
        comboProvider.setInput(getAvailableFeeds());
        GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(comboProvider.getControl());

        labelDetailData = new Label(grpFeed, SWT.NONE);
        GridDataFactory.fillDefaults().indent(0, 5).applyTo(labelDetailData);

        createDetailDataWidgets(null);
    }

    private void createDetailDataWidgets(Feed feed)
    {
        boolean dropDown = feed != null && feed.getId() != null && feed.getId().startsWith(YAHOO);
        boolean feedURL = feed != null && feed.getId() != null && feed.getId().indexOf(HTML) >= 0;

        if (dropDown)
        {
            labelDetailData.setText(Messages.LabelExchange);

            if (textFeedURL != null)
            {
                textFeedURL.dispose();
                textFeedURL = null;
            }

            if (comboExchange == null)
            {
                comboExchange = new ComboViewer(grpFeed, SWT.READ_ONLY);
                comboExchange.setContentProvider(ArrayContentProvider.getInstance());
                comboExchange.setLabelProvider(new LabelProvider()
                {
                    @Override
                    public String getText(Object element)
                    {
                        return ((Exchange) element).getName();
                    }
                });
                GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(comboExchange.getControl());

                comboExchange.addSelectionChangedListener(new ISelectionChangedListener()
                {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event)
                    {
                        onExchangeChanged(event);
                    }
                });
            }
        }
        else if (feedURL)
        {
            labelDetailData.setText(Messages.EditWizardEventFeedLabelFeedURL);

            if (comboExchange != null)
            {
                comboExchange.getControl().dispose();
                comboExchange = null;
            }

            if (textFeedURL == null)
            {
                textFeedURL = new Text(grpFeed, SWT.BORDER);
                GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(textFeedURL);

                textFeedURL.addModifyListener(new ModifyListener()
                {
                    @Override
                    public void modifyText(ModifyEvent e)
                    {
                        onFeedURLChanged();
                    }
                });
            }
        }
        else
        {
            labelDetailData.setText(""); //$NON-NLS-1$

            if (comboExchange != null)
            {
                comboExchange.getControl().dispose();
                comboExchange = null;
            }

            if (textFeedURL != null)
            {
                textFeedURL.dispose();
                textFeedURL = null;
            }
        }

        grpFeed.layout(true);
        grpFeed.getParent().layout();
    }

   final protected void setupInitialData()
    {
        Feed feed = getFeedProvider(getFeed());

        if (feed != null)
            comboProvider.setSelection(new StructuredSelection(feed));
        else
            comboProvider.getCombo().select(0);

        createDetailDataWidgets(feed);

        if (model.getTickerSymbol() != null && feed != null && feed.getId() != null && feed.getId().startsWith("YAHOO")) //$NON-NLS-1$
        {
            Exchange exchange = new Exchange(model.getTickerSymbol(), model.getTickerSymbol());
            ArrayList<Exchange> input = new ArrayList<Exchange>();
            input.add(exchange);
            comboExchange.setInput(input);
            comboExchange.setSelection(new StructuredSelection(exchange));
        }
        else if (textFeedURL != null)
        {
            textFeedURL.setText(getFeedURL());
        }
    }

    protected void onFeedProviderChanged(SelectionChangedEvent event)
    {
        String previousExchangeId = null;
        if (comboExchange != null)
        {
            Exchange exchange = (Exchange) ((IStructuredSelection) comboExchange.getSelection()).getFirstElement();
            if (exchange != null)
                previousExchangeId = exchange.getId();
        }

        if (previousExchangeId == null && model.getTickerSymbol() != null)
        {
            previousExchangeId = model.getTickerSymbol();
        }

        EventFeed feed = (EventFeed) ((IStructuredSelection) event.getSelection()).getFirstElement();

        createDetailDataWidgets(feed);

        if (comboExchange != null)
        {
            List<Exchange> exchanges = cacheExchanges.get(feed);
            comboExchange.setInput(exchanges);

            // select exchange if other provider supports same exchange id
            // (yahoo close vs. yahoo adjusted close)
            boolean exchangeSelected = false;
            if (exchanges != null && previousExchangeId != null)
            {
                for (Exchange e : exchanges)
                {
                    if (e.getId().equals(previousExchangeId))
                    {
                        comboExchange.setSelection(new StructuredSelection(e));
                        exchangeSelected = true;
                        break;
                    }
                }
            }

            if (!exchangeSelected)
                comboExchange.setSelection(null);

            setStatus(exchangeSelected ? null : MessageFormat.format(Messages.MsgErrorExchangeMissing, getTitle()));
        }
        else if (textFeedURL != null)
        {
            boolean hasURL = getFeedURL() != null && getFeedURL().length() > 0;

            if (hasURL)
                textFeedURL.setText(getFeedURL());

            setStatus(hasURL ? null : MessageFormat.format(Messages.EditWizardEventFeedMsgErrorMissingURL, getTitle()));
        }
        else
        {
            // get sample quotes?
            if (feed != null)
            {
                showSamples(feed, null);
            }
            else
            {
                clearSamples();
            }
            setStatus(null);
        }
    }
    
    final protected void onFeedURLChanged()
    {
        setFeedURL(textFeedURL.getText());

        boolean hasURL = getFeedURL() != null && getFeedURL().length() > 0;

        if (!hasURL)
        {
            clearSamples();
            setStatus(MessageFormat.format(Messages.EditWizardEventFeedMsgErrorMissingURL, getTitle()));
        }
        else
        {
            EventFeed feed = (EventFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSamples(feed, null);
            setStatus(null);
        }
    }

}
