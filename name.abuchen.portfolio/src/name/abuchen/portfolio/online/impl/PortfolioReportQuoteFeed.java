package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.osgi.framework.FrameworkUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.Exchange;
import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.QuoteFeedData;
import name.abuchen.portfolio.online.impl.PortfolioReportNet.MarketInfo;
import name.abuchen.portfolio.util.WebAccess;

public final class PortfolioReportQuoteFeed implements QuoteFeed
{
    public static final String ID = "PORTFOLIO-REPORT"; //$NON-NLS-1$
    public static final String MARKETS_PROPERTY_NAME = "PORTFOLIO-REPORT-MARKETS"; //$NON-NLS-1$
    public static final String MARKET_PROPERTY_NAME = "PORTFOLIO-REPORT-MARKET"; //$NON-NLS-1$

    private final PageCache<String> cache = new PageCache<>();

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Portfolio Report"; //$NON-NLS-1$
    }

    @Override
    public Optional<LatestSecurityPrice> getLatestQuote(Security security)
    {
        List<LatestSecurityPrice> prices = getHistoricalQuotes(security, true, LocalDate.now()).getLatestPrices();
        return prices.isEmpty() ? Optional.empty() : Optional.of(prices.get(prices.size() - 1));
    }

    @Override
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse)
    {
        LocalDate start = null;

        if (!security.getPrices().isEmpty())
            start = security.getPrices().get(security.getPrices().size() - 1).getDate();
        else
            start = LocalDate.of(2000, 1, 1);

        return getHistoricalQuotes(security, collectRawResponse, start);
    }

    @Override
    public QuoteFeedData previewHistoricalQuotes(Security security)
    {
        return getHistoricalQuotes(security, true, LocalDate.now().minusMonths(2));
    }

    @SuppressWarnings("unchecked")
    public QuoteFeedData getHistoricalQuotes(Security security, boolean collectRawResponse, LocalDate start)
    {
        if (security.getOnlineId() == null)
        {
            return QuoteFeedData.withError(new IOException(
                            MessageFormat.format(Messages.MsgErrorMissingOnlineId, security.getName())));
        }

        Optional<String> market = security.getPropertyValue(SecurityProperty.Type.FEED, MARKET_PROPERTY_NAME);

        if (!market.isPresent())
        {
            return QuoteFeedData.withError(new IOException(
                            MessageFormat.format(Messages.MsgErrorMissingPortfolioReportMarket, security.getName())));
        }

        QuoteFeedData data = new QuoteFeedData();

        try
        {
            @SuppressWarnings("nls")
            WebAccess webaccess = new WebAccess("www.portfolio-report.net",
                            "/api/securities/uuid/" + security.getOnlineId() + "/markets/" + market.get()) //
                                            .addUserAgent("PortfolioPerformance/"
                                                            + FrameworkUtil.getBundle(PortfolioReportNet.class)
                                                                            .getVersion().toString())
                                            .addParameter("from", start.toString());

            String url = webaccess.getURL();

            String response = cache.lookup(url);

            if (response == null)
            {
                response = webaccess.get();

                if (response != null)
                    cache.put(url, response);
            }

            if (collectRawResponse)
                data.addResponse(webaccess.getURL(), response);

            JSONObject json = (JSONObject) JSONValue.parse(response);

            JSONArray pricesJson = (JSONArray) json.get("prices"); //$NON-NLS-1$
            if (pricesJson == null)
            {
                data.addError(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "prices"))); //$NON-NLS-1$
                return data;
            }

            pricesJson.forEach(entry -> {
                JSONObject row = (JSONObject) entry;

                LocalDate date = LocalDate.parse(row.get("date").toString()); //$NON-NLS-1$

                Number c = (Number) row.get("close"); //$NON-NLS-1$
                long close = c == null ? LatestSecurityPrice.NOT_AVAILABLE : Values.Quote.factorize(c.doubleValue());

                if (close > 0)
                {
                    LatestSecurityPrice price = new LatestSecurityPrice();
                    price.setDate(date);
                    price.setValue(close);

                    price.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setLow(LatestSecurityPrice.NOT_AVAILABLE);
                    price.setVolume(LatestSecurityPrice.NOT_AVAILABLE);

                    data.addPrice(price);
                }
            });

        }
        catch (IOException | URISyntaxException e)
        {
            data.addError(e);
        }

        return data;
    }

    @Override
    public List<Exchange> getExchanges(Security security, List<Exception> errors)
    {
        if (security.getOnlineId() == null)
            return Collections.emptyList();

        Optional<String> markets = security.getPropertyValue(SecurityProperty.Type.FEED, MARKETS_PROPERTY_NAME);
        if (!markets.isPresent())
            return Collections.emptyList();

        Type collectionType = new TypeToken<List<MarketInfo>>()
        {
        }.getType();

        List<MarketInfo> marketInfos = new Gson().fromJson(markets.get(), collectionType);

        return marketInfos
                        .stream().map(
                                        m -> new Exchange(m.getMarketCode(),
                                                        MessageFormat.format(Messages.LabelXwithCurrencyY,
                                                                        m.getMarketCode(), m.getCurrencyCode())))
                        .collect(Collectors.toList());
    }

}
