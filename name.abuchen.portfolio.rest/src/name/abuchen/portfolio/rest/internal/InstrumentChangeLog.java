package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.util.List;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.Messages;

/**
 * Records the mutations the REST API applies to an instrument as a
 * human-readable entry in the application log, so the desktop user can see in
 * the Error Log view what the API changed. Entries are attributed generically
 * to "REST API"; naming the individual paired client is a deliberate follow-up
 * that first requires threading the authenticated client identity through the
 * request pipeline (today the token is validated as a boolean only).
 */
public final class InstrumentChangeLog
{
    /**
     * A single field change. A null {@code from} means the value was unset
     * before the change; a null {@code to} means it was removed or cleared.
     */
    public record Change(String field, String from, String to)
    {}

    private InstrumentChangeLog()
    {
    }

    public static void record(String fileLabel, String instrumentName, List<Change> changes)
    {
        if (changes.isEmpty())
            return;

        PortfolioLog.info(summary(fileLabel, instrumentName),
                        changes.stream().map(InstrumentChangeLog::detail).toList());
    }

    public static void recordDeletion(String fileLabel, String instrumentName)
    {
        PortfolioLog.info(deletionSummary(fileLabel, instrumentName), List.of());
    }

    /* package */ static String summary(String fileLabel, String instrumentName)
    {
        return MessageFormat.format(Messages.MsgApiInstrumentChanged, instrumentName, fileLabel);
    }

    /* package */ static String deletionSummary(String fileLabel, String instrumentName)
    {
        return MessageFormat.format(Messages.MsgApiInstrumentDeleted, instrumentName, fileLabel);
    }

    /* package */ static String detail(Change change)
    {
        var from = change.from() == null ? Messages.MsgApiValueUnset : quote(change.from());
        var to = change.to() == null ? Messages.MsgApiValueRemoved : quote(change.to());
        return MessageFormat.format(Messages.MsgApiInstrumentFieldChanged, change.field(), from, to);
    }

    private static String quote(String value)
    {
        return "'" + value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
