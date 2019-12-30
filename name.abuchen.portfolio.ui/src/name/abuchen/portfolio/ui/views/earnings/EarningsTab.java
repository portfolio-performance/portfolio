package name.abuchen.portfolio.ui.views.earnings;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface EarningsTab
{
    String getLabel();

    Control createControl(Composite parent);

    default void addExportActions(IMenuManager manager)
    {
        // no export actions added by default
    }

    default void addConfigActions(IMenuManager manager)
    {
        // no config actions added by default
    }
}
