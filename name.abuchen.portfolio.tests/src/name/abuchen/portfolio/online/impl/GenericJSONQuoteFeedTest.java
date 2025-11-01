package name.abuchen.portfolio.online.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeedData;

@SuppressWarnings("nls")
public class GenericJSONQuoteFeedTest
{
    String feedUrl;
    Security security;
    Security securityWithLowHigh;
    Security securityWithVolume;
    Security securityWithSpecialDateFormat1;
    Security securityWithSpecialDateFormat2;
    

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

        securityWithLowHigh = new Security();
        securityWithLowHigh.setTickerSymbol("AAPL");
        securityWithLowHigh.setFeedURL(feedUrl);
        securityWithLowHigh.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].date");
        securityWithLowHigh.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].close");
        securityWithLowHigh.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.LOW_PROPERTY_NAME_HISTORIC,
                        "$.data[*].low");
        securityWithLowHigh.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.HIGH_PROPERTY_NAME_HISTORIC,
                        "$.data[*].high");
        
        securityWithVolume = new Security();
        securityWithVolume.setTickerSymbol("AAPL");
        securityWithVolume.setFeedURL(feedUrl);
        securityWithVolume.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].date");
        securityWithVolume.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].close");
        securityWithVolume.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.VOLUME_PROPERTY_NAME_HISTORIC,
                        "$.data[*].vol");
        
        securityWithSpecialDateFormat1 = new Security();
        securityWithSpecialDateFormat1.setTickerSymbol("AAPL");
        securityWithSpecialDateFormat1.setFeedURL(feedUrl);
        securityWithSpecialDateFormat1.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].date");
        securityWithSpecialDateFormat1.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].close");
        securityWithSpecialDateFormat1.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC,
                        "yyyyMMdd");
        
        securityWithSpecialDateFormat2 = new Security();
        securityWithSpecialDateFormat2.setTickerSymbol("AAPL");
        securityWithSpecialDateFormat2.setFeedURL(feedUrl);
        securityWithSpecialDateFormat2.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].date");
        securityWithSpecialDateFormat2.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC,
                        "$.data[*].close");
        securityWithSpecialDateFormat2.setPropertyValue(SecurityProperty.Type.FEED, GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC,
                        "dd.MM.yyyy");
    }

    @Test
    public void testSimpleExtraction() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":123.88},{\"date\":2020-04-13,\"close\":124.123}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());

        SecurityPrice price2 = data.getPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
    }

    @Test
    public void testSimpleExtractionWithStrings() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\"},{\"date\":\"2020-04-13\",\"close\":\"124.123\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(security, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());

        SecurityPrice price2 = data.getPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
    }

    @Test
    public void testSimpleExtractionDateFormat1() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":20200412,\"close\":\"123.88\"},{\"date\":\"20200413\",\"close\":\"124.123\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithSpecialDateFormat1, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());

        SecurityPrice price2 = data.getPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
    }

    @Test
    public void testSimpleExtractionDateFormat1AsString() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"20200412\",\"close\":\"123.88\"},{\"date\":\"20200413\",\"close\":\"124.123\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithSpecialDateFormat1, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());

        SecurityPrice price2 = data.getPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
    }

    @Test
    public void testSimpleExtractionDateFormat2() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"12.04.2020\",\"close\":\"123.88\"},{\"date\":\"13.04.2020\",\"close\":\"124.123\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithSpecialDateFormat2, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());

        SecurityPrice price2 = data.getPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
    }

    @Test
    public void testExtractionWithLowHigh() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\",\"low\":\"122.34\","
                        + "\"high\":\"124.56\",\"vol\":\"154000\"},{\"date\":\"2020-04-13\",\"close\":\"124.123\","
                        + "\"low\":\"123.456\",\"high\":\"125.678\",\"vol\":\"888000\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithLowHigh, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        LatestSecurityPrice price = data.getLatestPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
        assertEquals(Values.Quote.factorize(122.34), price.getLow());
        assertEquals(Values.Quote.factorize(124.56), price.getHigh());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getVolume());

        LatestSecurityPrice price2 = data.getLatestPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
        assertEquals(Values.Quote.factorize(123.456), price2.getLow());
        assertEquals(Values.Quote.factorize(125.678), price2.getHigh());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getVolume());
    }

    @Test
    public void testExtractionWithVolume() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\",\"low\":\"122.34\","
                        + "\"high\":\"124.56\",\"vol\":154000},{\"date\":\"2020-04-13\",\"close\":\"124.123\","
                        + "\"low\":\"123.456\",\"high\":\"125.678\",\"vol\":888000}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithVolume, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        LatestSecurityPrice price = data.getLatestPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getHigh());
        assertEquals(154000L, price.getVolume());

        LatestSecurityPrice price2 = data.getLatestPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getHigh());
        assertEquals(888000L, price2.getVolume());
    }

    @Test
    public void testExtractionWithVolumeAsString() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\",\"low\":\"122.34\","
                        + "\"high\":\"124.56\",\"vol\":\"154000\"},{\"date\":\"2020-04-13\",\"close\":\"124.123\","
                        + "\"low\":\"123.456\",\"high\":\"125.678\",\"vol\":\"888000\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithVolume, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        LatestSecurityPrice price = data.getLatestPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getHigh());
        assertEquals(154000L, price.getVolume());

        LatestSecurityPrice price2 = data.getLatestPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getHigh());
        assertEquals(888000L, price2.getVolume());
    }

    @Test
    public void testExtractionWithDoubleVolumeAsString() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\",\"low\":\"122.34\","
                        + "\"high\":\"124.56\",\"vol\":\"154000.0\"},{\"date\":\"2020-04-13\",\"close\":\"124.123\","
                        + "\"low\":\"123.456\",\"high\":\"125.678\",\"vol\":\"888000.0\"}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithVolume, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        LatestSecurityPrice price = data.getLatestPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getHigh());
        assertEquals(154000L, price.getVolume());

        LatestSecurityPrice price2 = data.getLatestPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getHigh());
        assertEquals(888000L, price2.getVolume());
    }

    @Test
    public void testExtractionWithDoubleVolume() throws IOException, URISyntaxException
    {
        String jsonResponse = "{\"data\":[{\"date\":\"2020-04-12\",\"close\":\"123.88\",\"low\":\"122.34\","
                        + "\"high\":\"124.56\",\"vol\":154000.0},{\"date\":\"2020-04-13\",\"close\":\"124.123\","
                        + "\"low\":\"123.456\",\"high\":\"125.678\",\"vol\":888000.0}],\"info\":\"Json Feed for APPLE ORD\"}";

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithVolume, false);

        assertTrue(data.getErrors().isEmpty()); // NOSONAR
        assertTrue(data.getPrices().size() == 2);

        LatestSecurityPrice price = data.getLatestPrices().get(0);
        assertEquals(LocalDate.of(2020, 4, 12), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price.getHigh());
        assertEquals(154000L, price.getVolume());

        LatestSecurityPrice price2 = data.getLatestPrices().get(1);
        assertEquals(LocalDate.of(2020, 4, 13), price2.getDate());
        assertEquals(Values.Quote.factorize(124.123), price2.getValue());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getLow());
        assertEquals(LatestSecurityPrice.NOT_AVAILABLE, price2.getHigh());
        assertEquals(888000L, price2.getVolume());
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
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionLong()
    {
        // Date is in milliseconds from epoch
        String json = "{\"data\":[{\"date\":1586174400000,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionLongDays()
    {
        // Date is in days from epoch
        String json = "{\"data\":[{\"date\":18358,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionDouble()
    {
        // Date is in milliseconds (but as double) from epoch
        String json = "{\"data\":[{\"date\":1586174400.00,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2020, 4, 06), date);
    }

    @Test
    public void testDateExtractionMSCIFormat()
    {
        // Date is in milliseconds (but as double) from epoch
        String json = "{\"data\":[{\"date\":20210313,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.of(DateTimeFormatter.ofPattern("yyyyMMdd")), Optional.empty());

        assertEquals(LocalDate.of(2021, 3, 13), date);
    }

    @Test
    public void testDateExtractionGermanFormat()
    {
        // Date is in milliseconds (but as double) from epoch
        String json = "{\"data\":[{\"date\":14.03.2021,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.of(DateTimeFormatter.ofPattern("dd.MM.yyyy")), Optional.empty());

        assertEquals(LocalDate.of(2021, 3, 14), date);
    }

    @Test
    public void testDateExtractionISODateFormat()
    {
        String json = "{\"data\":[{\"date\":2011-12-03,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionISODateOffsetFormat()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03+01:00\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionISODateTimeFormat()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03T10:15:30\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionISODateTimeOffsetFormat()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03T10:15:30+01:00\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionISODateTimeZoneFormat()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03T10:15:30+01:00[Europe/Berlin]\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionCustomDateTimeFormat()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03 10:15:30\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionCustomDateTimeOffsetFormat()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03 10:15:30 -13:00\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss VV")), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionISODateFormatWithTimezoneOffset()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("+01:00"));

        assertEquals(LocalDate.of(2011, 12, 4), date);
    }

    @Test
    public void testDateExtractionISODateOffsetFormatWithTimezoneOffset()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03-01:00\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("+01:00"));

        assertEquals(LocalDate.of(2011, 12, 4), date);
    }

    @Test
    public void testDateExtractionISODateTimeFormatWithTimezoneOffset()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03T10:15:30\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("-10:16"));

        assertEquals(LocalDate.of(2011, 12, 2), date);
    }

    @Test
    public void testDateExtractionCustomDateTimeFormatWithTimezoneOffset()
    {
        String json = "{\"data\":[{\"date\":\"2011-12-03 10:15:30\",\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), Optional.of("-10:16"));

        assertEquals(LocalDate.of(2011, 12, 2), date);
    }

    @Test
    public void testDateExtractionIsoDateTimeFormatWithTimezone()
    {
        // Date is in seconds from epoch
        String json = "{\"data\":[{\"date\":2011-12-03T22:45:00,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);

        // During standard time
        try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class, CALLS_REAL_METHODS))
        {
            Instant fixedInstant = Instant.parse("2024-01-01T12:00:00Z");
            mockedInstant.when(Instant::now).thenReturn(fixedInstant);

            LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("Europe/Berlin"));

            assertEquals(LocalDate.of(2011, 12, 3), date);
        }

        // During daylight saving time
        try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class, CALLS_REAL_METHODS))
        {
            Instant fixedInstant = Instant.parse("2024-06-01T12:00:00Z");
            mockedInstant.when(Instant::now).thenReturn(fixedInstant);

            LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("Europe/Berlin"));

            assertEquals(LocalDate.of(2011, 12, 4), date);
        }
    }

    @Test
    public void testDateExtractionSeconds()
    {
        // Date is in seconds from epoch
        String json = "{\"data\":[{\"date\":1322907330,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertEquals(LocalDate.of(2011, 12, 3), date);
    }

    @Test
    public void testDateExtractionSecondsWithTimezoneOffset()
    {
        // Date is in seconds from epoch
        String json = "{\"data\":[{\"date\":1322907330,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("-10:16"));

        assertEquals(LocalDate.of(2011, 12, 2), date);
    }

    @Test
    public void testDateExtractionSecondsWithTimezone()
    {
        // Date is in seconds from epoch
        String json = "{\"data\":[{\"date\":1322952300,\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);

        // During standard time
        try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class, CALLS_REAL_METHODS))
        {
            Instant fixedInstant = Instant.parse("2024-01-01T12:00:00Z");
            mockedInstant.when(Instant::now).thenReturn(fixedInstant);

            LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("Europe/Berlin"));

            assertEquals(LocalDate.of(2011, 12, 3), date);
        }

        // During daylight saving time
        try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class, CALLS_REAL_METHODS))
        {
            Instant fixedInstant = Instant.parse("2024-06-01T12:00:00Z");
            mockedInstant.when(Instant::now).thenReturn(fixedInstant);

            LocalDate date = feed.extractDate(object, Optional.empty(), Optional.of("Europe/Berlin"));

            assertEquals(LocalDate.of(2011, 12, 4), date);
        }
    }

    @Test
    public void testDateExtractionInvalid()
    {
        // Date is an array -> invalid
        String json = "{\"data\":[{\"date\":[],\"close\":\"123.00\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC);
        LocalDate date = feed.extractDate(object, Optional.empty(), Optional.empty());

        assertNull(date);
    }

    @Test
    public void testValueExtractionInteger() throws ParseException
    {
        String json = "{\"data\":[{\"date\":1586174400,\"close\":123.234}],\"info\":\"Json Feed for APPLE ORD\"}";
        long expected = Values.Quote.factorize(123.234);
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractValue(object, BigDecimal.ONE);

        assertEquals(expected, result);
    }

    @Test
    public void testValueExtractionIntegerInvalid() throws ParseException
    {
        // Value is an array -> invalid
        String json = "{\"data\":[{\"date\":1586174400,\"close\":[]}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractValue(object, BigDecimal.ONE);

        assertEquals(0, result);
    }

    @Test
    public void testIntValueExtractionDouble() throws ParseException
    {
        String json = "{\"data\":[{\"date\":1586174400,\"close\":123.234}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractIntegerValue(object);

        assertEquals(123, result);
    }

    @Test
    public void testIntValueExtractionInteger() throws ParseException
    {
        String json = "{\"data\":[{\"date\":1586174400,\"close\":123}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractIntegerValue(object);

        assertEquals(123, result);
    }


    @Test
    public void testIntValueExtractionDoubleAsString() throws ParseException
    {
        String json = "{\"data\":[{\"date\":1586174400,\"close\":\"123.234\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractIntegerValue(object);

        assertEquals(123, result);
    }

    @Test
    public void testIntValueExtractionIntegerAsString() throws ParseException
    {
        String json = "{\"data\":[{\"date\":1586174400,\"close\":\"123\"}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractIntegerValue(object);

        assertEquals(123, result);
    }

    @Test
    public void testIntValueExtractionIntegerInvalid() throws ParseException
    {
        // Value is an array -> invalid
        String json = "{\"data\":[{\"date\":1586174400,\"close\":[]}],\"info\":\"Json Feed for APPLE ORD\"}";
        GenericJSONQuoteFeed feed = new GenericJSONQuoteFeed();

        Object object = this.readJson(json, security, GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC);
        long result = feed.extractIntegerValue(object);

        assertEquals(0, result);
    }

    @Test
    public void testDateExtractionWithLocaleEnglish() throws IOException, URISyntaxException
    {
        // Pattern with English month/day names - should work with en_US locale
        String jsonResponse = "{\"data\":[{\"date\":\"Wed Jun 03 2020 00:00:00 GMT+0000 (Coordinated Universal Time)\",\"close\":\"123.88\"}]}";

        Security securityWithLocale = new Security();
        securityWithLocale.setTickerSymbol("TEST");
        securityWithLocale.setFeedURL(feedUrl);
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC, "$.data[*].date");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC, "$.data[*].close");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC,
                        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_LOCALE_PROPERTY_NAME_HISTORIC, "en_US");

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithLocale, false);

        assertTrue(data.getErrors().isEmpty());
        assertThat(data.getPrices().size(), is(1));

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 6, 3), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
    }

    @Test(expected = DateTimeParseException.class)
    public void testDateExtractionWithLocaleGerman() throws IOException, URISyntaxException
    {
        // Pattern with English month/day names - should FAIL with de_DE locale
        String jsonResponse = "{\"data\":[{\"date\":\"Wed Jun 03 2020 00:00:00 GMT+0000 (Coordinated Universal Time)\",\"close\":\"123.88\"}]}";

        Security securityWithLocale = new Security();
        securityWithLocale.setTickerSymbol("TEST");
        securityWithLocale.setFeedURL(feedUrl);
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC, "$.data[*].date");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC, "$.data[*].close");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC,
                        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_LOCALE_PROPERTY_NAME_HISTORIC, "de_DE");

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        feed.getHistoricalQuotes(securityWithLocale, false);
    }

    @Test
    public void testDateExtractionWithLocaleBCP47Format() throws IOException, URISyntaxException
    {
        // Test BCP 47 format locale (en-US instead of en_US)
        String jsonResponse = "{\"data\":[{\"date\":\"Wed Jun 03 2020 00:00:00 GMT+0000 (Coordinated Universal Time)\",\"close\":\"123.88\"}]}";

        Security securityWithLocale = new Security();
        securityWithLocale.setTickerSymbol("TEST");
        securityWithLocale.setFeedURL(feedUrl);
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC, "$.data[*].date");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC, "$.data[*].close");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC,
                        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_LOCALE_PROPERTY_NAME_HISTORIC, "en-US");

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithLocale, false);

        assertTrue(data.getErrors().isEmpty());
        assertThat(data.getPrices().size(), is(1));

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 6, 3), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
    }

    @Test
    public void testDateExtractionWithGermanLocaleAndGermanDate() throws IOException, URISyntaxException
    {
        // Test German date format with German locale - should work
        String jsonResponse = "{\"data\":[{\"date\":\"Mi. Juni 03 2020 00:00:00 GMT+0000\",\"close\":\"123.88\"}]}";

        Security securityWithLocale = new Security();
        securityWithLocale.setTickerSymbol("TEST");
        securityWithLocale.setFeedURL(feedUrl);
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_PROPERTY_NAME_HISTORIC, "$.data[*].date");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.CLOSE_PROPERTY_NAME_HISTORIC, "$.data[*].close");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_FORMAT_PROPERTY_NAME_HISTORIC, "EEE MMMM dd yyyy HH:mm:ss 'GMT'Z");
        securityWithLocale.setPropertyValue(SecurityProperty.Type.FEED,
                        GenericJSONQuoteFeed.DATE_LOCALE_PROPERTY_NAME_HISTORIC, "de_DE");

        GenericJSONQuoteFeed feed = Mockito.spy(new GenericJSONQuoteFeed());
        Mockito.doReturn(jsonResponse).when(feed).getJson(feedUrl);

        QuoteFeedData data = feed.getHistoricalQuotes(securityWithLocale, false);

        assertTrue(data.getErrors().isEmpty());
        assertThat(data.getPrices().size(), is(1));

        SecurityPrice price = data.getPrices().get(0);
        assertEquals(LocalDate.of(2020, 6, 3), price.getDate());
        assertEquals(Values.Quote.factorize(123.88), price.getValue());
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
