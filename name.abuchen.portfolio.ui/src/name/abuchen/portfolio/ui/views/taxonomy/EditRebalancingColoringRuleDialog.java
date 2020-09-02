package name.abuchen.portfolio.ui.views.taxonomy;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.AbstractDialog;

public class EditRebalancingColoringRuleDialog extends AbstractDialog
{
    public EditRebalancingColoringRuleDialog(Shell parentShell, RebalancingColoringRule rule)
    {
        super(parentShell, Messages.LabelViewReBalancing, rule);
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        bindings().bindSpinner(editArea, Messages.ColumnRebalancingIndicatorAbsoluteThreshold, "absoluteThreshold", 1, //$NON-NLS-1$
                        100, 1);
        bindings().bindSpinner(editArea, Messages.ColumnRebalancingIndicatorRelativeThreshold, "relativeThreshold", 1, //$NON-NLS-1$
                        100, 1);
        bindings().bindSpinner(editArea, Messages.ColumnRebalancingIndicatorBarLength, "barLength", 1, 100, 1); //$NON-NLS-1$
    }
}
