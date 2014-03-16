package name.abuchen.portfolio.ui.log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LogEntry
{
    private int severity;
    private Date date;
    private String message;
    private String stacktrace;

    private List<LogEntry> children;

    public LogEntry(int severity, Date date, String message)
    {
        this.severity = severity;
        this.date = date;
        this.message = message;
    }

    public int getSeverity()
    {
        return severity;
    }

    public Date getDate()
    {
        return date;
    }

    public String getMessage()
    {
        return message;
    }

    public String getStacktrace()
    {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace)
    {
        this.stacktrace = stacktrace;
    }

    public List<LogEntry> getChildren()
    {
        return children;
    }

    public void addChild(LogEntry entry)
    {
        if (children == null)
            children = new ArrayList<LogEntry>();

        children.add(entry);
    }

    public String getText()
    {
        StringBuilder buffer = new StringBuilder();
        appendText(buffer);
        return buffer.toString();
    }

    private void appendText(StringBuilder buffer)
    {
        buffer.append(getDate()).append('\n');
        buffer.append(getMessage()).append('\n');
        if (getStacktrace() != null)
            buffer.append('\n').append(getStacktrace()).append('\n');

        if (getChildren() != null)
        {
            buffer.append("\n------\n"); //$NON-NLS-1$
            for (LogEntry child : getChildren())
                child.appendText(buffer);
            buffer.append("\n------\n"); //$NON-NLS-1$
        }
    }
}
