package name.abuchen.portfolio.ui.wizards.splits;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.jface.wizard.Wizard;

public class StockSplitWizard extends Wizard
{
    private StockSplitModel model;

    public StockSplitWizard(Client client, Security security)
    {
        this.model = new StockSplitModel(client, security);
    }

    public void addPages()
    {
        addPage(new SelectSplitPage(model));
        addPage(new PreviewTransactionsPage(model));
        addPage(new PreviewQuotesPage(model));

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

    @Override
    public boolean performFinish()
    {
        model.applyChanges();
        return true;
    }

}
