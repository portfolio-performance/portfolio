package name.abuchen.portfolio.rest.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.rest.Messages;

/**
 * Applies a stock split to an instrument, as the application's split wizard
 * does ({@code StockSplitModel#applyChanges}): records a {@code stock-split}
 * event with the ratio {@code new:old}, multiplies the shares of every
 * transaction of the instrument dated before the ex-date by {@code new/old},
 * and divides every historical price before the ex-date by the same ratio.
 * The latest price is left alone. Validation follows the wizard's first
 * page: an ex-date, both share counts positive, and different.
 */
public final class StockSplitAction
{
    private static final String KIND = "split"; //$NON-NLS-1$

    /** what a split with a {@code clientRef} answered, to replay it */
    private record Applied(SecurityEvent event, JsonObject response)
    {
    }

    /** a transaction leg and its shares after the split */
    private record Adjustment(TransactionPair<?> pair, long sharesAfter)
    {
    }

    private StockSplitAction()
    {
    }

    /**
     * Applies (or, on a dry run, previews) the split described by the body
     * {@code {exDate, newShares, oldShares, adjustTransactions, adjustPrices}}.
     * With a {@code clientRef} that an earlier split used, answers that split
     * ({@code replayed: true}) instead of splitting again. Must be called on
     * the UI thread.
     */
    public static JsonObject apply(WriteContext context, IdempotencyIndex idempotency, String uuid, JsonObject body)
    {
        var client = context.client();
        var security = SecuritiesHandler.find(client, uuid);

        var previous = idempotency.findObject(context.file().getPath(), KIND, context.clientRef())
                        .filter(Applied.class::isInstance).map(Applied.class::cast)
                        .filter(a -> security.getEvents().stream().anyMatch(e -> e == a.event()));
        if (previous.isPresent())
        {
            var json = previous.get().response().deepCopy();
            json.addProperty("replayed", true); //$NON-NLS-1$
            if (context.dryRun())
                json.addProperty("dryRun", true); //$NON-NLS-1$
            return json;
        }

        var json = new Json(body);
        json.ignore(WriteContext.CLIENT_REF_FIELD);
        var exDate = json.requireDate("exDate"); //$NON-NLS-1$
        var newShares = json.requireDecimal("newShares", Values.Share.precision()); //$NON-NLS-1$
        var oldShares = json.requireDecimal("oldShares", Values.Share.precision()); //$NON-NLS-1$
        var adjustTransactions = json.bool("adjustTransactions"); //$NON-NLS-1$
        var adjustPrices = json.bool("adjustPrices"); //$NON-NLS-1$
        json.rejectUnknownFields();

        if (newShares != null && newShares.signum() <= 0)
            json.add(new ApiException.FieldError("newShares", "must-be-positive", "newShares must be positive")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (oldShares != null && oldShares.signum() <= 0)
            json.add(new ApiException.FieldError("oldShares", "must-be-positive", "oldShares must be positive")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (newShares != null && oldShares != null && newShares.signum() > 0 && newShares.compareTo(oldShares) == 0)
            json.add(new ApiException.FieldError("newShares", "invalid-value", //$NON-NLS-1$ //$NON-NLS-2$
                            "newShares and oldShares must differ")); //$NON-NLS-1$
        json.throwIfErrors();

        var ratio = new Ratio(newShares, oldShares);
        var changeTransactions = adjustTransactions == null || adjustTransactions;
        var changePrices = adjustPrices == null || adjustPrices;

        var event = new SecurityEvent(exDate, SecurityEvent.Type.STOCK_SPLIT,
                        newShares.toPlainString() + ":" + oldShares.toPlainString()); //$NON-NLS-1$

        var adjustments = new ArrayList<Adjustment>();
        if (changeTransactions)
        {
            for (var pair : security.getTransactions(client))
            {
                var transaction = pair.getTransaction();
                if (transaction.getDateTime().toLocalDate().isBefore(exDate))
                    adjustments.add(new Adjustment(pair, ratio.newStock(transaction.getShares())));
            }
            adjustments.sort(Comparator.comparing(a -> a.pair().getTransaction().getDateTime()));
        }

        var prices = new ArrayList<SecurityPrice>();
        if (changePrices)
        {
            for (var price : security.getPrices())
            {
                if (price.getDate().isBefore(exDate))
                    prices.add(price);
            }
        }

        var response = response(security, event, changeTransactions, changePrices, adjustments, prices, ratio);

        if (context.dryRun())
        {
            response.addProperty("dryRun", true); //$NON-NLS-1$
            return response;
        }

        security.addEvent(event);
        for (var adjustment : adjustments)
            adjustment.pair().getTransaction().setShares(adjustment.sharesAfter());
        for (var price : prices)
            price.setValue(ratio.newQuote(price.getValue()));
        client.markDirty();

        idempotency.rememberObject(context.file().getPath(), KIND, context.clientRef(),
                        new Applied(event, response.deepCopy()));

        ChangeLog.recordEvent(Messages.MsgApiStockSplitApplied, event.getDetails(), security.getName(),
                        context.file().getLabel(), adjustments.size(), prices.size());

        return response;
    }

    private static JsonObject response(Security security, SecurityEvent event, boolean changeTransactions,
                    boolean changePrices, List<Adjustment> adjustments, List<SecurityPrice> prices, Ratio ratio)
    {
        var json = new JsonObject();
        json.addProperty("instrument", security.getUUID()); //$NON-NLS-1$
        json.add("event", EntityJson.toJson(event)); //$NON-NLS-1$
        json.addProperty("adjustTransactions", changeTransactions); //$NON-NLS-1$
        json.addProperty("adjustPrices", changePrices); //$NON-NLS-1$
        json.addProperty("transactionsAffected", adjustments.size()); //$NON-NLS-1$
        json.addProperty("pricesAffected", prices.size()); //$NON-NLS-1$

        var transactions = new JsonArray();
        for (var adjustment : adjustments)
        {
            var transaction = adjustment.pair().getTransaction();
            var item = new JsonObject();
            item.addProperty("uuid", transaction.getUUID()); //$NON-NLS-1$
            item.addProperty("date", transaction.getDateTime().toString()); //$NON-NLS-1$
            item.addProperty("type", TransactionTypes.wireType(transaction)); //$NON-NLS-1$
            item.add("sharesBefore", EntityJson.decimal(transaction.getShares(), Values.Share.precision())); //$NON-NLS-1$
            item.add("sharesAfter", EntityJson.decimal(adjustment.sharesAfter(), Values.Share.precision())); //$NON-NLS-1$
            transactions.add(item);
        }
        json.add("transactions", transactions); //$NON-NLS-1$

        if (!prices.isEmpty())
        {
            var sorted = prices.stream().sorted(Comparator.comparing(SecurityPrice::getDate)).toList();
            json.add("firstPrice", price(sorted.get(0), ratio)); //$NON-NLS-1$
            json.add("lastPrice", price(sorted.get(sorted.size() - 1), ratio)); //$NON-NLS-1$
        }
        return json;
    }

    private static JsonObject price(SecurityPrice price, Ratio ratio)
    {
        var json = new JsonObject();
        json.addProperty("date", price.getDate().toString()); //$NON-NLS-1$
        json.add("before", EntityJson.decimal(price.getValue(), Values.Quote.precision())); //$NON-NLS-1$
        json.add("after", EntityJson.decimal(ratio.newQuote(price.getValue()), Values.Quote.precision())); //$NON-NLS-1$
        return json;
    }

    /** the arithmetic of {@code StockSplitModel}, HALF_EVEN in {@link Values#MC} */
    private record Ratio(BigDecimal newShares, BigDecimal oldShares)
    {
        long newStock(long oldStock)
        {
            return BigDecimal.valueOf(oldStock).multiply(newShares).divide(oldShares, Values.MC)
                            .setScale(0, RoundingMode.HALF_EVEN).longValue();
        }

        long newQuote(long oldQuote)
        {
            return BigDecimal.valueOf(oldQuote).multiply(oldShares).divide(newShares, Values.MC)
                            .setScale(0, RoundingMode.HALF_EVEN).longValue();
        }
    }
}
