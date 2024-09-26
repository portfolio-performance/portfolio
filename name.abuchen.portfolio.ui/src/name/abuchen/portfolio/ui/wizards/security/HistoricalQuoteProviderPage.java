package name.abuchen.portfolio.ui.wizards.security;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.AMFIIndiaQuoteFeed;
import name.abuchen.portfolio.online.impl.AlphavantageQuoteFeed;
import name.abuchen.portfolio.online.impl.BinanceQuoteFeed;
import name.abuchen.portfolio.online.impl.BitfinexQuoteFeed;
import name.abuchen.portfolio.online.impl.CoinGeckoQuoteFeed;
import name.abuchen.portfolio.online.impl.EODHistoricalDataQuoteFeed;
import name.abuchen.portfolio.online.impl.FinnhubQuoteFeed;
import name.abuchen.portfolio.online.impl.GenericJSONQuoteFeed;
import name.abuchen.portfolio.online.impl.KrakenQuoteFeed;
import name.abuchen.portfolio.online.impl.LeewayQuoteFeed;
import name.abuchen.portfolio.online.impl.MFAPIQuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.online.impl.QuandlQuoteFeed;
import name.abuchen.portfolio.online.impl.TwelveDataQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;
import name.abuchen.portfolio.ui.util.QuotesTableViewer;

public class HistoricalQuoteProviderPage extends AbstractQuoteProviderPage
{
    private QuoteFeedData feedData;
    private QuotesTableViewer tableSampleData;
    private Button showRawResponse;

    private Map<Object, QuoteFeedData> cacheQuotes = new HashMap<>();

    // read and write 'currentJob' only from the UI thread; used to check
    // whether a more recent job has already been started
    private LoadHistoricalQuotes currentJob;

    public HistoricalQuoteProviderPage(final EditSecurityModel model, BindingHelper bindings)
    {
        super(model, bindings);

        setTitle(Messages.EditWizardQuoteFeedTitle);

        // validate that quote provider message is null -> no errors
        bindings.getBindingContext().addValidationStatusProvider(new MultiValidator()
        {
            IObservableValue<?> observable = BeanProperties.value("statusHistoricalQuotesProvider").observe(model); //$NON-NLS-1$

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
    protected String getJSONDatePropertyName()
    {
        return GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC;
    }

    @Override
    protected String getJSONClosePropertyName()
    {
        return GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC;
    }

    @Override
    protected String getJSONDateFormatPropertyName()
    {
        return GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC;
    }

    @Override
    protected String getJSONLowPathPropertyName()
    {
        return GenericJSONQuoteFeed.LOW_PROPERTY_NAME_HISTORIC;
    }

    @Override
    protected String getJSONHighPathPropertyName()
    {
        return GenericJSONQuoteFeed.HIGH_PROPERTY_NAME_HISTORIC;
    }

    @Override
    protected String getJSONFactorPropertyName()
    {
        return GenericJSONQuoteFeed.FACTOR_PROPERTY_NAME_HISTORIC;
    }

    @Override
    protected String getJSONVolumePathPropertyName()
    {
        return GenericJSONQuoteFeed.VOLUME_PROPERTY_NAME_HISTORIC;
    }

    @Override
    protected void setStatus(String status)
    {
        getModel().setStatusHistoricalQuotesProvider(status);
    }

    @Override
    protected List<QuoteFeed> getAvailableFeeds()
    {
        List<QuoteFeed> feeds = new ArrayList<>();
        feeds.addAll(Factory.getQuoteFeedProvider());

        if (getModel().getSecurity().getOnlineId() == null)
            feeds.remove(Factory.getQuoteFeedProvider(PortfolioReportQuoteFeed.ID));

        return feeds;
    }

    @Override
    protected QuoteFeed getQuoteFeedProvider(String feedId)
    {
        return Factory.getQuoteFeedProvider(feedId);
    }

    @Override
    protected void createAdditionalButtons(Composite buttonArea)
    {
        showRawResponse = new Button(buttonArea, SWT.NONE);
        showRawResponse.setText(Messages.LabelShowRawResponse);
        showRawResponse.setEnabled(false);

        showRawResponse.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            if (feedData == null || feedData.getResponses().isEmpty())
                return;

            new RawResponsesDialog(Display.getCurrent().getActiveShell(), feedData.getResponses()).open();
        }));
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
        cacheQuotes = new HashMap<>();
    }

