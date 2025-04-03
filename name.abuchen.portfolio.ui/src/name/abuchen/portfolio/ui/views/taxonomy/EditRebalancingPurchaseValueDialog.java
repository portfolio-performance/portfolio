package name.abuchen.portfolio.ui.views.taxonomy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.AbstractDialog;

public class EditRebalancingPurchaseValueDialog extends AbstractDialog
{
    public EditRebalancingPurchaseValueDialog(Shell parentShell, RebalancingPurchaseValue purchaseValue)
    {
        super(parentShell, Messages.LabelViewReBalancing, purchaseValue);
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        bindings().bindPositiveAmountInput(editArea, Messages.RebalancingPurchaseValue, "purchaseValue", //$NON-NLS-1$
                        SWT.NONE, 10);
    }
}
