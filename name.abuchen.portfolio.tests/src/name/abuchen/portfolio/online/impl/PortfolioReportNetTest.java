package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.PortfolioReportNet.OnlineItem;

@SuppressWarnings("nls")
public class PortfolioReportNetTest
{
    @Test
    public void testUpdateSecurity() throws ParseException
    {
        JSONObject jsonObject = (JSONObject) JSONValue.parseWithException(
                        "{\"code\":\"\",\"events\":[],\"isin\":\"DE0005190003\",\"logoUrl\":\"https://api.portfolio-report.net/v1/images/a4ed3816-3885-43c7-836e-acdeb6ec3248.png\",\"markets\":[{\"currencyCode\":\"EUR\",\"firstPriceDate\":\"2000-01-03\",\"lastPriceDate\":\"2024-04-26\",\"marketCode\":\"XETR\",\"symbol\":\"BMW\"}],\"name\":\"BMW AG St\",\"pricesAvailable\":true,\"securityTaxonomies\":[{\"rootTaxonomyUuid\":\"5b0d5647-a4e6-4db8-807b-c3a6d11697a7\",\"taxonomyUuid\":\"def0841b-cc83-408f-89e6-e0a5f3156a27\",\"weight\":\"100\"},{\"rootTaxonomyUuid\":\"072bba7b-ed7a-4cb4-aab3-91520d00fb00\",\"taxonomyUuid\":\"6ecc96ca-3a8b-4c94-8ebd-ef7a844e034e\",\"weight\":\"100\"}],\"securityType\":\"share\",\"symbolXfra\":\"BMW\",\"symbolXnas\":\"BAMXF\",\"symbolXnys\":\"\",\"tags\":[\"DAX\"],\"uuid\":\"f9c39f31b1f443639e462cd8e22e3ce7\",\"wkn\":\"519000\"}");

        OnlineItem item = OnlineItem.from(jsonObject);

        Security security = item.create(new Client());

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
    }

    private void assertValues(Security security)
    {
        assertThat(security.getIsin(), is("DE0005190003"));
        assertThat(security.getWkn(), is("519000"));
        assertThat(security.getName(), is("BMW AG St"));
        assertThat(security.getOnlineId(), is("f9c39f31b1f443639e462cd8e22e3ce7"));
    }
}
