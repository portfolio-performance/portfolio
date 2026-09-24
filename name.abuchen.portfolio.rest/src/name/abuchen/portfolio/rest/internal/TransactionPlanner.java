package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.AccountTransferEntry;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyUnit;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.internal.TransactionTypes.Kind;

/**
 * Turns a create body or a merge patch into fully resolved, validated model
 * objects - the rules and the construction mirror the application's
 * transaction dialogs ({@code ui/dialogs/transactions/*Model.java}):
 * <ul>
 * <li>buy/sell: fees and taxes (and the gross value in the instrument currency,
 * if it differs) are units of the investment-account leg only; the total is
 * the converted gross value plus (buy) or minus (sell) fees and taxes; a sale
 * with a total of zero must be booked as an outbound delivery</li>
 * <li>a {@code GROSS_VALUE} unit (amount, forex amount and exchange rate) is
 * written only if the instrument currency differs from the transaction
 * currency; the exchange rate is looked up from the file's exchange rates if
 * the client omits it</li>
 * <li>an explicit total is checked with the dialogs' tolerances: the gross
 * value within shares x (quote ± 0.01), the converted gross value within gross
 * x (rate ± 0.0001)</li>
 * <li>a cash transfer between currencies carries the {@code GROSS_VALUE} unit
 * on its outbound leg, with the inverse exchange rate</li>
 * </ul>
 * Planning never touches the model: {@link Plan#preview} builds detached
 * objects that are not added to any account; only {@link Plan#apply} and
 * {@link UpdatePlan#apply} change the model. Every violation is collected
 * before anything is thrown, as a 422 with all field errors at once.
 */
@SuppressWarnings("nls")
public final class TransactionPlanner
{
    public static final String TYPE = "type";
    public static final String DATE = "date";
    public static final String NOTE = "note";
    public static final String CASH_ACCOUNT = "cashAccount";
    public static final String INVESTMENT_ACCOUNT = "investmentAccount";
    public static final String INSTRUMENT = "instrument";
    public static final String SHARES = "shares";
    public static final String QUOTE = "quote";
    public static final String GROSS_VALUE = "grossValue";
    public static final String EXCHANGE_RATE = "exchangeRate";
    public static final String FEES = "fees";
    public static final String TAXES = "taxes";
    public static final String FOREX_FEES = "forexFees";
    public static final String FOREX_TAXES = "forexTaxes";
    public static final String AMOUNT = "amount";
    public static final String TARGET_AMOUNT = "targetAmount";
    public static final String EX_DATE = "exDate";
    public static final String CURRENCY = "currency";
    public static final String FROM_CASH_ACCOUNT = "fromCashAccount";
    public static final String TO_CASH_ACCOUNT = "toCashAccount";
    public static final String FROM_INVESTMENT_ACCOUNT = "fromInvestmentAccount";
    public static final String TO_INVESTMENT_ACCOUNT = "toInvestmentAccount";

    /** the maximum number of decimal places of an exchange rate */
    public static final int RATE_SCALE = 10;

    /** every field a transaction body may carry, besides {@code clientRef} */
    private static final List<String> ALL_FIELDS = List.of(TYPE, DATE, CASH_ACCOUNT, INVESTMENT_ACCOUNT, INSTRUMENT,
                    SHARES, QUOTE, GROSS_VALUE, EXCHANGE_RATE, FEES, TAXES, FOREX_FEES, FOREX_TAXES, AMOUNT,
                    TARGET_AMOUNT, EX_DATE, CURRENCY, FROM_CASH_ACCOUNT, TO_CASH_ACCOUNT, FROM_INVESTMENT_ACCOUNT,
                    TO_INVESTMENT_ACCOUNT, NOTE);

    /** fields whose change requires the amounts and units to be recomputed */
    private static final Set<String> MONETARY = Set.of(TYPE, INSTRUMENT, SHARES, QUOTE, GROSS_VALUE, EXCHANGE_RATE,
                    FEES, TAXES, FOREX_FEES, FOREX_TAXES, AMOUNT, TARGET_AMOUNT, CURRENCY);

    /** a leg as it is written into the model */
    /* package */ record Leg(TransactionOwner<?> owner, Enum<?> type, String currency, long amount, long shares,
                    List<Transaction.Unit> units)
    {
    }

    /** a fully resolved and validated transaction: what the dialogs' applyChanges writes */
    /* package */ record Spec(Kind kind, LocalDateTime date, Security security, String note, LocalDateTime exDate,
                    Leg main, Leg other)
    {
    }

    /** the amounts of a security transaction as the dialogs compute them */
    private record Computed(long total, List<Transaction.Unit> units)
    {
    }

    /** what the planning steps share */
    private record Context(Client client, ExchangeRateProviderFactory factory, Json json)
    {
        void error(ApiException.FieldError error)
        {
            json.add(error);
        }

        /** whether a field already has an error, e.g. could not be parsed */
        boolean failed(String field)
        {
            return json.errors().stream().anyMatch(e -> e.field().equals(field));
        }
    }

    /** a validated create: preview it or apply it */
    public static final class Plan
    {
        private final Spec spec;

        private Plan(Spec spec)
        {
            this.spec = spec;
        }

        /**
         * The transaction as it would be created: detached model objects, not
         * added to any account. The UUIDs are the ones the objects got now;
         * a later real create assigns new ones.
         */
        public TransactionPair<?> preview(String clientRef)
        {
            var pair = build(spec);
            if (clientRef != null)
                setSource(pair.getTransaction(), IdempotencyIndex.source(clientRef));
            return pair;
        }

        /** creates the transaction (both legs of a buy/sell or transfer); the caller marks the client dirty */
        public TransactionPair<?> apply(String clientRef)
        {
            var pair = preview(clientRef);
            var transaction = pair.getTransaction();
            if (transaction.getCrossEntry() != null)
                transaction.getCrossEntry().insert();
            else
                owner(pair.getOwner()).addTransaction(transaction);
            return pair;
        }
    }

    /** a validated merge patch of an existing transaction */
    public static final class UpdatePlan
    {
        private final Client client;
        private final TransactionPair<?> existing;
        private final Spec spec;
        private final Draft before;
        private final Draft after;
        private final Set<String> patched;

        private UpdatePlan(Client client, TransactionPair<?> existing, Spec spec, Draft before, Draft after,
                        Set<String> patched)
        {
            this.client = client;
            this.existing = existing;
            this.spec = spec;
            this.before = before;
            this.after = after;
            this.patched = patched;
        }

        /**
         * The transaction as it would be after the update, with the UUIDs of
         * the existing legs; without {@code updatedAt}.
         */
        public JsonObject preview()
        {
            var pair = build(spec);
            setSource(pair.getTransaction(), existing.getTransaction().getSource());

            var json = EntityJson.toJsonDetailed(pair);
            json.remove("updatedAt");
            json.addProperty("uuid", existing.getTransaction().getUUID());

            var linked = TransactionsHandler.linked(existing);
            if (linked != null && json.has("linked"))
                json.getAsJsonObject("linked").addProperty("uuid", linked.getTransaction().getUUID());

            return json;
        }

        /** true if the update would not change anything */
        public boolean isNoop()
        {
            var current = EntityJson.toJsonDetailed(existing);
            current.remove("updatedAt");
            return current.equals(preview());
        }

