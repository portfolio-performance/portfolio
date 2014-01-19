package name.abuchen.portfolio.ui.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class QuoteProviderPage extends AbstractWizardPage
{
    private static final String YAHOO = "YAHOO"; //$NON-NLS-1$
    private static final String HTML = "HTML"; //$NON-NLS-1$

    private ComboViewer comboProvider;
    private QuotesTableViewer tableSampleData;

    private Group grpQuoteFeed;
    private Label labelDetailData;
    private ComboViewer comboExchange;
    private Text textFeedURL;

    private EditSecurityModel model;

    /*
     * used to identify a changed ISIN and ticker symbol when switching pages
     * back and forth in the wizard
     */
    private String isin;
    private String tickerSymbol;

    private Map<QuoteFeed, List<Exchange>> cacheExchanges = new HashMap<QuoteFeed, List<Exchange>>();
    private Map<Object, List<LatestSecurityPrice>> cacheQuotes = new HashMap<Object, List<LatestSecurityPrice>>();

    /*
     * read & update only from UI thread; used to update prices from job only if
     * no newer job has been started in the meantime
     */
    private LoadHistoricalQuotes currentJob;

    protected QuoteProviderPage(EditSecurityModel model)
    {
        super("feedprovider"); //$NON-NLS-1$
        setTitle(Messages.EditWizardQuoteFeedTitle);
        setDescription(Messages.EditWizardQuoteFeedDescription);

        this.model = model;
    }

    @Override
    public void beforePage()
    {
        if (!areEqual(isin, model.getIsin()) || !areEqual(tickerSymbol, model.getTickerSymbol()))
        {
            this.isin = model.getIsin();
            this.tickerSymbol = model.getTickerSymbol();

            // clear caches
            cacheExchanges = new HashMap<QuoteFeed, List<Exchange>>();
            cacheQuotes = new HashMap<Object, List<LatestSecurityPrice>>();

            try
            {
                getContainer().run(true, false, new LoadExchangesJob());
            }
            catch (InvocationTargetException e)
            {
                PortfolioPlugin.log(e);
            }
            catch (InterruptedException e)
            {
                PortfolioPlugin.log(e);
            }

            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();

            if (feed.getId().indexOf(HTML) >= 0)
            {
                if (model.getFeedURL() == null || model.getFeedURL().length() == 0)
                    clearSampleQuotes();
                else
                    showSampleQuotes(feed, null, model.getFeedURL());
            }
        }
    }

    @Override
    public void afterPage()
    {
        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        model.setFeed(feed.getId());

        if (comboExchange != null && feed.getId().startsWith(YAHOO))
        {
            Exchange exchange = (Exchange) ((IStructuredSelection) comboExchange.getSelection()).getFirstElement();
            if (exchange != null)
            {
                model.setTickerSymbol(exchange.getId());
                tickerSymbol = exchange.getId();
            }
        }
        else if (textFeedURL != null)
        {
            model.setFeedURL(textFeedURL.getText());
        }
    }

    @Override
    public IWizardPage getNextPage()
    {
        return null;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());

        createProviderGroup(container);

        createSampleTable(container);

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

    private void setupInitialData()
    {
        if (model.getFeed() != null)
        {
            QuoteFeed feed = Factory.getQuoteFeedProvider(model.getFeed());
            comboProvider.setSelection(new StructuredSelection(feed));

            createDetailDataWidgets(feed);

            if (model.getTickerSymbol() != null && feed.getId().startsWith("YAHOO")) //$NON-NLS-1$
            {
                Exchange exchange = new Exchange(model.getTickerSymbol(), model.getTickerSymbol());
                ArrayList<Exchange> input = new ArrayList<Exchange>();
                input.add(exchange);
                comboExchange.setInput(input);
                comboExchange.setSelection(new StructuredSelection(exchange));
            }
            else if (textFeedURL != null)
            {
                textFeedURL.setText(model.getFeedURL());
            }
        }
    }

    private void onFeedProviderChanged(SelectionChangedEvent event)
    {
        String previousExchangeId = null;
        if (comboExchange != null)
        {
            Exchange exchange = (Exchange) ((IStructuredSelection) comboExchange.getSelection()).getFirstElement();
            if (exchange != null)
                previousExchangeId = exchange.getId();
        }

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) event.getSelection()).getFirstElement();

        createDetailDataWidgets(feed);

        if (comboExchange != null)
        {
            List<Exchange> exchanges = cacheExchanges.get(feed);
            comboExchange.setInput(exchanges);

            // select exchange if other provider supports same exchange id
            // (yahoo close vs. yahoo adjusted close)
            boolean exchangeSelected = false;
            if (exchanges != null || previousExchangeId != null)
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

            setErrorMessage(exchangeSelected ? null : Messages.MsgErrorExchangeMissing);
            setPageComplete(exchangeSelected);

        }
        else if (textFeedURL != null)
        {
            boolean hasURL = model.getFeedURL() != null && model.getFeedURL().length() > 0;

            if (hasURL)
                textFeedURL.setText(model.getFeedURL());

            setErrorMessage(hasURL ? null : Messages.EditWizardQuoteFeedMsgErrorMissingURL);
            setPageComplete(hasURL);
        }
        else
        {
            clearSampleQuotes();
            setErrorMessage(null);
            setPageComplete(true);
        }
    }

    private void onExchangeChanged(SelectionChangedEvent event)
    {
        Exchange exchange = (Exchange) ((IStructuredSelection) event.getSelection()).getFirstElement();
        setErrorMessage(null);
        setPageComplete(true);

        if (exchange == null)
        {
            clearSampleQuotes();
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, exchange, null);
        }
    }

    private void onFeedURLChanged()
    {
        model.setFeedURL(textFeedURL.getText());

        boolean hasURL = model.getFeedURL() != null && model.getFeedURL().length() > 0;

        if (!hasURL)
        {
            clearSampleQuotes();
            setErrorMessage(Messages.EditWizardQuoteFeedMsgErrorMissingURL);
            setPageComplete(false);
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, null, model.getFeedURL());
            setErrorMessage(null);
            setPageComplete(true);
        }
    }

    private void createProviderGroup(Composite container)
    {
        grpQuoteFeed = new Group(container, SWT.NONE);
        grpQuoteFeed.setText(Messages.LabelQuoteFeed);
        FormData fd_grpQuoteFeed = new FormData();
        fd_grpQuoteFeed.top = new FormAttachment(0);
        fd_grpQuoteFeed.left = new FormAttachment(0, 10);
        grpQuoteFeed.setLayoutData(fd_grpQuoteFeed);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(grpQuoteFeed);

        Label lblProvider = new Label(grpQuoteFeed, SWT.NONE);
        lblProvider.setText(Messages.LabelQuoteFeedProvider);

        comboProvider = new ComboViewer(grpQuoteFeed, SWT.READ_ONLY);
        comboProvider.setContentProvider(ArrayContentProvider.getInstance());
        comboProvider.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                return ((QuoteFeed) element).getName();
            }
        });
        comboProvider.setInput(Factory.getQuoteFeedProvider());
        GridDataFactory.fillDefaults().grab(true, false).applyTo(comboProvider.getControl());

        labelDetailData = new Label(grpQuoteFeed, SWT.NONE);
        GridDataFactory.fillDefaults().indent(0, 5).applyTo(labelDetailData);

        createDetailDataWidgets(null);
    }

    private void createDetailDataWidgets(QuoteFeed feed)
    {
        boolean dropDown = feed != null && feed.getId().startsWith(YAHOO);
        boolean feedURL = feed != null && feed.getId().indexOf(HTML) >= 0;

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
                comboExchange = new ComboViewer(grpQuoteFeed, SWT.READ_ONLY);
                comboExchange.setContentProvider(ArrayContentProvider.getInstance());
                comboExchange.setLabelProvider(new LabelProvider()
                {
                    @Override
                    public String getText(Object element)
                    {
                        return ((Exchange) element).getName();
                    }
                });
                GridDataFactory.fillDefaults().grab(true, false).applyTo(comboExchange.getControl());

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
            labelDetailData.setText(Messages.EditWizardQuoteFeedLabelFeedURL);

            if (comboExchange != null)
            {
                comboExchange.getControl().dispose();
                comboExchange = null;
            }

            if (textFeedURL == null)
            {
                textFeedURL = new Text(grpQuoteFeed, SWT.BORDER);
                GridDataFactory.fillDefaults().grab(true, false).applyTo(textFeedURL);

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

        grpQuoteFeed.layout();
    }

    private void createSampleTable(Composite container)
    {
        Label lblSampleData = new Label(container, SWT.NONE);
        lblSampleData.setText(Messages.LabelSampleData);
        FormData fd_lblSampleData = new FormData();
        fd_lblSampleData.top = new FormAttachment(grpQuoteFeed, 5);
        fd_lblSampleData.left = new FormAttachment(grpQuoteFeed, 10, SWT.LEFT);
        lblSampleData.setLayoutData(fd_lblSampleData);

        Composite composite = new Composite(container, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        composite.setLayout(layout);
        FormData fd_composite = new FormData();
        fd_composite.top = new FormAttachment(lblSampleData, 0);
        fd_composite.left = new FormAttachment(0, 10);
        fd_composite.right = new FormAttachment(100, -10);
        fd_composite.bottom = new FormAttachment(100, -10);
        composite.setLayoutData(fd_composite);

        tableSampleData = new QuotesTableViewer(composite);
    }

    private boolean areEqual(String s1, String s2)
    {
        if (s1 != null)
            return s1.equals(s2);
        return s2 == null ? true : false;
    }

    private void showSampleQuotes(QuoteFeed feed, Exchange exchange, String feedURL)
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

    private void clearSampleQuotes()
    {
        currentJob = null;
        tableSampleData.setInput(null);
        tableSampleData.refresh();
    }

    class LoadExchangesJob implements IRunnableWithProgress
    {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            List<QuoteFeed> provider = Factory.getQuoteFeedProvider();
            monitor.beginTask(Messages.JobMsgLoadingExchanges, provider.size());
            for (QuoteFeed feed : provider)
            {
                try
                {
                    Security s = new Security();
                    s.setTickerSymbol(model.getTickerSymbol());
                    cacheExchanges.put(feed, feed.getExchanges(s));
                }
                catch (IOException e)
                {
                    PortfolioPlugin.log(e);
                    cacheExchanges.put(feed, new ArrayList<Exchange>());
                }
                monitor.worked(1);
            }

            Display.getDefault().syncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection())
                                    .getFirstElement();

                    if (feed.getId().startsWith(YAHOO))
                    {
                        List<Exchange> exchanges = cacheExchanges.get(feed);
                        comboExchange.setInput(exchanges);

                        // run only after exchanges have been re-loaded
                        Exchange selectedExchange = null;
                        if (exchanges != null && comboExchange != null)
                        {
                            for (Exchange e : exchanges)
                            {
                                if (e.getId().equals(model.getTickerSymbol()))
                                {
                                    selectedExchange = e;
                                    comboExchange.setSelection(new StructuredSelection(e));

                                    break;
                                }
                            }
                        }

                        if (selectedExchange == null)
                            clearSampleQuotes();
                        else
                            showSampleQuotes(feed, selectedExchange, null);
                    }

                }
            });

            monitor.done();
        }
    }

    class LoadHistoricalQuotes extends Job
    {
        private QuoteFeed feed;
        private Exchange exchange;

        public LoadHistoricalQuotes(QuoteFeed feed, Exchange exchange)
        {
            super(MessageFormat.format(Messages.JobMsgSamplingHistoricalQuotes, exchange != null ? exchange.getName()
                            : "")); //$NON-NLS-1$
            this.feed = feed;
            this.exchange = exchange;

            currentJob = this;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            try
            {
                Security s = new Security();
                s.setIsin(model.getIsin());
                if (exchange != null)
                    s.setTickerSymbol(exchange.getId());
                s.setFeed(feed.getId());
                s.setFeedURL(model.getFeedURL());

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
                        if (LoadHistoricalQuotes.this.equals(currentJob) && !tableSampleData.getControl().isDisposed())
                        {
                            currentJob = null;
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
            catch (IOException e)
            {
                Display.getDefault().asyncExec(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (LoadHistoricalQuotes.this.equals(currentJob) && !tableSampleData.getControl().isDisposed())
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
