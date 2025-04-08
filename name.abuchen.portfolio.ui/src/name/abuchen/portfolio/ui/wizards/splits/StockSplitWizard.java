package name.abuchen.portfolio.ui.wizards.splits;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class StockSplitWizard extends Wizard
{
    private IStylingEngine stylingEngine;
    private StockSplitModel model;

    public StockSplitWizard(IStylingEngine stylingEngine, Client client, Security security)
    {
        this.model = new StockSplitModel(client, security);
        this.stylingEngine = stylingEngine;
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        addPage(new SelectSplitPage(stylingEngine, model));
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
