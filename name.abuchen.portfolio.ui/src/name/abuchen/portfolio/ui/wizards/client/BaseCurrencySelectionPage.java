package name.abuchen.portfolio.ui.wizards.client;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class BaseCurrencySelectionPage extends AbstractWizardPage
{
    private ComboViewer combo;
    protected String explanationIndividualCurrency;

    public BaseCurrencySelectionPage(String title, String description, String explanation)
    {
        super("base-currency-selection"); //$NON-NLS-1$

        setTitle(title);
        setDescription(description);
        this.explanationIndividualCurrency = explanation;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        setControl(container);
        container.setLayout(new FormLayout());

        Label label = new Label(container, SWT.NONE);
        label.setText(Messages.ColumnCurrency);

        List<CurrencyUnit> currencies = CurrencyUnit.getAvailableCurrencyUnits();
        Collections.sort(currencies);
        combo = new ComboViewer(container);
        combo.setContentProvider(ArrayContentProvider.getInstance());
        combo.setInput(currencies);
        combo.setSelection(new StructuredSelection(CurrencyUnit.getInstance(CurrencyUnit.EUR)));

        Label description = new Label(container, SWT.WRAP);
        description.setText(this.explanationIndividualCurrency);

        FormDataFactory.startingWith(combo.getControl(), label).thenBelow(description).width(500).left(label);

        container.pack();
        setPageComplete(true);
    }

    public CurrencyUnit getSelectedCurrency()
    {
        return (CurrencyUnit) ((IStructuredSelection) combo.getSelection()).getFirstElement();
    }
}
