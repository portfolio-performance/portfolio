package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class HTMLTableQuoteFeedHunTest
{

    @Test
    public void testHistorical()
    {
        HTMLTableQuoteFeed feed = new HTMLTableQuoteFeed();

        //https://www.bamosz.hu/en/alapoldal?isin=HU0000705272&interval=10Y
        String html = "<table width=\"100%\" cellspacing=\"0\" class=\"dataTable2\" style=\"margin-top: 20px;\"> <tbody><tr class=\"header\"> <td>Date</td> <td>Unit price</td> <td>Net asset value</td> <td>Paid return</td> <td>Daily turnover</td> <td>Daily turnover (%)</td> <td>3 monthly</td> <td>6 monthly</td> <td>1 yearly</td> <td>3 yearly</td> <td>5 yearly</td> <td>10 yearly</td> </tr> <tr> <td>2021.12.28.\r\n"
                        + "                                    </td> <td>2.307738\r\n"
                        + "                                    </td> <td>5,100,101,762\r\n"
                        + "                                    </td> <td>0\r\n"
                        + "                                    </td> <td>-814144\r\n"
                        + "                                   </td> <td>0%\r\n"
                        + "                                        \r\n"
                        + "                                    </td> <td>3,8%\r\n"
                        + "                                        \r\n"
                        + "                                    </td> <td>-0,3%\r\n"
                        + "                                        \r\n"
                        + "                                    </td> <td>8,5%\r\n"
                        + "                                        \r\n"
                        + "                                    </td> <td>13,7%\r\n"
                        + "                                        </td> <td>10%\r\n"
                        + "                                        \r\n"
                        + "                                    </td> <td>8,6%\r\n"
                        + "                                        \r\n"
                        + "                                    </td> </tr> \r\n"
                        + "                                    \r\n"
                        + "                                    </tbody></table>";

        QuoteFeedData data = feed.getHistoricalQuotes(html);

        List<LatestSecurityPrice> prices = data.getLatestPrices();

        assertThat(prices.size(), is(1));

        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.get(0).getDate(), is(LocalDate.parse("2021-12-28")));
        assertThat(prices.get(0).getValue(), is(Values.Quote.factorize(2.307738)));
    }
}
