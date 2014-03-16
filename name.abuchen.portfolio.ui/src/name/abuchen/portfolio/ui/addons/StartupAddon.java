package name.abuchen.portfolio.ui.addons;

import javax.annotation.PostConstruct;

import name.abuchen.portfolio.ui.log.LogEntryCache;

public class StartupAddon
{
    @PostConstruct
    public void initLogEntryCache(LogEntryCache cache)
    {
        // force creation of log entry cache
    }
}
