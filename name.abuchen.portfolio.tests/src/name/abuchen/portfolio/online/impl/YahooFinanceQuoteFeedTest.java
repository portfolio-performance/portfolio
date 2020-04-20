package name.abuchen.portfolio.online.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.Mockito;

import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class YahooFinanceQuoteFeedTest
{
    private String getHistoricalYahooQuotes()
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream("response_yahoo_historical.txt"), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }

    @Test
    public void testGetLatestQuoteValid() throws IOException
    {
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        String response = "{\"quoteResponse\":{\"result\":[{\"language\":\"en-US\",\"region\":\"US\",\"quoteType\":\"EQUITY\",\"quoteSourceName\":\"Nasdaq Real Time Price\",\"triggerable\":true,\"currency\":\"USD\",\"sourceInterval\":15,\"exchangeDataDelayedBy\":0,\"tradeable\":false,\"priceHint\":2,\"exchange\":\"NMS\",\"shortName\":\"Apple Inc.\",\"longName\":\"Apple Inc.\",\"messageBoardId\":\"finmb_24937\",\"exchangeTimezoneName\":\"America/New_York\",\"exchangeTimezoneShortName\":\"EDT\",\"gmtOffSetMilliseconds\":-14400000,\"market\":\"us_market\",\"esgPopulated\":false,\"postMarketChangePercent\":-0.1949872,\"postMarketTime\":1587416695,\"postMarketPrice\":276.39,\"postMarketChange\":-0.539978,\"regularMarketChange\":-5.869995,\"regularMarketChangePercent\":-2.0756702,\"regularMarketTime\":1587412802,\"regularMarketPrice\":276.93,\"regularMarketDayHigh\":281.66,\"regularMarketDayRange\":\"276.85 - 281.66\",\"regularMarketDayLow\":276.85,\"regularMarketVolume\":31089201,\"regularMarketPreviousClose\":282.8,\"bid\":277.14,\"ask\":277.06,\"bidSize\":10,\"askSize\":9,\"fullExchangeName\":\"NasdaqGS\",\"financialCurrency\":\"USD\",\"regularMarketOpen\":277.95,\"averageDailyVolume3Month\":51071277,\"averageDailyVolume10Day\":41379080,\"fiftyTwoWeekLowChange\":106.65999,\"fiftyTwoWeekLowChangePercent\":0.6264168,\"fiftyTwoWeekRange\":\"170.27 - 327.85\",\"fiftyTwoWeekHighChange\":-50.920013,\"fiftyTwoWeekHighChangePercent\":-0.15531497,\"fiftyTwoWeekLow\":170.27,\"fiftyTwoWeekHigh\":327.85,\"dividendDate\":1581552000,\"earningsTimestamp\":1588291200,\"earningsTimestampStart\":1588291200,\"earningsTimestampEnd\":1588291200,\"trailingAnnualDividendRate\":3.04,\"trailingPE\":21.987295,\"trailingAnnualDividendYield\":0.010749646,\"marketState\":\"POST\",\"epsTrailingTwelveMonths\":12.595,\"epsForward\":14.85,\"sharesOutstanding\":4375479808,\"bookValue\":20.418,\"fiftyDayAverage\":263.85883,\"fiftyDayAverageChange\":13.071167,\"fiftyDayAverageChangePercent\":0.049538486,\"twoHundredDayAverage\":275.44904,\"twoHundredDayAverageChange\":1.480957,\"twoHundredDayAverageChangePercent\":0.005376519,\"marketCap\":1211701526528,\"forwardPE\":18.648483,\"priceToBook\":13.563033,\"firstTradeDateMilliseconds\":345479400000,\"symbol\":\"AAPL\"}],\"error\":null}}";

        YahooFinanceQuoteFeed feed = Mockito.spy(new YahooFinanceQuoteFeed());

        // we mock the rpc call to return the above string
        Mockito.doReturn(response).when(feed).rpcLatestQuote(security);

        LatestSecurityPrice price = feed.getLatestQuote(security).get();

        assertThat(price.getDate(), is(LocalDate.of(2020, 4, 20)));
        assertThat(price.getHigh(), is(2816600L));
        assertThat(price.getLow(), is(2768500L));
        assertThat(price.getValue(), is(2769300L));
        assertThat(price.getVolume(), is(31089201L));
    }

    @Test
    public void testCalculateDate()
    {

        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed();

        Security security = new Security();
        security.setName("Daimler AG");
        security.setIsin("DE0007100000");
        security.setTickerSymbol("DAI.DE");

        LocalDate nineteenHundred = LocalDate.of(1900, 1, 1);

        LocalDate date = feed.caculateStart(security);
        assertThat(date, equalTo(nineteenHundred));

        security.addPrice(new SecurityPrice(LocalDate.now(), 100));
        date = feed.caculateStart(security);
        assertThat(date, equalTo(LocalDate.now()));
    }

    @Test
    public void testParsingHistoricalQuotes()
    {
        String rawQuotes = getHistoricalYahooQuotes();

        Security security = new Security();
        security.setTickerSymbol("DAI.DE");

        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed();
        QuoteFeedData data = feed.extractQuotes(rawQuotes);
        List<LatestSecurityPrice> prices = data.getLatestPrices();
        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.size(), is(123));

        LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.of(2017, Month.NOVEMBER, 27), //
                        Values.Quote.factorize(188.55), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(0), equalTo(price));

        price = new LatestSecurityPrice(LocalDate.of(2018, Month.MAY, 25), //
                        Values.Quote.factorize(188.3), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(prices.size() - 1), equalTo(price));
    }

    @Test
    public void testParsingHistoricalAdjustedCloseQuotes() throws IOException
    {
        String rawQuotes = getHistoricalYahooQuotes();

        Security security = new Security();
        security.setTickerSymbol("DAI.DE");

        YahooFinanceAdjustedCloseQuoteFeed feed = new YahooFinanceAdjustedCloseQuoteFeed();
        QuoteFeedData data = feed.extractQuotes(rawQuotes);
        List<LatestSecurityPrice> prices = data.getLatestPrices();
        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.size(), is(123));

        LatestSecurityPrice price = new LatestSecurityPrice(LocalDate.of(2017, Month.NOVEMBER, 27), //
                        Values.Quote.factorize(180.344), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(0), equalTo(price));

        price = new LatestSecurityPrice(LocalDate.of(2018, Month.MAY, 25), //
                        Values.Quote.factorize(188.3), //
                        0, //
                        0, //
                        0);
        assertThat(prices.get(prices.size() - 1), equalTo(price));
    }

    @Test
    public void testThatAtLeastTheGivenExchangeIsReturned() throws IOException
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed()
        {
            @Override
            protected Stream<YahooSymbolSearch.Result> searchSymbols(String query) throws IOException
            {
                throw new IOException();
            }
        };

        Security s = new Security();
        s.setTickerSymbol("BAS.DE");

        ArrayList<Exception> errors = new ArrayList<Exception>();
        List<Exchange> exchanges = feed.getExchanges(s, errors);

        assertThat(exchanges.size(), is(1));
        assertThat(exchanges.get(0).getId(), is("BAS.DE"));

        assertThat(errors.size(), is(1));
    }

}
