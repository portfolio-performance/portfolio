package name.abuchen.portfolio.online.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

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
        return getHistoricalYahooQuotes("response_yahoo_historical.txt");
    }

    private String getHistoricalYahooQuotesAX()
    {
        return getHistoricalYahooQuotes("yahoo_australian_quotes.txt");
    }

    private String getHistoricalYahooQuotes(String filename)
    {
        String responseBody = null;
        Scanner scanner = new Scanner(getClass().getResourceAsStream(filename), "UTF-8");
        responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        return responseBody;
    }

    @Test
    public void testGetLatestQuoteValid() throws IOException
    {
        Security security = new Security();
        security.setTickerSymbol("AAPL");
        String response = "{\"chart\":{\"result\":[{\"meta\":{\"currency\":\"USD\",\"symbol\":\"AAPL\",\"exchangeName\":\"NMS\",\"instrumentType\":\"EQUITY\",\"firstTradeDate\":345479400,\"regularMarketTime\":1689278404,\"gmtoffset\":-14400,\"timezone\":\"EDT\",\"exchangeTimezoneName\":\"America/New_York\",\"regularMarketPrice\":190.54,\"chartPreviousClose\":189.77,\"previousClose\":189.77,\"scale\":3,\"priceHint\":2,\"currentTradingPeriod\":{\"pre\":{\"timezone\":\"EDT\",\"end\":1689341400,\"start\":1689321600,\"gmtoffset\":-14400},\"regular\":{\"timezone\":\"EDT\",\"end\":1689364800,\"start\":1689341400,\"gmtoffset\":-14400},\"post\":{\"timezone\":\"EDT\",\"end\":1689379200,\"start\":1689364800,\"gmtoffset\":-14400}},\"tradingPeriods\":[[{\"timezone\":\"EDT\",\"end\":1689278400,\"start\":1689255000,\"gmtoffset\":-14400}]],\"dataGranularity\":\"1m\",\"range\":\"1d\",\"validRanges\":[\"1d\",\"5d\",\"1mo\",\"3mo\",\"6mo\",\"1y\",\"2y\",\"5y\",\"10y\",\"ytd\",\"max\"]},\"timestamp\":[1689255000,1689255060,1689278400],\"indicators\":{\"quote\":[{\"high\":[190.5050048828125,190.33999633789062,190.5399932861328],\"volume\":[1504949,291161,0],\"low\":[190.02000427246094,189.99000549316406,190.5399932861328],\"open\":[190.5,190.04010009765625,190.5399932861328],\"close\":[190.05999755859375,190.33999633789062,190.5399932861328]}]}}],\"error\":null}}";

        YahooFinanceQuoteFeed feed = Mockito.spy(new YahooFinanceQuoteFeed());

        // we mock the rpc call to return the above string
        Mockito.doReturn(response).when(feed).rpcLatestQuote(security);

        LatestSecurityPrice price = feed.getLatestQuote(security).get();

        assertThat(price.getDate(), is(LocalDate.of(2023, 7, 13)));
        assertThat(price.getHigh(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(price.getLow(), is(LatestSecurityPrice.NOT_AVAILABLE));
        assertThat(price.getValue(), is(Values.Quote.factorize(190.54)));
        assertThat(price.getVolume(), is(LatestSecurityPrice.NOT_AVAILABLE));
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
    public void testParsingAustralianTimezoneQuotes()
    {
        String rawQuotes = getHistoricalYahooQuotesAX();

        Security security = new Security();
        security.setTickerSymbol("ALL.AX");

        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed();
        QuoteFeedData data = feed.extractQuotes(rawQuotes);
        List<LatestSecurityPrice> prices = data.getLatestPrices();
        Collections.sort(prices, new SecurityPrice.ByDate());

        assertThat(prices.size(), is(64));

        // Timestamp 1641942000 => 2022-01-12
        assertThat(prices.get(62).getDate(), is(LocalDate.of(2022, 1, 12)));
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
    public void testThatAtLeastTheGivenExchangeIsReturned()
    {
        YahooFinanceQuoteFeed feed = new YahooFinanceQuoteFeed();

        Security s = new Security();
        s.setTickerSymbol("BAS.DE");

        ArrayList<Exception> errors = new ArrayList<>();
        List<Exchange> exchanges = feed.getExchanges(s, errors);

        Optional<Exchange> original = exchanges.stream().filter(e -> e.getId().equals("BAS.DE")).findAny();

        assertThat(original.isPresent(), is(true));
    }

}
