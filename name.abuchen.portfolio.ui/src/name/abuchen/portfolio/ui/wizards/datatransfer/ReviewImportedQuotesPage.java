package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.HTMLTableQuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.QuotesTableViewer;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ReviewImportedQuotesPage extends AbstractWizardPage
{
    private ImportSourcePage page;
    private QuotesTableViewer tableSampleData;

    private List<LatestSecurityPrice> quotes;

    protected ReviewImportedQuotesPage(ImportSourcePage page)
    {
        super("review"); //$NON-NLS-1$
        setTitle(Messages.ImportWizardReviewTitle);
        setDescription(Messages.ImportWizardReviewDescription);

        this.page = page;
        setPageComplete(false);
    }

    @Override
    public void beforePage()
    {
        String source = page.getSourceText();

        HTMLTableQuoteFeed feed = (HTMLTableQuoteFeed) Factory.getQuoteFeedProvider(HTMLTableQuoteFeed.ID);

        QuoteFeedData data = feed.getHistoricalQuotes(source);
        quotes = data.getLatestPrices();

        if (!data.getErrors().isEmpty())
            PortfolioPlugin.log(data.getErrors());

        setErrorMessage(null);
        setPageComplete(!quotes.isEmpty());

        tableSampleData.setInput(quotes);
        tableSampleData.refresh(true);

        // scroll up to top
        if (!quotes.isEmpty())
            tableSampleData.getTable().showItem(tableSampleData.getTable().getItem(0));
    }

    public List<LatestSecurityPrice> getQuotes()
    {
        return quotes;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        tableSampleData = new QuotesTableViewer(container);
    }

}
