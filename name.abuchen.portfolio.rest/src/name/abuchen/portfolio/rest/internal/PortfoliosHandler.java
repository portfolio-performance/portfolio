package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.rest.Messages;
import name.abuchen.portfolio.snapshot.PortfolioSnapshot;

public final class PortfoliosHandler
{
    /** the kind under which created investment accounts are remembered, and the log names them */
    private static final String KIND = "investment-account"; //$NON-NLS-1$

    private PortfoliosHandler()
    {
    }

    /**
     * Lists every investment account with its total value at the given date
     * (default: today) in the given currency (default: the file's base
     * currency) - the sum of every security position it holds, valued at the
     * date's quotes and converted like the holdings endpoint. The factory is
     * the host's per-file instance - it registers a listener on the client,
     * so this handler must not construct its own.
     */
    public static JsonElement list(Client client, ExchangeRateProviderFactory factory, String dateParam,
                    String currencyParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var date = parseDate(dateParam, errors);
        var currency = parseCurrency(client, currencyParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var converter = new CurrencyConverterImpl(factory, currency);
        return EntityJson.envelope(client.getPortfolios(), portfolio -> EntityJson.toJson(client, portfolio, valueOf(portfolio, converter, date)));
    }

    public static JsonElement get(Client client, ExchangeRateProviderFactory factory, String uuid, String dateParam,
                    String currencyParam)
    {
        var errors = new ArrayList<ApiException.FieldError>();
        var date = parseDate(dateParam, errors);
        var currency = parseCurrency(client, currencyParam, errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        var converter = new CurrencyConverterImpl(factory, currency);
        var portfolio = find(client, uuid);
        return EntityJson.toJson(client, portfolio, valueOf(portfolio, converter, date));
    }

    /**
     * Creates an investment account: {@code name} and
     * {@code referenceCashAccount} are required. With a {@code clientRef}
     * that an earlier create used, answers that account
     * ({@code replayed: true}). Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult create(WriteContext context, IdempotencyIndex idempotency,
                    JsonObject body)
    {
        var client = context.client();
        var converter = converter(context);

        var existing = idempotency.findEntity(context.file().getPath(), KIND, context.clientRef())
                        .flatMap(uuid -> client.getPortfolios().stream().filter(p -> uuid.equals(p.getUUID()))
                                        .findFirst());
        if (existing.isPresent())
            return MasterDataWrites.replayed(toJson(client, converter, existing.get(), existing.get()),
                            context.dryRun());

        var portfolio = new Portfolio();

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);
        for (var field : List.of("name", "referenceCashAccount")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            if (!json.has(field))
                json.add(new ApiException.FieldError(field, "required", MessageFormat.format("{0} is required", field))); //$NON-NLS-1$ //$NON-NLS-2$
        }
        var setters = stage(client, portfolio, json);
        json.throwIfErrors();

        setters.forEach(setter -> setter.accept(portfolio));

        if (context.dryRun())
            return MasterDataWrites.dryRun(toJson(client, converter, portfolio, portfolio));

        client.addPortfolio(portfolio);
        client.markDirty();
        idempotency.rememberEntity(context.file().getPath(), KIND, context.clientRef(), portfolio.getUUID());

        ChangeLog.recordEvent(Messages.MsgApiEntityCreated, KIND, portfolio.getName(), context.file().getLabel());

        return new MasterDataWrites.WriteResult(toJson(client, converter, portfolio, portfolio), true);
    }

    /**
     * Applies a JSON Merge Patch to the investment account. A patch that
     * changes nothing does not mark the file dirty. Must be called on the UI
     * thread.
     */
    public static MasterDataWrites.WriteResult patch(WriteContext context, String uuid, JsonObject body)
    {
        var client = context.client();
        var converter = converter(context);
        var portfolio = find(client, uuid);

        var json = new Json(body);
        var setters = stage(client, portfolio, json);
        json.throwIfErrors();

        var preview = copyOf(portfolio);
        setters.forEach(setter -> setter.accept(preview));

        var before = toJson(client, converter, portfolio, portfolio);
        var after = toJson(client, converter, portfolio, preview);

        if (context.dryRun())
            return MasterDataWrites.dryRun(after);

        var changes = MasterDataWrites.diff(before, after);
        if (changes.isEmpty())
            return new MasterDataWrites.WriteResult(before, false);

        setters.forEach(setter -> setter.accept(portfolio));
        client.markDirty();

        ChangeLog.recordChanges(changes, Messages.MsgApiEntityChanged, KIND, portfolio.getName(),
                        context.file().getLabel());

        return new MasterDataWrites.WriteResult(toJson(client, converter, portfolio, portfolio), true);
    }

    /**
     * Validates the writable fields of the body and answers the setters that
     * apply them; violations are recorded in {@code json}. The reference cash
     * account must exist and - unless it is the current one - must not be
     * retired, as the application only offers active accounts.
     */
    private static List<Consumer<Portfolio>> stage(Client client, Portfolio portfolio, Json json)
    {
        var setters = new ArrayList<Consumer<Portfolio>>();

        if (json.has("name")) //$NON-NLS-1$
        {
            var name = json.requireString("name"); //$NON-NLS-1$
            if (name != null)
                setters.add(p -> p.setName(name));
        }

        if (json.has("referenceCashAccount")) //$NON-NLS-1$
        {
            var uuid = json.requireString("referenceCashAccount"); //$NON-NLS-1$
            if (uuid != null)
            {
                var account = client.getAccounts().stream().filter(a -> uuid.equals(a.getUUID())).findFirst()
                                .orElse(null);
                if (account == null)
                    json.add(new ApiException.FieldError("referenceCashAccount", "unknown-reference", //$NON-NLS-1$ //$NON-NLS-2$
                                    MessageFormat.format("no cash account with uuid {0}", uuid))); //$NON-NLS-1$
                else if (account.isRetired() && !account.equals(portfolio.getReferenceAccount()))
                    json.add(new ApiException.FieldError("referenceCashAccount", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                    "the reference cash account must not be retired")); //$NON-NLS-1$
                else
                    setters.add(p -> p.setReferenceAccount(account));
            }
        }

        if (json.has("note")) //$NON-NLS-1$
        {
            var note = json.optString("note"); //$NON-NLS-1$
            setters.add(p -> p.setNote(note));
        }

        if (json.has("retired")) //$NON-NLS-1$
        {
            var retired = json.bool("retired"); //$NON-NLS-1$
            if (retired == null && json.isNull("retired")) //$NON-NLS-1$
                json.add(new ApiException.FieldError("retired", "invalid-type", "retired must be true or false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            else if (retired != null)
                setters.add(p -> p.setRetired(retired));
        }

        if (json.has("attributes")) //$NON-NLS-1$
        {
            var errors = new ArrayList<ApiException.FieldError>();
            var assignments = AttributePatch.stage(client, Portfolio.class, "investment accounts", //$NON-NLS-1$
                            portfolio.getAttributes(), json.body().get("attributes"), errors); //$NON-NLS-1$
            errors.forEach(json::add);
            setters.add(p -> AttributePatch.apply(p.getAttributes(), assignments));
        }

        json.rejectUnknownFields();
        return setters;
    }

    /**
     * Deletes the investment account, but only if it has no transactions -
     * the application offers the delete only then - and no investment plan
     * refers to it. A dry run answers the account that would be removed; a
     * real delete answers null. Must be called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String uuid)
    {
        var client = context.client();
        var portfolio = find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        if (!portfolio.getTransactions().isEmpty())
            errors.add(new ApiException.FieldError("transactions", "referenced", "investment account has transactions")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (client.getPlans().stream().anyMatch(plan -> portfolio.equals(plan.getPortfolio())))
            errors.add(new ApiException.FieldError("plans", "referenced", //$NON-NLS-1$ //$NON-NLS-2$
                            "investment account is used by investment plans")); //$NON-NLS-1$
        if (!errors.isEmpty())
            throw ApiException.conflict("delete-blocked", //$NON-NLS-1$
                            "Investment account is referenced and cannot be deleted", null, errors); //$NON-NLS-1$

        if (context.dryRun())
            return MasterDataWrites.deletePreview(toJson(client, converter(context), portfolio, portfolio));

        client.removePortfolio(portfolio);
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiEntityDeleted, KIND, portfolio.getName(), context.file().getLabel());
        return null;
    }

    /* package */ static Portfolio find(Client client, String uuid)
    {
        return Entities.byUuid(client.getPortfolios(), Portfolio::getUUID, uuid);
    }

    /** a detached copy of the investment account to preview a patch on; it is never added to the client */
    private static Portfolio copyOf(Portfolio portfolio)
    {
        var copy = new Portfolio(portfolio.getName());
        copy.setNote(portfolio.getNote());
        copy.setReferenceAccount(portfolio.getReferenceAccount());
        copy.setRetired(portfolio.isRetired());
        copy.setAttributes(portfolio.getAttributes().copy());
        return copy;
    }

    /**
     * The investment account {@code shown} (the account itself or a preview
     * of it) as the API reports {@code portfolio}: its UUID and today's value
     * in the file's base currency.
     */
    private static JsonObject toJson(Client client, CurrencyConverter converter, Portfolio portfolio,
                    Portfolio shown)
    {
        var json = EntityJson.toJson(client, shown, valueOf(portfolio, converter, LocalDate.now()));
        json.addProperty("uuid", portfolio.getUUID()); //$NON-NLS-1$
        return json;
    }

    private static CurrencyConverter converter(WriteContext context)
    {
        return new CurrencyConverterImpl(context.file().getExchangeRateProviderFactory(),
                        context.client().getBaseCurrency());
    }

    private static Money valueOf(Portfolio portfolio, CurrencyConverter converter, LocalDate date)
    {
        return PortfolioSnapshot.create(portfolio, converter, date).getValue();
    }

    private static LocalDate parseDate(String dateParam, List<ApiException.FieldError> errors)
    {
        if (dateParam == null)
            return LocalDate.now();

        try
        {
            return LocalDate.parse(dateParam);
        }
        catch (DateTimeParseException e)
        {
            errors.add(new ApiException.FieldError("date", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "date must be an ISO 8601 date (YYYY-MM-DD)")); //$NON-NLS-1$
            return LocalDate.now();
        }
    }

    private static String parseCurrency(Client client, String currencyParam, List<ApiException.FieldError> errors)
    {
        if (currencyParam == null)
            return client.getBaseCurrency();

        if (CurrencyUnit.getInstance(currencyParam) == null)
        {
            errors.add(new ApiException.FieldError("currency", "unknown-currency", //$NON-NLS-1$ //$NON-NLS-2$
                            currencyParam + " is not a known currency")); //$NON-NLS-1$
            return client.getBaseCurrency();
        }

        return currencyParam;
    }
}
