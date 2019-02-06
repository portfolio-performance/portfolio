package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.TradeCalendar;

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
        LocalDate quoteStartDate = LocalDate.MIN;

        if (!security.getPrices().isEmpty())
            quoteStartDate = security.getPrices().get(security.getPrices().size() - 1).getDate();

        List<SecurityPrice> quotes = internalGetQuotes(SecurityPrice.class, security, quoteStartDate, errors);

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
        security.setCalendar(TradeCalendar.EMPTY_CODE);
        return internalGetQuotes(LatestSecurityPrice.class, security, start, errors);
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

        String eurostatHICPRegions = "AT;BE;BG;CH;CY;CZ;DE;DK;EA;EE;EL;ES;EU;FI;FR;HR;HU;IE;IS;IT;LT;LU;LV;MK;MT;NL;NO;PL;PT;RO;RS;SE;SI;SK;TR;UK;US"; //$NON-NLS-1$

        StringTokenizer st = new StringTokenizer(eurostatHICPRegions, ";"); //$NON-NLS-1$
        while (st.hasMoreTokens())
        {
            String hicpRegion = st.nextToken();
            answer.add(new Exchange(hicpRegion, EurostatHICPLabels.getString("region." + hicpRegion))); //$NON-NLS-1$
        }

        Collections.sort(answer, (r, l) -> r.getName().compareTo(l.getName()));

        return answer;
    }

    private <T extends SecurityPrice> List<T> internalGetQuotes(Class<T> klass, Security security, LocalDate startDate,
                    List<Exception> errors)
    {
        if (security.getTickerSymbol() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgMissingTickerSymbol, security.getName())));
            return Collections.emptyList();
        }

        try (CloseableHttpClient client = HttpClients.createSystem())
        {
            String responseBody = requestData(security, startDate);
            return extractQuotes(klass, responseBody, errors);
        }
        catch (IOException e)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorDownloadEurostatHICP, 1,
                            security.getTickerSymbol(), e.getMessage()), e));
        }

        return Collections.emptyList();
    }

    private String requestData(Security security, LocalDate startDate) throws IOException
    {
        try (CloseableHttpClient client = HttpClients.createSystem())
        {
            URIBuilder uriBuilder = new URIBuilder().setScheme("http").setHost(EUROSTAT_HOST) //$NON-NLS-1$
                            .setPath(EUROSTAT_PAGE);
            uriBuilder.addParameter("filterNonGeo", "1"); //$NON-NLS-1$ //$NON-NLS-2$
            uriBuilder.addParameter("precision", "1"); //$NON-NLS-1$ //$NON-NLS-2$
            uriBuilder.addParameter("geo", security.getTickerSymbol().toUpperCase()); //$NON-NLS-1$
            uriBuilder.addParameter("unit", "I15"); //$NON-NLS-1$ //$NON-NLS-2$
            uriBuilder.addParameter("unitLabel", "code"); //$NON-NLS-1$ //$NON-NLS-2$
            uriBuilder.addParameter("coicop", "CP00"); //$NON-NLS-1$ //$NON-NLS-2$
            uriBuilder.addParameter("groupedIndicators", "1"); //$NON-NLS-1$ //$NON-NLS-2$
            uriBuilder.addParameter("shortLabel", "1"); //$NON-NLS-1$ //$NON-NLS-2$
            URL objectURL = uriBuilder.build().toURL();
            try (CloseableHttpResponse response = client.execute(new HttpGet(objectURL.toString())))
            {
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                    throw new IOException(objectURL.toString() + " --> " + response.getStatusLine().getStatusCode()); //$NON-NLS-1$

                return EntityUtils.toString(response.getEntity());
            }
        }
        catch (URISyntaxException e)
        {
            return null;
        }
    }

    private <T extends SecurityPrice> List<T> extractQuotes(Class<T> klass, String responseBody, List<Exception> errors)
    {
        List<T> answer = new ArrayList<>();

        try
        {
            JSONObject responseData = (JSONObject) JSONValue.parse(responseBody);
            if (responseData == null)
                throw new IOException("responseBody"); //$NON-NLS-1$

            // the periods are 4th JSON hierarchy level @ dimensions > time > category > index
            JSONObject eurostatValue = (JSONObject) responseData.get("value"); //$NON-NLS-1$
            JSONObject eurostatDimension = (JSONObject) responseData.get("dimension"); //$NON-NLS-1$
            JSONObject eurostatTime = (JSONObject) eurostatDimension.get("time"); //$NON-NLS-1$
            JSONObject eurostatCategory = (JSONObject) eurostatTime.get("category"); //$NON-NLS-1$
            JSONObject eurostatIndex = (JSONObject) eurostatCategory.get("index"); //$NON-NLS-1$

            // EUROSTAT hicp values
            // The key of the hicpValues is linked to hicpPeriods value
            // parameter
            // {230=100.2, 110=85.4, 231=100.1, 111=85.4, 232=100.2, ...
            HashMap<String, Double> hicpValues = new HashMap<String, Double>();
            for (Object key : eurostatValue.keySet())
                hicpValues.put(key.toString(), parseIndex(eurostatValue.get(key).toString()));

            // EUROSTAT periods
            HashMap<String, String> hicpPeriods = new HashMap<String, String>();
            for (Object key : eurostatIndex.keySet())
                hicpPeriods.put(eurostatIndex.get(key).toString(), key.toString());

            for (int ii = 12; ii < hicpPeriods.size(); ii++)
            {
                String pricePeriod = hicpPeriods.get(Integer.toString(ii));
                LocalDate ts = LocalDate.of(Integer.parseInt(pricePeriod.substring(0, 4)),
                                Integer.parseInt(pricePeriod.substring(5, 7)), 1);;
                Double q = (Double) hicpValues.get(Integer.toString(ii));

                if (ts != null && q != null)
                {
                    T price = klass.newInstance();
                    price.setDate(ts);
                    price.setValue(Values.Quote.factorize(q));
                    answer.add(price);
                }
            }
        }
        catch (IOException | InstantiationException | IllegalAccessException | NumberFormatException
                        | IndexOutOfBoundsException e)
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
