package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Test;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

@SuppressWarnings("nls")
public class MOEXSearchProviderTest
{
    private String read(String filename) throws IOException
    {
        try (InputStream in = getClass().getResourceAsStream(filename))
        {
            try (Scanner scanner = new Scanner(in, StandardCharsets.UTF_8))
            {
                return scanner.useDelimiter("\\A").next();
            }
        }
    }

    @Test
    public void testSearchSBER() throws IOException
    {
        MOEXSearchProvider provider = new MOEXSearchProvider();

        List<ResultItem> answer = new ArrayList<>();
        provider.extract(answer, read("sber_search_fixture.json"));

        // only tradeable securities with a supported type and a market price
        // board are returned, i.e. the price fixing and the unsupported
        // interval fund (which has a market price board) must be filtered out
        assertThat(answer.size(), is(3));

        // an unsupported traded instrument with a non-null market price board
        // is excluded
        assertThat(answer.stream().filter(r -> "RU000A0ZZMD7".equals(r.getSymbol())).findAny().isPresent(), is(false));

        ResultItem sber = answer.stream().filter(r -> "SBER".equals(r.getSymbol())).findAny().orElseThrow();
        assertEquals("Sberbank", sber.getName());
        assertEquals("RU0009029540", sber.getIsin());
        assertEquals("RUB", sber.getCurrencyCode());
        assertEquals("MISX", sber.getExchange());
        assertEquals(MOEXQuoteFeed.ID, sber.getFeedId());

        // the market path is stored as a feed property
        Security security = sber.create(null);
        assertEquals("SBER", security.getTickerSymbol());
        assertEquals("shares", security.getPropertyValue(
                        name.abuchen.portfolio.model.SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET).orElse(null));
    }

    @Test
    public void testSearchBond() throws IOException
    {
        MOEXSearchProvider provider = new MOEXSearchProvider();

        List<ResultItem> answer = new ArrayList<>();
        provider.extract(answer, read("sber_search_fixture.json"));

        ResultItem bond = answer.stream().filter(r -> "RU000A10DS74".equals(r.getSymbol())).findAny().orElseThrow();
        assertEquals("SBER51", bond.getName());

        Security security = bond.create(null);
        assertEquals("bonds", security.getPropertyValue(
                        name.abuchen.portfolio.model.SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET).orElse(null));
    }

    @Test
    public void testSearchIndex() throws IOException
    {
        MOEXSearchProvider provider = new MOEXSearchProvider();

        List<ResultItem> answer = new ArrayList<>();
        provider.extract(answer, read("imoex_search_fixture.json"));

        // the plain index is returned even without a market price board; the
        // index variant IMOEX2 is filtered out because it has no price board
        assertThat(answer.size(), is(1));

        ResultItem index = answer.get(0);
        assertEquals("IMOEX", index.getSymbol());

        Security security = index.create(null);
        assertEquals("index", security.getPropertyValue(
                        name.abuchen.portfolio.model.SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET).orElse(null));
    }

    @Test
    public void testEmptyResponse() throws IOException
    {
        MOEXSearchProvider provider = new MOEXSearchProvider();

        List<ResultItem> answer = new ArrayList<>();
        provider.extract(answer, "{}");

        assertThat(answer.isEmpty(), is(true));
    }

    @Test
    public void testSearchCurrency() throws IOException
    {
        var provider = new MOEXSearchProvider();

        var answer = new ArrayList<ResultItem>();
        provider.extract(answer, read("gldrub_search_fixture.json"));

        assertThat(answer.size(), is(1));

        var gldrub = answer.get(0);
        assertEquals("GLDRUB_TOM", gldrub.getSymbol());
        assertEquals("GLD/RUB", gldrub.getCurrencyCode());

        // the created security is an exchange rate with the base currency as
        // currency and the term currency as target currency
        var security = gldrub.create(null);
        assertEquals("GLD", security.getCurrencyCode());
        assertEquals("RUB", security.getTargetCurrencyCode());
        assertEquals("currency", security.getPropertyValue(
                        name.abuchen.portfolio.model.SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_ENGINE).orElse(null));
        assertEquals("selt", security.getPropertyValue(
                        name.abuchen.portfolio.model.SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_MARKET).orElse(null));
        assertEquals("CETS", security.getPropertyValue(
                        name.abuchen.portfolio.model.SecurityProperty.Type.FEED, MOEXQuoteFeed.MOEX_BOARD).orElse(null));
    }
}
