package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.snapshot.AccountSnapshot;

public final class AccountsHandler
{
    private AccountsHandler()
    {
    }

    /**
     * Lists every cash account with its current balance at the given date
     * (default: today), in the account's own currency - a plain running total
     * of its transactions, not affected by exchange rates. The factory is the
     * host's per-file instance - it registers a listener on the client, so
     * this handler must not construct its own.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String dateParam)
    {
        var date = parseDate(dateParam);
        var converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        return EntityJson.envelope(client.getAccounts(), account -> EntityJson.toJson(account, balanceOf(account, converter, date)));
    }

    public static JsonElement get(Client client, ExchangeRateProviderFactory factory, String uuid, String dateParam)
    {
        var date = parseDate(dateParam);
        var converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        var account = Entities.byUuid(client.getAccounts(), Account::getUUID, uuid);
        return EntityJson.toJson(account, balanceOf(account, converter, date));
    }

    private static Money balanceOf(Account account, CurrencyConverterImpl converter, LocalDate date)
    {
        return AccountSnapshot.create(account, converter, date).getUnconvertedFunds();
    }

    private static LocalDate parseDate(String dateParam)
    {
        if (dateParam == null)
            return LocalDate.now();

        try
        {
            return LocalDate.parse(dateParam);
        }
        catch (DateTimeParseException e)
        {
            throw ApiException.badRequest(List.of(new ApiException.FieldError("date", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "date must be an ISO 8601 date (YYYY-MM-DD)"))); //$NON-NLS-1$
        }
    }
}
