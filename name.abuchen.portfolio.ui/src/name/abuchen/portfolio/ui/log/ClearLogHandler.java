package name.abuchen.portfolio.ui.log;

import org.eclipse.e4.core.di.annotations.Execute;

public class ClearLogHandler
{
    @Execute
    public void execute(LogEntryCache cache)
    {
        cache.clear();
    }
}
