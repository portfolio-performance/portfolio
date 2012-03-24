package name.abuchen.portfolio.ui.app;

import name.abuchen.portfolio.ui.Messages;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor
{

    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer)
    {
        super(configurer);
    }

    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer)
    {
        return new ApplicationActionBarAdvisor(configurer);
    }

    public void preWindowOpen()
    {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(1024, 756));
        configurer.setShowCoolBar(false);
        configurer.setShowStatusLine(true);
        configurer.setShowProgressIndicator(true);
        configurer.setTitle(Messages.LabelPortfolioPerformance);
    }
}
