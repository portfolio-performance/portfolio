package name.abuchen.portfolio.ui.wizards.client;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class ClientMigrationDialog extends WizardDialog
{
    private static class MigrationWizard extends Wizard
    {
        private Client client;

        private BaseCurrencyMigrationPage page;

        public MigrationWizard(Client client)
        {
            this.client = client;
        }

        @Override
        public void addPages()
        {
            page = new BaseCurrencyMigrationPage(client);
            addPage(page);
        }

        @Override
        public boolean performFinish()
        {
            CurrencyUnit currency = page.getSelectedCurrency();

            if (!CurrencyUnit.EUR.equals(currency))
                ClientFactory.setAllCurrencies(client, currency.getCurrencyCode());

            return true;
        }
    }

    private static class BaseCurrencyMigrationPage extends BaseCurrencySelectionPage
    {

        public BaseCurrencyMigrationPage(Client client)
        {
            super(client); //$NON-NLS-1$

            setDescription(Messages.BaseCurrencyMigrationPage_Description);
            this.explanationIndividualCurrency = Messages.BaseCurrencyMigrationPage_ExplanationIndividualCurrency;
        }

        public CurrencyUnit getSelectedCurrency()
        {
            return (CurrencyUnit) ((IStructuredSelection) combo.getSelection()).getFirstElement();
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
