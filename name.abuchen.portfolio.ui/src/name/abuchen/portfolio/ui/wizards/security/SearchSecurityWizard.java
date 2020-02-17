package name.abuchen.portfolio.ui.wizards.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.Factory;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.online.impl.PortfolioReportNet;
import name.abuchen.portfolio.online.impl.PortfolioReportQuoteFeed;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class SearchSecurityWizard extends Wizard
{
    private final Client client;

    public SearchSecurityWizard(Client client)
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
        addPage(new SearchSecurityWizardPage(client));
    }

    public Security getSecurity()
    {
        ResultItem item = getResultItem();

        if (item == null)
            return null;

        Security security = new Security();
        item.applyTo(security);

        if (security.getOnlineId() != null)
            completePortfolioReportData(security);

        return security;
    }

    private void completePortfolioReportData(Security security)
    {
        try
        {
            new PortfolioReportNet().getUpdatedValues(security.getOnlineId())
                            .ifPresent(item -> PortfolioReportNet.updateWith(security, item));

            QuoteFeed feed = Factory.getQuoteFeedProvider(PortfolioReportQuoteFeed.ID);
            List<Exchange> exchanges = feed.getExchanges(security, new ArrayList<>());

            if (!exchanges.isEmpty())
            {
                security.setFeed(feed.getId());
                security.setPropertyValue(SecurityProperty.Type.FEED, PortfolioReportQuoteFeed.MARKET_PROPERTY_NAME,
                                exchanges.get(0).getId());
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
        }
    }

    @Override
    public boolean performFinish()
    {
        return getResultItem() != null;
    }

    private ResultItem getResultItem()
    {
        return ((SearchSecurityWizardPage) this.getPage(SearchSecurityWizardPage.PAGE_ID)).getResult();
    }
}
