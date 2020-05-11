package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.variableurl.Factory;
import name.abuchen.portfolio.online.impl.variableurl.urls.VariableURL;
import name.abuchen.portfolio.util.WebAccess;

public final class GenericJSONQuoteFeed implements QuoteFeed
{
    public static final String ID = "GENERIC-JSON"; //$NON-NLS-1$
    public static final String DATE_PROPERTY_NAME = "GENERIC-JSON-DATE"; //$NON-NLS-1$
    public static final String CLOSE_PROPERTY_NAME = "GENERIC-JSON-CLOSE"; //$NON-NLS-1$

    private final PageCache<String> cache = new PageCache<>();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "JSON"; //$NON-NLS-1$
    }

    @Override
    public Optional<String> getHelpURL()
    {
        return Optional.of("https://help.portfolio-performance.info/kursdaten_laden/#json"); //$NON-NLS-1$
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        return getHistoricalQuotes(security, security.getFeedURL(), collectRawResponse, false);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, security.getFeedURL(), true, true);
    }

    private QuoteFeedData getHistoricalQuotes(Security security, String feedURL, boolean collectRawResponse,
                    boolean isPreview)
    {
        Optional<String> dateProperty = security.getPropertyValue(SecurityProperty.Type.FEED, DATE_PROPERTY_NAME);
        Optional<String> closeProperty = security.getPropertyValue(SecurityProperty.Type.FEED, CLOSE_PROPERTY_NAME);

        if (!dateProperty.isPresent() || !closeProperty.isPresent())
        {
            return QuoteFeedData.withError(new IOException(
                            MessageFormat.format(Messages.MsgErrorMissingPathToDateOrClose, security.getName())));
        }

        if (feedURL == null || feedURL.length() == 0)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingFeedURL, security.getName())));
        }

        VariableURL variableURL = Factory.fromString(feedURL);
        variableURL.setSecurity(security);

        QuoteFeedData data = new QuoteFeedData();

        SortedSet<LatestSecurityPrice> newPricesByDate = new TreeSet<>(new SecurityPrice.ByDate());
        long failedAttempts = 0;
        long maxFailedAttempts = variableURL.getMaxFailedAttempts();

        for (String url : variableURL) // NOSONAR
        {
            String json = cache.lookup(url);

            if (json == null)
            {
                try
                {
                    json = new WebAccess(url).get();
                }
                catch (IOException | URISyntaxException e)
                {
                    data.addError(new IOException(url + '\n' + e.getMessage(), e));
                }

                if (json != null)
                    cache.put(url, json);
            }

            if (collectRawResponse)
                data.addResponse(url, json);

            int sizeBefore = newPricesByDate.size();

            if (json != null)
                newPricesByDate.addAll(parse(url, json, dateProperty.get(), closeProperty.get(), data));

            if (newPricesByDate.size() > sizeBefore)
                failedAttempts = 0;
            else if (++failedAttempts > maxFailedAttempts)
                break;

            if (isPreview && newPricesByDate.size() >= 100)
                break;
        }

        data.addAllPrices(newPricesByDate);
        return data;
    }

    protected List<LatestSecurityPrice> parse(String url, String json, String datePath, String closePath,
                    QuoteFeedData data)
    {
        try
        {
            JsonPath dateP = JsonPath.compile(datePath);
            JsonPath closeP = JsonPath.compile(closePath);

            Configuration configuration = Configuration.defaultConfiguration().addOptions(Option.ALWAYS_RETURN_LIST)
                            .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

            ReadContext ctx = JsonPath.parse(json, configuration);

            List<Object> dates = ctx.read(dateP);
            List<Object> close = ctx.read(closeP);

            if (dates.size() != close.size())
            {
                data.addError(new IOException(MessageFormat.format(
                                Messages.MsgErrorNumberOfDateAndCloseRecordsDoNotMatch, dates.size(), close.size())));
                return Collections.emptyList();
            }

            List<LatestSecurityPrice> prices = new ArrayList<>();

            int size = dates.size();

            for (int index = 0; index < size; index++)
            {
                LatestSecurityPrice price = new LatestSecurityPrice();

                // date
                Object object = dates.get(index);

                if (object instanceof String)
                    price.setDate(YahooHelper.fromISODate((String) object));
                else if (object instanceof Long)
                    price.setDate(parseDateTimestamp((Long) object));
                else if (object instanceof Integer)
                    price.setDate(parseDateTimestamp(Long.valueOf((Integer) object)));
                else if (object instanceof Double)
                    price.setDate(parseDateTimestamp(((Double) object).longValue()));
                else if (object instanceof LocalDate)
                    price.setDate((LocalDate) object);

                // close
                object = close.get(index);

                if (object instanceof Number)
                    price.setValue(Values.Quote.factorize(((Number) object).doubleValue()));
                else if (object instanceof String)
                    price.setValue(YahooHelper.asPrice((String) object));

                if (price.getDate() != null && price.getValue() > 0)
                {
                    price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);
                    prices.add(price);
                }
            }

            return prices;
        }
        catch (JsonPathException | ParseException e)
        {
            data.addError(new IOException(url + '\n' + e.getMessage(), e));
            return Collections.emptyList();
        }
    }

    private LocalDate parseDateTimestamp(Long object)
    {
        Long futureEpoch = LocalDateTime.of(2200, 1, 1, 0, 0, 0, 0).toEpochSecond(ZoneOffset.UTC);

        if (object > futureEpoch)
        {
            // if the timestamp represents a date further than year 2200, then
            // it is probably in milliseconds
            // Note: This means that millisecond timestamps before 1970-03-26
            // 00:08:38 can't be parsed by this method
            object = object / 1000;

        }
        else if (object < futureEpoch / (24 * 60 * 60))
        {
            // if the timestamp is smaller than the number of days between 1970
            // and 2200, then it is probably in days
            // Note: This means that second timestamps before 1970-01-01
            // 23:20:06 can't be parsed by this method
            object = object * 24 * 60 * 60;
        }

        return LocalDateTime.ofEpochSecond(object, 0, ZoneOffset.UTC).toLocalDate();
    }
}
