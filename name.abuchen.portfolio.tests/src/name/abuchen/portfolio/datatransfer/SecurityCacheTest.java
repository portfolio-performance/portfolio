package name.abuchen.portfolio.datatransfer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class SecurityCacheTest
{
    private Client client;

    @Before
    public void setupClient()
    {
        client = new Client();

        Security security = new Security();
        security.setName("Security Name");
        security.setIsin("DE0007164600");
        security.setWkn("716460");
        security.setCurrencyCode("EUR");
        client.addSecurity(security);

        security = new Security();
        security.setName("Security Name USD");
        security.setIsin("DE0007164600");
        security.setCurrencyCode("USD");
        client.addSecurity(security);

        for (int i = 0; i < 500; i++)
        {
            security = new Security();
            security.setName("Security Name" + i);
            security.setIsin("DE00071646" + i);
            security.setCurrencyCode("USD");
            client.addSecurity(security);
        }
        for (int i = 0; i < 500; i++)
        {
            security = new Security();
            security.setName("Security Name USD" + i);
            security.setIsin("DE00071646" + i);
            security.setCurrencyCode("USD");
            client.addSecurity(security);
        }
    }

    @Test
    public void testThatSecurityIsMatchedByNameOnlyIfISINMatches()
    {
        SecurityCache cache = new SecurityCache(client);
        Security lookup = cache.lookup("DE000BASF111", null, null, "Security Name", () -> new Security());
        assertThat(client.getSecurities().get(0), not(is(lookup)));
    }

    @Test
    public void testThatSecurityIsMatchedByNameOnlyIfWKNMatches()
    {
        SecurityCache cache = new SecurityCache(client);
        Security lookup = cache.lookup(null, null, "BASF11", "Security Name", () -> new Security());
        assertThat(client.getSecurities().get(0), not(is(lookup)));
    }

    @Test
    public void testThatSecurityIsMatchedByName()
    {
        SecurityCache cache = new SecurityCache(client);
        Security lookup = cache.lookup(null, null, null, "Security Name", () -> new Security());
        assertThat(client.getSecurities().get(0), is(lookup));
    }

    @Test
    public void testThatSecurityIsMatchedByISINUnique()
    {
        SecurityCache cache = new SecurityCache(client);
        try
        {
            cache.lookup("DE0007164600", null, null, null, null, () -> new Security());
            assertThat(true, is(false));
        }
        catch (IllegalArgumentException e)
        {
            assertThat(true, is(true));
        }
    }

    @Test
    public void testThatSecurityIsMatchedByISINandName()
    {
        SecurityCache cache = new SecurityCache(client);
        Security lookup = cache.lookup("DE0007164600", null, null, "Security Name", () -> new Security());
        assertThat(client.getSecurities().get(0), is(lookup));
        assertThat(client.getSecurities().get(1), not(is(lookup)));
    }

    @Test
    public void testThatSecurityIsMatchedByISINandCurrency()
    {
        SecurityCache cache = new SecurityCache(client);
        Security lookup = cache.lookup("DE0007164600", null, null, null, "USD", () -> new Security());
        assertThat(client.getSecurities().get(0), not(is(lookup)));
        assertThat(client.getSecurities().get(1), is(lookup));
    }
}
