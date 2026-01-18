package name.abuchen.portfolio.ui.dialogs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.dialogs.IBFlexConfigurationDialog.IBFlexModel;

public class IBFlexModelTest
{
    private Client client;

    @Before
    public void setUp()
    {
        client = new Client();
    }

    @Test
    public void testModelReadsStoredValues()
    {
        client.setProperty("ibflex-token", "mytoken");
        client.setProperty("ibflex-query-id", "12345");

        IBFlexModel model = new IBFlexModel(client);

        assertThat(model.getToken(), is("mytoken"));
        assertThat(model.getQueryId(), is("12345"));
    }

    @Test
    public void testApplyChangesStoresAndTrimsValues()
    {
        IBFlexModel model = new IBFlexModel(client);
        model.setToken("  newtoken  ");
        model.setQueryId("  67890  ");

        model.applyChanges();

        assertThat(IBFlexModel.getToken(client), is("newtoken"));
        assertThat(IBFlexModel.getQueryId(client), is("67890"));
    }

    @Test
    public void testApplyChangesRemovesBlankValues()
    {
        client.setProperty("ibflex-token", "existing");
        client.setProperty("ibflex-query-id", "existing");

        IBFlexModel model = new IBFlexModel(client);
        model.setToken("   ");
        model.setQueryId("");

        model.applyChanges();

        assertThat(IBFlexModel.getToken(client), is(nullValue()));
        assertThat(IBFlexModel.getQueryId(client), is(nullValue()));
    }

    @Test
    public void testClearConfiguration()
    {
        client.setProperty("ibflex-token", "mytoken");
        client.setProperty("ibflex-query-id", "12345");

        IBFlexModel.clearConfiguration(client);

        assertThat(IBFlexModel.getToken(client), is(nullValue()));
        assertThat(IBFlexModel.getQueryId(client), is(nullValue()));
    }

    @Test
    public void testHasConfiguration()
    {
        assertThat(IBFlexModel.hasConfiguration(client), is(false));

        client.setProperty("ibflex-token", "mytoken");
        client.setProperty("ibflex-query-id", "12345");

        assertThat(IBFlexModel.hasConfiguration(client), is(true));
    }

    @Test
    public void testHasConfigurationIgnoresBlankValues()
    {
        client.setProperty("ibflex-token", "   ");
        client.setProperty("ibflex-query-id", "");

        assertThat(IBFlexModel.hasConfiguration(client), is(false));
    }
}
