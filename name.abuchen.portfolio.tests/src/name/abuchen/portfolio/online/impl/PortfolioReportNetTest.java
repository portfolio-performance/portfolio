package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import name.abuchen.portfolio.model.ClientSettings;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.impl.PortfolioReportNet.OnlineItem;

@SuppressWarnings("nls")
public class PortfolioReportNetTest
{
    @Test
    public void testUpdateSecurity() throws ParseException
    {
        JSONObject jsonObject = (JSONObject) JSONValue.parseWithException("{\n" + //
                        "    \"isin\": \"DE0005190003\",\n" + //
                        "    \"symbolXfra\": \"BMW\"," + //
                        "    \"symbolXnas\": \"BAMXF\"," + //
                        "    \"symbolXnys\": null," + //
                        "    \"name\": \"BAY.MOTOREN WERKE AG ST\",\n" + //
                        "    \"securityType\": \"share\",\n" + //
                        "    \"uuid\": \"f9c39f31b1f443639e462cd8e22e3ce7\",\n" + //
                        "    \"wkn\": \"519000\"\n" + //
                        "}");

        OnlineItem item = OnlineItem.from(jsonObject);

        Security security = item.create(new ClientSettings());

        assertValues(security);

        assertThat(item.update(security), is(false));

        security.setIsin("x");
        assertThat(item.update(security), is(true));
        assertValues(security);
        assertThat(item.update(security), is(false));

        security.setWkn("x");
        assertThat(item.update(security), is(true));
        assertValues(security);
        assertThat(item.update(security), is(false));

        security.getProperties().findAny().ifPresent(security::removeProperty);
        assertThat(item.update(security), is(true));
        assertValues(security);
        assertThat(item.update(security), is(false));

        security.addProperty(new SecurityProperty(SecurityProperty.Type.MARKET, "XLSE", "x"));
        assertThat(item.update(security), is(true));
        assertValues(security);
        assertThat(item.update(security), is(false));

        security.getProperties().findAny().ifPresent(property -> {
            security.removeProperty(property);
            security.addProperty(new SecurityProperty(SecurityProperty.Type.MARKET, property.getName(), "x"));
        });
        assertThat(item.update(security), is(true));
        assertValues(security);
        assertThat(item.update(security), is(false));
    }

    private void assertValues(Security security)
    {
        assertThat(security.getIsin(), is("DE0005190003"));
        assertThat(security.getWkn(), is("519000"));
        assertThat(security.getName(), is("BAY.MOTOREN WERKE AG ST"));
        assertThat(security.getOnlineId(), is("f9c39f31b1f443639e462cd8e22e3ce7"));
        assertThat(security.getProperties().count(), is(2L));
        assertThat(security.getProperties().sorted((r, l) -> r.getName().compareTo(l.getName()))
                        .map(p -> p.getName() + "=" + p.getValue()).reduce((r, l) -> r + ";" + l).orElse(null),
                        is("XFRA=BMW;XNAS=BAMXF"));
    }

}
