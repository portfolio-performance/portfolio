package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;

public class EditSecurityWizard extends Wizard
{
    private final Client client;
    private final Security security;

    private final Security editable;

    private SecurityMasterDataPage dataPage;
    private QuoteProviderPage quotesPage;
    private SearchSecurityWizardPage searchPage;

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

        quotesPage = new QuoteProviderPage(editable);
        addPage(quotesPage);

        searchPage = new SearchSecurityWizardPage(client, editable);
        addPage(searchPage);

        addPage(new IndustryClassificationPage(client, editable));

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

    @Override
    public boolean performFinish()
    {
        ((AbstractWizardPage) this.getContainer().getCurrentPage()).afterPage();

        boolean hasQuotes = !security.getPrices().isEmpty();
        boolean providerChanged = (editable.getFeed() != null ? !editable.getFeed().equals(security.getFeed())
                        : security.getFeed() != null)
                        || (editable.getTickerSymbol() != null ? !editable.getTickerSymbol().equals(
                                        security.getTickerSymbol()) : security.getTickerSymbol() != null);

        shallowCopy(editable, security);

        if (hasQuotes && providerChanged)
        {
            MessageDialog dialog = new MessageDialog(getShell(), //
                            Messages.MessageDialogProviderChanged, null, //
                            Messages.MessageDialogProviderChangedText, //
                            MessageDialog.QUESTION, //
                            new String[] { Messages.MessageDialogProviderAnswerKeep,
                                            Messages.MessageDialogProviderAnswerReplace }, 0);
            if (dialog.open() == 1)
                security.removeAllPrices();
        }

        return true;
    }

    private void shallowCopy(Security source, Security target)
    {
        target.setName(source.getName());
        target.setIsin(source.getIsin());
        target.setTickerSymbol(source.getTickerSymbol());
        target.setWkn(source.getWkn());
        target.setType(source.getType());
        target.setFeed(source.getFeed());
        target.setIndustryClassification(source.getIndustryClassification());
        target.setRetired(source.isRetired());
    }

}
