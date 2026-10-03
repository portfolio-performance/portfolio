package name.abuchen.portfolio.rest.internal;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.TransactionOwner;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.rest.Messages;

public final class TransactionsHandler
{
    /** the filters of the transaction list; every field is optional (null) */
    public record Filter(String from, String to, String type, String instrument, String cashAccount,
                    String investmentAccount)
    {
        public static final Filter NONE = new Filter(null, null, null, null, null, null);
    }

    private TransactionsHandler()
    {
    }

    /**
     * Every transaction across every account and investment account, newest
     * first. {@link Client#getAllTransactions()} already de-duplicates a
     * buy/sell pair (only the investment-account side survives) and a transfer
     * (only the outbound leg survives).
     */
    public static JsonElement list(Client client)
    {
        return list(client, Filter.NONE);
    }

    /**
     * The transaction list, narrowed by the filter. The account filters match
     * a transaction if either of its legs is booked on the account, so that a
     * buy shows up for its cash account although the list reports it as its
     * investment-account leg. All filters combine with AND.
     */
    public static JsonElement list(Client client, Filter filter)
    {
        return EntityJson.envelope(select(client, filter), EntityJson::toJson);
    }

    /** the transactions the filter selects, newest first; see {@link #list(Client, Filter)} */
    /* package */ static List<TransactionPair<?>> select(Client client, Filter filter)
    {
        var predicate = predicate(filter);

        var transactions = new ArrayList<>(client.getAllTransactions().stream().filter(predicate).toList());
        transactions.sort(TransactionPair.BY_DATE.reversed());
        return transactions;
    }

    /**
     * One transaction with its units and its linked leg. Either leg of a
     * buy/sell or a transfer resolves; the answer is always the leg the list
     * reports: the investment-account leg of a buy/sell and the outbound leg
     * of a transfer.
     */
    public static JsonElement get(Client client, String uuid)
    {
        return EntityJson.toJsonDetailed(find(client, uuid));
    }

    /**
     * The outcome of a create or update: the transaction in detail, and
     * whether the model was changed. A dry run, a replayed create (same
     * {@code clientRef}) and an update that changes nothing leave the model
     * untouched.
     */
    public record WriteResult(JsonObject entity, boolean changed)
    {
    }

    /**
     * Creates a transaction of any type, see {@link TransactionPlanner}. With
     * a {@code clientRef} that an earlier create already used, answers that
     * transaction ({@code replayed: true}) instead of creating a duplicate.
     * A dry run answers the fully resolved transaction ({@code dryRun: true})
     * without adding it. Must be called on the UI thread.
     */
    public static WriteResult create(WriteContext context, JsonObject body)
    {
        var client = context.client();

        var existing = IdempotencyIndex.findTransaction(client, context.clientRef());
        if (existing.isPresent())
        {
            var json = EntityJson.toJsonDetailed(canonical(existing.get()));
            json.addProperty("replayed", true); //$NON-NLS-1$
            if (context.dryRun())
                json.addProperty("dryRun", true); //$NON-NLS-1$
            return new WriteResult(json, false);
        }

        var plan = TransactionPlanner.plan(client, context.file().getExchangeRateProviderFactory(), body);

        if (context.dryRun())
        {
            var json = EntityJson.toJsonDetailed(plan.preview(context.clientRef()));
            json.addProperty("dryRun", true); //$NON-NLS-1$
            return new WriteResult(json, false);
        }

        var pair = plan.apply(context.clientRef());
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiTransactionCreated, TransactionTypes.wireType(pair.getTransaction()),
                        pair.getTransaction().getUUID(), context.file().getLabel());

