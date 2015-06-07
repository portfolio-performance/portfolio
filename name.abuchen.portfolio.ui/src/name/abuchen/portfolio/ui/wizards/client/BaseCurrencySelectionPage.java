package name.abuchen.portfolio.ui.wizards.client;

import java.util.Collections;
import java.util.List;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.wizards.AbstractWizardPage;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class BaseCurrencySelectionPage extends AbstractWizardPage
{
    protected ComboViewer combo;
    protected String explanationIndividualCurrency;
    private Client client;    

    public BaseCurrencySelectionPage(Client client)
    {
        super("base-currency-selection"); //$NON-NLS-1$

        this.client = client;
        setTitle(Messages.BaseCurrencySelectionPage_Title);
        setDescription(Messages.BaseCurrencySelectionPage_Description);
        this.explanationIndividualCurrency = Messages.BaseCurrencySelectionPage_ExplanationIndividualCurrency;
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
        combo.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
              IStructuredSelection selection = (IStructuredSelection) event
                .getSelection();
              
              client.setBaseCurrency(((CurrencyUnit)selection.getFirstElement()).getCurrencyCode());
                                    
              }
            });
        

        Label description = new Label(container, SWT.WRAP);
        description.setText( this.explanationIndividualCurrency );

        FormDataFactory.startingWith(combo.getControl(), label).thenBelow(description).left(label);

        FormData data = (FormData) description.getLayoutData();
        data.width = 500;
        
        container.pack();
        setPageComplete(true);
        
    }

}