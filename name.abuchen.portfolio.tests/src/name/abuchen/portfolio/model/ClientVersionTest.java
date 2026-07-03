package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class ClientVersionTest
{
    @Test
    public void testCurrentVersionIncludesCorporateActions()
    {
        assertThat(Client.CURRENT_VERSION, is(71));
        assertThat(Client.VERSION_WITH_CORPORATE_ACTIONS, is(71));
        assertThat(new Client().getVersion(), is(71));
    }
}
