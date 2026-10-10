package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.datatransfer.ImportAction;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;

/**
 * Where imported items are booked unless an item names its accounts itself:
 * the {@code targets} of an import request, as the drop-downs of the
 * application's import wizard do it.
 * <p/>
 * {@code targets} is
 * {@code {cashAccount, cashAccountsByCurrency: {EUR: uuid}, investmentAccount,
 * secondaryCashAccount, secondaryInvestmentAccount, instrument}}, all
 * optional. An item in a currency is booked on the cash account named for
 * that currency, else on {@code cashAccount} if it has that currency, else -
 * as the wizard preselects - on the first active cash account in that
 * currency by name. Investment-account items go to {@code investmentAccount},
 * else the first active investment account. The receiving side of a
 * transfer goes to {@code secondaryCashAccount} (if the currency matches,
 * else the first cash account in the currency) and
 * {@code secondaryInvestmentAccount} (no default: the wizard makes the user
 * choose). {@code instrument} is the instrument a price import is for.
 */
public final class ImportContext implements ImportAction.Context
{
    private static final String PREFIX = "targets."; //$NON-NLS-1$

    private final Client client;
    private final Map<String, Account> accountsByCurrency;
    private final Account account;
    private final Portfolio portfolio;
    private final Account secondaryAccount;
    private final Portfolio secondaryPortfolio;
    private final Security instrument;

    private ImportContext(Client client, Map<String, Account> accountsByCurrency, Account account,
                    Portfolio portfolio, Account secondaryAccount, Portfolio secondaryPortfolio, Security instrument)
    {
        this.client = client;
        this.accountsByCurrency = accountsByCurrency;
        this.account = account;
        this.portfolio = portfolio;
        this.secondaryAccount = secondaryAccount;
        this.secondaryPortfolio = secondaryPortfolio;
        this.instrument = instrument;
    }

