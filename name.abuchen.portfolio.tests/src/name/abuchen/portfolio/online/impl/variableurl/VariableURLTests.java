package name.abuchen.portfolio.online.impl.variableurl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;

@SuppressWarnings("nls")
public class VariableURLTests
{
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-32");
    private static Security security;

    @BeforeClass
    public static void setup()
    {
        security = new Security();
        security.setIsin("LU0635178014");
        security.setWkn("ETF127");
        security.setTickerSymbol("E127");
    }

    @Test
    public void testISINbeforeDATE()
    {
        VariableURL url = Factory.fromString(
                        "https://www.server.de/{ISIN}/historische_kurse?month={DATE:yyyy-MM-32}&currency=EUR");

        url.setSecurity(security);

        Iterator<String> iter = url.iterator();

        assertTrue(iter.hasNext());
        assertThat(iter.next(), is("https://www.server.de/LU0635178014/historische_kurse?month="
                        + LocalDate.now().format(FORMATTER) + "&currency=EUR"));
    }

    @Test
    public void testDATEbeforeISIN()
    {
        VariableURL url = Factory
                        .fromString("https://www.server.de/historische_kurse?month={DATE:yyyy-MM-32}&isin={ISIN}");

        url.setSecurity(security);

        Iterator<String> iter = url.iterator();

        assertTrue(iter.hasNext());
        assertThat(iter.next(), is("https://www.server.de/historische_kurse?month=" + LocalDate.now().format(FORMATTER)
                        + "&isin=LU0635178014"));
    }

    @Test
    public void testDATEwithOthers()
    {
        VariableURL url = Factory.fromString(
                        "https://www.server.de/historische_kurse?month={DATE:yyyy-MM-32}&isin={ISIN}&ticker={TICKER}&wkn={WKN}&currency={CURRENCY}");

        url.setSecurity(security);

        Iterator<String> iter = url.iterator();

        assertTrue(iter.hasNext());
        assertThat(iter.next(),
                        is("https://www.server.de/historische_kurse?month="
                                        + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-32"))
                                        + "&isin=LU0635178014&ticker=E127&wkn=ETF127&currency=EUR"));
    }
}
