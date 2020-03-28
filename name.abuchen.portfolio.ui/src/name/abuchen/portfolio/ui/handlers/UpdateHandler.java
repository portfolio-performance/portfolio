package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;

import name.abuchen.portfolio.ui.update.UpdateHelper;

public class UpdateHandler
{
    @Execute
    public void execute()
    {
        UpdateHelper updateHelper = new UpdateHelper();
        updateHelper.runUpdateWithUIMonitor();
    }
}
