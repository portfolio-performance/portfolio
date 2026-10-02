package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.util.ArrayList;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.snapshot.ClientSnapshot;

public final class HoldingsHandler
{
    private HoldingsHandler()
    {
    }

    /**
     * Values all holdings at the given date (default: today) in the given
     * currency (default: the file's base currency). A currency without an
     * exchange rate series converts 1:1 - the same silent fallback the UI
     * applies. The factory is the host's per-file instance - it registers a
     * listener on the client, so this handler must not construct its own.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String dateParam,
                    String currencyParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var date = dateParam == null ? LocalDate.now() : CalcParams.date("date", dateParam, errors); //$NON-NLS-1$
        var currency = CalcParams.reportingCurrency(client, currencyParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var converter = new CurrencyConverterImpl(factory, currency);
        return EntityJson.toJson(ClientSnapshot.create(client, converter, date), currency);
    }
}
