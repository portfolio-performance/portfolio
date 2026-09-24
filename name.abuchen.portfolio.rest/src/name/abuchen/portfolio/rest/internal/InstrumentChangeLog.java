package name.abuchen.portfolio.rest.internal;

import java.util.List;

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
        ChangeLog.recordChanges(changes.stream().map(c -> new ChangeLog.Change(c.field(), c.from(), c.to())).toList(),
                        Messages.MsgApiInstrumentChanged, instrumentName, fileLabel);
    }

    public static void recordDeletion(String fileLabel, String instrumentName)
    {
        ChangeLog.recordEvent(Messages.MsgApiInstrumentDeleted, instrumentName, fileLabel);
    }

    /* package */ static String summary(String fileLabel, String instrumentName)
    {
        return ChangeLog.summary(Messages.MsgApiInstrumentChanged, instrumentName, fileLabel);
    }

    /* package */ static String deletionSummary(String fileLabel, String instrumentName)
    {
        return ChangeLog.summary(Messages.MsgApiInstrumentDeleted, instrumentName, fileLabel);
    }

    /* package */ static String detail(Change change)
    {
        return ChangeLog.detail(new ChangeLog.Change(change.field(), change.from(), change.to()));
    }
}
