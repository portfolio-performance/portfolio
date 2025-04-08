package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.widgets.Composite;

public final class DashboardResources
{
    private LocalResourceManager resourceManager = new LocalResourceManager(JFaceResources.getResources());

    public DashboardResources(Composite container)
    {
        container.addDisposeListener(e -> resourceManager.dispose());
    }

    public LocalResourceManager getResourceManager()
    {
        return resourceManager;
    }

}
