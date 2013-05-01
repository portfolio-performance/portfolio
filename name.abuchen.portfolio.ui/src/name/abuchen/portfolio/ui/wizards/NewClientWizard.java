package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;

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
        addPage(new NewPortfolioAccountPage(client));
        addPage(new NewAccountPage(client));
        addPage(new ImportIndizesPage(client));
    }

}
