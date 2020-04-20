package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.util.WebAccess;

public final class QuandlQuoteFeed implements QuoteFeed
{
    public static final String ID = "QUANDL-DATASETS"; //$NON-NLS-1$
    public static final String QUANDL_CODE_PROPERTY_NAME = "QUANDLCODE"; //$NON-NLS-1$

    /**
     * Property that holds the column name that contains the "close" value
     */
    public static final String QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME = "QUANDLCLOSE"; //$NON-NLS-1$

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
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        QuoteFeedData data = getHistoricalQuotes(security, false, a -> a.addParameter("limit", "1")); //$NON-NLS-1$ //$NON-NLS-2$
        List<LatestSecurityPrice> prices = data.getLatestPrices();
        return prices.isEmpty() ? Optional.empty() : Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        Consumer<WebAccess> parameters = null;

        if (!security.getPrices().isEmpty())
        {
            LocalDate startDate = security.getPrices().get(security.getPrices().size() - 1).getDate();
            parameters = a -> a.addParameter("start_date", startDate.toString()); //$NON-NLS-1$
        }

        return getHistoricalQuotes(security, collectRawResponse, parameters);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, a -> a.addParameter("limit", "100")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @SuppressWarnings("unchecked")
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse,
                    Consumer<WebAccess> parameters)
    {
        if (apiKey == null)
            throw new IllegalArgumentException(Messages.MsgErrorQuandlMissingAPIKey);

        Optional<String> quandlCode = security.getPropertyValue(SecurityProperty.Type.FEED, QUANDL_CODE_PROPERTY_NAME);

        if (!quandlCode.isPresent())
        {
            return QuoteFeedData.withError(new IOException(
                            MessageFormat.format(Messages.MsgErrorQuandlMissingCode, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("www.quandl.com", "/api/v3/datasets/" + quandlCode.get() + "/data.json")
                            .addParameter("api_key", apiKey);

            if (parameters != null)
                parameters.accept(webaccess);

            String response = webaccess.get();

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), response);

            JSONObject json = (JSONObject) JSONValue.parse(response);

            JSONObject dataset = (JSONObject) json.get("dataset_data"); //$NON-NLS-1$
            if (dataset == null)
            {
                data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "dataset"))); //$NON-NLS-1$
                return data;
            }

            JSONArray columnNames = (JSONArray) dataset.get("column_names"); //$NON-NLS-1$
            if (columnNames == null)
            {
                data.addError(new IOException(
                                MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "column_names"))); //$NON-NLS-1$
                return data;
            }

            int[] mapping = extractColumnMapping(security, columnNames);

            if (mapping[0] == -1)
            {
                data.addError(new IOException(
                                MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "column_names[data]"))); //$NON-NLS-1$
                return data;
            }

            if (mapping[4] == -1)
            {
                data.addError(new IOException(
                                MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "column_names[close]"))); //$NON-NLS-1$
                return data;
            }

            JSONArray jsondata = (JSONArray) dataset.get("data"); //$NON-NLS-1$
            if (jsondata == null)
                return data;

            jsondata.forEach(entry -> {
                try
                {
                    JSONArray row = (JSONArray) entry;
                    LocalDate date = LocalDate.parse(row.get(mapping[0]).toString());

                    long high = mapping[2] == -1 ? -1 : YahooHelper.asPrice(String.valueOf(row.get(mapping[2])));
                    long low = mapping[3] == -1 ? -1 : YahooHelper.asPrice(String.valueOf(row.get(mapping[3])));
                    long close = mapping[4] == -1 ? -1 : YahooHelper.asPrice(String.valueOf(row.get(mapping[4])));
                    int volume = mapping[5] == -1 ? -1 : YahooHelper.asNumber(String.valueOf(row.get(mapping[5])));

                    if (close > 0)
                    {
                        LatestSecurityPrice price = new LatestSecurityPrice();
                        price.setDate(date);
                        price.setValue(close);
                        price.setHigh(high);
                        price.setLow(low);
                        price.setVolume(volume);

                        data.addPrice(price);
                    }
                }
                catch (ParseException | IllegalArgumentException ex)
                {
                    data.addError(ex);
                }
            });

        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    /**
     * Returns an int array with six elements (for date, open, high, low, close,
     * and volume) specifying the index into a data set row where to find the
     * data. -1 indicates that the data is not available.
     */
    @SuppressWarnings("nls")
    private int[] extractColumnMapping(Security security, JSONArray columnNames)
    {
        int[] mapping = new int[6];
        Arrays.fill(mapping, -1);

        int columnSize = columnNames.size();

        // first: attempt a default mapping of columns to security prices

        for (int index = 0; index < columnSize; index++)
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
                case "Close":
                    mapping[4] = index;
                    break;

                case "Traded Volume":
                    mapping[5] = index;
                    break;

                default:
            }
        }

        // second: if a close column name is configured, try to map

        Optional<String> closeColumnName = security.getPropertyValue(SecurityProperty.Type.FEED,
                        QUANDL_CLOSE_COLUMN_NAME_PROPERTY_NAME);

        closeColumnName.ifPresent(columnName -> {
            for (int index = 0; index < columnSize; index++)
            {
                String name = columnNames.get(index).toString();
                if (name.equals(columnName))
                {
                    mapping[4] = index;
                    break;
                }
            }
        });

        return mapping;
    }
}
