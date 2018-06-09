package name.abuchen.portfolio.ui.wizards.client;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Image;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class NewClientWizard extends Wizard
{
    private Client client;
    private BaseCurrencySelectionPage page;

    public NewClientWizard()
    {
        client = new Client();
    }

    @Override
    public Image getDefaultPageImage()
    {
        return Images.BANNER.image();
    }

    @Override
    public boolean performFinish()
    {
        CurrencyUnit currency = page.getSelectedCurrency();
        client.setBaseCurrency(currency.getCurrencyCode());
        client.getAccounts().stream().forEach(a -> a.setCurrencyCode(currency.getCurrencyCode()));

        return true;
    }

    public Client getClient()
    {
        return client;
    }

    @Override
    public void addPages()
    {
        page = new BaseCurrencySelectionPage(Messages.BaseCurrencySelectionPage_Title,
                        Messages.BaseCurrencySelectionPage_Description,
                        Messages.BaseCurrencySelectionPage_ExplanationIndividualCurrency);
        addPage(page);

        addPage(new NewPortfolioAccountPage(client));
        addPage(new NewAccountPage(client));
        addPage(new ImportIndizesPage(client));
        addPage(new AddTaxonomyPage(client));

        AbstractWizardPage.attachPageListenerTo(this.getContainer());
    }

}
