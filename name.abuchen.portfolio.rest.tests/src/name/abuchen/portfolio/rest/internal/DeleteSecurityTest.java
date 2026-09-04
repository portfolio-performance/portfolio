package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class DeleteSecurityTest
{
    @Test
    public void testUnreferencedSecurityIsDeleted()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        SecuritiesHandler.delete(client, security.getUUID());

        assertThat(client.getSecurities().isEmpty(), is(true));
    }

    @Test
    public void testUnreferencedSecurityMarksClientDirty()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        var dirty = new AtomicBoolean();
        client.addPropertyChangeListener("dirty", event -> dirty.set(true));

        SecuritiesHandler.delete(client, security.getUUID());

        assertThat(dirty.get(), is(true));
    }

    @Test
    public void testSecurityWithTransactionsIs409AndModelUnchanged()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        new PortfolioBuilder().buy(security, "2024-01-02", Values.Share.factorize(100), Values.Amount.factorize(1000))
                        .addTo(client);

        try
        {
            SecuritiesHandler.delete(client, security.getUUID());
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(409));
            assertThat(e.getType(), is("delete-blocked"));
            assertThat(e.getErrors().get(0).field(), is("transactions"));
            assertThat(client.getSecurities().size(), is(1));
        }
    }
}
