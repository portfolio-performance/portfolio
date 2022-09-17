package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.TextUtil;

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
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(this.getTruncatedUrl()));
        }

        manager.add(new SimpleAction(Messages.MenuChangeUrl, a -> {
            InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(), Messages.MenuChangeUrl,
                            Messages.Url, urlValue, null);

            if (dialog.open() != InputDialog.OK) // NOSONAR
                return;

            delegate.getWidget().getConfiguration().put(Dashboard.Config.URL.name(), String.valueOf(dialog.getValue()));

            delegate.update();
            delegate.getClient().touch();
        }));
    }

    @Override
    public String getLabel()
    {
        return Messages.Url + ": " + this.getTruncatedUrl(); //$NON-NLS-1$
    }

    private String getTruncatedUrl()
    {
        String urlValue = delegate.getWidget().getConfiguration().get(Dashboard.Config.URL.name());
        if (urlValue == null)
            return ""; //$NON-NLS-1$

        return TextUtil.limit(urlValue, 40);
    }
}
