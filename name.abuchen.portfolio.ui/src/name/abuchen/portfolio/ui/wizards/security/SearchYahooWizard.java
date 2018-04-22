package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;
import name.abuchen.portfolio.ui.Images;

public class SearchYahooWizard extends Wizard
{
    private final Client client;
    private SearchSecurityWizardPage page;

    public SearchYahooWizard(Client client)
    {
        this.client = client;

        this.setNeedsProgressMonitor(true);
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        addPage(page = new SearchSecurityWizardPage(client));
    }

    public Security getSecurity()
    {
        ResultItem item = page.getResult();

        if (item == null)
            return null;

        Security security = new Security();
        security.setName(item.getName());
        security.setTickerSymbol(item.getSymbol());
        security.setFeed(YahooFinanceQuoteFeed.ID);

        return security;
    }

    @Override
    public boolean performFinish()
    {
        return page.getResult() != null;
    }
}
