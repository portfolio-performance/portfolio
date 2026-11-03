package name.abuchen.portfolio.datatransfer.actions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.datatransfer.ImportAction.Status;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class CheckSecurityIdentifierActionTest
{
    CheckSecurityIdentifierAction action = new CheckSecurityIdentifierAction();

    @Test
    public void testSecurityWithoutIdentifier()
    {
        var security = new Security("AVENIR RENDEMENT (PART I)", "EUR");

        var status = action.process(security);

        assertThat(status.getCode(), is(Status.Code.WARNING));
        assertThat(status.getMessage(), is(Messages.MsgCheckSecurityWithoutIdentifier));
    }

    @Test
    public void testSecurityWithBlankIdentifiers()
    {
        var security = new Security("AVENIR RENDEMENT (PART I)", "EUR");
        security.setIsin(" ");
        security.setWkn("");
        security.setTickerSymbol(" ");

        assertThat(action.process(security).getCode(), is(Status.Code.WARNING));
    }

    @Test
    public void testSecurityWithIsin()
    {
        var security = new Security("iShares Core MSCI World UCITS ETF", "EUR");
        security.setIsin("IE00B4L5Y983");

        assertThat(action.process(security).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testSecurityWithWkn()
    {
        var security = new Security("iShares Core MSCI World UCITS ETF", "EUR");
        security.setWkn("A0RPWH");

        assertThat(action.process(security).getCode(), is(Status.Code.OK));
    }

    @Test
    public void testSecurityWithTicker()
    {
        var security = new Security("Apple Inc.", "USD");
        security.setTickerSymbol("AAPL");

        assertThat(action.process(security).getCode(), is(Status.Code.OK));
    }
}