        return new WriteResult(EntityJson.toJsonDetailed(pair), true);
    }

    /**
     * Updates a transaction with a JSON Merge Patch, see
     * {@link TransactionPlanner#planUpdate}. Either leg's UUID addresses a
     * buy/sell or transfer; both legs are kept consistent and keep their
     * UUIDs. Must be called on the UI thread.
     */
    public static WriteResult patch(WriteContext context, String uuid, JsonObject patch)
    {
        var client = context.client();
        var existing = find(client, uuid);

        var plan = TransactionPlanner.planUpdate(client, context.file().getExchangeRateProviderFactory(), existing,
                        patch);

        if (context.dryRun())
        {
            var json = plan.preview();
            json.addProperty("dryRun", true); //$NON-NLS-1$
            return new WriteResult(json, false);
        }

        if (plan.isNoop())
            return new WriteResult(EntityJson.toJsonDetailed(existing), false);

        var pair = plan.apply();
        client.markDirty();

        ChangeLog.recordChanges(plan.changes(), Messages.MsgApiTransactionUpdated,
                        TransactionTypes.wireType(pair.getTransaction()), pair.getTransaction().getUUID(),
                        context.file().getLabel());

        return new WriteResult(EntityJson.toJsonDetailed(pair), true);
    }

    /**
     * Deletes a transaction including the other leg of a buy/sell or transfer
     * and its links from investment plans, as the application does. A dry run
     * answers the legs that would be removed and changes nothing; a real
     * delete answers null. Must be called on the UI thread.
     */
    public static JsonObject delete(WriteContext context, String uuid)
    {
        var client = context.client();
        var pair = find(client, uuid);

        if (context.dryRun())
        {
            var removed = new JsonArray();
            removed.add(EntityJson.toJson(pair));
            var linked = linked(pair);
            if (linked != null)
                removed.add(EntityJson.toJson(linked));

            var json = new JsonObject();
            json.addProperty("dryRun", true); //$NON-NLS-1$
            json.add("removed", removed); //$NON-NLS-1$
            return json;
        }

        @SuppressWarnings("unchecked")
        var owner = (TransactionOwner<Transaction>) pair.getOwner();
        owner.deleteTransaction(pair.getTransaction(), client);
        client.markDirty();

        ChangeLog.recordEvent(Messages.MsgApiTransactionDeleted, TransactionTypes.wireType(pair.getTransaction()),
                        pair.getTransaction().getUUID(), context.file().getLabel());

        return null;
    }

    /**
     * The canonical pair of the transaction with the given UUID of either leg;
     * 404 if there is none.
     */
    public static TransactionPair<?> find(Client client, String uuid)
    {
        var found = Stream.concat(
                        client.getPortfolios().stream().flatMap(p -> p.getTransactions().stream().map(t -> pair(p, t))),
                        client.getAccounts().stream().flatMap(a -> a.getTransactions().stream().map(t -> pair(a, t))))
                        .filter(pair -> pair.getTransaction().getUUID().equals(uuid)) //
                        .findFirst().orElseThrow(ApiException::notFound);

        return canonical(found);
    }

    /**
     * The leg {@link Client#getAllTransactions()} reports for this
     * transaction: the investment-account leg of a buy/sell, the outbound leg
     * of a transfer, the transaction itself otherwise.
     */
    public static TransactionPair<?> canonical(TransactionPair<?> pair)
    {
        var transaction = pair.getTransaction();
        var crossEntry = transaction.getCrossEntry();
        if (crossEntry == null)
            return pair;

        var isHiddenLeg = transaction instanceof AccountTransaction t && (t.getType() == AccountTransaction.Type.BUY
                        || t.getType() == AccountTransaction.Type.SELL
                        || t.getType() == AccountTransaction.Type.TRANSFER_IN)
                        || transaction instanceof PortfolioTransaction p
                                        && p.getType() == PortfolioTransaction.Type.TRANSFER_IN;

        if (!isHiddenLeg)
            return pair;

        return pair(crossEntry.getCrossOwner(transaction), crossEntry.getCrossTransaction(transaction));
    }

    /** the other leg of a buy/sell or transfer, or null */
    public static TransactionPair<?> linked(TransactionPair<?> pair)
    {
        var transaction = pair.getTransaction();
        var crossEntry = transaction.getCrossEntry();
        if (crossEntry == null)
            return null;
        return pair(crossEntry.getCrossOwner(transaction), crossEntry.getCrossTransaction(transaction));
    }

    @SuppressWarnings("unchecked")
    /* package */ static TransactionPair<?> pair(TransactionOwner<?> owner, Transaction transaction)
    {
        return new TransactionPair<>((TransactionOwner<Transaction>) owner, transaction);
    }

    private static Predicate<TransactionPair<?>> predicate(Filter filter)
    {
        var errors = new ArrayList<ApiException.FieldError>();

        var from = parseDate("from", filter.from(), errors); //$NON-NLS-1$
        var to = parseDate("to", filter.to(), errors); //$NON-NLS-1$
        var types = parseTypes(filter.type(), errors);

        if (!errors.isEmpty())
            throw ApiException.badRequest(errors);

        if (from != null && to != null && from.isAfter(to))
            throw ApiException.badRequest(List.of(new ApiException.FieldError("from", "invalid-range", //$NON-NLS-1$ //$NON-NLS-2$
                            "from must not be after to"))); //$NON-NLS-1$

        Predicate<TransactionPair<?>> predicate = pair -> true;

        if (from != null)
            predicate = predicate.and(pair -> !pair.getTransaction().getDateTime().toLocalDate().isBefore(from));
        if (to != null)
            predicate = predicate.and(pair -> !pair.getTransaction().getDateTime().toLocalDate().isAfter(to));
        if (types != null)
            predicate = predicate.and(pair -> types.contains(EntityJson.wireType(pair.getTransaction())));
        if (filter.instrument() != null)
            predicate = predicate.and(pair -> pair.getTransaction().getSecurity() != null
                            && pair.getTransaction().getSecurity().getUUID().equals(filter.instrument()));
        if (filter.cashAccount() != null)
            predicate = predicate.and(pair -> involves(pair, Account.class, filter.cashAccount()));
        if (filter.investmentAccount() != null)
            predicate = predicate.and(pair -> involves(pair, Portfolio.class, filter.investmentAccount()));

        return predicate;
    }

    /** whether either leg of the transaction is booked on the owner with the given UUID */
    private static boolean involves(TransactionPair<?> pair, Class<?> ownerType, String uuid)
    {
        if (isOwner(pair.getOwner(), ownerType, uuid))
            return true;

        var linked = linked(pair);
        return linked != null && isOwner(linked.getOwner(), ownerType, uuid);
    }

    private static boolean isOwner(TransactionOwner<?> owner, Class<?> ownerType, String uuid)
    {
        if (!ownerType.isInstance(owner))
            return false;
        if (owner instanceof Account account)
            return account.getUUID().equals(uuid);
        if (owner instanceof Portfolio portfolio)
            return portfolio.getUUID().equals(uuid);
        return false;
    }

    private static LocalDate parseDate(String name, String value, List<ApiException.FieldError> errors)
    {
        if (value == null)
            return null;

        try
        {
            return LocalDate.parse(value);
        }
        catch (DateTimeParseException e)
        {
            errors.add(new ApiException.FieldError(name, "invalid-value", //$NON-NLS-1$
                            MessageFormat.format("{0} must be an ISO 8601 date (YYYY-MM-DD)", name))); //$NON-NLS-1$
            return null;
        }
    }

    /** a comma-separated list of wire types, or null for no type filter */
    private static Set<String> parseTypes(String value, List<ApiException.FieldError> errors)
    {
        if (value == null)
            return null;

        var types = new HashSet<String>();
        for (var type : Arrays.stream(value.split(",")).map(String::strip).toList()) //$NON-NLS-1$
        {
            if (!TransactionTypes.WIRE_TYPES.contains(type))
                errors.add(new ApiException.FieldError("type", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                                MessageFormat.format("{0} is not a transaction type", type))); //$NON-NLS-1$
            else
                types.add(type);
        }
        return types;
    }
}
