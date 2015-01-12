package name.abuchen.portfolio.ui.wizards.client;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.money.CurrencyUnit;
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

        private BaseCurrencySelectionPage page;

        public MigrationWizard(Client client)
        {
            this.client = client;
        }

        @Override
        public void addPages()
        {
            page = new BaseCurrencySelectionPage();
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

    private static class BaseCurrencySelectionPage extends WizardPage
    {
        private ComboViewer combo;

        public BaseCurrencySelectionPage()
        {
            super("base-currency-selection"); //$NON-NLS-1$

            setTitle("Währung wählen");
            setDescription("Ab dieser Version brauchen Konten und Wertpapiere eine Währung.");
        }

        @Override
        public void createControl(Composite parent)
        {
            Composite editArea = new Composite(parent, SWT.NONE);
            editArea.setLayout(new FormLayout());

            Label label = new Label(editArea, SWT.NONE);
            label.setText("Währung");

            List<CurrencyUnit> currencies = CurrencyUnit.getAvailableCurrencyUnits();
            Collections.sort(currencies);
            combo = new ComboViewer(editArea);
            combo.setContentProvider(ArrayContentProvider.getInstance());
            combo.setInput(currencies);
            combo.setSelection(new StructuredSelection(CurrencyUnit.getInstance(CurrencyUnit.EUR)));

            Label description = new Label(editArea, SWT.WRAP);
            description.setText("Die aktuelle Datei wurde mit einer früheren Version von Portfolio Performance erstellt. "
                            + "Ab jetzt brauchen Konten und Wertpapiere eine Währung. Mit diesem Dialog legen Sie die Währung "
                            + "fest, die zunächst allen Konten und Wertpapieren zugewiesen wird."
                            + "\n\n"
                            + "Die Währung einzelner Wertpapiere kann anschließend über das Kontektmenü in der Wertpapier-Übersicht geändert werden.");

            FormDataFactory.startingWith(combo.getControl(), label).thenBelow(description).left(label);

            FormData data = (FormData) description.getLayoutData();
            data.width = 500;

            setControl(editArea);
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
