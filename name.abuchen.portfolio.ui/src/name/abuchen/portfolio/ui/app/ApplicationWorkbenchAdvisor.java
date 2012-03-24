package name.abuchen.portfolio.ui.app;

import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor
{

    private static final String PERSPECTIVE_ID = "name.abuchen.portfolio.perspective"; //$NON-NLS-1$

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
    {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }

    public String getInitialWindowPerspectiveId()
    {
        return PERSPECTIVE_ID;
    }

    @Override
    public void initialize(IWorkbenchConfigurer configurer)
    {
        super.initialize(configurer);
        configurer.setSaveAndRestore(true);
    }

}
