package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;

@SuppressWarnings("nls")
public class PatchInstrumentAttributesTest
{
    private static JsonObject json(String body)
    {
        return JsonParser.parseString(body).getAsJsonObject();
    }

    private static AttributeType typeById(Client client, String id)
    {
        return client.getSettings().getAttributeTypes().filter(a -> id.equals(a.getId())).findFirst().orElseThrow();
    }

    @Test
    public void testSetAndClearScalarAttributes()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.getAttributes().put(typeById(client, "vendor"), "OLD");

        SecuritiesHandler.patch(client, security.getUUID(),
                        json("{\"attributes\":{\"ter\":0.008,\"vendor\":null}}"));

        assertThat(security.getAttributes().get(typeById(client, "ter")), is(Double.valueOf(0.008d)));
        assertThat(security.getAttributes().get(typeById(client, "vendor")), nullValue());
    }

    @Test
    public void testAmountAttributeIsScaledAndRounded()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);

        // aum is an AmountPlain (precision 2) attribute on Security
        SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{\"aum\":12.999}}"));

        assertThat(security.getAttributes().get(typeById(client, "aum")), is(Long.valueOf(1300L)));
    }

    @Test
    public void testUnknownAttributeRejected()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{\"nope\":1}}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).field(), is("attributes.nope"));
            assertThat(e.getErrors().get(0).code(), is("unknown-attribute"));
        }
    }

    @Test
    public void testUnsupportedTypeRejected()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        try
        {
            // logo is an Image attribute -> unsupported
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{\"logo\":\"x\"}}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).code(), is("unsupported-attribute-type"));
        }
    }

    @Test
    public void testNotApplicableTypeRejected()
    {
        var client = new Client();
        // an attribute that targets accounts, not securities
        var accountOnly = new AttributeType("accountRating");
        accountOnly.setName("Rating");
        accountOnly.setTarget(name.abuchen.portfolio.model.Account.class);
        accountOnly.setType(String.class);
        accountOnly.setConverter(AttributeType.StringConverter.class);
        client.getSettings().addAttributeType(accountOnly);
        var security = new SecurityBuilder().addTo(client);
        try
        {
            SecuritiesHandler.patch(client, security.getUUID(),
                            json("{\"attributes\":{\"accountRating\":\"A\"}}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).code(), is("attribute-not-applicable"));
        }
    }

    @Test
    public void testWrongValueTypeRejectedAndNothingApplied()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{\"ter\":\"lots\"}}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            assertThat(security.getAttributes().get(typeById(client, "ter")), nullValue());
        }
    }

    @Test
    public void testAttributesMustBeObjectRejected()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":\"nope\"}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).field(), is("attributes"));
            assertThat(e.getErrors().get(0).code(), is("invalid-type"));
        }
    }

    @Test
    public void testAttributesArrayRejected()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        try
        {
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":[1,2]}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).field(), is("attributes"));
            assertThat(e.getErrors().get(0).code(), is("invalid-type"));
        }
    }

    @Test
    public void testTopLevelAndAttributeErrorsReportedTogether()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.setName("ORIGINAL");
        try
        {
            SecuritiesHandler.patch(client, security.getUUID(),
                            json("{\"name\":null,\"attributes\":{\"nope\":1}}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getErrors().size(), is(2));
            assertThat(security.getName(), is("ORIGINAL"));
            assertThat(security.getAttributes().get(typeById(client, "ter")), nullValue());
        }
    }

    @Test
    public void testPercentOverflowRejectedAndNothingApplied()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        try
        {
            // 1e400 exceeds Double's range -> would decode to Infinity
            SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{\"ter\":1e400}}"));
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().get(0).code(), is("invalid-value"));
            assertThat(security.getAttributes().get(typeById(client, "ter")), nullValue());
        }
    }

    @Test
    public void testEmptyAttributesObjectDoesNotMarkDirty()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        var dirty = new AtomicBoolean();
        client.addPropertyChangeListener("dirty", event -> dirty.set(true));

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{}}"));
        assertThat(dirty.get(), is(false));
    }

    @Test
    public void testClearingUnsetAttributeDoesNotMarkDirty()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        var dirty = new AtomicBoolean();
        client.addPropertyChangeListener("dirty", event -> dirty.set(true));

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{\"vendor\":null}}"));

        assertThat(security.getAttributes().get(typeById(client, "vendor")), nullValue());
        assertThat(dirty.get(), is(false));
    }

    @Test
    public void testClearingSetAttributeStillMarksDirty()
    {
        var client = new Client();
        var security = new SecurityBuilder().addTo(client);
        security.getAttributes().put(typeById(client, "vendor"), "OLD");
        var dirty = new AtomicBoolean();
        client.addPropertyChangeListener("dirty", event -> dirty.set(true));

        SecuritiesHandler.patch(client, security.getUUID(), json("{\"attributes\":{\"vendor\":null}}"));

        assertThat(security.getAttributes().get(typeById(client, "vendor")), nullValue());
        assertThat(dirty.get(), is(true));
    }
}
