package name.abuchen.portfolio.ui.wizards.client;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.jface.wizard.Wizard;

public class NewClientWizard extends Wizard
{
    private Client client;

    public NewClientWizard()
    {
        client = new Client();
    }

    @Override
    public boolean performFinish()
    {
        return true;
    }

    public Client getClient()
    {
        return client;
    }

    @Override
    public void addPages()
    {
        addPage(new BaseCurrencySelectionPage(client));
        addPage(new NewPortfolioAccountPage(client));
        addPage(new NewAccountPage(client));
        addPage(new ImportIndizesPage(client));
        addPage(new AddTaxonomyPage(client));

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

}
