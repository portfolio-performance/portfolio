package name.abuchen.portfolio.ui.wizards;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

public class SecurityMasterDataPage extends AbstractWizardPage
{
    public static final String PAGE_NAME = "masterdata"; //$NON-NLS-1$

    private BindingHelper bindings;
    private EditSecurityModel model;

    protected SecurityMasterDataPage(EditSecurityModel model)
    {
        super(PAGE_NAME);

        this.model = model;

        setTitle(Messages.EditWizardMasterDataTitle);
        setDescription(Messages.EditWizardMasterDataDescription);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        container.setLayout(new FormLayout());
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        bindings = new BindingHelper(model)
        {
            @Override
            public void onValidationStatusChanged(IStatus status)
            {
                boolean isOK = status.getSeverity() == IStatus.OK;
                setErrorMessage(isOK ? null : status.getMessage());
                setPageComplete(isOK);
            }
        };

        bindings.bindMandatoryStringInput(container, Messages.ColumnName, "name").setFocus(); //$NON-NLS-1$
        bindings.bindISINInput(container, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnTicker, "tickerSymbol"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnWKN, "wkn"); //$NON-NLS-1$
        bindings.bindBooleanInput(container, Messages.ColumnRetired, "retired"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnNote, "note"); //$NON-NLS-1$

        Link link = new Link(container, SWT.UNDERLINE_LINK);
        link.setText(Messages.EditWizardMasterDataLinkToSearch);
        GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(link);

        link.addSelectionListener(new SelectionListener()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                getContainer().showPage(getWizard().getPage(SearchSecurityWizardPage.PAGE_NAME));
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {}
        });

    }
}
