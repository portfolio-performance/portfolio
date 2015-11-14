package name.abuchen.portfolio.ui.wizards.security;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.BindingHelper;

public class SecurityMasterDataPage extends AbstractPage
{
    private final EditSecurityModel model;
    private final BindingHelper bindings;

    protected SecurityMasterDataPage(EditSecurityModel model, BindingHelper bindings)
    {
        this.model = model;
        this.bindings = bindings;

        setTitle(Messages.EditWizardMasterDataTitle);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NULL);
        setControl(container);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);

        ComboViewer currencyCode = bindings.bindCurrencyCodeCombo(container, Messages.ColumnCurrency, "currencyCode"); //$NON-NLS-1$
        if (model.getSecurity().hasTransactions(model.getClient()))
        {
            currencyCode.getCombo().setEnabled(false);

            // empty cell
            new Label(container, SWT.NONE).setText(""); //$NON-NLS-1$

            Composite info = new Composite(container, SWT.NONE);
            info.setLayout(new RowLayout());

            Label l = new Label(info, SWT.NONE);
            l.setImage(Images.INFO.image());

            l = new Label(info, SWT.NONE);
            l.setText(Messages.MsgInfoChangingCurrencyNotPossible);

        }

        bindings.bindISINInput(container, Messages.ColumnISIN, "isin"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnTicker, "tickerSymbol", SWT.NONE, 12); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnWKN, "wkn", SWT.NONE, 12); //$NON-NLS-1$
        bindings.bindBooleanInput(container, Messages.ColumnRetired, "retired"); //$NON-NLS-1$
        bindings.bindStringInput(container, Messages.ColumnNote, "note"); //$NON-NLS-1$
    }
}
