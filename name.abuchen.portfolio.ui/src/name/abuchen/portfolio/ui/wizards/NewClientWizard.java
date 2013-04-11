package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;

import org.eclipse.jface.wizard.Wizard;

public class NewClientWizard extends Wizard
{
    Client client;
    NewPortfolioAccountPage pfAccPage;
    
    public NewClientWizard() {
        client = new Client();
    }

    @Override
    public boolean performFinish()
    {
        return true;
    }

    @Override
    public void addPages()
    {
        pfAccPage = new NewPortfolioAccountPage(client);
        addPage(pfAccPage);
    }
    

}
