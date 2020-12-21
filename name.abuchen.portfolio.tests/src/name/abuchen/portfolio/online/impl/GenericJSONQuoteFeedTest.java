package name.abuchen.portfolio.online.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class GenericJSONQuoteFeedTest
{
    String feedUrl;
    Security security;

    @Before
    public void setup()
    {
        feedUrl = "https://google.com/appl";
        security = new Security();
        security.setTickerSymbol("AAPL");
        security.setFeedURL(feedUrl);
        security.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].date");
        security.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].close");
    }

    @Test
    public void testSimpleExtraction() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\"},{\"date\":\"2020-04-13\",\"close\":\"124.123\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertTrue(data.getErrors().isEmpty());
        assertTrue(data.getPrices().size() == 2);

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(1238800, price.getValue());

        SecurityPrice price2 = data.getPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(1241230, price2.getValue());
    }

    @Test
    public void testNoFeedUrl()
    {
        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        security.setFeedURL(null);

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertTrue(data.getErrors().size() == 1);

        Exception e = data.getErrors().get(0);
        assertEquals(e.getClass(), IOException.class);
    }

    @Test
    public void testNoExtractionProperties()
    {
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();
        security.setFeedURL(null);
        security.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC, null);
        security.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC, null);

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertTrue(data.getErrors().size() == 1);

        Exception e = data.getErrors().get(0);
        assertEquals(e.getClass(), IOException.class);
    }

    @Test
    public void testInvalidJson() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\"},{\"date\":\"2020-04-13\",\"close\":\"124.123\"],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertTrue(data.getErrors().size() == 1);

        Exception e = data.getErrors().get(0);
        assertEquals(e.getCause().getClass(), InvalidJsonException.class);
    }

    @Test
    public void testDateExtractionInteger()
    {
        // Date is in seconds from epoch
        String json = "{\"data\":[{\"date\":1586174400,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object);

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionLong()
    {
        // Date is in milliseconds from epoch
        String json = "{\"data\":[{\"date\":1586174400000,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object);

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionLongDays()
    {
        // Date is in days from epoch
        String json = "{\"data\":[{\"date\":18358,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object);

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionDouble()
    {
        // Date is in milliseconds (but as double) from epoch
        String json = "{\"data\":[{\"date\":1586174400.00,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object);

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionInvalid()
    {
        // Date is an array -> invalid
        String json = "{\"data\":[{\"date\":[],\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object);

        assertEquals(null, date);
    }

    @Test
    public void testValueExtractionInteger() throws ParseException
    {
        String json = "{\"data\":[{\"date\":1586174400,\"close\":123.234}],\"info\":\"Json Feed for APPLE ORD\"}";
        long expected = 1232340l;
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractValue(object);

        assertEquals(expected, result);
    }

    @Test
    public void testValueExtractionIntegerInvalid() throws ParseException
    {
        // Value is an array -> invalid
        String json = "{\"data\":[{\"date\":1586174400,\"close\":[]}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractValue(object);

        assertEquals(0, result);
    }

    private Object readJson(String json, Security security, String type)
    {
        JsonPath path = JsonPath.compile(security.getPropertyValue(SecurityProperty.Type.FEED, type)
                        .orElseThrow(IllegalArgumentException::new));
        Configuration configuration = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST)
                        .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

        ReadContext ctx = JsonPath.parse(json, configuration);
        List<Object> dates = ctx.read(path);

        return dates.get(0);
    }
}
