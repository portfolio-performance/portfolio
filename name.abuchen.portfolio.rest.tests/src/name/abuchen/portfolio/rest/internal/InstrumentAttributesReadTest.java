package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

@SuppressWarnings("nls")
public class InstrumentAttributesReadTest
{
    private static AttributeType typeById(Client client, String id)
    {
        return client.getSettings().getAttributeTypes().filter(a -> id.equals(a.getId())).findFirst().orElseThrow();
    }

    @Test
    public void testSetScalarAttributesAreSerialized()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        security.getAttributes().put(typeById(client, "ter"), Double.valueOf(0.007d));
        security.getAttributes().put(typeById(client, "vendor"), "ACME");

        var json = EntityJson.toJson(client, security);
        var attributes = json.getAsJsonObject("attributes");

        assertThat(attributes.get("ter").getAsString(), is("0.007"));
        assertThat(attributes.get("vendor").getAsString(), is("ACME"));
    }

    @Test
    public void testAttributesKeyOmittedWhenNoneSet()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        var json = EntityJson.toJson(client, security);

        assertThat(json.has("attributes"), is(false));
    }

    @Test
    public void testUnsupportedAndOrphanedAttributesAreSkipped()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        // the built-in "logo" is an Image attribute on Security -> not exposed
        security.getAttributes().put(typeById(client, "logo"), "base64data");
        // a value keyed by an id with no matching type definition -> orphaned
        var orphan = new AttributeType("gone");
        orphan.setTarget(Security.class);
        orphan.setType(String.class);
        orphan.setConverter(AttributeType.StringConverter.class);
        security.getAttributes().put(orphan, "x"); // not registered in settings

        var json = EntityJson.toJson(client, security);

        assertThat(json.has("attributes"), is(false));
    }

    @Test
    public void testNonFinitePercentValueIsSkippedNotThrown()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        // a poisoned stored value (e.g. from a corrupted file) must not blow up serialization
        security.getAttributes().put(typeById(client, "ter"), Double.valueOf(Double.POSITIVE_INFINITY));

        var json = EntityJson.toJson(client, security);

        // ter was the only attribute set -> the whole attributes key is omitted
        assertThat(json.has("attributes"), is(false));
    }

    @Test
    public void testTypeMismatchedAttributeIsSkippedNotThrown()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        // aum is declared Amount/Long-typed, but a value of the wrong runtime type
        // (e.g. from a field type edited after values were stored) must not blow up
        // serialization of the whole instruments collection
        security.getAttributes().put(typeById(client, "vendor"), "ACME");
        security.getAttributes().put(typeById(client, "aum"), "not-a-long");

        var json = EntityJson.toJson(client, security);
        var attributes = json.getAsJsonObject("attributes");

        assertThat(attributes.has("vendor"), is(true));
        assertThat(attributes.has("aum"), is(false));
    }

    @Test
    public void testBooleanAttributeSerializes()
    {
        var client = new Client();
        var reviewed = new AttributeType("reviewed");
        reviewed.setName("Reviewed");
        reviewed.setTarget(Security.class);
        reviewed.setType(Boolean.class);
        reviewed.setConverter(AttributeType.BooleanConverter.class);
        client.getSettings().addAttributeType(reviewed);

        var security = new SecurityBuilder().addTo(client);
        security.getAttributes().put(reviewed, Boolean.TRUE);

        var json = EntityJson.toJson(client, security);
        assertThat(json.getAsJsonObject("attributes").get("reviewed").getAsBoolean(), is(true));
    }
}
