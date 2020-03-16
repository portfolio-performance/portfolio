package name.abuchen.portfolio.online.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
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
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.PortfolioReportNet.MarketInfo;
import name.abuchen.portfolio.util.WebAccess;

public final class PortfolioReportQuoteFeed implements QuoteFeed
{
    public static final String ID = "PORTFOLIO-REPORT"; //$NON-NLS-1$
    public static final String MARKETS_PROPERTY_NAME = "PORTFOLIO-REPORT-MARKETS"; //$NON-NLS-1$
    public static final String MARKET_PROPERTY_NAME = "PORTFOLIO-REPORT-MARKET"; //$NON-NLS-1$

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
    public boolean updateLatestQuotes(Security security, List<Exception> errors)
    {
        return false;
    }

    @Override
    public boolean updateHistoricalQuotes(Security security, List<Exception> errors)
    {
        LocalDate start = null;

        if (!security.getPrices().isEmpty())
        {
            start = security.getPrices().get(security.getPrices().size() - 1).getDate();
        }
        else
        {
            start = LocalDate.of(2000, 1, 1);
        }

        List<SecurityPrice> prices = getHistoricalQuotes(SecurityPrice.class, security, start, errors);

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
        return getHistoricalQuotes(LatestSecurityPrice.class, security, start, errors);
    }

    @SuppressWarnings("unchecked")
    public <T extends SecurityPrice> List<T> getHistoricalQuotes(Class<T> klass, Security security, LocalDate start,
                    List<Exception> errors)
    {
        if (security.getOnlineId() == null)
        {
            errors.add(new IOException(MessageFormat.format(Messages.MsgErrorMissingOnlineId, security.getName())));
            return Collections.emptyList();
        }

        Optional<String> market = security.getPropertyValue(SecurityProperty.Type.FEED, MARKET_PROPERTY_NAME);

        if (!market.isPresent())
        {
            errors.add(new IOException(
                            MessageFormat.format(Messages.MsgErrorMissingPortfolioReportMarket, security.getName())));
            return Collections.emptyList();
        }

        try
        {
            @SuppressWarnings("nls")
            String response = new WebAccess("www.portfolio-report.net",
                            "/api/securities/uuid/" + security.getOnlineId() + "/markets/" + market.get()) //
                                            .addUserAgent("PortfolioPerformance/"
                                                            + FrameworkUtil.getBundle(PortfolioReportNet.class)
                                                                            .getVersion().toString())
                                            .addParameter("from", start.toString()).get();

            JSONObject json = (JSONObject) JSONValue.parse(response);

            JSONArray data = (JSONArray) json.get("prices"); //$NON-NLS-1$
            if (data == null)
            {
                errors.add(new IOException(MessageFormat.format(Messages.MsgErrorMissingKeyValueInJSON, "prices"))); //$NON-NLS-1$
                return Collections.emptyList();
            }

            List<T> prices = new ArrayList<>();

            data.forEach(entry -> {
                try
                {
                    JSONObject row = (JSONObject) entry;

                    LocalDate date = LocalDate.parse(row.get("date").toString()); //$NON-NLS-1$

                    Number c = (Number) row.get("close"); //$NON-NLS-1$
                    long close = c == null ? LatestSecurityPrice.NOT_AVAILABLE
                                    : Values.Quote.factorize(c.doubleValue());

                    if (close > 0)
                    {
                        T price = klass.getConstructor().newInstance();
                        price.setDate(date);
                        price.setValue(close);

                        if (price instanceof LatestSecurityPrice)
                        {
                            LatestSecurityPrice lsp = (LatestSecurityPrice) price;
                            lsp.setHigh(LatestSecurityPrice.NOT_AVAILABLE);
                            lsp.setLow(LatestSecurityPrice.NOT_AVAILABLE);
                            lsp.setVolume(LatestSecurityPrice.NOT_AVAILABLE);
                            lsp.setPreviousClose(LatestSecurityPrice.NOT_AVAILABLE);
                        }

                        prices.add(price);
                    }
                }
                catch (ReflectiveOperationException | IllegalArgumentException | SecurityException ex)
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

    @Override
    public List<LatestSecurityPrice> getHistoricalQuotes(String response, List<Exception> errors)
    {
        throw new UnsupportedOperationException();
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
