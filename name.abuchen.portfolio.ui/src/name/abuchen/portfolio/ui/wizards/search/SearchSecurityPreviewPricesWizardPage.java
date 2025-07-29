package name.abuchen.portfolio.ui.wizards.search;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.QuoteFeedException;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.MarketIdentifierCodes;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.QuotesTableViewer;

public class SearchSecurityPreviewPricesWizardPage extends WizardPage
{
    public static final String PAGE_ID = "preview"; //$NON-NLS-1$

    private final SearchSecurityDataModel model;

    private QuotesTableViewer tableSampleData;

    private LinkedHashMap<ResultItem, QuoteFeedData> cache = new LinkedHashMap<>()
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<ResultItem, QuoteFeedData> eldest)
        {
            return size() > 10;
        }
    };

    public SearchSecurityPreviewPricesWizardPage(SearchSecurityDataModel model)
    {
        super(PAGE_ID);
        setTitle(Messages.SecurityMenuAddNewSecurity);

        this.model = model;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableSampleData = new QuotesTableViewer(container);

        setControl(container);
    }

    @Override
    public IWizardPage getPreviousPage()
    {
        return getWizard().getPage(
                        model.getSelectedInstrument() == model.getSelectedMarket() ? SearchSecurityWizardPage.PAGE_ID
                                        : SelectMarketsWizardPage.PAGE_ID);
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        if (visible)
        {
            // check if need to load data
            var displayedItem = tableSampleData.getTable().getData();
            var selectedItem = model.getSelectedMarket();
            if (selectedItem == displayedItem)
                return;

            // if selected item is null, clear the table
            if (selectedItem == null)
            {
                tableSampleData.getTable().setData(null);
                tableSampleData.setInput(Collections.emptyList());
                return;
            }

            if (selectedItem.getExchange() != null)
            {
                setTitle(MessageFormat.format(Messages.LabelColonSeparated, selectedItem.getName(),
                                MarketIdentifierCodes.getLabel(selectedItem.getExchange())));
            }
            else
            {
                setTitle(selectedItem.getName());
            }

            // check the cache
            QuoteFeedData data = cache.get(selectedItem);
            if (data != null)
            {
                tableSampleData.getTable().setData(selectedItem);

                // check for an error message in the response
                // object if no prices are returned
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
            }
            else
            {
                tableSampleData.getTable().setData(null);
                tableSampleData.setInput(Collections.emptyList());

                var instrument = selectedItem.create(model.getClient());
                var feed = Factory.getQuoteFeedProvider(instrument.getFeed());

                if (feed != null)
                {
                    try
                    {
                        getContainer().run(true, false, progressMonitor -> {

                            var exchange = selectedItem.getExchange() != null
                                            ? MarketIdentifierCodes.getLabel(selectedItem.getExchange())
                                            : ""; //$NON-NLS-1$

                            progressMonitor.beginTask(
                                            MessageFormat.format(Messages.JobMsgSamplingHistoricalQuotes, exchange), 1);

                            QuoteFeedData previewData;
                            try
                            {
                                previewData = feed.previewHistoricalQuotes(instrument);
                            }
                            catch (QuoteFeedException e)
                            {
                                previewData = QuoteFeedData.withError(e);
                            }

                            progressMonitor.worked(1);

                            var feedData = previewData;
                            Display.getDefault().asyncExec(() -> {
                                cache.put(selectedItem, feedData);

                                tableSampleData.getTable().setData(selectedItem);

                                // check for an error message in the response
                                // object if no prices are returned
                                if (feedData.getLatestPrices().isEmpty() && !feedData.getErrors().isEmpty())
                                {
                                    String[] messages = feedData.getErrors().stream().map(e -> e.getMessage()).toList()
                                                    .toArray(new String[0]);
                                    tableSampleData.setMessages(messages);
                                }
                                else
                                {
                                    tableSampleData.setInput(feedData.getLatestPrices());
                                }
                            });

                        });
                    }
                    catch (InvocationTargetException | InterruptedException e)
                    {
                        PortfolioPlugin.log(e);
                    }
                }

            }
        }
    }
}