        /** the changed fields, for the application log */
        public List<ChangeLog.Change> changes()
        {
            return patched.stream().filter(ALL_FIELDS::contains) //
                            .map(field -> new ChangeLog.Change(field, before.describe(field), after.describe(field)))
                            .filter(change -> !Objects.equals(change.from(), change.to())) //
                            .toList();
        }

        /**
         * Writes the update into the existing objects, which keep their UUIDs.
         * Moving a transaction to another account removes and re-inserts it,
         * as the application does when the owner is edited in a table. The
         * caller marks the client dirty.
         */
        public TransactionPair<?> apply()
        {
            var main = existing.getTransaction();
            var crossEntry = main.getCrossEntry();
            var other = crossEntry != null ? crossEntry.getCrossTransaction(main) : null;

            var moved = spec.main().owner() != existing.getOwner()
                            || (other != null && spec.other().owner() != crossEntry.getCrossOwner(main));

            if (moved)
            {
                owner(existing.getOwner()).deleteTransaction(main, client);
                if (crossEntry != null)
                {
                    crossEntry.setOwner(main, spec.main().owner());
                    crossEntry.setOwner(other, spec.other().owner());
                }
            }

            write(main, spec, spec.main());
            if (other != null)
                write(other, spec, spec.other());

            if (moved)
            {
                if (crossEntry != null)
                    crossEntry.insert();
                else
                    owner(spec.main().owner()).addTransaction(main);
            }

            return TransactionsHandler.pair(spec.main().owner(), main);
        }
    }

    private TransactionPlanner()
    {
    }

    /**
     * Validates a create body and resolves it into a {@link Plan}; throws a
     * 422 with every violation. Must be called on the UI thread.
     */
    public static Plan plan(Client client, ExchangeRateProviderFactory factory, JsonObject body)
    {
        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);

        var type = json.requireString(TYPE);
        if (type != null && TransactionTypes.kindOf(type) == null)
            json.add(new ApiException.FieldError(TYPE, "invalid-value", MessageFormat.format("{0} is not a transaction type, use one of {1}", type, String.join(", ", TransactionTypes.CREATE_TYPES))));
        json.throwIfErrors();

        var draft = new Draft();
        draft.type = type;

        var allowed = allowedFields(type);
        for (var field : allowed)
            read(client, json, draft, field);

        for (var field : ALL_FIELDS)
        {
            if (!TYPE.equals(field) && !allowed.contains(field) && json.has(field) && !json.isNull(field))
                json.add(new ApiException.FieldError(field, "not-allowed-for-type", MessageFormat.format("{0} does not apply to a {1} transaction", field, type)));
        }
        json.rejectUnknownFields();

