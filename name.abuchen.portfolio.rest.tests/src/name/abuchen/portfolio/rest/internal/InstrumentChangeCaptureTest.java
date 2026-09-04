package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.rest.internal.InstrumentChangeLog.Change;

@SuppressWarnings("nls")
public class InstrumentChangeCaptureTest
{
    private static JsonObject json(String body)
    {
        return JsonParser.parseString(body).getAsJsonObject();
    }

    private static AttributeType vendorType(Client client)
    {
        var vendor = new AttributeType("vendor-x");
        vendor.setName("Data vendor");
        vendor.setTarget(Security.class);
        vendor.setType(String.class);
        vendor.setConverter(AttributeType.StringConverter.class);
        client.getSettings().addAttributeType(vendor);
        return vendor;
    }

    @Test
    public void testRenameCapturedAsOldToNew()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Old");

        var result = SecuritiesHandler.patch(client, security.getUUID(), json("{\"name\":\"New\"}"));

        assertThat(result.instrumentName(), is("New"));
        assertThat(result.changes(), contains(new Change("name", "Old", "New")));
    }

    @Test
    public void testSettingOptionalFieldCapturedWithUnsetOldSide()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setIsin(null); // SecurityBuilder sets a default ISIN; clear it to test the unset case

        var result = SecuritiesHandler.patch(client, security.getUUID(), json("{\"isin\":\"DE0001234567\"}"));

        assertThat(result.changes(), contains(new Change("isin", null, "DE0001234567")));
    }

    @Test
    public void testClearingOptionalFieldCapturedWithRemovedNewSide()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setIsin("DE0001234567");

        var result = SecuritiesHandler.patch(client, security.getUUID(), json("{\"isin\":null}"));

        assertThat(result.changes(), contains(new Change("isin", "DE0001234567", null)));
    }

    @Test
    public void testUnchangedValueProducesNoChange()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Same");

        var result = SecuritiesHandler.patch(client, security.getUUID(), json("{\"name\":\"Same\"}"));

        assertThat(result.changes(), is(empty()));
    }

    @Test
    public void testEmptyPatchProducesNoChange()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        var result = SecuritiesHandler.patch(client, security.getUUID(), json("{}"));

        assertThat(result.changes(), is(empty()));
    }

    @Test
    public void testCustomAttributeSetCapturedWithTypeNameAndRenderedValue()
    {
        var client = new Client();
        var vendor = vendorType(client);
        var security = new SecurityBuilder().addTo(client);

        var result = SecuritiesHandler.patch(client, security.getUUID(),
                        json("{\"attributes\":{\"vendor-x\":\"ACME\"}}"));

        assertThat(result.changes(), contains(new Change(vendor.getName(), null, "ACME")));
    }

    @Test
    public void testCustomAttributeRemovalCapturedWithRemovedNewSide()
    {
        var client = new Client();
        var vendor = vendorType(client);
        var security = new SecurityBuilder().addTo(client);
        security.getAttributes().put(vendor, "ACME");

        var result = SecuritiesHandler.patch(client, security.getUUID(),
                        json("{\"attributes\":{\"vendor-x\":null}}"));

        assertThat(result.changes(), contains(new Change(vendor.getName(), "ACME", null)));
    }

    @Test
    public void testDeleteReturnsInstrumentName()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("Doomed");

        var name = SecuritiesHandler.delete(client, security.getUUID());

        assertThat(name, is("Doomed"));
    }
}
