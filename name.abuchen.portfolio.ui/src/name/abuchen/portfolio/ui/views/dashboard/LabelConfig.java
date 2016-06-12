package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class LabelConfig implements WidgetConfig
{
    private final WidgetDelegate delegate;

    public LabelConfig(WidgetDelegate delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(delegate.getWidget().getLabel()));

        manager.add(new SimpleAction("Edit label...", a -> {
            InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), "Label umbenennen", "Label",
                            delegate.getWidget().getLabel(), null);

            if (dialog.open() != InputDialog.OK)
                return;

            delegate.getWidget().setLabel(dialog.getValue());
            delegate.getClient().markDirty();
        }));
    }

}
