package name.abuchen.portfolio.ui.wizards;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.online.ArivaHistQuotesSoup;
import name.abuchen.portfolio.online.ImportOnvistaQuotes;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

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
            quotes = new ImportOnvistaQuotes().extract(source);
            if (quotes.isEmpty())
                quotes = new ArivaHistQuotesSoup().extractFromString(source);

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
