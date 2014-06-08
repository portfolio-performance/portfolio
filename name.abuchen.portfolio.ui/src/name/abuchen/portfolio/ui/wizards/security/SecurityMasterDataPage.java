package name.abuchen.portfolio.ui.wizards.security;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class SecurityMasterDataPage extends AbstractPage
{
    private final BindingHelper bindings;

    protected SecurityMasterDataPage(BindingHelper bindings)
    {
        this.bindings = bindings;

        setTitle(Messages.EditWizardMasterDataTitle);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        bindings.bindISINInput(container, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnTicker, "tickerSymbol", SWT.NONE, 12); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnWKN, "wkn", SWT.NONE, 12); //$NON-NLS-1$
        bindings.bindBooleanInput(container, Messages.ColumnRetired, "retired"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnNote, "note"); //$NON-NLS-1$
    }
}
