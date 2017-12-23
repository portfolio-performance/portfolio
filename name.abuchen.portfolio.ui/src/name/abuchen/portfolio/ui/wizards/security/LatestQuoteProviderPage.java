package name.abuchen.portfolio.ui.wizards.security;

import static name.abuchen.portfolio.ui.util.SWTHelper.dateWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.placeBelow;
import static name.abuchen.portfolio.ui.util.SWTHelper.widestWidget;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class LatestQuoteProviderPage extends AbstractQuoteProviderPage
{
    private static class DummyQuoteFeed implements QuoteFeed
    {
        @Override
        public String getId()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return Messages.EditWizardOptionSameAsHistoricalQuoteFeed;
        }

        @Override
        public boolean updateLatestQuotes(Security security, List<Exception> errors)
        {
            return false;
        }

        @Override
        public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
        {
            return false;
        }

        @Override
        public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
        {
            return null;
        }

        @Override
        public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
        {
            return null;
        }

        @Override
        public List<Exchange> getExchanges(Security subject, List<Exception> errors)
        {
            return null;
        }
    }

    private class LoadLatestQuote extends Job
    {
        private QuoteFeed feed;
        private Exchange exchange;

        public LoadLatestQuote(QuoteFeed feed, Exchange exchange)
        {
            super(MessageFormat.format(Messages.JobMsgSamplingHistoricalQuotes,
                            exchange != null ? exchange.getName() : "")); //$NON-NLS-1$
            this.feed = feed;
            this.exchange = exchange;
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

                feed.updateLatestQuotes(s, new ArrayList<Exception>());

                Display.getDefault().asyncExec(() -> {
                    if (valueLatestPrices == null || valueLatestPrices.isDisposed())
                        return;

                    if (s.getLatest() != null)
                    {
                        LatestSecurityPrice p = s.getLatest();

                        valueLatestPrices.setText(Values.Quote.format(p.getValue()));
                        valueLatestTrade.setText(Values.Date.format(p.getDate()));
                        long daysHigh = p.getHigh();
                        valueDaysHigh.setText(
                                        daysHigh == -1 ? Messages.LabelNotAvailable : Values.Quote.format(daysHigh));
                        long daysLow = p.getLow();
                        valueDaysLow.setText(daysLow == -1 ? Messages.LabelNotAvailable : Values.Quote.format(daysLow));
                        long volume = p.getVolume();
                        valueVolume.setText(volume == -1 ? Messages.LabelNotAvailable : String.format("%,d", volume)); //$NON-NLS-1$
                        long prevClose = p.getPreviousClose();
                        valuePreviousClose.setText(
                                        prevClose == -1 ? Messages.LabelNotAvailable : Values.Quote.format(prevClose));

                    }
                    else
                    {
                        clearSampleQuotes();
                    }
                });
            }
            catch (Exception e)
            {
                Display.getDefault().asyncExec(() -> clearSampleQuotes());

                PortfolioPlugin.log(e);
            }

            return Status.OK_STATUS;
        }
    }

    private static final String EMPTY_LABEL = ""; //$NON-NLS-1$
    private static final QuoteFeed EMTPY_QUOTE_FEED = new DummyQuoteFeed();

    private Label valueLatestPrices;
    private Label valueLatestTrade;
    private Label valueDaysHigh;
    private Label valueDaysLow;
    private Label valueVolume;
    private Label valuePreviousClose;

    public LatestQuoteProviderPage(final EditSecurityModel model, BindingHelper bindings)
    {
        super(model, bindings);

        setTitle(Messages.EditWizardLatestQuoteFeedTitle);

        // validate that quote provider message is null -> no errors
        bindings.getBindingContext().addValidationStatusProvider(new MultiValidator()
        {
            IObservableValue observable = BeanProperties.value("statusLatestQuotesProvider").observe(model); //$NON-NLS-1$

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
        return getModel().getLatestFeed();
    }

    @Override
    protected void setFeed(String feed)
    {
        getModel().setLatestFeed(feed);
    }

    @Override
    protected String getFeedURL()
    {
        return getModel().getLatestFeedURL();
    }

    @Override
    protected void setFeedURL(String feedURL)
    {
        getModel().setLatestFeedURL(feedURL);
    }

    @Override
    protected void setStatus(String status)
    {
        getModel().setStatusLatestQuotesProvider(status);
    }

    @Override
    protected List<QuoteFeed> getAvailableFeeds()
    {
        List<QuoteFeed> feeds = new ArrayList<>();
        feeds.add(EMTPY_QUOTE_FEED);

        for (QuoteFeed feed : Factory.getQuoteFeedProvider())
        {
            // do not include adjusted close (the difference between close and
            // adjusted close are only relevant for historical quotes)
            if (feed.getId().equals("YAHOO-ADJUSTEDCLOSE")) //$NON-NLS-1$
                continue;

            feeds.add(feed);
        }

        return feeds;
    }

    @Override
    protected QuoteFeed getQuoteFeedProvider(String feedId)
    {
        if (feedId == null)
            return EMTPY_QUOTE_FEED;

        return Factory.getQuoteFeedProvider(feedId);
    }

    @Override
    protected void createSampleArea(Composite container)
    {
        Composite composite = new Composite(container, SWT.NONE);

        Label labelLatestPrice = new Label(composite, SWT.NONE);
        labelLatestPrice.setText(Messages.ColumnLatestPrice);
        valueLatestPrices = new Label(composite, SWT.RIGHT);

        Label labelLatestTrade = new Label(composite, SWT.NONE);
        labelLatestTrade.setText(Messages.ColumnLatestTrade);
        valueLatestTrade = new Label(composite, SWT.RIGHT);

        Label labelDaysHigh = new Label(composite, SWT.NONE);
        labelDaysHigh.setText(Messages.ColumnDaysHigh);
        valueDaysHigh = new Label(composite, SWT.RIGHT);

        Label labelDaysLow = new Label(composite, SWT.NONE);
        labelDaysLow.setText(Messages.ColumnDaysLow);
        valueDaysLow = new Label(composite, SWT.RIGHT);

        Label labelVolume = new Label(composite, SWT.NONE);
        labelVolume.setText(Messages.ColumnVolume);
        valueVolume = new Label(composite, SWT.RIGHT);

        Label labelPreviousClose = new Label(composite, SWT.NONE);
        labelPreviousClose.setText(Messages.ColumnPreviousClose);
        valuePreviousClose = new Label(composite, SWT.RIGHT);

        // layout

        FormLayout layout = new FormLayout();
        layout.marginLeft = 5;
        layout.marginRight = 5;
        composite.setLayout(layout);

        Control biggest = widestWidget(labelLatestPrice, labelLatestTrade, labelDaysHigh, labelDaysLow, labelVolume,
                        labelPreviousClose);
        int width = dateWidth(composite);

        FormData data = new FormData();
        data.top = new FormAttachment(valueLatestPrices, 0, SWT.CENTER);
        labelLatestPrice.setLayoutData(data);

        data = new FormData();
        data.top = new FormAttachment(0, 5);
        data.left = new FormAttachment(biggest, 5);
        data.width = width;
        valueLatestPrices.setLayoutData(data);

        placeBelow(valueLatestPrices, labelLatestTrade, valueLatestTrade);
        placeBelow(valueLatestTrade, labelDaysHigh, valueDaysHigh);
        placeBelow(valueDaysHigh, labelDaysLow, valueDaysLow);
        placeBelow(valueDaysLow, labelVolume, valueVolume);
        placeBelow(valueVolume, labelPreviousClose, valuePreviousClose);
    }

    @Override
    protected void reinitCaches()
    {
        // no caches
    }

    @Override
    protected void clearSampleQuotes()
    {
        valueLatestPrices.setText(EMPTY_LABEL);
        valueLatestTrade.setText(EMPTY_LABEL);
        valueDaysHigh.setText(EMPTY_LABEL);
        valueDaysLow.setText(EMPTY_LABEL);
        valueVolume.setText(EMPTY_LABEL);
        valuePreviousClose.setText(EMPTY_LABEL);
    }

    @Override
    protected void showSampleQuotes(QuoteFeed feed, Exchange exchange)
    {
        new LoadLatestQuote(feed, exchange).schedule();
    }
}
