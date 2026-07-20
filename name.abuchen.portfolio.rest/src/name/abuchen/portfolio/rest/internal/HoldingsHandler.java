package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
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

        var date = LocalDate.now();
        if (dateParam != null)
        {
            try
            {
                date = LocalDate.parse(dateParam);
            }
            catch (DateTimeParseException e)
            {
                errors.add(new ApiException.FieldError("date", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                "date must be an ISO 8601 date (YYYY-MM-DD)")); //$NON-NLS-1$
            }
        }

        var currency = client.getBaseCurrency();
        if (currencyParam != null)
        {
            if (CurrencyUnit.getInstance(currencyParam) == null)
                errors.add(new ApiException.FieldError("currency", "unknown-currency", //$NON-NLS-1$ //$NON-NLS-2$
                                currencyParam + " is not a known currency")); //$NON-NLS-1$
            else
                currency = currencyParam;
        }

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var converter = new CurrencyConverterImpl(factory, currency);
        return EntityJson.toJson(ClientSnapshot.create(client, converter, date));
    }
}
