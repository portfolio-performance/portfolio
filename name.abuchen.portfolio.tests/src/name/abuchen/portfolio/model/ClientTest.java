package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

@SuppressWarnings("nls")
public class ClientTest
{
    @Test
    public void testCorporateActionRegistry()
    {
        Client client = new Client();
        var entry = new CorporateActionEntry();
        entry.setType(CorporateActionEntry.Type.SPIN_OFF);

        client.addCorporateAction(entry);
        assertThat(client.getCorporateActions(), hasItem(entry));

        client.removeCorporateAction(entry);
        assertThat(client.getCorporateActions().isEmpty(), is(true));
    }
}
