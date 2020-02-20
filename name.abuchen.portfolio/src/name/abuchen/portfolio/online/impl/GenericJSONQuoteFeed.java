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
import java.util.stream.Collectors;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
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
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        return false;
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        List<SecurityPrice> prices = getHistoricalQuotes(security, security.getFeedURL(), errors).stream()
                        .map(p -> new SecurityPrice(p.getDate(), p.getValue())).collect(Collectors.toList());

        boolean isUpdated = false;
        for (SecurityPrice p : prices)
        {
            if (p.getDate().isBefore(LocalDate.now()))
            {
                boolean isAdded = security.addPrice(p);
                isUpdated = isUpdated || isAdded;
            }
        }
        return isUpdated;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(Security security, LocalDate start, List<Exception> errors)
    {
        return getHistoricalQuotes(security, security.getFeedURL(), errors).stream()
                        .map(p -> new LatestSecurityPrice(p.getDate(), p.getValue(), LatestSecurityPrice.NOT_AVAILABLE,
                                        LatestSecurityPrice.NOT_AVAILABLE, LatestSecurityPrice.NOT_AVAILABLE))
                        .collect(Collectors.toList());
    }

    public List<SecurityPrice> getHistoricalQuotes(Security security, String feedURL, List<Exception> errors)
    {
        Optional<String> dateProperty = security.getPropertyValue(SecurityProperty.Type.FEED, DATE_PROPERTY_NAME);
        Optional<String> closeProperty = security.getPropertyValue(SecurityProperty.Type.FEED, CLOSE_PROPERTY_NAME);

        if (!dateProperty.isPresent() || !closeProperty.isPresent())
        {
            errors.add(new IOException(
                            MessageFormat.format(Messages.MsgErrorMissingPathToDateOrClose, security.getName())));
            return Collections.emptyList();
        }

        if (feedURL == null || feedURL.length() == 0)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingFeedURL, security.getName())));
            return Collections.emptyList();
        }

        VariableURL variableURL = Factory.fromString(feedURL);
        variableURL.setSecurity(security);

        SortedSet<SecurityPrice> newPricesByDate = new TreeSet<>(new SecurityPrice.ByDate());
        long failedAttempts = 0;
        long maxFailedAttempts = variableURL.getMaxFailedAttempts();

        for (String url : variableURL)
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
                    errors.add(new IOException(url + '\n' + e.getMessage(), e));
                }

                if (json != null)
                    cache.put(url, json);
            }

            int sizeBefore = newPricesByDate.size();

            if (json != null)
                newPricesByDate.addAll(parse(url, json, dateProperty.get(), closeProperty.get(), errors));

            if (newPricesByDate.size() > sizeBefore)
                failedAttempts = 0;
            else if (++failedAttempts > maxFailedAttempts)
                break;
        }

        return new ArrayList<>(newPricesByDate);
    }

    protected List<SecurityPrice> parse(String url, String json, String datePath, String closePath,
                    List<Exception> errors)
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
                errors.add(new IOException(MessageFormat.format(Messages.MsgErrorNumberOfDateAndCloseRecordsDoNotMatch,
                                dates.size(), close.size())));
                return Collections.emptyList();
            }

            List<SecurityPrice> prices = new ArrayList<>();

            int size = dates.size();

            for (int index = 0; index < size; index++)
            {
                SecurityPrice price = new SecurityPrice();

                // date
                Object object = dates.get(index);

                if (object instanceof String)
                    price.setDate(YahooHelper.fromISODate((String) object));
                else if (object instanceof Long)
                    price.setDate(LocalDateTime.ofEpochSecond((Long) object, 0, ZoneOffset.UTC).toLocalDate());
                else if (object instanceof LocalDate)
                    price.setDate((LocalDate) object);

                // close
                object = close.get(index);

                if (object instanceof Number)
                    price.setValue(Values.Quote.factorize(((Number) object).doubleValue()));
                else if (object instanceof String)
                    price.setValue(YahooHelper.asPrice((String) object));

                if (price.getDate() != null && price.getValue() > 0)
                    prices.add(price);
            }

            return prices;
        }
        catch (JsonPathException | ParseException e)
        {
            errors.add(new IOException(url + '\n' + e.getMessage(), e));
            return Collections.emptyList();
        }
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exchange> getExchanges(Security security, List<Exception> errors)
    {
        return Collections.emptyList();
    }
}
