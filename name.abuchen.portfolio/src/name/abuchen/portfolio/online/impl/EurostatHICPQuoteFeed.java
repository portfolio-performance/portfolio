package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

public final class EurostatHICPQuoteFeed implements QuoteFeed
{
    public static final String ID = "EUROSTATHICP"; //$NON-NLS-1$
    public static final String DATASET_VERSION = "prc_hicp_minr@I25/TOTAL"; //$NON-NLS-1$

    private static final String EUROSTAT_HOST = "ec.europa.eu"; //$NON-NLS-1$
    private static final String EUROSTAT_PAGE = "/eurostat/api/dissemination/statistics/1.0/data/prc_hicp_minr"; //$NON-NLS-1$

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
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        return Optional.empty();
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        if (security.getTickerSymbol() == null)
        {
            return QuoteFeedData.withError(
                            new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            String responseBody = requestData(security, collectRawResponse, data);
            extractQuotes(responseBody, data);
        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(new IOException(MessageFormat.format(Messages.MsgErrorDownloadEurostatHICP, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }

        return data;
    }

    @Override
    public HistoricalUpdatePolicy getHistoricalUpdatePolicy(Security security)
    {
        return HistoricalUpdatePolicy.REPLACE_IF_SOURCE_CHANGED;
    }

    @Override
    public Optional<String> getHistoricalDataIdentity(Security security)
    {
        return Optional.of(DATASET_VERSION);
    }

    @SuppressWarnings("nls")
    /* package */ String requestData(Security security, boolean collectRawResponse, QuoteFeedData data)
                    throws IOException, URISyntaxException
    {
        WebAccess webaccess = new WebAccess(EUROSTAT_HOST, EUROSTAT_PAGE) //
                        .addParameter("format", "JSON") //
                        .addParameter("coicop18", "TOTAL") //
                        .addParameter("geo", security.getTickerSymbol().toUpperCase()) //
                        .addParameter("unit", "I25") //
                        .addParameter("lang", "EN"); //$NON-NLS-1$ //$NON-NLS-2$

        String text = webaccess.get();

        if (collectRawResponse)
            data.addResponse(webaccess.getURL(), text);

        return text;
    }

    /* package */ void extractQuotes(String responseBody, QuoteFeedData data)
    {
        try
        {
            JSONObject responseData = (JSONObject) JSONValue.parse(responseBody);
            if (responseData == null)
                throw new IOException("responseBody"); //$NON-NLS-1$

            JSONObject eurostatValue = getRequiredObject(responseData, "value"); //$NON-NLS-1$
            JSONObject eurostatDimension = getRequiredObject(responseData, "dimension"); //$NON-NLS-1$
            JSONObject eurostatTime = getRequiredObject(eurostatDimension, "time"); //$NON-NLS-1$
            JSONObject eurostatCategory = getRequiredObject(eurostatTime, "category"); //$NON-NLS-1$
            JSONObject eurostatIndex = getRequiredObject(eurostatCategory, "index"); //$NON-NLS-1$

            HashMap<String, Double> hicpValues = new HashMap<>();
            for (Object key : eurostatValue.keySet())
                hicpValues.put(key.toString(), parseIndex(eurostatValue.get(key).toString()));

            HashMap<String, String> hicpPeriods = new HashMap<>();
            for (Object key : eurostatIndex.keySet())
                hicpPeriods.put(eurostatIndex.get(key).toString(), key.toString());

            for (int ii = 0; ii < hicpPeriods.size(); ii++)
            {
                String pricePeriod = hicpPeriods.get(Integer.toString(ii));
                LocalDate ts = parsePeriod(pricePeriod);
                Double q = hicpValues.get(Integer.toString(ii));

                if (q != null)
                {
                    LatestSecurityPrice price = new LatestSecurityPrice();
                    price.setDate(ts);
                    price.setValue(Values.Quote.factorize(q));
                    price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

                    if (price.getValue() > 0)
                        data.addPrice(price);
                }
            }
        }
        catch (IOException | IllegalArgumentException | SecurityException e)
        {
            data.addError(e);
        }
    }

    private JSONObject getRequiredObject(JSONObject object, String key) throws IOException
    {
        Object value = object.get(key);
        if (value instanceof JSONObject jsonObject)
            return jsonObject;

        throw new IOException(MessageFormat.format("Unexpected Eurostat response for {0}: missing object ''{1}''", //$NON-NLS-1$
                        DATASET_VERSION, key));
    }

    private LocalDate parsePeriod(String text)
    {
        return YearMonth.parse(text).atDay(1);
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
}
