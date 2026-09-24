package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.util.List;

import name.abuchen.portfolio.PortfolioLog;
import name.abuchen.portfolio.rest.Messages;

/**
 * Records the mutations the REST API applies as human-readable entries in the
 * application log, so the desktop user can see in the Error Log view what the
 * API changed. Entries are attributed generically to "REST API"; see
 * {@link InstrumentChangeLog} for why the individual client is not named.
 * <p/>
 * Every write logs one entry after it changed the model (never for a dry run
 * or a write that changes nothing): creates and deletes a single line
 * ({@code MsgApiEntityCreated/Deleted}, {@code MsgApiTransactionCreated/Deleted}),
 * updates a summary with one detail line per changed field
 * ({@code MsgApiEntityChanged}, {@code MsgApiInstrumentChanged},
 * {@code MsgApiTransactionUpdated}, detail {@code MsgApiFieldChanged}), and
 * the actions a line with their outcome (stock split, investment plan
 * generation, price update, import, open and save).
 */
public final class ChangeLog
{
    /**
     * A single field change. A null {@code from} means the value was unset
     * before the change; a null {@code to} means it was removed or cleared.
     */
    public record Change(String field, String from, String to)
    {
    }

    private ChangeLog()
    {
    }

    /**
     * Logs a summary - {@code pattern} formatted with {@code arguments} - plus
     * one detail line per change. Nothing is logged if there are no changes.
     */
    public static void recordChanges(List<Change> changes, String pattern, Object... arguments)
    {
        if (changes.isEmpty())
            return;

        PortfolioLog.info(summary(pattern, arguments), changes.stream().map(ChangeLog::detail).toList());
    }

    /** logs a single summary line, e.g. for a create or a delete */
    public static void recordEvent(String pattern, Object... arguments)
    {
        PortfolioLog.info(summary(pattern, arguments), List.of());
    }

    /* package */ static String summary(String pattern, Object... arguments)
    {
        return MessageFormat.format(pattern, arguments);
    }

    /* package */ static String detail(Change change)
    {
        var from = change.from() == null ? Messages.MsgApiValueUnset : quote(change.from());
        var to = change.to() == null ? Messages.MsgApiValueRemoved : quote(change.to());
        return MessageFormat.format(Messages.MsgApiFieldChanged, change.field(), from, to);
    }

    private static String quote(String value)
    {
        return "'" + value + "'"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
