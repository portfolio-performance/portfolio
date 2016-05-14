package name.abuchen.portfolio.online.impl;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.SecuritySearchProvider.ResultItem;

@SuppressWarnings("nls")
public class YahooSearchProviderTest
{

    @Test
    public void testParsingHtml() throws IOException
    {
        // search: https://de.finance.yahoo.com/lookup?s=BASF*&t=A&b=0&m=ALL

        try (Scanner scanner = new Scanner(getClass().getResourceAsStream("response_yahoo_search.txt"), "UTF-8"))
        {
            String html = scanner.useDelimiter("\\A").next();
            Document document = Jsoup.parse(html);

            List<ResultItem> items = new YahooSearchProvider().extractFrom(document);

            assertThat(items.size(), equalTo(20));

            ResultItem p = items.get(0);
            assertThat(p.getSymbol(), equalTo("D979C.LS"));
            assertThat(p.getName(), equalTo("BASF AG/CITI WT 14"));
            assertThat(p.getIsin(), equalTo("DE000CF79JW9"));
            assertThat(p.getLastTrade(), equalTo(Values.Quote.factorize(0.11)));
            assertThat(p.getType(), equalTo("Zertifikate & OS"));
            assertThat(p.getExchange(), equalTo("LIS"));
        }
    }
}
