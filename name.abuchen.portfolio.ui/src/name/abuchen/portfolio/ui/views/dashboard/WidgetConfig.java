package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.IMenuManager;

public interface WidgetConfig
{
    void menuAboutToShow(IMenuManager manager);

    String getLabel();
}
