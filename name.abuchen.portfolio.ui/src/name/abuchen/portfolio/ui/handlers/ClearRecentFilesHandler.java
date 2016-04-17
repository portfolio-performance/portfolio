package name.abuchen.portfolio.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;

import name.abuchen.portfolio.ui.util.RecentFilesCache;

public class ClearRecentFilesHandler
{
    @Execute
    public void execute(RecentFilesCache recentFiles)
    {
        recentFiles.clearRecentFiles();
    }
}
