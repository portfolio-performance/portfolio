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
        String urlValue = delegate.getWidget().getConfiguration().get(Dashboard.Config.URL.name());

        if (urlValue != null)
        {
            String label = urlValue.substring(0, Math.min(40, urlValue.length())) + "..."; //$NON-NLS-1$
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(label));
        }

        manager.add(new SimpleAction(Messages.UrlChange, a -> {
            InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuRenameLabel,
                            Messages.Url, urlValue, null);

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
        return Messages.Url + ": " + delegate.getWidget().getLabel(); //$NON-NLS-1$
    }
}
