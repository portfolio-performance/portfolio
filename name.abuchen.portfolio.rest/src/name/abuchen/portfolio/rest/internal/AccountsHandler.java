package name.abuchen.portfolio.rest.internal;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.rest.Messages;
import name.abuchen.portfolio.snapshot.AccountSnapshot;

public final class AccountsHandler
{
    /** the kind under which created cash accounts are remembered, and the log names them */
    private static final String KIND = "cash-account"; //$NON-NLS-1$

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
        return EntityJson.envelope(client.getAccounts(), account -> EntityJson.toJson(client, account, balanceOf(account, converter, date)));
    }

    public static JsonElement get(Client client, ExchangeRateProviderFactory factory, String uuid, String dateParam)
    {
        var date = parseDate(dateParam);
        var converter = new CurrencyConverterImpl(factory, client.getBaseCurrency());
        var account = find(client, uuid);
        return EntityJson.toJson(client, account, balanceOf(account, converter, date));
    }

    /**
     * Creates a cash account: {@code name} is required, {@code currencyCode}
     * defaults to the file's base currency as in the application. With a
     * {@code clientRef} that an earlier create used, answers that account
     * ({@code replayed: true}). Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult create(WriteContext context, IdempotencyIndex idempotency,
                    JsonObject body)
    {
        var client = context.client();
        var converter = converter(context);

        var existing = idempotency.findEntity(context.file().getPath(), KIND, context.clientRef())
                        .flatMap(uuid -> client.getAccounts().stream().filter(a -> uuid.equals(a.getUUID()))
                                        .findFirst());
        if (existing.isPresent())
            return MasterDataWrites.replayed(toJson(client, converter, existing.get(), existing.get()),
                            context.dryRun());

        var account = new Account();
        account.setCurrencyCode(client.getBaseCurrency());

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);
        if (!json.has("name")) //$NON-NLS-1$
            json.add(new ApiException.FieldError("name", "required", "name is required")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        var setters = stage(client, account, json);
        json.throwIfErrors();

        setters.forEach(setter -> setter.accept(account));

        if (context.dryRun())
            return MasterDataWrites.dryRun(toJson(client, converter, account, account));

        client.addAccount(account);
        client.markDirty();
        idempotency.rememberEntity(context.file().getPath(), KIND, context.clientRef(), account.getUUID());

        ChangeLog.recordEvent(Messages.MsgApiEntityCreated, KIND, account.getName(), context.file().getLabel());

        return new MasterDataWrites.WriteResult(toJson(client, converter, account, account), true);
    }

    /**
     * Applies a JSON Merge Patch to the cash account. The currency cannot be
     * changed while the account has transactions, as in the application's
     * account list. A patch that changes nothing does not mark the file dirty.
     * Must be called on the UI thread.
     */
    public static MasterDataWrites.WriteResult patch(WriteContext context, String uuid, JsonObject body)
    {
        var client = context.client();
        var converter = converter(context);
        var account = find(client, uuid);

        var json = new Json(body);
        var setters = stage(client, account, json);
        json.throwIfErrors();

        var preview = copyOf(account);
        setters.forEach(setter -> setter.accept(preview));

        var before = toJson(client, converter, account, account);
        var after = toJson(client, converter, account, preview);

        if (context.dryRun())
            return MasterDataWrites.dryRun(after);

        var changes = MasterDataWrites.diff(before, after);
        if (changes.isEmpty())
            return new MasterDataWrites.WriteResult(before, false);

        setters.forEach(setter -> setter.accept(account));
        client.markDirty();

        ChangeLog.recordChanges(changes, Messages.MsgApiEntityChanged, KIND, account.getName(),
                        context.file().getLabel());

        return new MasterDataWrites.WriteResult(toJson(client, converter, account, account), true);
    }

    /**
     * Validates the writable fields of the body and answers the setters that
     * apply them; violations are recorded in {@code json}.
     */
    private static List<Consumer<Account>> stage(Client client, Account account, Json json)
    {
        var setters = new ArrayList<Consumer<Account>>();

        if (json.has("name")) //$NON-NLS-1$
        {
            var name = json.requireString("name"); //$NON-NLS-1$
            if (name != null)
                setters.add(a -> a.setName(name));
        }

        if (json.has("currencyCode")) //$NON-NLS-1$
        {
            var currency = json.requireString("currencyCode"); //$NON-NLS-1$
            if (currency != null && CurrencyUnit.getInstance(currency) == null)
                json.add(new ApiException.FieldError("currencyCode", "unknown-currency", //$NON-NLS-1$ //$NON-NLS-2$
                                currency + " is not a known currency")); //$NON-NLS-1$
            else if (currency != null && !currency.equals(account.getCurrencyCode())
                            && !account.getTransactions().isEmpty())
                json.add(new ApiException.FieldError("currencyCode", "locked-by-transactions", //$NON-NLS-1$ //$NON-NLS-2$
                                "currency cannot be changed while the cash account has transactions")); //$NON-NLS-1$
            else if (currency != null)
                setters.add(a -> a.setCurrencyCode(currency));
        }

        if (json.has("note")) //$NON-NLS-1$
        {
            var note = json.optString("note"); //$NON-NLS-1$
            setters.add(a -> a.setNote(note));
        }

        if (json.has("retired")) //$NON-NLS-1$
        {
            var retired = json.bool("retired"); //$NON-NLS-1$
            if (retired == null && json.isNull("retired")) //$NON-NLS-1$
                json.add(new ApiException.FieldError("retired", "invalid-type", "retired must be true or false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            else if (retired != null)
                setters.add(a -> a.setRetired(retired));
        }

        if (json.has("attributes")) //$NON-NLS-1$
        {
            var errors = new ArrayList<ApiException.FieldError>();
            var assignments = AttributePatch.stage(client, Account.class, "cash accounts", account.getAttributes(), //$NON-NLS-1$
                            json.body().get("attributes"), errors); //$NON-NLS-1$
            errors.forEach(json::add);
            setters.add(a -> AttributePatch.apply(a.getAttributes(), assignments));
        }

        json.rejectUnknownFields();
        return setters;
    }

    /**
     * Deletes the cash account, but only if it has no transactions and no
     * investment plan or investment account refers to it: the application
     * offers the delete only for an account without transactions, and
     * removing a referenced account would silently clear the reference
     * account of an investment account and delete plans. A dry run answers
     * the account that would be removed; a real delete answers null. Must be
     * called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String uuid)
    {
        var client = context.client();
        var account = find(client, uuid);

        var errors = new ArrayList<ApiException.FieldError>();
        if (!account.getTransactions().isEmpty())
            errors.add(new ApiException.FieldError("transactions", "referenced", "cash account has transactions")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (client.getPlans().stream().anyMatch(plan -> account.equals(plan.getAccount())))
            errors.add(new ApiException.FieldError("plans", "referenced", "cash account is used by investment plans")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (client.getPortfolios().stream().anyMatch(p -> account.equals(p.getReferenceAccount())))
            errors.add(new ApiException.FieldError("investmentAccounts", "referenced", //$NON-NLS-1$ //$NON-NLS-2$
                            "cash account is the reference account of an investment account")); //$NON-NLS-1$
        if (!errors.isEmpty())
            throw ApiException.conflict("delete-blocked", "Cash account is referenced and cannot be deleted", null, //$NON-NLS-1$ //$NON-NLS-2$
                            errors);

        if (context.dryRun())
        {
            var converter = converter(context);
            return MasterDataWrites.deletePreview(toJson(client, converter, account, account));
        }

        client.removeAccount(account);
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiEntityDeleted, KIND, account.getName(), context.file().getLabel());
        return null;
    }

    /* package */ static Account find(Client client, String uuid)
    {
        return Entities.byUuid(client.getAccounts(), Account::getUUID, uuid);
    }

    /** a detached copy of the account to preview a patch on; it is never added to the client */
    private static Account copyOf(Account account)
    {
        var copy = new Account(account.getName());
        copy.setCurrencyCode(account.getCurrencyCode());
        copy.setNote(account.getNote());
        copy.setRetired(account.isRetired());
        copy.setAttributes(account.getAttributes().copy());
        return copy;
    }

    /**
     * The account {@code shown} (the account itself or a preview of it) as
     * the API reports {@code account}: its UUID and today's balance, in the
     * shown currency (a currency can only change while the balance is zero).
     */
    private static JsonObject toJson(Client client, CurrencyConverter converter, Account account, Account shown)
    {
        var balance = balanceOf(account, converter, LocalDate.now());
        var json = EntityJson.toJson(client, shown, Money.of(shown.getCurrencyCode(), balance.getAmount()));
        json.addProperty("uuid", account.getUUID()); //$NON-NLS-1$
        return json;
    }

    private static CurrencyConverter converter(WriteContext context)
    {
        return new CurrencyConverterImpl(context.file().getExchangeRateProviderFactory(),
                        context.client().getBaseCurrency());
    }

    private static Money balanceOf(Account account, CurrencyConverter converter, LocalDate date)
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
