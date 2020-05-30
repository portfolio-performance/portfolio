package name.abuchen.portfolio.ui.wizards.client;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

public class ClientMigrationDialog extends WizardDialog
{
    private static class MigrationWizard extends Wizard
    {
        private Client client;

        private BaseCurrencySelectionPage currencySelectionPage;
        private MarkSecurityAsIndexPage markSecuritiesPage;

        public MigrationWizard(Client client)
        {
            this.client = client;
        }

        @Override
        public Image getDefaultPageImage()
        {
            return Images.BANNER.image();
        }

        @Override
        public void addPages()
        {
            currencySelectionPage = new BaseCurrencySelectionPage(Messages.BaseCurrencySelectionPage_Title,
                            Messages.BaseCurrencyMigrationPage_Description,
                            Messages.BaseCurrencyMigrationPage_ExplanationIndividualCurrency);
            addPage(currencySelectionPage);

            markSecuritiesPage = new MarkSecurityAsIndexPage(client);
            addPage(markSecuritiesPage);

            AbstractWizardPage.attachPageListenerTo(getContainer());
        }

        @Override
        public boolean performFinish()
        {
            CurrencyUnit currency = currencySelectionPage.getSelectedCurrency();

            if (!CurrencyUnit.EUR.equals(currency.getCurrencyCode()))
                ClientFactory.setAllCurrencies(client, currency.getCurrencyCode());

            markSecuritiesPage.getSelectedSecurities().forEach(s -> s.setCurrencyCode(null));

            return true;
        }
    }

    public ClientMigrationDialog(Shell parentShell, Client client)
    {
        super(parentShell, new MigrationWizard(client));
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);

        // migration cannot be aborted, use must choose a currency
        getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
    }
}
