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
        security.setTickerSymbol("SAP.DE");
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

    @Test
    public void testThatSecurityIsMatchedByTickerSymbol()
    {
        SecurityCache cache = new SecurityCache(client);
        Security lookup = cache.lookup(null, "SAP", null, null, () -> new Security());
        assertThat(client.getSecurities().get(0), is(lookup));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatSecurityWithDuplicateIdentiferAreNotMatched()
    {
        Security duplicate = new Security();
        duplicate.setName("Security Name");
        duplicate.setIsin("DE0007164600");
        duplicate.setTickerSymbol("SAP.DE");
        duplicate.setWkn("716460");
        client.addSecurity(duplicate);

        SecurityCache cache = new SecurityCache(client);
        cache.lookup("DE0007164600", null, null, null, () -> new Security());
    }

    @Test
    public void testThatSecurityWithDuplicateIdentiferAreMatchedIfOneIsRetired()
    {
        Security duplicate = new Security();
        duplicate.setName("Security Name");
        duplicate.setIsin("DE0007164600");
        duplicate.setTickerSymbol("SAP.DE");
        duplicate.setWkn("716460");
        duplicate.setRetired(true);
        client.addSecurity(duplicate);

        SecurityCache cache = new SecurityCache(client);
        Security lookup = cache.lookup("DE0007164600", null, null, null, () -> new Security());
        assertThat(client.getSecurities().get(0), is(lookup));

        Security duplicate2 = new Security();
        duplicate2.setName("Security Name");
        duplicate2.setIsin("DE0007164600");
        duplicate2.setTickerSymbol("SAP.DE");
        duplicate2.setWkn("716460");
        duplicate2.setRetired(true);
        client.addSecurity(duplicate2);

        cache = new SecurityCache(client);
        lookup = cache.lookup("DE0007164600", null, null, null, () -> new Security());
        assertThat(client.getSecurities().get(0), is(lookup));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatSecurityWithDuplicateIdentiferAreNotMatchedWithMultipleActive()
    {
        Security active2 = new Security();
        active2.setName("Security Name");
        active2.setIsin("DE0007164600");
        active2.setTickerSymbol("SAP.DE");
        active2.setWkn("716460");
        client.addSecurity(active2);
        
        Security duplicate = new Security();
        duplicate.setName("Security Name");
        duplicate.setIsin("DE0007164600");
        duplicate.setTickerSymbol("SAP.DE");
        duplicate.setWkn("716460");
        duplicate.setRetired(true);
        client.addSecurity(duplicate);

        Security duplicate2 = new Security();
        duplicate2.setName("Security Name");
        duplicate2.setIsin("DE0007164600");
        duplicate2.setTickerSymbol("SAP.DE");
        duplicate2.setWkn("716460");
        duplicate2.setRetired(true);
        client.addSecurity(duplicate2);

        SecurityCache cache = new SecurityCache(client);
        cache.lookup("DE0007164600", null, null, null, () -> new Security());
    }
}
