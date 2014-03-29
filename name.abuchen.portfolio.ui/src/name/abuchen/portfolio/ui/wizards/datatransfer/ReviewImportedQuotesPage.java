package name.abuchen.portfolio.ui.wizards.datatransfer;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.QuotesTableViewer;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

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

        try
        {
            QuoteFeed feed = Factory.getQuoteFeedProvider("GENERIC_HTML_TABLE"); //$NON-NLS-1$

            List<Exception> errors = new ArrayList<Exception>();
            quotes = feed.getHistoricalQuotes(source, errors);
            PortfolioPlugin.log(errors);

            setErrorMessage(null);
            setPageComplete(!quotes.isEmpty());

            tableSampleData.setInput(quotes);
            tableSampleData.refresh(true);

            // scroll up to top
            if (!quotes.isEmpty())
                tableSampleData.getTable().showItem(tableSampleData.getTable().getItem(0));
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            setErrorMessage(MessageFormat.format(Messages.ImportWizardReviewError, e.getMessage()));
            setPageComplete(false);

            tableSampleData.setInput(null);
            tableSampleData.refresh();
        }
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
