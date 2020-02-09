package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.util.WebAccess;

public final class QuandlQuoteFeed implements QuoteFeed
{
    public static final String ID = "QUANDL-DATASETS"; //$NON-NLS-1$
    public static final String QUANDL_CODE_PROPERTY_NAME = "QUANDLCODE"; //$NON-NLS-1$

    private String apiKey;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Quandl"; //$NON-NLS-1$
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        List<LatestSecurityPrice> prices = getHistoricalQuotes(LatestSecurityPrice.class, security,
                        a -> a.addParameter("limit", "1"), errors); //$NON-NLS-1$ //$NON-NLS-2$

        if (prices.isEmpty())
        {
            return false;
        }
        else
        {
            LatestSecurityPrice price = prices.get(prices.size() - 1);
            if (price.getValue() != 0)
                security.setLatest(price);

            return true;
        }
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        Consumer<WebAccess> parameters = null;

        if (!security.getPrices().isEmpty())
        {
            LocalDate startDate = security.getPrices().get(security.getPrices().size() - 1).getDate();
            parameters = a -> a.addParameter("start_date", startDate.toString()); //$NON-NLS-1$
        }

        List<SecurityPrice> prices = getHistoricalQuotes(SecurityPrice.class, security, parameters, errors);

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
        return getHistoricalQuotes(LatestSecurityPrice.class, security, null, errors);
    }

    @SuppressWarnings("unchecked")
    public <T extends SecurityPrice> List<T> getHistoricalQuotes(Class<T> klass, Security security,
                    Consumer<WebAccess> parameters, List<Exception> errors)
    {
        if (apiKey == null)
            throw new IllegalArgumentException(Messages.MsgErrorQuandlMissingAPIKey);

        Optional<String> quandlCode = security.getPropertyValue(SecurityProperty.Type.FEED, QUANDL_CODE_PROPERTY_NAME);

        if (!quandlCode.isPresent())
        {
            errors.add(new IOException(
                            MessageFormat.format(Messages.MsgErrorQuandlMissingCode, security.getName())));
            return Collections.emptyList();
        }

        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("www.quandl.com", "/api/v3/datasets/" + quandlCode.get() + "/data.json")
                            .addParameter("api_key", apiKey);

            if (parameters != null)
                parameters.accept(webaccess);

            String response = webaccess.get();

            JSONObject json = (JSONObject) JSONValue.parse(response);

            JSONObject dataset = (JSONObject) json.get("dataset_data"); //$NON-NLS-1$
            if (dataset == null)
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "dataset"))); //$NON-NLS-1$
                return Collections.emptyList();
            }

            JSONArray columnNames = (JSONArray) dataset.get("column_names"); //$NON-NLS-1$
            if (columnNames == null)
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "column_names"))); //$NON-NLS-1$
                return Collections.emptyList();
            }

            int[] mapping = extractColumnMapping(columnNames);

            if (mapping[0] == -1)
            {
                errors.add(new IOException(
                                MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "column_names[data]"))); //$NON-NLS-1$
                return Collections.emptyList();
            }

            JSONArray data = (JSONArray) dataset.get("data"); //$NON-NLS-1$
            if (data == null)
                return Collections.emptyList();

            List<T> prices = new ArrayList<>();

            data.forEach(entry -> {
                try
                {
                    JSONArray row = (JSONArray) entry;
                    LocalDate date = LocalDate.parse(row.get(mapping[0]).toString());

                    long open = mapping[1] == -1 ? -1 : YahooHelper.asPrice(String.valueOf(row.get(mapping[1])));
                    long high = mapping[2] == -1 ? -1 : YahooHelper.asPrice(String.valueOf(row.get(mapping[2])));
                    long low = mapping[3] == -1 ? -1 : YahooHelper.asPrice(String.valueOf(row.get(mapping[3])));
                    long close = mapping[4] == -1 ? -1 : YahooHelper.asPrice(String.valueOf(row.get(mapping[4])));
                    int volume = mapping[5] == -1 ? -1 : YahooHelper.asNumber(String.valueOf(row.get(mapping[5])));

                    T price = klass.getConstructor().newInstance();
                    price.setDate(date);
                    price.setValue(close);

                    if (price instanceof LatestSecurityPrice)
                    {
                        LatestSecurityPrice lsp = (LatestSecurityPrice) price;
                        lsp.setHigh(high);
                        lsp.setLow(low);
                        lsp.setVolume(volume);
                        lsp.setPreviousClose(open);
                    }

                    prices.add(price);
                }
                catch (ReflectiveOperationException | ParseException | IllegalArgumentException | SecurityException ex)
                {
                    errors.add(ex);
                }
            });
            return prices;

        }
        catch (IOException e)
        {
            errors.add(e);
            return Collections.emptyList();
        }
    }

    /**
     * Returns an int array with six elements (for date, open, high, low, close,
     * and volume) specifying the index into a data set row where to find the
     * data. -1 indicates that the data is not available.
     */
    @SuppressWarnings("nls")
    private int[] extractColumnMapping(JSONArray columnNames)
    {
        int[] mapping = new int[6];
        Arrays.fill(mapping, -1);

        int size = columnNames.size();

        for (int index = 0; index < size; index++)
        {
            String name = columnNames.get(index).toString();

            switch (name)
            {
                case "Date":
                    mapping[0] = index;
                    break;

                case "Open":
                    mapping[1] = index;
                    break;

                case "High":
                    mapping[2] = index;
                    break;

                case "Low":
                    mapping[3] = index;
                    break;

                case "Net Asset Value":
                    mapping[4] = index;
                    break;
                case "Close":
                    mapping[4] = index;
                    break;

                case "Traded Volume":
                    mapping[5] = index;
                    break;

                default:
            }
        }

        return mapping;
    }

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exchange> getExchanges(Security subject, List<Exception> errors)
    {
        return Collections.emptyList();
    }

}
