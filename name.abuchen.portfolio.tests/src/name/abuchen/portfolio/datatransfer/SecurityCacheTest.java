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
        client.addSecurity(security);
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
}
