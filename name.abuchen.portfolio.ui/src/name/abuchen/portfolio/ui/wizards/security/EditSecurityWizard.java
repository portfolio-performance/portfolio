package name.abuchen.portfolio.ui.wizards.security;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;

public class EditSecurityWizard extends Wizard
{
    private final Security security;

    private final EditSecurityModel model;

    private SecurityMasterDataPage dataPage;
    private SecurityTaxonomyPage taxonomyPage;
    private AttributesPage attributesPage;
    private QuoteProviderPage quotesPage;
    private SearchSecurityWizardPage searchPage;

    public EditSecurityWizard(Client client, Security security)
    {
        this.security = security;

        this.model = new EditSecurityModel(client, security);

        this.setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages()
    {
        dataPage = new SecurityMasterDataPage(model);
        addPage(dataPage);

        attributesPage = new AttributesPage(model);
        addPage(attributesPage);

        taxonomyPage = new SecurityTaxonomyPage(model);
        addPage(taxonomyPage);

        quotesPage = new QuoteProviderPage(model);
        addPage(quotesPage);

        searchPage = new SearchSecurityWizardPage(model);
        addPage(searchPage);

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

    @Override
    public boolean performFinish()
    {
        ((AbstractWizardPage) this.getContainer().getCurrentPage()).afterPage();

        boolean hasQuotes = !security.getPrices().isEmpty();
        boolean providerChanged = (model.getFeed() != null ? !model.getFeed().equals(security.getFeed()) : security
                        .getFeed() != null)
                        || (model.getTickerSymbol() != null ? !model.getTickerSymbol().equals(
                                        security.getTickerSymbol()) : security.getTickerSymbol() != null);

        model.applyChanges();

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

}
