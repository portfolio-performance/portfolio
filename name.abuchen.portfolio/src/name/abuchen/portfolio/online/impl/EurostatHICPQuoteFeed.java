package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.WebAccess;;

public final class EurostatHICPQuoteFeed implements QuoteFeed
{

    public static final String ID = "EUROSTATHICP"; //$NON-NLS-1$

    private static final String EUROSTAT_HOST = "ec.europa.eu"; //$NON-NLS-1$

    private static final String EUROSTAT_PAGE = "/eurostat/wdds/rest/data/v2.1/json/en/prc_hicp_midx"; //$NON-NLS-1$

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return Messages.LabelEurostatHICP;
    }

    @Override
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        return false;
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        List<SecurityPrice> quotes = internalGetQuotes(SecurityPrice.class, security, errors);

        boolean isUpdated = false;
        for (SecurityPrice p : quotes)
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
        return internalGetQuotes(LatestSecurityPrice.class, security, errors);
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        List<Exchange> answer = new ArrayList<>();

        Enumeration<String> enumeration = EurostatHICPLabels.getKeys();
        while (enumeration.hasMoreElements())
        {
            String key = enumeration.nextElement();
            String hicpRegion = key.substring("region.".length()); //$NON-NLS-1$
            answer.add(new Exchange(hicpRegion, EurostatHICPLabels.getString(key)));
        }

        Collections.sort(answer, (r, l) -> r.getName().compareTo(l.getName()));

        return answer;
    }

    private <T extends SecurityPrice> List<T> internalGetQuotes(Class<T> klass, Security security,
                    List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return Collections.emptyList();
        }

        try
        {
            String responseBody = requestData(security);
            return extractQuotes(klass, responseBody, errors);
        }
        catch (IOException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorDownloadEurostatHICP, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }

        return Collections.emptyList();
    }

    @SuppressWarnings("nls")
    private String requestData(Security security) throws IOException
    {
        return new WebAccess(EUROSTAT_HOST, EUROSTAT_PAGE) //
                        .withScheme("http") //
                        .addParameter("filterNonGeo", "1") //
                        .addParameter("precision", "1")
                        .addParameter("geo",
                                        security.getTickerSymbol().toUpperCase())
                        .addParameter("unit", "I15") //
                        .addParameter("unitLabel", "code") //
                        .addParameter("coicop", "CP00") //
                        .addParameter("groupedIndicators", "1") //
                        .addParameter("shortLabel", "1") //
                        .get();
    }

    private <T extends SecurityPrice> List<T> extractQuotes(Class<T> klass, String responseBody, List<Exception> errors)
    {
        List<T> answer = new ArrayList<>();

        try
        {
            JSONObject responseData = (JSONObject) JSONValue.parse(responseBody);
            if (responseData == null)
                throw new IOException("responseBody"); //$NON-NLS-1$

            // the periods are 4th JSON hierarchy level @ dimensions > time >
            // category > index
            JSONObject eurostatValue = (JSONObject) responseData.get("value"); //$NON-NLS-1$
            JSONObject eurostatDimension = (JSONObject) responseData.get("dimension"); //$NON-NLS-1$
            JSONObject eurostatTime = (JSONObject) eurostatDimension.get("time"); //$NON-NLS-1$
            JSONObject eurostatCategory = (JSONObject) eurostatTime.get("category"); //$NON-NLS-1$
            JSONObject eurostatIndex = (JSONObject) eurostatCategory.get("index"); //$NON-NLS-1$

            // EUROSTAT hicp values
            // The key of the hicpValues is linked to hicpPeriods value
            // parameter
            // {230=100.2, 110=85.4, 231=100.1, 111=85.4, 232=100.2, ...
            HashMap<String, Double> hicpValues = new HashMap<>();
            for (Object key : eurostatValue.keySet())
                hicpValues.put(key.toString(), parseIndex(eurostatValue.get(key).toString()));

            // EUROSTAT periods
            HashMap<String, String> hicpPeriods = new HashMap<>();
            for (Object key : eurostatIndex.keySet())
                hicpPeriods.put(eurostatIndex.get(key).toString(), key.toString());

            for (int ii = 0; ii < hicpPeriods.size(); ii++)
            {
                String pricePeriod = hicpPeriods.get(Integer.toString(ii));
                LocalDate ts = LocalDate.of(Integer.parseInt(pricePeriod.substring(0, 4)),
                                Integer.parseInt(pricePeriod.substring(5, 7)), 1);
                Double q = hicpValues.get(Integer.toString(ii));

                if (q != null)
                {
                    T price = klass.getDeclaredConstructor().newInstance();
                    price.setDate(ts);
                    price.setValue(Values.Quote.factorize(q));
                    answer.add(price);
                }
            }
        }
        catch (IOException | InstantiationException | IllegalAccessException | IndexOutOfBoundsException
                        | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                        | SecurityException e)
        {
            errors.add(e);
        }

        return answer;
    }

    private Double parseIndex(String text) throws IOException
    {
        try
        {
            DecimalFormat fmt = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.ENGLISH)); //$NON-NLS-1$
            Number q = fmt.parse(text);
            return q.doubleValue();
        }
        catch (ParseException e)
        {
            throw new IOException(e);
        }
    }

}
