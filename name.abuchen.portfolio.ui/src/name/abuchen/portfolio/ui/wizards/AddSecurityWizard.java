package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

import org.eclipse.jface.wizard.Wizard;

public class AddSecurityWizard extends Wizard
{
    private final Client client;
    private SearchSecurityWizardPage searchPage;

    public AddSecurityWizard(Client client)
    {
        this.client = client;
    }

    @Override
    public void addPages()
    {
        searchPage = new SearchSecurityWizardPage(client);
        addPage(searchPage);
    }

    @Override
    public boolean performFinish()
    {
        ResultItem item = searchPage.getItem();

        Security security = new Security();
        security.setTickerSymbol(item.getSymbol());
        security.setName(item.getName());
        security.setIsin(item.getIsin());
        security.setType(AssetClass.EQUITY);

        client.addSecurity(security);

        return true;
    }

}
