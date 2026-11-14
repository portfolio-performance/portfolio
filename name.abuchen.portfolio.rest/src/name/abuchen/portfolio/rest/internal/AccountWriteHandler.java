package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.rest.WriteApprovals;

/**
 * The two accounts a file is built from: one holding money in one currency, and
 * the depot that holds papers against a reference account of money. Both have
 * been readable since the first day of this API; neither could be made by it,
 * so no caller could grow a structure the file does not have yet.
 *
 * <p>
 * Creation only. An account that exists can be renamed, retired, or emptied in
 * the application, and nothing here does any of that: an account carries
 * transactions in one direction and a depot points at its money in the other,
 * and a tool that could cut those ties would answer for them too. A name the
 * file already carries is refused rather than doubled up - an account has a
 * uuid, so two of one name could be told apart, but only by a caller that went
 * and looked, which is exactly the mistake this refuses to let through quietly.
 * </p>
 */
public final class AccountWriteHandler
{
    private AccountWriteHandler()
    {
    }

    // ------------------------------------------------------------------ cash

    /**
     * What creating one cash account would do, settled before anybody is asked:
     * the name free of its duplicates and the currency present in the file, so
     * the change can never fail after a user said yes.
     */
    public static WriteApprovals.Facts describeCash(Client client, String fileLabel, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var name = name(body, errors);
        var currency = currency(client, body, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var taken = sameNameAndCurrency(client, name, currency);
        if (taken != null)
            throw nameTaken("cash account", name, taken.getUUID());

        return new WriteApprovals.Facts(null, fileLabel, "cash-account", name, null, currency); //$NON-NLS-1$
    }

    /**
     * A new account of one currency. The currency left out is the file's own,
     * which is the one account every file wants first.
     */
    public static JsonObject createCash(Client client, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var name = name(body, errors);
        var currency = currency(client, body, errors);
        var note = note(body, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var taken = sameNameAndCurrency(client, name, currency);
        if (taken != null)
            throw nameTaken("cash account", name, taken.getUUID());

        var account = new Account(name);
        account.setCurrencyCode(currency);
        if (note != null)
            account.setNote(note);

        client.addAccount(account);
        client.markDirty();

        var json = EntityJson.toJson(account);
        json.addProperty("created", true); //$NON-NLS-1$
        return json;
    }

    // ------------------------------------------------------------ investment

    /**
     * What creating one depot would do. The reference account is settled here as
     * well, including the one picked when the request leaves it out: a user is
     * asked about the depot and the money account it will point at, not about a
     * choice the change would have to make up for itself afterwards.
     */
    public static WriteApprovals.Facts describeInvestment(Client client, String fileLabel, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var name = name(body, errors);
        var reference = reference(client, body, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var taken = sameName(client.getPortfolios(), name);
        if (taken != null)
            throw nameTaken("investment account", name, taken.getUUID());

        return new WriteApprovals.Facts(null, fileLabel, "investment-account", name, reference.getName(), null); //$NON-NLS-1$
    }

    /**
     * A new depot pointing at the money account that belongs with it. A depot
     * holds positions and nothing else, so the reference is not decoration:
     * every purchase booked into it draws its money from that account.
     */
    public static JsonObject createInvestment(Client client, JsonObject body)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var name = name(body, errors);
        var reference = reference(client, body, errors);
        var note = note(body, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var taken = sameName(client.getPortfolios(), name);
        if (taken != null)
            throw nameTaken("investment account", name, taken.getUUID());

        var portfolio = new Portfolio(name);
        portfolio.setReferenceAccount(reference);
        if (note != null)
            portfolio.setNote(note);

        client.addPortfolio(portfolio);
        client.markDirty();

        var json = EntityJson.toJson(portfolio);
        json.addProperty("created", true); //$NON-NLS-1$
        return json;
    }

    // -------------------------------------------------------------- shared

    /** the name, asked for the same way everywhere a thing is made by hand */
    private static String name(JsonObject body, List<ApiException.FieldError> errors)
    {
        var element = body.get("name"); //$NON-NLS-1$
        if (element == null || element.isJsonNull())
        {
            errors.add(new ApiException.FieldError("name", "required", "name is required to create an account")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            errors.add(new ApiException.FieldError("name", "invalid-type", "name must be a string")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        var name = element.getAsString().trim();
        if (name.isEmpty())
        {
            errors.add(new ApiException.FieldError("name", "required", "name must not be blank")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }
        return name;
    }

    /** the optional note, trimmed; absent is a note left out, not a note cleared */
    private static String note(JsonObject body, List<ApiException.FieldError> errors)
    {
        var element = body.get("note"); //$NON-NLS-1$
        if (element == null || element.isJsonNull())
            return null;

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            errors.add(new ApiException.FieldError("note", "invalid-type", "note must be a string")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        var note = element.getAsString().trim();
        return note.isEmpty() ? null : note;
    }

    /**
     * The currency of the new account. Left out it is the file's own currency -
     * the one every file counts in. Given, it has to be one the file already
     * moves in: prices, transfers and reports all read their rates from series
     * the file holds, and an account in a currency without them is money no
     * report can place.
     */
    private static String currency(Client client, JsonObject body, List<ApiException.FieldError> errors)
    {
        var element = body.get("currencyCode"); //$NON-NLS-1$
        if (element == null || element.isJsonNull())
            return client.getBaseCurrency();

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            errors.add(new ApiException.FieldError("currencyCode", "invalid-type", "currencyCode must be a string")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        var code = element.getAsString().trim();
        if (code.isEmpty())
        {
            errors.add(new ApiException.FieldError("currencyCode", "required", "currencyCode must not be blank")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return null;
        }

        if (code.equals(client.getBaseCurrency()))
            return code;

        for (var unit : client.getUsedCurrencies())
            if (unit.getCurrencyCode().equals(code))
                return code;

        errors.add(new ApiException.FieldError("currencyCode", "unknown-currency", //$NON-NLS-1$ //$NON-NLS-2$
                        "the file moves in " + usedCurrencies(client) + " - an account cannot hold a currency outside them")); //$NON-NLS-1$
        return null;
    }

    private static String usedCurrencies(Client client)
    {
        var codes = new ArrayList<String>();
        codes.add(client.getBaseCurrency());
        for (var unit : client.getUsedCurrencies())
            if (!codes.contains(unit.getCurrencyCode()))
                codes.add(unit.getCurrencyCode());
        return String.join(", ", codes); //$NON-NLS-1$
    }

    /**
     * The depot's money account, as the request gives it or as the file suggests
     * it. Left out, the account must be the obvious one: exactly one account in
     * the file's own currency. None or several leave the choice to the caller,
     * because guessing which of two brokers an agent meant is not this API's to
     * make.
     */
    private static Account reference(Client client, JsonObject body, List<ApiException.FieldError> errors)
    {
        var element = body.get("referenceCashAccount"); //$NON-NLS-1$

        if (element != null && !element.isJsonNull())
        {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            {
                errors.add(new ApiException.FieldError("referenceCashAccount", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "referenceCashAccount must be the uuid of a cash account")); //$NON-NLS-1$
                return null;
            }

            var uuid = element.getAsString().trim();
            for (var account : client.getAccounts())
                if (account.getUUID().equals(uuid))
                    return account;

            errors.add(new ApiException.FieldError("referenceCashAccount", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "no cash account has this uuid - see pp_list_cash_accounts")); //$NON-NLS-1$
            return null;
        }

        Account found = null;
        var ambiguous = false;
        for (var account : client.getAccounts())
        {
            if (client.getBaseCurrency().equals(account.getCurrencyCode()))
            {
                if (found != null)
                {
                    ambiguous = true;
                    break;
                }
                found = account;
            }
        }

        if (ambiguous)
        {
            errors.add(new ApiException.FieldError("referenceCashAccount", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "the file holds more than one cash account in its base currency - name one by uuid")); //$NON-NLS-1$
            return null;
        }
        if (found == null)
        {
            errors.add(new ApiException.FieldError("referenceCashAccount", "required", //$NON-NLS-1$ //$NON-NLS-2$
                            "the file holds no cash account in its base currency - name one by uuid")); //$NON-NLS-1$
            return null;
        }

        return found;
    }

    /** an account of this very name, or null when the name is free */
    private static Account sameNameAndCurrency(Client client, String name, String currency)
    {
        if (name == null)
            return null;

        for (var account : client.getAccounts())
            if (account.getName().equals(name) && account.getCurrencyCode().equals(currency))
                return account;

        return null;
    }

    /** a depot of this very name, or null when the name is free */
    private static Portfolio sameName(List<Portfolio> portfolios, String name)
    {
        if (name == null)
            return null;

        for (var portfolio : portfolios)
            if (portfolio.getName().equals(name))
                return portfolio;

        return null;
    }

    /** a name twice is refused, not doubled - and the refusal names what holds it */
    private static ApiException nameTaken(String kind, String name, String uuid)
    {
        var field = new ApiException.FieldError("name", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                        "the file already has a " + kind + " named \"" + name + "\" with uuid " + uuid); //$NON-NLS-1$
        return ApiException.conflict("name-taken", "name already in use", field.message(), List.of(field));
    }
}
