package name.abuchen.portfolio.ui.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import name.abuchen.portfolio.ui.UIConstants;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.events.IEventBroker;

@Creatable
@Singleton
public class LogEntryCache implements ILogListener
{
    private LinkedList<LogEntry> entries = new LinkedList<LogEntry>();

    @Inject
    private IEventBroker eventBroker;

    @PostConstruct
    public void init()
    {
        Platform.addLogListener(this);
    }

    @PreDestroy
    public void dispose()
    {
        Platform.removeLogListener(this);
    }

    @Override
    public void logging(IStatus status, String plugin)
    {
        LogEntry entry = createEntry(status);
        entries.add(entry);

        if (entries.size() > 100)
            entries.removeFirst();

        eventBroker.post(UIConstants.Event.Log.CREATED, entry);
    }

    private LogEntry createEntry(IStatus status)
    {
        LogEntry entry = new LogEntry(status.getSeverity(), new Date(), status.getMessage());

        if (status.getException() != null)
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            status.getException().printStackTrace(pw);
            entry.setStacktrace(sw.toString());
        }

        if (status.isMultiStatus())
        {
            for (IStatus s : status.getChildren())
                entry.addChild(createEntry(s));
        }

        return entry;
    }

    public List<LogEntry> getEntries()
    {
        return new ArrayList<LogEntry>(entries);
    }

    public void clear()
    {
        entries.clear();
        eventBroker.post(UIConstants.Event.Log.CLEARED, null);
    }

}
