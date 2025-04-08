package name.abuchen.portfolio.ui.wizards.security;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.MarketIdentifierCodes;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.QuotesTableViewer;

public class SearchSecurityPreviewPricesWizardPage extends WizardPage
{
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
        super("preview"); //$NON-NLS-1$
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
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        if (visible)
        {
            // check if need to load data
            var displayedItem = tableSampleData.getTable().getData();
            var selectedItem = model.getSelectedItem();
            if (selectedItem == displayedItem)
                return;

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

                            var previewData = feed.previewHistoricalQuotes(instrument);

                            progressMonitor.worked(1);

                            Display.getDefault().asyncExec(() -> {
                                cache.put(selectedItem, previewData);

                                tableSampleData.getTable().setData(selectedItem);

                                // check for an error message in the response
                                // object if no prices are returned
                                if (previewData.getLatestPrices().isEmpty() && !previewData.getErrors().isEmpty())
                                {
                                    String[] messages = previewData.getErrors().stream().map(e -> e.getMessage())
                                                    .toList().toArray(new String[0]);
                                    tableSampleData.setMessages(messages);
                                }
                                else
                                {
                                    tableSampleData.setInput(previewData.getLatestPrices());
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
