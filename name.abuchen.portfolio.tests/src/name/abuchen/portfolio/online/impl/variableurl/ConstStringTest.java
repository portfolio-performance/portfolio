package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.Iterators;

import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("nls")
public class ConstStringTest
{
    @Test
    public void testMaxFailedAttempts()
    {
        assertThat(getURL().getMaxFailedAttempts(), equalTo(0L));
    }

    @Test
    public void testVariations()
    {
        List<String> variations = new LinkedList<>();
        Iterators.limit(getURL().iterator(), 2).forEachRemaining(variations::add);

        assertThat(variations, equalTo(Collections.singletonList(
            "https://192.0.2.1/quotes.php?isin=DE0007100000&wkn=710000&ticker=DAI.DE&currency=EUR"
        )));
    }

    private VariableURL getURL()
    {
        Security security = new Security();
        security.setIsin("DE0007100000");
        security.setWkn("710000");
        security.setTickerSymbol("DAI.DE");
        security.setCurrencyCode("EUR");

        VariableURL variableURL = Factory.fromString(
            "https://192.0.2.1/quotes.php?isin={ISIN}&wkn={WKN}&ticker={TICKER}&currency={CURRENCY}"
        );
        variableURL.setSecurity(security);
        return variableURL;
    }
}
