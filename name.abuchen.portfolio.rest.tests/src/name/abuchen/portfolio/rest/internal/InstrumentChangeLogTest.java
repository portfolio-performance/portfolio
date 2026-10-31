package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.text.MessageFormat;

import org.junit.Test;

import name.abuchen.portfolio.rest.Messages;
import name.abuchen.portfolio.rest.internal.InstrumentChangeLog.Change;

@SuppressWarnings("nls")
public class InstrumentChangeLogTest
{
    @Test
    public void testDetailShowsOldToNew()
    {
        assertThat(InstrumentChangeLog.detail(new Change("note", "foo", "bar")),
                        is(MessageFormat.format(Messages.MsgApiInstrumentFieldChanged, "note", "'foo'", "'bar'")));
    }

    @Test
    public void testDetailRendersUnsetOldSide()
    {
        assertThat(InstrumentChangeLog.detail(new Change("note", null, "bar")), is(MessageFormat
                        .format(Messages.MsgApiInstrumentFieldChanged, "note", Messages.MsgApiValueUnset, "'bar'")));
    }

    @Test
    public void testDetailRendersRemovedNewSide()
    {
        assertThat(InstrumentChangeLog.detail(new Change("note", "foo", null)), is(MessageFormat
                        .format(Messages.MsgApiInstrumentFieldChanged, "note", "'foo'", Messages.MsgApiValueRemoved)));
    }

    @Test
    public void testSummaryNamesInstrumentAndFile()
    {
        assertThat(InstrumentChangeLog.summary("portfolio.xml", "Apple Inc."),
                        is(MessageFormat.format(Messages.MsgApiInstrumentChanged, "Apple Inc.", "portfolio.xml")));
    }

    @Test
    public void testDeletionSummaryNamesInstrumentAndFile()
    {
        assertThat(InstrumentChangeLog.deletionSummary("portfolio.xml", "Apple Inc."),
                        is(MessageFormat.format(Messages.MsgApiInstrumentDeleted, "Apple Inc.", "portfolio.xml")));
    }
}
