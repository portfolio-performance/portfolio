package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;

import org.eclipse.jface.wizard.Wizard;

public class NewClientWizard extends Wizard
{
    Client client;
    NewPortfolioAccountPage pfAccPage;
    NewAccountPage accPage;
    ImportIndizesPage indPage;
    
    public NewClientWizard() {
        client = new Client();
    }

    @Override
    public boolean performFinish()
    {
        return true;
    }
    
    public Client getClient() {
        return client;
    }

    @Override
    public void addPages()
    {
        pfAccPage = new NewPortfolioAccountPage(client);
        addPage(pfAccPage);
        accPage = new NewAccountPage(client);
        addPage(accPage);
        indPage = new ImportIndizesPage(client);
        addPage(indPage);
    }
    

}