        var spec = compute(new Context(client, factory, json), draft, true, null);
        json.throwIfErrors();
        return new Plan(spec);
    }

    /**
     * Validates a merge patch of an existing transaction (given as the leg
     * the transaction list reports) and resolves it into an
     * {@link UpdatePlan}; throws a 422 with every violation. Fields that are
     * omitted keep their value, an explicit {@code null} clears a field. The
     * type can only change within its kind (buy and sell, the two deliveries,
     * the cash bookings). Must be called on the UI thread.
     */
    public static UpdatePlan planUpdate(Client client, ExchangeRateProviderFactory factory,
                    TransactionPair<?> existing, JsonObject patch)
    {
        var json = new Json(patch);

        var kind = TransactionTypes.kindOf(existing.getTransaction());
        if (kind == null)
            throw ApiException.validation(List.of(new ApiException.FieldError(TYPE, "not-allowed-for-type", "this transaction cannot be edited through the API")));

        var before = Draft.extract(kind, existing);
        var after = Draft.extract(kind, existing);

        if (json.has(TYPE))
        {
            var type = json.isNull(TYPE) ? null : json.optString(TYPE);
            if (json.isNull(TYPE))
                json.add(new ApiException.FieldError(TYPE, "required", "type cannot be removed"));
            else if (type != null && !type.equals(before.type)
                            && !type.equals(TransactionTypes.wireType(existing.getTransaction())))
            {
                if (kind.types().contains(type))
                    after.type = type;
                else if (TransactionTypes.kindOf(type) != null || TransactionTypes.WIRE_TYPES.contains(type))
                    json.add(new ApiException.FieldError(TYPE, "not-allowed-for-type", MessageFormat.format("a {0} transaction cannot be changed into a {1} transaction, delete it and create a new one", before.type, type)));
                else
                    json.add(new ApiException.FieldError(TYPE, "invalid-value", MessageFormat.format("{0} is not a transaction type", type)));
            }
        }

        var allowed = allowedFields(after.type);
        for (var field : allowed)
            read(client, json, after, field);

        // a field the (new) type has no use for may only be cleared, e.g. the
        // taxes of an interest booking that becomes a deposit
        for (var field : ALL_FIELDS)
        {
            if (TYPE.equals(field) || allowed.contains(field) || !json.has(field))
                continue;

            if (json.isNull(field))
                after.clear(field);
            else
                json.add(new ApiException.FieldError(field, "not-allowed-for-type", MessageFormat.format("{0} does not apply to a {1} transaction", field, after.type)));
        }

        if (patch.has(WriteContext.CLIENT_REF_FIELD))
        {
            json.ignore(WriteContext.CLIENT_REF_FIELD);
            json.add(new ApiException.FieldError(WriteContext.CLIENT_REF_FIELD, "unknown-field", "clientRef cannot be changed"));
        }
        json.rejectUnknownFields();

        // a type change must not silently drop values the new type has no
        // field for (e.g. the taxes of an interest booking turned deposit)
        if (!after.type.equals(before.type))
        {
            for (var field : ALL_FIELDS)
            {
                if (!allowed.contains(field) && !TYPE.equals(field) && !EX_DATE.equals(field) && after.isSet(field))
                    json.add(new ApiException.FieldError(field, "not-allowed-for-type", MessageFormat.format("{0} does not apply to a {1} transaction, set it to null to change the type", field, after.type)));
            }
        }

        // as the application's owner editing: a cash-account leg can only move
        // to an account in the same currency
        checkSameCurrency(json, CASH_ACCOUNT, before.cashAccount, after.cashAccount);
        checkSameCurrency(json, FROM_CASH_ACCOUNT, before.fromCashAccount, after.fromCashAccount);
        checkSameCurrency(json, TO_CASH_ACCOUNT, before.toCashAccount, after.toCashAccount);

        // values derived from what the patch replaces are recomputed
        if (kind != Kind.CASH_TRANSFER && kind != Kind.SECURITY_TRANSFER)
        {
            if (patch.has(QUOTE) && !patch.has(GROSS_VALUE))
                after.grossValue = null;
            if (patch.has(AMOUNT) && !patch.has(GROSS_VALUE) && !patch.has(QUOTE))
                after.grossValue = null;
        }
        if (kind == Kind.SECURITY_TRANSFER && patch.has(QUOTE) && !patch.has(AMOUNT))
            after.amount = null;
        if (!patch.has(EXCHANGE_RATE) && !Objects.equals(ratePair(kind, before), ratePair(kind, after)))
            after.exchangeRate = null;

        var monetary = patch.keySet().stream().anyMatch(MONETARY::contains);

        var spec = compute(new Context(client, factory, json), after, monetary, existing);
        json.throwIfErrors();

        return new UpdatePlan(client, existing, spec, before, after, new LinkedHashSet<>(patch.keySet()));
    }

    /** the fields a transaction of the given (valid) create type accepts */
    private static List<String> allowedFields(String type)
    {
        return switch (TransactionTypes.kindOf(type))
        {
            case BUY_SELL -> List.of(DATE, INVESTMENT_ACCOUNT, CASH_ACCOUNT, INSTRUMENT, SHARES, QUOTE, GROSS_VALUE,
                            EXCHANGE_RATE, FEES, TAXES, FOREX_FEES, FOREX_TAXES, AMOUNT, NOTE);
            case DELIVERY -> List.of(DATE, INVESTMENT_ACCOUNT, INSTRUMENT, SHARES, QUOTE, GROSS_VALUE, CURRENCY,
                            EXCHANGE_RATE, FEES, TAXES, FOREX_FEES, FOREX_TAXES, AMOUNT, NOTE);
            case DIVIDEND -> List.of(DATE, CASH_ACCOUNT, INSTRUMENT, EX_DATE, SHARES, GROSS_VALUE, EXCHANGE_RATE, FEES,
                            TAXES, FOREX_FEES, FOREX_TAXES, AMOUNT, NOTE);
            case CASH -> switch (type)
            {
                case "interest" -> List.of(DATE, CASH_ACCOUNT, AMOUNT, TAXES, NOTE);
                case "fees", "fees-refund", "taxes", "tax-refund" -> List.of(DATE, CASH_ACCOUNT, INSTRUMENT,
                                EXCHANGE_RATE, AMOUNT, NOTE);
                default -> List.of(DATE, CASH_ACCOUNT, AMOUNT, NOTE);
            };
            case CASH_TRANSFER -> List.of(DATE, FROM_CASH_ACCOUNT, TO_CASH_ACCOUNT, AMOUNT, TARGET_AMOUNT, NOTE);
            case SECURITY_TRANSFER -> List.of(DATE, FROM_INVESTMENT_ACCOUNT, TO_INVESTMENT_ACCOUNT, INSTRUMENT, SHARES,
                            QUOTE, AMOUNT, NOTE);
        };
    }

    /** reads one field of the body into the draft; an explicit null clears it */
    private static void read(Client client, Json json, Draft draft, String field)
    {
        if (!json.has(field))
            return;

        if (json.isNull(field))
        {
            draft.clear(field);
            return;
        }

        switch (field)
        {
            case DATE -> draft.date = json.dateTime(DATE);
            case NOTE -> draft.note = json.optString(NOTE);
            case EX_DATE -> {
                var exDate = json.date(EX_DATE);
                draft.exDate = exDate != null ? exDate.atStartOfDay() : null;
            }
            case CASH_ACCOUNT -> draft.cashAccount = account(client, json, CASH_ACCOUNT);
            case FROM_CASH_ACCOUNT -> draft.fromCashAccount = account(client, json, FROM_CASH_ACCOUNT);
            case TO_CASH_ACCOUNT -> draft.toCashAccount = account(client, json, TO_CASH_ACCOUNT);
            case INVESTMENT_ACCOUNT -> draft.investmentAccount = portfolio(client, json, INVESTMENT_ACCOUNT);
            case FROM_INVESTMENT_ACCOUNT -> draft.fromInvestmentAccount = portfolio(client, json,
                            FROM_INVESTMENT_ACCOUNT);
            case TO_INVESTMENT_ACCOUNT -> draft.toInvestmentAccount = portfolio(client, json, TO_INVESTMENT_ACCOUNT);
            case INSTRUMENT -> draft.instrument = security(client, json, INSTRUMENT);
            case SHARES -> draft.shares = toLong(json.decimal(SHARES, Values.Share.precision()), true);
            case QUOTE -> draft.quote = json.decimal(QUOTE, Values.Quote.precision());
            case EXCHANGE_RATE -> draft.exchangeRate = json.decimal(EXCHANGE_RATE, RATE_SCALE);
            case CURRENCY -> {
                var code = json.optString(CURRENCY);
                if (code != null && CurrencyUnit.getInstance(code) == null)
                    json.add(new ApiException.FieldError(CURRENCY, "unknown-currency", MessageFormat.format("{0} is not a known currency", code)));
                else
                    draft.currency = code;
            }
            case GROSS_VALUE, AMOUNT, TARGET_AMOUNT, FEES, TAXES, FOREX_FEES, FOREX_TAXES -> draft.setAmount(field,
                            toLong(json.decimal(field, Values.Amount.precision()), false));
            default -> throw new IllegalArgumentException(field);
        }
    }

    private static Long toLong(BigDecimal value, boolean shares)
    {
        if (value == null)
            return null;
        return shares ? Amounts.toShares(value) : Amounts.toAmount(value);
    }

    private static Account account(Client client, Json json, String field)
    {
        var uuid = json.optString(field);
        if (uuid == null)
            return null;

        var account = client.getAccounts().stream().filter(a -> a.getUUID().equals(uuid)).findFirst();
        if (account.isEmpty())
            json.add(new ApiException.FieldError(field, "unknown-reference", MessageFormat.format("there is no cash account {0}", uuid)));
        return account.orElse(null);
    }

    private static Portfolio portfolio(Client client, Json json, String field)
    {
        var uuid = json.optString(field);
        if (uuid == null)
            return null;

        var portfolio = client.getPortfolios().stream().filter(p -> p.getUUID().equals(uuid)).findFirst();
        if (portfolio.isEmpty())
            json.add(new ApiException.FieldError(field, "unknown-reference", MessageFormat.format("there is no investment account {0}", uuid)));
        return portfolio.orElse(null);
    }

    private static Security security(Client client, Json json, String field)
    {
        var uuid = json.optString(field);
        if (uuid == null)
            return null;

        var security = client.getSecurities().stream().filter(s -> s.getUUID().equals(uuid)).findFirst();
        if (security.isEmpty())
            json.add(new ApiException.FieldError(field, "unknown-reference", MessageFormat.format("there is no instrument {0}", uuid)));
        return security.orElse(null);
    }

    private static void checkSameCurrency(Json json, String field, Account before, Account after)
    {
        if (before != null && after != null && before != after
                        && !before.getCurrencyCode().equals(after.getCurrencyCode()))
            json.add(new ApiException.FieldError(field, "currency-mismatch", MessageFormat.format("{0} must be a cash account in {1}, the currency of the transaction", field, before.getCurrencyCode())));
    }

    /** the currency pair an exchange rate of the draft converts, or null */
    private static String ratePair(Kind kind, Draft draft)
    {
        var instrument = draft.instrument != null ? draft.instrument.getCurrencyCode() : null;
        return switch (kind)
        {
            case BUY_SELL, DIVIDEND, CASH -> instrument + "/"
                            + (draft.cashAccount != null ? draft.cashAccount.getCurrencyCode() : null);
            case DELIVERY -> instrument + "/" + draft.currency;
            case CASH_TRANSFER, SECURITY_TRANSFER -> null;
        };
    }

    private static Spec compute(Context context, Draft draft, boolean monetary, TransactionPair<?> existing)
    {
        try
        {
            return switch (TransactionTypes.kindOf(draft.type))
            {
                case BUY_SELL -> buySell(context, draft, monetary, existing);
                case DELIVERY -> delivery(context, draft, monetary, existing);
                case DIVIDEND -> dividend(context, draft, monetary, existing);
                case CASH -> cash(context, draft, monetary, existing);
                case CASH_TRANSFER -> cashTransfer(context, draft, monetary, existing);
                case SECURITY_TRANSFER -> securityTransfer(context, draft, monetary, existing);
            };
        }
        catch (ArithmeticException e)
        {
            // an amount beyond the range of the model's fixed-point numbers
            context.error(new ApiException.FieldError(AMOUNT, "invalid-value", "the amounts are too large"));
            return null;
        }
    }

    private static Spec buySell(Context context, Draft draft, boolean monetary, TransactionPair<?> existing)
    {
        var portfolio = required(context, INVESTMENT_ACCOUNT, draft.investmentAccount);
        var account = required(context, CASH_ACCOUNT, draft.cashAccount);
        var security = required(context, INSTRUMENT, draft.instrument);
        var date = required(context, DATE, draft.date);
        var type = TransactionTypes.portfolioType(draft.type);
        var accountType = AccountTransaction.Type.valueOf(type.name());

        if (!monetary)
        {
            if (portfolio == null || account == null || security == null || date == null)
                return null;

            var linked = TransactionsHandler.linked(existing).getTransaction();
            return new Spec(Kind.BUY_SELL, date, security, draft.note, null, keep(existing.getTransaction(), portfolio),
                            keep(linked, account));
        }

        var currency = account != null ? account.getCurrencyCode() : null;
        var computed = security != null && currency != null && date != null
                        ? securityAmounts(context, draft, type, security.getCurrencyCode(), currency, date)
                        : null;
        if (computed == null || portfolio == null)
            return null;

        var linked = existing != null ? TransactionsHandler.linked(existing).getTransaction() : null;
        return new Spec(Kind.BUY_SELL, date, security, draft.note, null,
                        new Leg(portfolio, type, currency, computed.total(), draft.shares, computed.units()),
                        new Leg(account, accountType, currency, computed.total(), linked != null ? linked.getShares() : 0,
                                        linked != null ? units(linked) : List.of()));
    }

    private static Spec delivery(Context context, Draft draft, boolean monetary, TransactionPair<?> existing)
    {
        var portfolio = required(context, INVESTMENT_ACCOUNT, draft.investmentAccount);
        var security = required(context, INSTRUMENT, draft.instrument);
        var date = required(context, DATE, draft.date);
        var type = TransactionTypes.portfolioType(draft.type);

        // as the delivery dialog: the currency of the reference cash account
        // unless given explicitly
        var currency = draft.currency;
        if (currency == null && portfolio != null && portfolio.getReferenceAccount() != null)
            currency = portfolio.getReferenceAccount().getCurrencyCode();
        if (currency == null && portfolio != null && !context.failed(CURRENCY))
            context.error(new ApiException.FieldError(CURRENCY, "required", "currency is required, the investment account has no reference cash account"));

        if (!monetary)
        {
            if (portfolio == null || security == null || date == null)
                return null;
            return new Spec(Kind.DELIVERY, date, security, draft.note, null, keep(existing.getTransaction(), portfolio),
                            null);
        }

        var computed = security != null && currency != null && date != null
                        ? securityAmounts(context, draft, type, security.getCurrencyCode(), currency, date)
                        : null;
        if (computed == null || portfolio == null)
            return null;

        return new Spec(Kind.DELIVERY, date, security, draft.note, null,
                        new Leg(portfolio, type, currency, computed.total(), draft.shares, computed.units()), null);
    }

    /**
     * The total and the units of a buy, sell or delivery, as
     * {@code AbstractSecurityTransactionModel} computes and validates them.
     */
    private static Computed securityAmounts(Context context, Draft draft, PortfolioTransaction.Type type,
                    String securityCurrency, String currency, LocalDateTime date)
    {
        if (draft.shares == null)
            required(context, SHARES, null);
        else if (draft.shares <= 0)
            context.error(new ApiException.FieldError(SHARES, "must-be-positive", "shares must be greater than zero"));

        nonNegative(context, QUOTE, draft.quote != null ? draft.quote.signum() : 0);
        for (var field : List.of(GROSS_VALUE, AMOUNT, FEES, TAXES, FOREX_FEES, FOREX_TAXES))
            nonNegative(context, field, Long.signum(draft.amount(field)));

        var fx = !securityCurrency.equals(currency);
        forexOnlyWithForeignCurrency(context, draft, fx, securityCurrency, currency);
        var rate = exchangeRate(context, draft.exchangeRate, securityCurrency, currency, date);

        if (draft.grossValue == null && draft.quote == null && draft.amount == null && !context.failed(GROSS_VALUE)
                        && !context.failed(QUOTE) && !context.failed(AMOUNT))
            context.error(new ApiException.FieldError(GROSS_VALUE, "required", "one of grossValue, quote or amount is required"));

        if (context.json().hasErrors() || rate == null)
            return null;

        long shares = draft.shares;
        long fees = draft.amount(FEES);
        long taxes = draft.amount(TAXES);
        long forexFees = fx ? draft.amount(FOREX_FEES) : 0;
        long forexTaxes = fx ? draft.amount(FOREX_TAXES) : 0;
        long feesAndTaxes = fees + taxes + Amounts.convert(forexFees + forexTaxes, rate);
        var inbound = type == PortfolioTransaction.Type.BUY || type == PortfolioTransaction.Type.DELIVERY_INBOUND;

        long gross;
        long converted;
        long total;

        if (draft.grossValue != null || draft.quote != null)
        {
            gross = draft.grossValue != null ? draft.grossValue : Amounts.grossValue(shares, draft.quote);
            if (draft.grossValue != null && draft.quote != null
                            && !Amounts.isGrossValueWithinQuoteTolerance(shares, draft.quote, gross))
                context.error(new ApiException.FieldError(GROSS_VALUE, "gross-mismatch", MessageFormat.format("grossValue {0} does not match shares x quote ({1})", Amounts.amount(gross).toPlainString(), Amounts.amount(Amounts.grossValue(shares, draft.quote)).toPlainString())));

            converted = Amounts.convert(gross, rate);
            total = inbound ? converted + feesAndTaxes : Math.max(0, converted - feesAndTaxes);

            // an explicit total wins if it is plausible - as in the dialog,
            // where typing the total recomputes the converted gross value
            if (draft.amount != null && draft.amount != total)
            {
                long derived = inbound ? Math.max(0, draft.amount - feesAndTaxes) : draft.amount + feesAndTaxes;
                var plausible = fx ? Amounts.isConvertedValueWithinRateTolerance(gross, rate, derived)
                                : draft.grossValue == null
                                                && Amounts.isGrossValueWithinQuoteTolerance(shares, draft.quote, derived);
                if (!plausible)
                {
                    context.error(new ApiException.FieldError(AMOUNT, "total-mismatch", MessageFormat.format("amount {0} does not match the computed total {1}", Amounts.amount(draft.amount).toPlainString(), Amounts.amount(total).toPlainString())));
                }
                else
                {
                    total = draft.amount;
                    converted = derived;
                    if (!fx)
                        gross = derived;
                }
            }
        }
        else
        {
            total = draft.amount;
            converted = inbound ? Math.max(0, total - feesAndTaxes) : total + feesAndTaxes;
            gross = fx ? Amounts.convertBack(converted, rate) : converted;
        }

        if ((gross == 0 || converted == 0) && type != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            context.error(new ApiException.FieldError(GROSS_VALUE, "must-be-positive", "the gross value must be greater than zero"));
        else if (total == 0 && type == PortfolioTransaction.Type.SELL)
            context.error(new ApiException.FieldError(AMOUNT, "zero-total-use-delivery", "a sale with a total of zero cannot be booked, book an outbound delivery instead"));
        else if (total == 0 && type != PortfolioTransaction.Type.DELIVERY_OUTBOUND)
            context.error(new ApiException.FieldError(AMOUNT, "must-be-positive", "the total must be greater than zero"));

        var units = new ArrayList<Transaction.Unit>();
        if (fees != 0)
            units.add(new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of(currency, fees)));
        if (taxes != 0)
            units.add(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(currency, taxes)));
        if (fx)
        {
            if (forexFees != 0)
                units.add(forexUnit(context, FOREX_FEES, Transaction.Unit.Type.FEE,
                                Money.of(currency, Amounts.convert(forexFees, rate)),
                                Money.of(securityCurrency, forexFees), rate));
            if (forexTaxes != 0)
                units.add(forexUnit(context, FOREX_TAXES, Transaction.Unit.Type.TAX,
                                Money.of(currency, Amounts.convert(forexTaxes, rate)),
                                Money.of(securityCurrency, forexTaxes), rate));
            units.add(forexUnit(context, EXCHANGE_RATE, Transaction.Unit.Type.GROSS_VALUE,
                            Money.of(currency, converted), Money.of(securityCurrency, gross), rate));
        }

        return context.json().hasErrors() ? null : new Computed(total, units);
    }

    private static Spec dividend(Context context, Draft draft, boolean monetary, TransactionPair<?> existing)
    {
        var account = required(context, CASH_ACCOUNT, draft.cashAccount);
        var security = required(context, INSTRUMENT, draft.instrument);
        var date = required(context, DATE, draft.date);

        // as the dividend dialog: the ex-date must not be after the payment
        if (draft.exDate != null && date != null && draft.exDate.toLocalDate().isAfter(date.toLocalDate()))
            context.error(new ApiException.FieldError(EX_DATE, "ex-date-after-date", "exDate must not be after date"));

        if (account == null || security == null || date == null)
            return null;

        if (!monetary)
            return new Spec(Kind.DIVIDEND, date, security, draft.note, draft.exDate,
                            keep(existing.getTransaction(), account), null);

        // the dividend dialog allows 0 shares
        if (draft.shares != null && draft.shares < 0)
            context.error(new ApiException.FieldError(SHARES, "must-be-positive", "shares must not be negative"));
        for (var field : List.of(GROSS_VALUE, AMOUNT, FEES, TAXES, FOREX_FEES, FOREX_TAXES))
            nonNegative(context, field, Long.signum(draft.amount(field)));

        var currency = account.getCurrencyCode();
        var fx = !security.getCurrencyCode().equals(currency);
        forexOnlyWithForeignCurrency(context, draft, fx, security.getCurrencyCode(), currency);
        var rate = exchangeRate(context, draft.exchangeRate, security.getCurrencyCode(), currency, date);

        if (draft.grossValue == null && draft.amount == null && !context.failed(GROSS_VALUE)
                        && !context.failed(AMOUNT))
            context.error(new ApiException.FieldError(GROSS_VALUE, "required", "grossValue or amount is required"));

        if (context.json().hasErrors() || rate == null)
            return null;

        long fees = draft.amount(FEES);
        long taxes = draft.amount(TAXES);
        long forexFees = fx ? draft.amount(FOREX_FEES) : 0;
        long forexTaxes = fx ? draft.amount(FOREX_TAXES) : 0;
        long feesAndTaxes = fees + taxes + Amounts.convert(forexFees + forexTaxes, rate);

        long gross = draft.grossValue != null ? draft.grossValue : draft.amount + feesAndTaxes;
        long total = Math.max(0, gross - feesAndTaxes);

        if (draft.grossValue != null && draft.amount != null && draft.amount != total)
            context.error(new ApiException.FieldError(AMOUNT, "total-mismatch", MessageFormat.format("amount {0} does not match the computed total {1}", Amounts.amount(draft.amount).toPlainString(), Amounts.amount(total).toPlainString())));
        if (gross == 0)
            context.error(new ApiException.FieldError(GROSS_VALUE, "must-be-positive", "the gross value must be greater than zero"));

        var units = new ArrayList<Transaction.Unit>();
        if (fees != 0)
            units.add(new Transaction.Unit(Transaction.Unit.Type.FEE, Money.of(currency, fees)));
        if (taxes != 0)
            units.add(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(currency, taxes)));
        if (fx)
        {
            var securityCurrency = security.getCurrencyCode();
            units.add(forexUnit(context, EXCHANGE_RATE, Transaction.Unit.Type.GROSS_VALUE, Money.of(currency, gross),
                            Money.of(securityCurrency, Amounts.convertBack(gross, rate)), rate));
            if (forexFees != 0)
                units.add(forexUnit(context, FOREX_FEES, Transaction.Unit.Type.FEE,
                                Money.of(currency, Amounts.convert(forexFees, rate)),
                                Money.of(securityCurrency, forexFees), rate));
            if (forexTaxes != 0)
                units.add(forexUnit(context, FOREX_TAXES, Transaction.Unit.Type.TAX,
                                Money.of(currency, Amounts.convert(forexTaxes, rate)),
                                Money.of(securityCurrency, forexTaxes), rate));
        }

        if (context.json().hasErrors())
            return null;

        var shares = draft.shares != null ? draft.shares : 0;
        return new Spec(Kind.DIVIDEND, date, security, draft.note, draft.exDate,
                        new Leg(account, AccountTransaction.Type.DIVIDENDS, currency, total, shares, units), null);
    }

    private static Spec cash(Context context, Draft draft, boolean monetary, TransactionPair<?> existing)
    {
        var account = required(context, CASH_ACCOUNT, draft.cashAccount);
        var date = required(context, DATE, draft.date);
        var type = TransactionTypes.accountType(draft.type);
        var security = draft.instrument;

        if (account == null || date == null)
            return null;

        // an ex-date is kept, but can only be edited on a dividend
        if (!monetary)
            return new Spec(Kind.CASH, date, security, draft.note, draft.exDate,
                            keep(existing.getTransaction(), account), null);

        if (draft.amount == null)
            required(context, AMOUNT, null);
        nonNegative(context, AMOUNT, Long.signum(draft.amount(AMOUNT)));
        nonNegative(context, TAXES, Long.signum(draft.amount(TAXES)));

        var currency = account.getCurrencyCode();
        var securityCurrency = security != null ? security.getCurrencyCode() : currency;
        var rate = exchangeRate(context, draft.exchangeRate, securityCurrency, currency, date);

        if (context.json().hasErrors() || rate == null)
            return null;

        long amount = draft.amount;
        long taxes = draft.amount(TAXES);
        long gross = amount + taxes;

        if (gross == 0)
            context.error(new ApiException.FieldError(AMOUNT, "must-be-positive", "amount must be greater than zero"));

        var units = new ArrayList<Transaction.Unit>();
        if (taxes != 0)
            units.add(new Transaction.Unit(Transaction.Unit.Type.TAX, Money.of(currency, taxes)));
        if (!securityCurrency.equals(currency))
            units.add(forexUnit(context, EXCHANGE_RATE, Transaction.Unit.Type.GROSS_VALUE, Money.of(currency, gross),
                            Money.of(securityCurrency, Amounts.convertBack(gross, rate)), rate));

        if (context.json().hasErrors())
            return null;

        return new Spec(Kind.CASH, date, security, draft.note, draft.exDate,
                        new Leg(account, type, currency, amount, 0, units), null);
    }

    private static Spec cashTransfer(Context context, Draft draft, boolean monetary, TransactionPair<?> existing)
    {
        var from = required(context, FROM_CASH_ACCOUNT, draft.fromCashAccount);
        var to = required(context, TO_CASH_ACCOUNT, draft.toCashAccount);
        var date = required(context, DATE, draft.date);

        if (from != null && from == to)
            context.error(new ApiException.FieldError(TO_CASH_ACCOUNT, "same-account", "fromCashAccount and toCashAccount must be different accounts"));

        if (from == null || to == null || date == null)
            return null;

        var target = existing != null ? TransactionsHandler.linked(existing).getTransaction() : null;

        if (!monetary)
            return new Spec(Kind.CASH_TRANSFER, date, null, draft.note, null, keep(existing.getTransaction(), from),
                            keep(target, to));

        if (draft.amount == null)
            required(context, AMOUNT, null);
        else if (draft.amount <= 0)
            context.error(new ApiException.FieldError(AMOUNT, "must-be-positive", "amount must be greater than zero"));

        var fx = !from.getCurrencyCode().equals(to.getCurrencyCode());
        if (fx && draft.targetAmount == null && !context.failed(TARGET_AMOUNT))
            context.error(new ApiException.FieldError(TARGET_AMOUNT, "required", MessageFormat.format("targetAmount is required, the accounts have different currencies ({0}, {1})", from.getCurrencyCode(), to.getCurrencyCode())));
        else if (fx && draft.targetAmount != null && draft.targetAmount <= 0)
            context.error(new ApiException.FieldError(TARGET_AMOUNT, "must-be-positive", "targetAmount must be greater than zero"));
        else if (!fx && draft.targetAmount != null && !draft.targetAmount.equals(draft.amount))
            context.error(new ApiException.FieldError(TARGET_AMOUNT, "currency-mismatch", "targetAmount only applies to a transfer between accounts in different currencies"));

        if (context.json().hasErrors())
            return null;

        long amount = draft.amount;
        long targetAmount = fx ? draft.targetAmount : amount;

        // as the transfer dialog: the outbound leg carries the target amount
        // as forex, converted with the inverse of target / source
        var units = new ArrayList<Transaction.Unit>();
        if (fx)
        {
            var rate = BigDecimal.valueOf(targetAmount).divide(BigDecimal.valueOf(amount), RATE_SCALE,
                            RoundingMode.HALF_UP);
            units.add(forexUnit(context, TARGET_AMOUNT, Transaction.Unit.Type.GROSS_VALUE,
                            Money.of(from.getCurrencyCode(), amount), Money.of(to.getCurrencyCode(), targetAmount),
                            Amounts.inverseRate(rate)));
        }

        if (context.json().hasErrors())
            return null;

        return new Spec(Kind.CASH_TRANSFER, date, null, draft.note, null,
                        new Leg(from, AccountTransaction.Type.TRANSFER_OUT, from.getCurrencyCode(), amount, 0, units),
                        new Leg(to, AccountTransaction.Type.TRANSFER_IN, to.getCurrencyCode(), targetAmount, 0,
                                        target != null ? units(target) : List.of()));
    }

    private static Spec securityTransfer(Context context, Draft draft, boolean monetary,
                    TransactionPair<?> existing)
    {
        var from = required(context, FROM_INVESTMENT_ACCOUNT, draft.fromInvestmentAccount);
        var to = required(context, TO_INVESTMENT_ACCOUNT, draft.toInvestmentAccount);
        var security = required(context, INSTRUMENT, draft.instrument);
        var date = required(context, DATE, draft.date);

        if (from != null && from == to)
            context.error(new ApiException.FieldError(TO_INVESTMENT_ACCOUNT, "same-investment-account", "fromInvestmentAccount and toInvestmentAccount must be different investment accounts"));

        if (from == null || to == null || security == null || date == null)
            return null;

        var target = existing != null ? TransactionsHandler.linked(existing).getTransaction() : null;

        if (!monetary)
            return new Spec(Kind.SECURITY_TRANSFER, date, security, draft.note, null,
                            keep(existing.getTransaction(), from), keep(target, to));

        if (draft.shares == null)
            required(context, SHARES, null);
        else if (draft.shares <= 0)
            context.error(new ApiException.FieldError(SHARES, "must-be-positive", "shares must be greater than zero"));
        nonNegative(context, QUOTE, draft.quote != null ? draft.quote.signum() : 0);
        nonNegative(context, AMOUNT, Long.signum(draft.amount(AMOUNT)));
        if (draft.amount == null && draft.quote == null && !context.failed(AMOUNT) && !context.failed(QUOTE))
            context.error(new ApiException.FieldError(AMOUNT, "required", "amount or quote is required"));

        if (context.json().hasErrors())
            return null;

        long shares = draft.shares;
        long amount = draft.amount != null ? draft.amount : Amounts.grossValue(shares, draft.quote);

        if (draft.amount != null && draft.quote != null
                        && !Amounts.isGrossValueWithinQuoteTolerance(shares, draft.quote, amount))
            context.error(new ApiException.FieldError(AMOUNT, "gross-mismatch", MessageFormat.format("amount {0} does not match shares x quote ({1})", Amounts.amount(amount).toPlainString(), Amounts.amount(Amounts.grossValue(shares, draft.quote)).toPlainString())));
        else if (amount == 0)
            context.error(new ApiException.FieldError(AMOUNT, "must-be-positive", "amount must be greater than zero"));

        if (context.json().hasErrors())
            return null;

        // as the security transfer dialog: both legs in the instrument currency
        var currency = security.getCurrencyCode();
        var source = existing != null ? existing.getTransaction() : null;
        return new Spec(Kind.SECURITY_TRANSFER, date, security, draft.note, null,
                        new Leg(from, PortfolioTransaction.Type.TRANSFER_OUT, currency, amount, shares,
                                        source != null ? units(source) : List.of()),
                        new Leg(to, PortfolioTransaction.Type.TRANSFER_IN, currency, amount, shares,
                                        target != null ? units(target) : List.of()));
    }

    /**
     * The exchange rate from the instrument currency into the transaction
     * currency: 1 for the same currency, else the given rate, else the file's
     * rate for the date (as the dialogs suggest it for a new transaction).
     * Null (and an error recorded) if there is none.
     */
    private static BigDecimal exchangeRate(Context context, BigDecimal given, String from, String to,
                    LocalDateTime date)
    {
        if (from.equals(to))
        {
            if (given != null && given.compareTo(BigDecimal.ONE) != 0)
            {
                context.error(new ApiException.FieldError(EXCHANGE_RATE, "currency-mismatch", MessageFormat.format("exchangeRate only applies if the instrument currency differs from the transaction currency ({0})", to)));
                return null;
            }
            return context.failed(EXCHANGE_RATE) ? null : BigDecimal.ONE;
        }

        if (context.failed(EXCHANGE_RATE))
            return null;

        if (given != null)
        {
            if (given.signum() > 0)
                return given;
            context.error(new ApiException.FieldError(EXCHANGE_RATE, "must-be-positive", "exchangeRate must be greater than zero"));
            return null;
        }

        var series = context.factory().getTimeSeries(from, to);
        Optional<ExchangeRate> rate = series != null ? series.lookupRate(date.toLocalDate()) : Optional.empty();
        if (rate.isEmpty() || rate.get().getValue().signum() <= 0)
        {
            context.error(new ApiException.FieldError(EXCHANGE_RATE, "exchange-rate-required", MessageFormat.format("no {0}/{1} exchange rate is available for {2}, exchangeRate is required", from, to, date.toLocalDate())));
            return null;
        }
        return rate.get().getValue();
    }

    private static void forexOnlyWithForeignCurrency(Context context, Draft draft, boolean fx, String securityCurrency,
                    String currency)
    {
        if (fx)
            return;

        for (var field : List.of(FOREX_FEES, FOREX_TAXES))
        {
            if (draft.amount(field) != 0)
                context.error(new ApiException.FieldError(field, "currency-mismatch", MessageFormat.format("{0} only applies if the instrument currency ({1}) differs from the transaction currency ({2})", field, securityCurrency, currency)));
        }
    }

    /** a unit with forex amount; an inconsistent one is reported against the field */
    private static Transaction.Unit forexUnit(Context context, String field, Transaction.Unit.Type type, Money amount,
                    Money forex, BigDecimal rate)
    {
        try
        {
            return new Transaction.Unit(type, amount, forex, rate);
        }
        catch (IllegalArgumentException e)
        {
            context.error(new ApiException.FieldError(field, "invalid-value", MessageFormat.format("{0} is not consistent with the exchange rate: {1}", field, e.getMessage())));
            return null;
        }
    }

    private static <T> T required(Context context, String field, T value)
    {
        if (value == null && !context.failed(field))
            context.error(new ApiException.FieldError(field, "required", MessageFormat.format("{0} is required", field)));
        return value;
    }

    private static void nonNegative(Context context, String field, int signum)
    {
        if (signum < 0)
            context.error(new ApiException.FieldError(field, "must-be-positive", MessageFormat.format("{0} must not be negative", field)));
    }

    /** a leg whose amounts and units stay as they are, possibly with a new owner */
    private static Leg keep(Transaction transaction, TransactionOwner<?> owner)
    {
        Enum<?> type = transaction instanceof AccountTransaction t ? t.getType()
                        : ((PortfolioTransaction) transaction).getType();
        return new Leg(owner, type, transaction.getCurrencyCode(), transaction.getAmount(), transaction.getShares(),
                        units(transaction));
    }

    private static List<Transaction.Unit> units(Transaction transaction)
    {
        return transaction.getUnits().toList();
    }

    /** builds detached model objects: nothing is added to an account */
    private static TransactionPair<?> build(Spec spec)
    {
        var main = spec.main();
        var other = spec.other();

        return switch (spec.kind())
        {
            case BUY_SELL -> {
                var entry = new BuySellEntry((Portfolio) main.owner(), (Account) other.owner());
                write(entry.getPortfolioTransaction(), spec, main);
                write(entry.getAccountTransaction(), spec, other);
                yield TransactionsHandler.pair(main.owner(), entry.getPortfolioTransaction());
            }
            case DELIVERY -> {
                var transaction = new PortfolioTransaction();
                write(transaction, spec, main);
                yield TransactionsHandler.pair(main.owner(), transaction);
            }
            case DIVIDEND, CASH -> {
                var transaction = new AccountTransaction();
                write(transaction, spec, main);
                yield TransactionsHandler.pair(main.owner(), transaction);
            }
            case CASH_TRANSFER -> {
                var entry = new AccountTransferEntry((Account) main.owner(), (Account) other.owner());
                write(entry.getSourceTransaction(), spec, main);
                write(entry.getTargetTransaction(), spec, other);
                yield TransactionsHandler.pair(main.owner(), entry.getSourceTransaction());
            }
            case SECURITY_TRANSFER -> {
                var entry = new PortfolioTransferEntry((Portfolio) main.owner(), (Portfolio) other.owner());
                write(entry.getSourceTransaction(), spec, main);
                write(entry.getTargetTransaction(), spec, other);
                yield TransactionsHandler.pair(main.owner(), entry.getSourceTransaction());
            }
        };
    }

    /** writes a leg: the currency first, because units must be in the transaction currency */
    private static void write(Transaction transaction, Spec spec, Leg leg)
    {
        transaction.setDateTime(spec.date());
        transaction.setCurrencyCode(leg.currency());
        transaction.setSecurity(spec.security());
        transaction.setShares(leg.shares());
        transaction.setAmount(leg.amount());
        transaction.setNote(spec.note());

        if (transaction instanceof AccountTransaction t)
        {
            t.setType((AccountTransaction.Type) leg.type());
            if (spec.kind() == Kind.DIVIDEND || spec.kind() == Kind.CASH)
                t.setExDate(spec.exDate());
        }
        else if (transaction instanceof PortfolioTransaction t)
        {
            t.setType((PortfolioTransaction.Type) leg.type());
        }

        transaction.clearUnits();
        leg.units().forEach(transaction::addUnit);
    }

    private static void setSource(Transaction transaction, String source)
    {
        if (transaction.getCrossEntry() != null)
            transaction.getCrossEntry().setSource(source);
        else
            transaction.setSource(source);
    }

    @SuppressWarnings("unchecked")
    private static TransactionOwner<Transaction> owner(TransactionOwner<?> owner)
    {
        return (TransactionOwner<Transaction>) owner;
    }

    /**
     * The field values of a transaction, in the vocabulary of the create
     * body; null means absent. A patch is merged into the draft of the
     * existing transaction.
     */
    /* package */ static final class Draft
    {
        String type;
        LocalDateTime date;
        String note;
        Account cashAccount;
        Portfolio investmentAccount;
        Security instrument;
        Account fromCashAccount;
        Account toCashAccount;
        Portfolio fromInvestmentAccount;
        Portfolio toInvestmentAccount;
        Long shares;
        BigDecimal quote;
        Long grossValue;
        Long amount;
        Long targetAmount;
        Long fees;
        Long taxes;
        Long forexFees;
        Long forexTaxes;
        BigDecimal exchangeRate;
        String currency;
        LocalDateTime exDate;

        /** an amount field, 0 if absent */
        long amount(String field)
        {
            var value = (Long) get(field);
            return value != null ? value : 0;
        }

        void setAmount(String field, Long value)
        {
            switch (field)
            {
                case GROSS_VALUE -> grossValue = value;
                case AMOUNT -> amount = value;
                case TARGET_AMOUNT -> targetAmount = value;
                case FEES -> fees = value;
                case TAXES -> taxes = value;
                case FOREX_FEES -> forexFees = value;
                case FOREX_TAXES -> forexTaxes = value;
                default -> throw new IllegalArgumentException(field);
            }
        }

        Object get(String field)
        {
            return switch (field)
            {
                case TYPE -> type;
                case DATE -> date;
                case NOTE -> note;
                case CASH_ACCOUNT -> cashAccount;
                case INVESTMENT_ACCOUNT -> investmentAccount;
                case INSTRUMENT -> instrument;
                case FROM_CASH_ACCOUNT -> fromCashAccount;
                case TO_CASH_ACCOUNT -> toCashAccount;
                case FROM_INVESTMENT_ACCOUNT -> fromInvestmentAccount;
                case TO_INVESTMENT_ACCOUNT -> toInvestmentAccount;
                case SHARES -> shares;
                case QUOTE -> quote;
                case GROSS_VALUE -> grossValue;
                case AMOUNT -> amount;
                case TARGET_AMOUNT -> targetAmount;
                case FEES -> fees;
                case TAXES -> taxes;
                case FOREX_FEES -> forexFees;
                case FOREX_TAXES -> forexTaxes;
                case EXCHANGE_RATE -> exchangeRate;
                case CURRENCY -> currency;
                case EX_DATE -> exDate;
                default -> throw new IllegalArgumentException(field);
            };
        }

        void clear(String field)
        {
            switch (field)
            {
                case DATE -> date = null;
                case NOTE -> note = null;
                case CASH_ACCOUNT -> cashAccount = null;
                case INVESTMENT_ACCOUNT -> investmentAccount = null;
                case INSTRUMENT -> instrument = null;
                case FROM_CASH_ACCOUNT -> fromCashAccount = null;
                case TO_CASH_ACCOUNT -> toCashAccount = null;
                case FROM_INVESTMENT_ACCOUNT -> fromInvestmentAccount = null;
                case TO_INVESTMENT_ACCOUNT -> toInvestmentAccount = null;
                case SHARES -> shares = null;
                case QUOTE -> quote = null;
                case EXCHANGE_RATE -> exchangeRate = null;
                case CURRENCY -> currency = null;
                case EX_DATE -> exDate = null;
                default -> setAmount(field, null);
            }
        }

        /** whether the field has a value that matters: not absent, not zero */
        boolean isSet(String field)
        {
            var value = get(field);
            if (value instanceof Long l)
                return l != 0;
            if (value instanceof BigDecimal d)
                return EXCHANGE_RATE.equals(field) ? d.compareTo(BigDecimal.ONE) != 0 : d.signum() != 0;
            return value != null;
        }

        /** a human-readable value for the application log, null if absent */
        String describe(String field)
        {
            var value = get(field);
            if (value == null)
                return null;
            if (value instanceof Account a)
                return a.getName();
            if (value instanceof Portfolio p)
                return p.getName();
            if (value instanceof Security s)
                return s.getName();
            if (value instanceof Long l)
                return (SHARES.equals(field) ? Amounts.shares(l) : Amounts.amount(l)).stripTrailingZeros()
                                .toPlainString();
            if (value instanceof BigDecimal d)
                return d.toPlainString();
            return value.toString();
        }

        /** the draft of an existing transaction, given as the leg the list reports */
        static Draft extract(Kind kind, TransactionPair<?> pair)
        {
            var transaction = pair.getTransaction();
            var crossEntry = transaction.getCrossEntry();

            var draft = new Draft();
            draft.date = transaction.getDateTime();
            draft.note = transaction.getNote();

            switch (kind)
            {
                case BUY_SELL -> {
                    draft.type = TransactionTypes.wireType(transaction);
                    draft.investmentAccount = (Portfolio) pair.getOwner();
                    draft.cashAccount = (Account) crossEntry.getCrossOwner(transaction);
                    draft.instrument = transaction.getSecurity();
                    draft.shares = transaction.getShares();
                    draft.extractSecurityUnits((PortfolioTransaction) transaction);
                }
                case DELIVERY -> {
                    draft.type = TransactionTypes.wireType(transaction);
                    draft.investmentAccount = (Portfolio) pair.getOwner();
                    draft.instrument = transaction.getSecurity();
                    draft.shares = transaction.getShares();
                    draft.currency = transaction.getCurrencyCode();
                    draft.extractSecurityUnits((PortfolioTransaction) transaction);
                }
                case DIVIDEND -> {
                    var t = (AccountTransaction) transaction;
                    draft.type = TransactionTypes.wireType(t);
                    draft.cashAccount = (Account) pair.getOwner();
                    draft.instrument = t.getSecurity();
                    draft.shares = t.getShares();
                    draft.exDate = t.getExDate();
                    draft.extractDividendUnits(t);
                }
                case CASH -> {
                    var t = (AccountTransaction) transaction;
                    draft.type = TransactionTypes.wireType(t);
                    draft.cashAccount = (Account) pair.getOwner();
                    draft.instrument = t.getSecurity();
                    draft.exDate = t.getExDate();
                    draft.amount = t.getAmount();
                    if (t.getType() == AccountTransaction.Type.INTEREST)
                        draft.taxes = t.getUnits().filter(u -> u.getType() == Transaction.Unit.Type.TAX)
                                        .mapToLong(u -> u.getAmount().getAmount()).sum();
                    t.getUnit(Transaction.Unit.Type.GROSS_VALUE).ifPresent(u -> draft.exchangeRate = u.getExchangeRate());
                }
                case CASH_TRANSFER -> {
                    var target = crossEntry.getCrossTransaction(transaction);
                    draft.type = TransactionTypes.CASH_TRANSFER;
                    draft.fromCashAccount = (Account) pair.getOwner();
                    draft.toCashAccount = (Account) crossEntry.getCrossOwner(transaction);
                    draft.amount = transaction.getAmount();
                    if (!transaction.getCurrencyCode().equals(target.getCurrencyCode()))
                        draft.targetAmount = target.getAmount();
                }
                case SECURITY_TRANSFER -> {
                    var target = crossEntry.getCrossTransaction(transaction);
                    draft.type = TransactionTypes.SECURITY_TRANSFER;
                    draft.fromInvestmentAccount = (Portfolio) pair.getOwner();
                    draft.toInvestmentAccount = (Portfolio) crossEntry.getCrossOwner(transaction);
                    draft.instrument = transaction.getSecurity();
                    draft.shares = transaction.getShares();
                    draft.amount = target.getAmount();
                }
            }

            return draft;
        }

        /** as {@code AbstractSecurityTransactionModel#fillFromTransaction} */
        private void extractSecurityUnits(PortfolioTransaction transaction)
        {
            fees = 0L;
            taxes = 0L;
            forexFees = 0L;
            forexTaxes = 0L;

            transaction.getUnits().forEach(unit -> {
                switch (unit.getType())
                {
                    case GROSS_VALUE -> {
                        grossValue = unit.getForex().getAmount();
                        exchangeRate = unit.getExchangeRate();
                    }
                    case FEE -> {
                        if (unit.getForex() != null)
                            forexFees += unit.getForex().getAmount();
                        else
                            fees += unit.getAmount().getAmount();
                    }
                    case TAX -> {
                        if (unit.getForex() != null)
                            forexTaxes += unit.getForex().getAmount();
                        else
                            taxes += unit.getAmount().getAmount();
                    }
                }
            });

            if (grossValue == null)
                grossValue = transaction.getGrossValueAmount();
        }

        /** as {@code AccountTransactionModel#presetFromSource} */
        private void extractDividendUnits(AccountTransaction transaction)
        {
            fees = 0L;
            taxes = 0L;
            forexFees = 0L;
            forexTaxes = 0L;

            transaction.getUnits().forEach(unit -> {
                switch (unit.getType())
                {
                    case GROSS_VALUE -> {
                        grossValue = unit.getAmount().getAmount();
                        exchangeRate = unit.getExchangeRate();
                    }
                    case FEE -> {
                        if (unit.getForex() != null)
                            forexFees += unit.getForex().getAmount();
                        else
                            fees += unit.getAmount().getAmount();
                    }
                    case TAX -> {
                        if (unit.getForex() != null)
                            forexTaxes += unit.getForex().getAmount();
                        else
                            taxes += unit.getAmount().getAmount();
                    }
                }
            });

            if (grossValue == null)
                grossValue = transaction.getGrossValueAmount();
        }
    }
}
