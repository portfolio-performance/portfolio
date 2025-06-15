package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class SearchSecurityWizard extends Wizard
{
    private final SearchSecurityDataModel model;

    public SearchSecurityWizard(Client client)
    {
        this.model = new SearchSecurityDataModel(client);

        this.setNeedsProgressMonitor(true);
        setDialogSettings(PortfolioPlugin.getDefault().getDialogSettings());
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public void addPages()
    {
        addPage(new SearchSecurityWizardPage(model));
        addPage(new SearchSecurityPreviewPricesWizardPage(model));
    }

    public Security getSecurity()
    {
        var item = model.getSelectedItem();

        if (item == null)
            return null;

        return item.create(model.getClient());
    }

    public Client getClient()
    {
        return model.getClient();
    }

    @Override
    public boolean performFinish()
    {
        return model.getSelectedItem() != null;
    }
}
