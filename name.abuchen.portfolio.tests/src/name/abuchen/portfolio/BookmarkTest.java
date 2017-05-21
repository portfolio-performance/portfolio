package name.abuchen.portfolio;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

@SuppressWarnings("nls")
public class BookmarkTest
{

    @Test
    public void testURLCreation()
    {
        Bookmark page = new Bookmark("", "http://{tickerSymbol,isin,wkn,name}");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(security), equalTo("http://DE0007100000"));

        security.setTickerSymbol("DAI.DE");
        assertThat(page.constructURL(security), equalTo("http://DAI.DE"));
        
        page = new Bookmark("", "http://{isin,tickerSymbol,wkn,name}");
        assertThat(page.constructURL(security), equalTo("http://DE0007100000"));
    }

    @Test
    public void testDuplicateKeyReplacement()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/?q={isin}&isin={isin}");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(security),
                        equalTo("https://www.flatex.de/suche/?q=DE0007100000&isin=DE0007100000"));
    }

    @Test
    public void testMultipleKeyReplacement()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/?q={wkn}&isin={isin}");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(security), equalTo("https://www.flatex.de/suche/?q=12345&isin=DE0007100000"));
    }

    @Test
    public void testSuffixIsAppended()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/?q={wkn}&isin={isin}XXX");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(security), equalTo("https://www.flatex.de/suche/?q=12345&isin=DE0007100000XXX"));
    }

    @Test
    public void testIfNoReplacmentsAreFound()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/XXX");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(security), equalTo("https://www.flatex.de/suche/XXX"));
    }

    @Test
    public void testIfUnknownKeysAreFound()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/{something}");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(security), equalTo("https://www.flatex.de/suche/"));
    }

    @Test
    public void testTickerSymbolPrefix()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/{tickerSymbolPrefix}");
        Security security = new Security("Daimler", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID);

        assertThat(page.constructURL(security), equalTo("https://www.flatex.de/suche/DAI"));
    }
}
