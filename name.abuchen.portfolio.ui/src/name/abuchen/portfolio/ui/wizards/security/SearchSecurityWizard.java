package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;
import name.abuchen.portfolio.ui.Images;

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

        return item.create();
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
