package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;

public class EditSecurityWizard extends Wizard
{
    private final Client client;
    private final Security security;

    private final Security editable;

    private SecurityMasterDataPage dataPage;
    private QuoteProviderPage quotesPage;

    private AbstractWizardPage currentPage;

    public EditSecurityWizard(Client client, Security security)
    {
        this.client = client;
        this.security = security;

        this.editable = new Security();

        shallowCopy(security, editable);

        this.setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages()
    {
        dataPage = new SecurityMasterDataPage(client, editable);
        addPage(dataPage);

        quotesPage = new QuoteProviderPage(client, editable);
        addPage(quotesPage);

        final IWizardContainer c = this.getContainer();
        if (c instanceof IPageChangeProvider)
        {
            ((IPageChangeProvider) c).addPageChangedListener(new IPageChangedListener()
            {
                @Override
                public void pageChanged(PageChangedEvent event)
                {
                    if (currentPage != null)
                        currentPage.afterPage();
                    currentPage = (AbstractWizardPage) event.getSelectedPage();
                    currentPage.beforePage();
                }
            });
        }
    }

    @Override
    public boolean performFinish()
    {
        if (currentPage != null)
            currentPage.afterPage();

        shallowCopy(editable, security);

        return true;
    }

    private void shallowCopy(Security source, Security target)
    {
        target.setName(source.getName());
        target.setIsin(source.getIsin());
        target.setTickerSymbol(source.getTickerSymbol());
        target.setType(source.getType());
        target.setFeed(source.getFeed());
    }

}
