package name.abuchen.portfolio;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.Test;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.model.Client;
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

        assertThat(page.constructURL(new Client(), security), equalTo("http://DE0007100000"));

        security.setTickerSymbol("DAI.DE");
        assertThat(page.constructURL(new Client(), security), equalTo("http://DAI.DE"));
        
        page = new Bookmark("", "http://{isin,tickerSymbol,wkn,name}");
        assertThat(page.constructURL(new Client(), security), equalTo("http://DE0007100000"));
    }

    @Test
    public void testDuplicateKeyReplacement()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/?q={isin}&isin={isin}");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(new Client(), security),
                        equalTo("https://www.flatex.de/suche/?q=DE0007100000&isin=DE0007100000"));
    }

    @Test
    public void testMultipleKeyReplacement()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/?q={wkn}&isin={isin}");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(new Client(), security),
                        equalTo("https://www.flatex.de/suche/?q=12345&isin=DE0007100000"));
    }

    @Test
    public void testSuffixIsAppended()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/?q={wkn}&isin={isin}XXX");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(new Client(), security),
                        equalTo("https://www.flatex.de/suche/?q=12345&isin=DE0007100000XXX"));
    }

    @Test
    public void testIfNoReplacmentsAreFound()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/XXX");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(new Client(), security), equalTo("https://www.flatex.de/suche/XXX"));
    }

    @Test
    public void testIfUnknownKeysAreFound()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/{something}");
        Security security = new Security("Daimler", "DE0007100000", "", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(new Client(), security), equalTo("https://www.flatex.de/suche/"));
    }

    @Test
    public void testTickerSymbolPrefix()
    {
        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/{tickerSymbolPrefix}");
        Security security = new Security("Daimler", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID);

        assertThat(page.constructURL(new Client(), security), equalTo("https://www.flatex.de/suche/DAI"));
    }

    @Test
    public void testCustomAttributes()
    {
        AttributeType attribute = new AttributeType(UUID.randomUUID().toString());
        attribute.setType(String.class);
        attribute.setName("CUSIP Number");
        attribute.setColumnLabel("CUSIP");
        attribute.setConverter(AttributeType.StringConverter.class);
        attribute.setTarget(Security.class);

        Client client = new Client();
        client.getSettings().addAttributeType(attribute);

        Bookmark page = new Bookmark("", "https://www.flatex.de/suche/{CUSIP}");
        Security security = new Security("Daimler", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID);
        security.getAttributes().put(attribute, "D1668R123");

        assertThat(page.constructURL(client, security), equalTo("https://www.flatex.de/suche/D1668R123"));
    }
}
