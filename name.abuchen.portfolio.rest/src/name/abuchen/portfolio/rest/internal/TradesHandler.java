package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.trades.Trade;
import name.abuchen.portfolio.snapshot.trades.TradeCollector;
import name.abuchen.portfolio.snapshot.trades.TradeCollectorException;

public final class TradesHandler
{
    private TradesHandler()
    {
    }

    /**
     * Buy/sell activity paired into trades, one entry per round trip.
     * <p>
     * Unlike the other endpoints this is not bounded by a reporting period: a trade
     * is a fact about a holding, so the collector pairs a security's whole history
     * and the client filters. Pass {@code onlyClosed=true} for completed round trips
     * only.
     * <p>
     * An open trade is valued at <em>today's</em> price and its holding period runs
     * to today, so those two fields are not reproducible for a past date - which is
     * why there is no date parameter to suggest otherwise.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String currencyParam,
                    String onlyClosedParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var currency = client.getBaseCurrency();
        if (currencyParam != null)
        {
            if (CurrencyUnit.getInstance(currencyParam) == null)
                errors.add(new ApiException.FieldError("currency", "unknown-currency",
                                currencyParam + " is not a known currency"));
            else
                currency = currencyParam;
        }

        var onlyClosed = false;
        if (onlyClosedParam != null)
        {
            if ("true".equals(onlyClosedParam))
                onlyClosed = true;
            else if ("false".equals(onlyClosedParam))
                onlyClosed = false;
            else
                errors.add(new ApiException.FieldError("onlyClosed", "invalid-value",
                                "onlyClosed must be true or false"));
        }

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var converter = new CurrencyConverterImpl(factory, currency);
        var collector = new TradeCollector(client, converter);

        // One security whose transaction history the collector cannot pair must not
        // fail the whole response - the same choice the desktop's trades view makes.
        var trades = new ArrayList<Trade>();
        var skipped = new ArrayList<String>();
        for (var security : client.getSecurities())
        {
            try
            {
                trades.addAll(collector.collect(security));
            }
            catch (TradeCollectorException | RuntimeException e)
            {
                skipped.add(security.getName());
            }
        }

        List<Trade> selected = onlyClosed ? trades.stream().filter(Trade::isClosed).toList() : trades;

        return EntityJson.trades(currency, selected, skipped);
    }
}
