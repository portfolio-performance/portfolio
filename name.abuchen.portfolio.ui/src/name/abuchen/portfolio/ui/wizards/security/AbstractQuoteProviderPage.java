package name.abuchen.portfolio.ui.wizards.security;

import java.beans.PropertyChangeListener;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.BinanceQuoteFeed;
import name.abuchen.portfolio.online.impl.CSQuoteFeed;
import name.abuchen.portfolio.online.impl.EurostatHICPQuoteFeed;
import name.abuchen.portfolio.online.impl.FinnhubQuoteFeed;
import name.abuchen.portfolio.online.impl.GenericJSONQuoteFeed;
import name.abuchen.portfolio.online.impl.HTMLTableQuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.DesktopAPI;
import name.abuchen.portfolio.ui.util.SWTHelper;

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

                                String code = feed.getId().equals(PortfolioReportQuoteFeed.ID)
                                                ? model.getFeedProperty(PortfolioReportQuoteFeed.MARKET_PROPERTY_NAME)
                                                : model.getTickerSymbol();

                                // if ticker symbol matches any of the
                                // exchanges, select this exchange in the
                                // combo list
                                exchanges.stream() //
                                                .filter(e -> e.getId().equals(code)) //
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

    private Label labelJsonPathDate;
    private Text textJsonPathDate;
    private Label labelJsonPathClose;
    private Text textJsonPathClose;

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

    protected abstract String getJSONDatePropertyName();

    protected abstract String getJSONClosePropertyName();

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
        }

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        if (feed != null && feed.getId() != null && feed.getId().indexOf(HTML) >= 0)
        {
            if (getFeedURL() == null || getFeedURL().length() == 0)
                clearSampleQuotes();
            else
                showSampleQuotes(feed, null);
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

        if (textJsonPathDate != null
                        && !textJsonPathDate.getText().equals(model.getFeedProperty(getJSONDatePropertyName())))
        {
            String path = model.getFeedProperty(getJSONDatePropertyName());
            textJsonPathDate.setText(path != null ? path : ""); //$NON-NLS-1$
        }

        if (textJsonPathClose != null
                        && !textJsonPathClose.getText().equals(model.getFeedProperty(getJSONClosePropertyName())))
        {
            String path = model.getFeedProperty(getJSONClosePropertyName());
            textJsonPathClose.setText(path != null ? path : ""); //$NON-NLS-1$
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
        else if (comboExchange != null && feed.getId() != null && feed.getId().equals(PortfolioReportQuoteFeed.ID))
        {
            Exchange exchange = (Exchange) ((IStructuredSelection) comboExchange.getSelection()).getFirstElement();
            model.setFeedProperty(PortfolioReportQuoteFeed.MARKET_PROPERTY_NAME,
                            exchange != null ? exchange.getId() : null);
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

        Composite buttonArea = new Composite(container, SWT.NONE);
        buttonArea.setLayout(new RowLayout(SWT.VERTICAL));
        createAdditionalButtons(buttonArea);

        FormData data = new FormData();
        data.top = new FormAttachment(grpQuoteFeed, 20, SWT.TOP);
        data.left = new FormAttachment(grpQuoteFeed, 10);
        data.right = new FormAttachment(100, -10);
        data.bottom = new FormAttachment(grpQuoteFeed, -10, SWT.BOTTOM);
        buttonArea.setLayoutData(data);

        Composite sampleArea = new Composite(container, SWT.NONE);
        sampleArea.setLayout(new FillLayout());
        createSampleArea(sampleArea);

        data = new FormData();
        data.top = new FormAttachment(grpQuoteFeed, 5);
        data.left = new FormAttachment(0, 10);
        data.right = new FormAttachment(100, -10);
        data.bottom = new FormAttachment(100, -10);
        sampleArea.setLayoutData(data);

        setupInitialData();

        comboProvider.addSelectionChangedListener(this::onFeedProviderChanged);
    }

    protected void createAdditionalButtons(Composite container)
    {
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
        GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(5, 15, 5, 5).applyTo(grpQuoteFeed);

        Label lblProvider = new Label(grpQuoteFeed, SWT.NONE);
        lblProvider.setText(Messages.LabelQuoteFeedProvider);

        comboProvider = SWTHelper.createComboViewer(grpQuoteFeed);
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

        Link link = new Link(grpQuoteFeed, SWT.UNDERLINE_LINK);
        link.setText("<a>" + Messages.IntroLabelHelp + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        link.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            try
            {
                QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();

                String url = "https://help.portfolio-performance.info/kursdaten_laden/"; //$NON-NLS-1$

                if (feed != null && feed.getHelpURL().isPresent())
                    url = feed.getHelpURL().get();

                // Use Google translate for non-German users (as the help pages
                // are currently only available in German). Taking care to
                // encode the #.
                if (!Locale.getDefault().getLanguage().equals(Locale.GERMAN.getLanguage()))
                    url = MessageFormat.format(Messages.HelpURL, URLEncoder.encode(url, StandardCharsets.UTF_8.name()));

                DesktopAPI.browse(url);
            }
            catch (UnsupportedEncodingException ignore)
            {
                // UTF-8 is supported
            }
        }));

        labelDetailData = new Label(grpQuoteFeed, SWT.NONE);
        GridDataFactory.fillDefaults().indent(0, 5).applyTo(labelDetailData);

        createDetailDataWidgets(null);
    }

    private void createDetailDataWidgets(QuoteFeed feed)
    {
        boolean dropDown = feed != null && feed.getId() != null
                        && (feed.getId().startsWith(YAHOO) || feed.getId().equals(EurostatHICPQuoteFeed.ID)
                                        || feed.getId().equals(PortfolioReportQuoteFeed.ID));

        boolean feedURL = feed != null && feed.getId() != null && (feed.getId().equals(HTMLTableQuoteFeed.ID)
                        || feed.getId().equals(CSQuoteFeed.ID) || feed.getId().equals(GenericJSONQuoteFeed.ID));

        boolean needsTicker = feed != null && feed.getId() != null && (feed.getId().equals(AlphavantageQuoteFeed.ID)
                        || feed.getId().equals(FinnhubQuoteFeed.ID) || feed.getId().equals(BinanceQuoteFeed.ID));

        boolean needsQuandlCode = feed != null && feed.getId() != null && feed.getId().equals(QuandlQuoteFeed.ID);

        boolean needsJsonPath = feed != null && feed.getId() != null && feed.getId().equals(GenericJSONQuoteFeed.ID);

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

        labelJsonPathDate = disposeIf(labelJsonPathDate);
        textJsonPathDate = disposeIf(textJsonPathDate);
        labelJsonPathClose = disposeIf(labelJsonPathClose);
        textJsonPathClose = disposeIf(textJsonPathClose);

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
            GridDataFactory.fillDefaults().span(2, 1).hint(300, SWT.DEFAULT).applyTo(comboExchange.getControl());

            comboExchange.addSelectionChangedListener(this::onExchangeChanged);
        }

        if (feedURL)
        {
            labelDetailData.setText(Messages.EditWizardQuoteFeedLabelFeedURL);

            textFeedURL = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(300, SWT.DEFAULT).applyTo(textFeedURL);

            textFeedURL.addModifyListener(e -> onFeedURLChanged());
        }

        if (needsTicker)
        {
            labelDetailData.setText(Messages.ColumnTicker);

            textTicker = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textTicker);

            IObservableValue<?> observeText = WidgetProperties.text(SWT.Modify).observe(textTicker);
            IObservableValue<?> observable = BeanProperties.value("tickerSymbol").observe(model); //$NON-NLS-1$
            bindings.getBindingContext().bindValue(observeText, observable);

            model.addPropertyChangeListener("tickerSymbol", tickerSymbolPropertyChangeListener); //$NON-NLS-1$
        }

        if (needsQuandlCode)
        {
            labelDetailData.setText(Messages.LabelQuandlCode);

            textQuandlCode = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textQuandlCode);
            textQuandlCode.addModifyListener(e -> onQuandlCodeChanged());

            labelQuandlCloseColumnName = new Label(grpQuoteFeed, SWT.NONE);
            labelQuandlCloseColumnName.setText(Messages.LabelQuandlColumnNameQuote);

            textQuandlCloseColumnName = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textQuandlCloseColumnName);

            ControlDecoration deco = new ControlDecoration(textQuandlCloseColumnName, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelQuandlColumnNameQuoteHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            textQuandlCloseColumnName.addModifyListener(e -> onQuandlColumnNameChanged());
        }

        if (needsJsonPath)
        {
            labelJsonPathDate = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonPathDate.setText(Messages.LabelJSONPathToDate);

            textJsonPathDate = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonPathDate);
            textJsonPathDate.addModifyListener(e -> onJsonPathDateChanged());

            ControlDecoration deco = new ControlDecoration(textJsonPathDate, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONPathHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();

            labelJsonPathClose = new Label(grpQuoteFeed, SWT.NONE);
            labelJsonPathClose.setText(Messages.LabelJSONPathToClose);

            textJsonPathClose = new Text(grpQuoteFeed, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).hint(100, SWT.DEFAULT).applyTo(textJsonPathClose);
            textJsonPathClose.addModifyListener(e -> onJsonPathCloseChanged());

            deco = new ControlDecoration(textJsonPathClose, SWT.CENTER | SWT.RIGHT);
            deco.setDescriptionText(Messages.LabelJSONPathHint);
            deco.setImage(Images.INFO.image());
            deco.setMarginWidth(2);
            deco.show();
        }

        if (!dropDown && !feedURL && !needsTicker && !needsQuandlCode && !needsJsonPath)
        {
            labelDetailData.setText(""); //$NON-NLS-1$
        }

        grpQuoteFeed.layout(true);
        grpQuoteFeed.getParent().layout();
    }

    private <T extends Control> T disposeIf(T control)
    {
        if (control != null && !control.isDisposed())
            control.dispose();
        return null;
    }

    private void setupInitialData()
    {
        this.tickerSymbol = model.getTickerSymbol();

        new LoadExchangesJob().schedule();

        QuoteFeed feed = getQuoteFeedProvider(getFeed());

        if (feed != null)
            comboProvider.setSelection(new StructuredSelection(feed));
        else
            comboProvider.setSelection(new StructuredSelection(getAvailableFeeds().get(0)));

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

        if (feed != null && feed.getId() != null && feed.getId().equals(PortfolioReportQuoteFeed.ID))
        {
            String code = model.getFeedProperty(PortfolioReportQuoteFeed.MARKET_PROPERTY_NAME);

            if (code != null)
            {
                Exchange exchange = new Exchange(code, code);
                comboExchange.setInput(Arrays.asList(exchange));
                comboExchange.setSelection(new StructuredSelection(exchange));
            }
        }

        if (textFeedURL != null && getFeedURL() != null)
        {
            textFeedURL.setText(getFeedURL());
        }

        if (textQuandlCode != null)
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            if (code != null)
                textQuandlCode.setText(code);

            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            if (columnName != null)
                textQuandlCloseColumnName.setText(columnName);
        }

        if (textJsonPathDate != null)
        {
            String datePath = model.getFeedProperty(getJSONDatePropertyName());
            if (datePath != null)
                textJsonPathDate.setText(datePath);

            String closePath = model.getFeedProperty(getJSONClosePropertyName());
            if (closePath != null)
                textJsonPathClose.setText(closePath);
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
        if (feed != null)
            setFeed(feed.getId());

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

            if (!exchangeSelected && exchanges != null && exchanges.size() == 1)
            {
                comboExchange.setSelection(new StructuredSelection(exchanges.get(0)));
                exchangeSelected = true;
            }

            if (!exchangeSelected)
                comboExchange.setSelection(null);

            setStatus(exchangeSelected ? null : MessageFormat.format(Messages.MsgErrorExchangeMissing, getTitle()));
        }

        if (textFeedURL != null)
        {
            boolean hasURL = getFeedURL() != null && getFeedURL().length() > 0;

            if (hasURL)
                textFeedURL.setText(getFeedURL());

            setStatus(hasURL ? null : MessageFormat.format(Messages.EditWizardQuoteFeedMsgErrorMissingURL, getTitle()));
        }

        if (textQuandlCode != null)
        {
            String code = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME);
            if (code != null)
                textQuandlCode.setText(code);

            String columnName = model.getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);
            if (columnName != null)
                textQuandlCloseColumnName.setText(columnName);
        }

        if (textJsonPathDate != null)
        {
            String datePath = model.getFeedProperty(getJSONDatePropertyName());
            if (datePath != null)
                textJsonPathDate.setText(datePath);

            String closePath = model.getFeedProperty(getJSONClosePropertyName());
            if (closePath != null)
                textJsonPathClose.setText(closePath);
        }

        if (comboExchange == null && textFeedURL == null && textQuandlCode == null && textJsonPathDate == null)
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

    private void onJsonPathDateChanged()
    {
        String datePath = textJsonPathDate.getText();

        model.setFeedProperty(getJSONDatePropertyName(), datePath.isEmpty() ? null : datePath);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }

    private void onJsonPathCloseChanged()
    {
        String closePath = textJsonPathClose.getText();

        model.setFeedProperty(getJSONClosePropertyName(), closePath.isEmpty() ? null : closePath);

        QuoteFeed feed = (QuoteFeed) ((IStructuredSelection) comboProvider.getSelection()).getFirstElement();
        showSampleQuotes(feed, null);
        setStatus(null);
    }
}