    @Override
    protected void clearSampleQuotes()
    {
        currentJob = null;
        tableSampleData.setInput(null);
        tableSampleData.refresh();
        feedData = null;
        showRawResponse.setEnabled(false);
    }

    private Object buildCacheKey(Exchange exchange)
    {
        if (exchange != null)
            return getFeed() + exchange.getId();
        else if (PortfolioReportQuoteFeed.ID.equals(getFeed()))
            return PortfolioReportQuoteFeed.ID + getModel().getCurrencyCode();
        else if (AlphavantageQuoteFeed.ID.equals(getFeed()))
            return AlphavantageQuoteFeed.ID + getModel().getTickerSymbol();
        else if (AMFIIndiaQuoteFeed.ID.equals(getFeed()))
            return AMFIIndiaQuoteFeed.ID + getModel().getIsin();
        else if (FinnhubQuoteFeed.ID.equals(getFeed()))
            return FinnhubQuoteFeed.ID + getModel().getTickerSymbol();
        else if (BinanceQuoteFeed.ID.equals(getFeed()))
            return BinanceQuoteFeed.ID + getModel().getTickerSymbol();
        else if (BitfinexQuoteFeed.ID.equals(getFeed()))
            return BitfinexQuoteFeed.ID + getModel().getTickerSymbol();
        else if (KrakenQuoteFeed.ID.equals(getFeed()))
            return KrakenQuoteFeed.ID + getModel().getTickerSymbol();
        else if (LeewayQuoteFeed.ID.equals(getFeed()))
            return LeewayQuoteFeed.ID + getModel().getTickerSymbol();
        else if (TwelveDataQuoteFeed.ID.equals(getFeed()))
            return TwelveDataQuoteFeed.ID + getModel().getTickerSymbol();
        else if (CoinGeckoQuoteFeed.ID.equals(getFeed()))
            return CoinGeckoQuoteFeed.ID //
                            + getModel().getTickerSymbol() //
                            + getModel().getCurrencyCode()
                            + String.valueOf(getModel().getFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID));
        else if (MFAPIQuoteFeed.ID.equals(getFeed()))
            return MFAPIQuoteFeed.ID + String.valueOf(getModel().getFeedProperty(CoinGeckoQuoteFeed.COINGECKO_COIN_ID));
        else if (EODHistoricalDataQuoteFeed.ID.equals(getFeed()))
            return EODHistoricalDataQuoteFeed.ID + getModel().getTickerSymbol();
        else if (QuandlQuoteFeed.ID.equals(getFeed()))
            return QuandlQuoteFeed.ID
                            + String.valueOf(getModel().getFeedProperty(QuandlQuoteFeed.QUANDL_CODE_PROPERTY_NAME))
                            + String.valueOf(getModel()
                                            .getFeedProperty(QuandlQuoteFeed.QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME));
        else if (GenericJSONQuoteFeed.ID.equals(getFeed()))
            return GenericJSONQuoteFeed.ID + getModel().getFeedURL()
                            + String.valueOf(getModel()
                                            .getFeedProperty(GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC))
                            + String.valueOf(getModel()
                                            .getFeedProperty(GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC))
                            + String.valueOf(getModel()
                                            .getFeedProperty(GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC))
                            + String.valueOf(
                                            getModel().getFeedProperty(GenericJSONQuoteFeed.LOW_PROPERTY_NAME_HISTORIC))
                            + String.valueOf(getModel()
                                            .getFeedProperty(GenericJSONQuoteFeed.HIGH_PROPERTY_NAME_HISTORIC))
                            + String.valueOf(getModel()
                                            .getFeedProperty(GenericJSONQuoteFeed.FACTOR_PROPERTY_NAME_HISTORIC))
                            + String.valueOf(getModel()
                                            .getFeedProperty(GenericJSONQuoteFeed.VOLUME_PROPERTY_NAME_HISTORIC));
        else
            return getFeed() + getModel().getFeedURL();
    }

    @Override
    protected void showSampleQuotes(QuoteFeed feed, Exchange exchange)
    {
        Object cacheKey = buildCacheKey(exchange);

        QuoteFeedData data = cacheQuotes.get(cacheKey);

        if (data != null)
        {
            feedData = data;

            tableSampleData.setInput(data.getLatestPrices());
            tableSampleData.refresh();

            showRawResponse.setEnabled(!data.getResponses().isEmpty());
        }
        else
        {
            feedData = null;

            tableSampleData.setMessage(Messages.EditWizardQuoteFeedMsgLoading);
            tableSampleData.refresh();

            showRawResponse.setEnabled(false);

            Job job = new LoadHistoricalQuotes(feed, exchange, cacheKey);
            job.setUser(true);
            job.schedule(150);
        }
    }

    private class LoadHistoricalQuotes extends Job
    {
        private QuoteFeed feed;
        private Exchange exchange;
        private Object cacheKey;

        public LoadHistoricalQuotes(QuoteFeed feed, Exchange exchange, Object cacheKey)
        {
            super(MessageFormat.format(Messages.JobMsgSamplingHistoricalQuotes,
                            exchange != null ? exchange.getName() : "")); //$NON-NLS-1$
            this.feed = feed;
            this.exchange = exchange;
            this.cacheKey = cacheKey;

            HistoricalQuoteProviderPage.this.currentJob = this;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            try
            {
                Security s = buildTemporarySecurity();
                if (exchange != null)
                {
                    s.setTickerSymbol(exchange.getId());
                }
                s.setFeed(feed.getId());

                QuoteFeedData data = feed.previewHistoricalQuotes(s);

                Display.getDefault().asyncExec(() -> {
                    if (LoadHistoricalQuotes.this.equals(HistoricalQuoteProviderPage.this.currentJob)
                                    && !tableSampleData.getControl().isDisposed())
                    {
                        HistoricalQuoteProviderPage.this.currentJob = null;
                        cacheQuotes.put(cacheKey, data);

                        feedData = data;

                        // check for an error message in the response object if
                        // no prices are returned
                        if (data.getLatestPrices().isEmpty() && !data.getErrors().isEmpty())
                        {
                            String[] messages = data.getErrors().stream().map(e -> e.getMessage()).toList()
                                            .toArray(new String[0]);
                            tableSampleData.setMessages(messages);
                        }
                        else
                        {
                            tableSampleData.setInput(data.getLatestPrices());
                        }

                        tableSampleData.refresh();

                        showRawResponse.setEnabled(!data.getResponses().isEmpty());
                    }
                });
            }
            catch (Exception e)
            {
                Display.getDefault().asyncExec(() -> {
                    if (LoadHistoricalQuotes.this.equals(HistoricalQuoteProviderPage.this.currentJob)
                                    && !tableSampleData.getControl().isDisposed())
                    {
                        currentJob = null;

                        feedData = null;

                        String message = e.getMessage();
                        if (message == null || message.isEmpty())
                            message = Messages.EditWizardQuoteFeedMsgErrorOrNoData;

                        tableSampleData.setMessage(message);
                        tableSampleData.refresh();

                        showRawResponse.setEnabled(false);
                    }
                });

                PortfolioPlugin.log(e);
            }

            return Status.OK_STATUS;
        }

    }
}
