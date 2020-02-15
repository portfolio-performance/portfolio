package name.abuchen.portfolio.ui.wizards.security;

import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.CSQuoteFeed;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;
import name.abuchen.portfolio.online.impl.FinnhubQuoteFeed;
import name.abuchen.portfolio.online.impl.HTMLTableQuoteFeed;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;

public abstract class AbstractQuoteProviderPage extends AbstractPage
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
            List<QuoteFeed> provider = getAvailableFeeds();
            monitor.beginTask(Messages.JobMsgLoadingExchanges, provider.size());
            for (QuoteFeed feed : provider)
            {
                Security s = buildTemporarySecurity();

                List<Exception> errors = new ArrayList<>();
                cacheExchanges.put(feed, feed.getExchanges(s, errors));

                PortfolioPlugin.log(errors);

                monitor.worked(1);
            }

            Display.getDefault().asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection())
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
                                clearSampleQuotes();
                            else
                                showSampleQuotes(feed, (Exchange) ((StructuredSelection) comboExchange.getSelection())
                                                .getFirstElement());
                        }
                        else
                        {
                            if (exchanges == null || exchanges.isEmpty())
                            {
                                showSampleQuotes(feed, null);
                            }
                        }
                    }

                }
            });

            monitor.done();
            return Status.OK_STATUS;
        }
    }

    private static final String YAHOO = "YAHOO"; //$NON-NLS-1$
    private static final String HTML = "HTML"; //$NON-NLS-1$

    private ComboViewer comboProvider;

    private Group grpQuoteFeed;
    private Label labelDetailData;

    private ComboViewer comboExchange;
    private Text textFeedURL;
    private Text textTicker;
    private Text textQuandlCode;
    private Label labelQuandlCloseColumnName;
    private Text textQuandlCloseColumnName;

    private PropertyChangeListener tickerSymbolPropertyChangeListener = e -> onTickerSymbolChanged();

    private final EditSecurityModel model;
    private final BindingHelper bindings;

    // used to identify if the ticker has been changed on another page
    private String tickerSymbol;

    private Map<QuoteFeed, List<Exchange>> cacheExchanges = new HashMap<>();

    protected AbstractQuoteProviderPage(EditSecurityModel model, BindingHelper bindings)
    {
        this.model = model;
        this.bindings = bindings;
    }

    protected final EditSecurityModel getModel()
    {
        return model;
    }

    protected abstract String getFeed();

    protected abstract void setFeed(String feed);

    protected abstract String getFeedURL();

    protected abstract void setFeedURL(String feedURL);

    protected abstract void setStatus(String status);

    protected abstract void createSampleArea(Composite parent);

    protected abstract List<QuoteFeed> getAvailableFeeds();

    protected abstract QuoteFeed getQuoteFeedProvider(String feedId);

    protected abstract void reinitCaches();

    protected abstract void clearSampleQuotes();

    protected abstract void showSampleQuotes(QuoteFeed feed, Exchange exchange);

    @Override
    public void beforePage()
    {
        if (!Objects.equals(tickerSymbol, model.getTickerSymbol()))
        {
            this.tickerSymbol = model.getTickerSymbol();

            // clear caches
            cacheExchanges = new HashMap<>();
            reinitCaches();

            new LoadExchangesJob().schedule();

            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();

            if (feed.getId() != null && feed.getId().indexOf(HTML) >= 0)
            {
                if (getFeedURL() == null || getFeedURL().length() == 0)
                    clearSampleQuotes();
                else
                    showSampleQuotes(feed, null);
            }
        }

        if (textQuandlCode != null && !textQuandlCode.getText()
                        .equals(model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME)))
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            textQuandlCode.setText(code != null ? code : ""); //$NON-NLS-1$
        }

        if (textQuandlCloseColumnName != null && !textQuandlCloseColumnName.getText()
                        .equals(model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME)))
        {
            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            textQuandlCloseColumnName.setText(columnName != null ? columnName : ""); //$NON-NLS-1$
        }
    }

    @Override
    public void afterPage()
    {
        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        setFeed(feed.getId());

        if (comboExchange != null && feed.getId() != null
                        && (feed.getId().startsWith(YAHOO) || feed.getId().equals(EurostatHICPQuoteFeed.ID)))
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
        data.top = new FormAttachment(grpQuoteFeed, 5);
        data.left = new FormAttachment(0, 10);
        data.right = new FormAttachment(100, -10);
        data.bottom = new FormAttachment(100, -10);
        sampleArea.setLayoutData(data);

        setupInitialData();

        comboProvider.addSelectionChangedListener(this::onFeedProviderChanged);
    }

    /**
     * Builds a temporary {@link Security} from the currently selected values.
     * 
     * @return {@link Security}
     */
    protected Security buildTemporarySecurity()
    {
        // create a temporary security and set all attributes
        Security security = new Security();
        model.setAttributes(security);
        model.getSecurity().getProperties().forEach(security::addProperty);
        return security;
    }

    private void createProviderGroup(Composite container)
    {
        grpQuoteFeed = new Group(container, SWT.NONE);
        grpQuoteFeed.setText(Messages.LabelQuoteFeed);
        FormData formData = new FormData();
        formData.top = new FormAttachment(0, 5);
        formData.left = new FormAttachment(0, 10);
        grpQuoteFeed.setLayoutData(formData);
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
        comboProvider.setInput(getAvailableFeeds());
        GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(comboProvider.getControl());

        labelDetailData = new Label(grpQuoteFeed, SWT.NONE);
        GridDataFactory.fillDefaults().indent(0, 5).applyTo(labelDetailData);

        createDetailDataWidgets(null);
    }

    private void createDetailDataWidgets(QuoteFeed feed)
    {
        boolean dropDown = feed != null && feed.getId() != null
                        && (feed.getId().startsWith(YAHOO) || feed.getId().equals(EurostatHICPQuoteFeed.ID));
        boolean feedURL = feed != null && feed.getId() != null
                        && (feed.getId().equals(HTMLTableQuoteFeed.ID) || feed.getId().equals(CSQuoteFeed.ID));
        boolean needsTicker = feed != null && feed.getId() != null
                        && (feed.getId().equals(AlphavantageQuoteFeed.ID) || feed.getId().equals(FinnhubQuoteFeed.ID));

        boolean needsQuandlCode = feed != null && feed.getId() != null && feed.getId().equals(QuandlQuoteFeed.ID);

        if (textFeedURL != null)
        {
            textFeedURL.dispose();
            textFeedURL = null;
        }

        if (comboExchange != null)
        {
            comboExchange.getControl().dispose();
            comboExchange = null;
        }

        if (textTicker != null)
        {
            textTicker.dispose();
            textTicker = null;

            model.removePropertyChangeListener("tickerSymbol", tickerSymbolPropertyChangeListener); //$NON-NLS-1$
        }

        if (textQuandlCode != null)
        {
            textQuandlCode.dispose();
            textQuandlCode = null;
        }

        if (labelQuandlCloseColumnName != null)
        {
            labelQuandlCloseColumnName.dispose();
            labelQuandlCloseColumnName = null;
        }

        if (textQuandlCloseColumnName != null)
        {
            textQuandlCloseColumnName.dispose();
            textQuandlCloseColumnName = null;
        }

        if (dropDown)
        {
            labelDetailData.setText(Messages.LabelExchange);

            comboExchange = new ComboViewer(grpQuoteFeed, SWT.READ_ONLY);
            comboExchange.setContentProvider(ArrayContentProvider.getInstance());
            comboExchange.setLabelProvider(new LabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    Exchange exchange = (Exchange) element;
                    return MessageFormat.format("{0} ({1})", exchange.getId(), exchange.getName()); //$NON-NLS-1$
                }
            });
            GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(comboExchange.getControl());

            comboExchange.addSelectionChangedListener(this::onExchangeChanged);
        }
        else if (feedURL)
        {
            labelDetailData.setText(Messages.EditWizardQuoteFeedLabelFeedURL);

            textFeedURL = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().hint(300, SWT.DEFAULT).applyTo(textFeedURL);

            textFeedURL.addModifyListener(e -> onFeedURLChanged());
        }
        else if (needsTicker)
        {
            labelDetailData.setText(Messages.ColumnTicker);

            textTicker = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).applyTo(textTicker);

            IObservableValue<?> observeText = WidgetProperties.text(SWT.Modify).observe(textTicker);
            @SuppressWarnings("unchecked")
            IObservableValue<?> observable = BeanProperties.value("tickerSymbol").observe(model); //$NON-NLS-1$
            bindings.getBindingContext().bindValue(observeText, observable);

            model.addPropertyChangeListener("tickerSymbol", tickerSymbolPropertyChangeListener); //$NON-NLS-1$
        }
        else if (needsQuandlCode)
        {
            labelDetailData.setText(Messages.LabelQuandlCode);

            textQuandlCode = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).applyTo(textQuandlCode);
            textQuandlCode.addModifyListener(e -> onQuandlCodeChanged());

            labelQuandlCloseColumnName = new Label(grpQuoteFeed, SWT.NONE);
            labelQuandlCloseColumnName.setText(Messages.LabelQuandlColumnNameQuote);

            textQuandlCloseColumnName = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().hint(100, SWT.DEFAULT).applyTo(textQuandlCloseColumnName);

            Image image = FieldDecorationRegistry.getDefault()
                            .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage();
            ControlDecoration deco = new ControlDecoration(textQuandlCloseColumnName, SWT.TOP | SWT.LEFT);
            deco.setDescriptionText(Messages.LabelQuandlColumnNameQuoteHint);
            deco.setImage(image);
            deco.show();

            textQuandlCloseColumnName.addModifyListener(e -> onQuandlColumnNameChanged());
        }
        else
        {
            labelDetailData.setText(""); //$NON-NLS-1$
        }

        grpQuoteFeed.layout(true);
        grpQuoteFeed.getParent().layout();
    }

    private void setupInitialData()
    {
        QuoteFeed feed = getQuoteFeedProvider(getFeed());

        if (feed != null)
            comboProvider.setSelection(new StructuredSelection(feed));
        else
            comboProvider.getCombo().select(0);

        createDetailDataWidgets(feed);

        if (model.getTickerSymbol() != null && feed != null && feed.getId() != null
                        && (feed.getId().startsWith(YAHOO) || feed.getId().equals(EurostatHICPQuoteFeed.ID)))
        {
            Exchange exchange = new Exchange(model.getTickerSymbol(), model.getTickerSymbol());
            ArrayList<Exchange> input = new ArrayList<>();
            input.add(exchange);
            comboExchange.setInput(input);
            comboExchange.setSelection(new StructuredSelection(exchange));
        }
        else if (textFeedURL != null)
        {
            textFeedURL.setText(getFeedURL());
        }
        else if (textQuandlCode != null)
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            if (code != null)
                textQuandlCode.setText(code);

            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            if (columnName != null)
                textQuandlCloseColumnName.setText(columnName);
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

        if (previousExchangeId == null && model.getTickerSymbol() != null)
        {
            previousExchangeId = model.getTickerSymbol();
        }

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) event.getSelection()).getFirstElement();

        createDetailDataWidgets(feed);

        clearSampleQuotes();

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

            setStatus(hasURL ? null : MessageFormat.format(Messages.EditWizardQuoteFeedMsgErrorMissingURL, getTitle()));
        }
        else if (textQuandlCode != null)
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            if (code != null)
                textQuandlCode.setText(code);

            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            if (columnName != null)
                textQuandlCloseColumnName.setText(columnName);
        }
        else
        {
            // get sample quotes?
            if (feed != null)
            {
                showSampleQuotes(feed, null);
            }
            else
            {
                clearSampleQuotes();
            }
            setStatus(null);
        }
    }

    private void onExchangeChanged(SelectionChangedEvent event)
    {
        Exchange exchange = (Exchange) ((IStructuredSelection) event.getSelection()).getFirstElement();
        setStatus(null);

        if (exchange == null)
        {
            clearSampleQuotes();
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, exchange);
        }
    }

    private void onFeedURLChanged()
    {
        setFeedURL(textFeedURL.getText());

        boolean hasURL = getFeedURL() != null && getFeedURL().length() > 0;

        if (!hasURL)
        {
            clearSampleQuotes();
            setStatus(MessageFormat.format(Messages.EditWizardQuoteFeedMsgErrorMissingURL, getTitle()));
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, null);
            setStatus(null);
        }
    }

    private void onTickerSymbolChanged()
    {
        boolean hasTicker = model.getTickerSymbol() != null && !model.getTickerSymbol().isEmpty();

        if (!hasTicker)
        {
            clearSampleQuotes();
            setStatus(MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnTicker));
        }
        else
        {
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, null);
            setStatus(null);
        }
    }

    private void onQuandlCodeChanged()
    {
        String quandlCode = textQuandlCode.getText();

        boolean hasCode = quandlCode != null && Pattern.matches("^.+/.+$", quandlCode); //$NON-NLS-1$

        if (!hasCode)
        {
            clearSampleQuotes();
            setStatus(Messages.MsgErrorMissingQuandlCode);
        }
        else
        {
            model.setFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME, quandlCode);
            QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
            showSampleQuotes(feed, null);
            setStatus(null);
        }
    }

    private void onQuandlColumnNameChanged()
    {
        String closeColumnName = textQuandlCloseColumnName.getText();

        model.setFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME,
                        closeColumnName.isEmpty() ? null : closeColumnName);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

}