    /**
     * Parses the {@code targets} object (null: all defaults); problems are
     * added to {@code json} as errors of {@code targets.<field>}.
     */
    public static ImportContext of(Client client, JsonElement targets, Json json)
    {
        if (targets == null || targets.isJsonNull())
            return new ImportContext(client, Map.of(), null, null, null, null, null);

        if (!targets.isJsonObject())
        {
            json.add(new ApiException.FieldError("targets", "invalid-type", "targets must be an object")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return new ImportContext(client, Map.of(), null, null, null, null, null);
        }

        var object = targets.getAsJsonObject();
        var known = List.of("cashAccount", "cashAccountsByCurrency", "investmentAccount", "secondaryCashAccount", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                        "secondaryInvestmentAccount", "instrument"); //$NON-NLS-1$ //$NON-NLS-2$
        for (var key : object.keySet())
        {
            if (!known.contains(key))
                json.add(new ApiException.FieldError(PREFIX + key, "unknown-field", //$NON-NLS-1$
                                MessageFormat.format("{0} is not a known field", PREFIX + key))); //$NON-NLS-1$
        }

        var account = resolve(object, "cashAccount", client.getAccounts(), Account::getUUID, json); //$NON-NLS-1$
        var portfolio = resolve(object, "investmentAccount", client.getPortfolios(), Portfolio::getUUID, json); //$NON-NLS-1$
        var secondaryAccount = resolve(object, "secondaryCashAccount", client.getAccounts(), Account::getUUID, json); //$NON-NLS-1$
        var secondaryPortfolio = resolve(object, "secondaryInvestmentAccount", client.getPortfolios(), //$NON-NLS-1$
                        Portfolio::getUUID, json);
        var instrument = resolve(object, "instrument", client.getSecurities(), Security::getUUID, json); //$NON-NLS-1$

        var byCurrency = new HashMap<String, Account>();
        var map = object.get("cashAccountsByCurrency"); //$NON-NLS-1$
        if (map != null && !map.isJsonNull())
        {
            if (!map.isJsonObject())
            {
                json.add(new ApiException.FieldError(PREFIX + "cashAccountsByCurrency", "invalid-type", //$NON-NLS-1$ //$NON-NLS-2$
                                "cashAccountsByCurrency must map currency codes to cash account UUIDs")); //$NON-NLS-1$
            }
            else
            {
                for (var entry : map.getAsJsonObject().entrySet())
                {
                    var field = PREFIX + "cashAccountsByCurrency." + entry.getKey(); //$NON-NLS-1$
                    var found = resolve(entry.getValue(), field, client.getAccounts(), Account::getUUID, json);
                    if (found == null)
                        continue;
                    if (!found.getCurrencyCode().equals(entry.getKey()))
                        json.add(new ApiException.FieldError(field, "currency-mismatch", //$NON-NLS-1$
                                        MessageFormat.format("{0} is a {1} account", found.getName(), //$NON-NLS-1$
                                                        found.getCurrencyCode())));
                    else
                        byCurrency.put(entry.getKey(), found);
                }
            }
        }

        return new ImportContext(client, byCurrency, account, portfolio, secondaryAccount, secondaryPortfolio,
                        instrument);
    }

    private static <T> T resolve(JsonObject object, String field, List<T> candidates, Function<T, String> uuid,
                    Json json)
    {
        return resolve(object.get(field), PREFIX + field, candidates, uuid, json);
    }

    private static <T> T resolve(JsonElement element, String field, List<T> candidates, Function<T, String> uuid,
                    Json json)
    {
        if (element == null || element.isJsonNull())
            return null;

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
        {
            json.add(new ApiException.FieldError(field, "invalid-type", //$NON-NLS-1$
                            MessageFormat.format("{0} must be a UUID string", field))); //$NON-NLS-1$
            return null;
        }

        var wanted = element.getAsString();
        var found = candidates.stream().filter(c -> uuid.apply(c).equals(wanted)).findFirst();
        if (found.isEmpty())
            json.add(new ApiException.FieldError(field, "unknown-reference", //$NON-NLS-1$
                            MessageFormat.format("{0} is not known", wanted))); //$NON-NLS-1$
        return found.orElse(null);
    }

    /** the instrument a price import is for, or null */
    public Security getInstrument()
    {
        return instrument;
    }

    @Override
    public Account getAccount(String currencyCode)
    {
        var named = accountsByCurrency.get(currencyCode);
        if (named != null)
            return named;
        if (account != null && account.getCurrencyCode().equals(currencyCode))
            return account;
        return firstAccount(currencyCode);
    }

    @Override
    public Portfolio getPortfolio()
    {
        if (portfolio != null)
            return portfolio;

        var active = client.getActivePortfolios();
        if (!active.isEmpty())
            return active.get(0);
        return client.getPortfolios().isEmpty() ? null : client.getPortfolios().get(0);
    }

    @Override
    public Account getSecondaryAccount(String currencyCode)
    {
        if (secondaryAccount != null && secondaryAccount.getCurrencyCode().equals(currencyCode))
            return secondaryAccount;
        return firstAccount(currencyCode);
    }

    @Override
    public Portfolio getSecondaryPortfolio()
    {
        return secondaryPortfolio;
    }

    /** the first active cash account in the currency by name, else any in the currency */
    private Account firstAccount(String currencyCode)
    {
        var inCurrency = client.getAccounts().stream().filter(a -> a.getCurrencyCode().equals(currencyCode))
                        .sorted(new Account.ByName()).toList();
        return inCurrency.stream().filter(a -> !a.isRetired()).findFirst()
                        .orElse(inCurrency.isEmpty() ? null : inCurrency.get(0));
    }

    /** a reference to a cash or investment account in a preview, or null */
    /* package */ static JsonObject reference(Object owner)
    {
        if (owner instanceof Account a)
            return reference(a.getUUID(), a.getName());
        if (owner instanceof Portfolio p)
            return reference(p.getUUID(), p.getName());
        return null;
    }

    private static JsonObject reference(String uuid, String name)
    {
        var json = new JsonObject();
        json.addProperty("uuid", uuid); //$NON-NLS-1$
        json.addProperty("name", name); //$NON-NLS-1$
        return json;
    }
}
