package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class UrlConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;

    public UrlConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(delegate.getWidget().getLabel()));

        manager.add(new SimpleAction("URL ändern...", a -> {
            InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuRenameLabel,
                            "URL", delegate.getWidget().getLabel(), null);

            if (dialog.open() != InputDialog.OK)
                return;

            delegate.getWidget().getConfiguration().put(Dashboard.Config.URL.name(), String.valueOf(dialog.getValue()));

            delegate.update();
            delegate.getClient().touch();
        }));
    }

    @Override
    public String getLabel()
    {
        // @todo translations
        return "URL" + ": " + delegate.getWidget().getLabel(); //$NON-NLS-1$
    }
}
