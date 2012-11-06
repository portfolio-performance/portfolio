package name.abuchen.portfolio.ui.util;

import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;

public class ViewDropdownMenu extends ToolBarDropdownMenu<Control>
{

    public ViewDropdownMenu(ToolBar toolBar)
    {
        super(toolBar);
    }

    @Override
    protected void itemSelected(Control viewer)
    {
        Composite parent = viewer.getParent();
        StackLayout layout = (StackLayout) parent.getLayout();
        layout.topControl = viewer;
        parent.layout();
    }
}
