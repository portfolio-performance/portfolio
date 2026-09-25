package name.abuchen.portfolio.rest.internal;

import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.money.Values;

/**
 * Earnings: the dividend, interest and interest charge transactions of a
 * period, as the transaction list reports them, plus their totals per
 * currency. The totals are not converted: every transaction is booked in its
 * cash account's currency, and a conversion would need a rate per
 * transaction date (see the performance calendar for converted figures).
 */
public final class EarningsHandler
{
    /** the transaction types that count as earnings */
    /* package */ static final String TYPES = "dividends,interest,interest-charge"; //$NON-NLS-1$

    private EarningsHandler()
    {
    }

    /** sums per currency, in the transaction currency's smallest unit */
    private static final class Totals
    {
        long dividends;
        long interest;
        long interestCharge;
        long taxes;
        long fees;
        int count;
    }

    /**
     * The earnings the filter selects; the filter's {@code type} is ignored
     * and replaced with the earnings types. Must be called on the UI thread.
     */
    public static JsonElement list(Client client, TransactionsHandler.Filter filter)
    {
        var earningsFilter = new TransactionsHandler.Filter(filter.from(), filter.to(), TYPES, filter.instrument(),
                        filter.cashAccount(), filter.investmentAccount());
        var transactions = TransactionsHandler.select(client, earningsFilter);

        var totals = new TreeMap<String, Totals>();
        var items = new JsonArray();
        for (var pair : transactions)
        {
            items.add(EntityJson.toJson(pair));

            // the earnings types are all account transactions
            var transaction = (AccountTransaction) pair.getTransaction();
            var sum = totals.computeIfAbsent(transaction.getCurrencyCode(), c -> new Totals());
            sum.count++;
            sum.taxes += transaction.getUnitSum(Transaction.Unit.Type.TAX).getAmount();
            sum.fees += transaction.getUnitSum(Transaction.Unit.Type.FEE).getAmount();
            switch (transaction.getType())
            {
                case DIVIDENDS -> sum.dividends += transaction.getAmount();
                case INTEREST -> sum.interest += transaction.getAmount();
                case INTEREST_CHARGE -> sum.interestCharge += transaction.getAmount();
                default -> throw new IllegalStateException(transaction.getType().name());
            }
        }

        var json = new JsonObject();
        json.add("items", items); //$NON-NLS-1$
        json.add("totals", totals(totals)); //$NON-NLS-1$
        return json;
    }

    private static JsonArray totals(Map<String, Totals> totals)
    {
        var precision = Values.Amount.precision();

        var array = new JsonArray();
        for (var entry : totals.entrySet())
        {
            var sum = entry.getValue();
            var json = new JsonObject();
            json.addProperty("currency", entry.getKey()); //$NON-NLS-1$
            json.add("value", EntityJson.decimal(sum.dividends + sum.interest - sum.interestCharge, precision)); //$NON-NLS-1$
            json.add("dividends", EntityJson.decimal(sum.dividends, precision)); //$NON-NLS-1$
            json.add("interest", EntityJson.decimal(sum.interest, precision)); //$NON-NLS-1$
            json.add("interestCharge", EntityJson.decimal(sum.interestCharge, precision)); //$NON-NLS-1$
            json.add("taxes", EntityJson.decimal(sum.taxes, precision)); //$NON-NLS-1$
            json.add("fees", EntityJson.decimal(sum.fees, precision)); //$NON-NLS-1$
            json.addProperty("count", sum.count); //$NON-NLS-1$
            array.add(json);
        }
        return array;
    }
}
