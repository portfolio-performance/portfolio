package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class EurostatHICPQuoteFeedTest
{
    @Test
    public void testExtractQuotesParsesMonthlySeries()
    {
        String jsonResponse = """
                        {
                          "value": {
                            "0": 100.67,
                            "1": 100.79
                          },
                          "dimension": {
                            "time": {
                              "category": {
                                "index": {
                                  "2025-12": 0,
                                  "2026-01": 1
                                }
                              }
                            }
                          }
                        }
                        """;

        QuoteFeedData data = new QuoteFeedData();

        new EurostatHICPQuoteFeed().extractQuotes(jsonResponse, data);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertThat(data.getLatestPrices().size(), is(2));
        assertThat(data.getLatestPrices().get(0).getDate(), is(LocalDate.of(2025, 12, 1)));
        assertThat(data.getLatestPrices().get(0).getValue(), is(Values.Quote.factorize(100.67)));
        assertThat(data.getLatestPrices().get(1).getDate(), is(LocalDate.of(2026, 1, 1)));
        assertThat(data.getLatestPrices().get(1).getValue(), is(Values.Quote.factorize(100.79)));
    }

    @Test
    public void testExtractQuotesReportsSchemaErrors()
    {
        String jsonResponse = """
                        {
                          "value": {
                            "0": 100.67
                          }
                        }
                        """;

        QuoteFeedData data = new QuoteFeedData();

        new EurostatHICPQuoteFeed().extractQuotes(jsonResponse, data);

        assertThat(data.getErrors().size(), is(1));
        assertThat(data.getErrors().get(0).getMessage(), containsString(EurostatHICPQuoteFeed.DATASET_VERSION));
    }
}
