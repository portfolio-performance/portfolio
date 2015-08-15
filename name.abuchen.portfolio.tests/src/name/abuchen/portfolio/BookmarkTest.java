package name.abuchen.portfolio;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Bookmark;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;

import org.junit.Test;

@SuppressWarnings("nls")
public class BookmarkTest
{

    @Test
    public void testURLCreation()
    {
        Bookmark page = new Bookmark("", "http://-{tickerSymbol}-{isin}-{wkn}-{name}");
        Security security = new Security("Daimler", "DE0007100000", "DAI.DE", YahooFinanceQuoteFeed.ID);
        security.setWkn("12345");

        assertThat(page.constructURL(security), equalTo("http://-DAI.DE-DE0007100000-12345-Daimler"));
    }
}
