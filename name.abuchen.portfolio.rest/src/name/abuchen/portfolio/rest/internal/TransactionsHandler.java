package name.abuchen.portfolio.rest.internal;

import java.util.ArrayList;

import com.google.gson.JsonElement;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.TransactionPair;

public final class TransactionsHandler
{
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
        var transactions = new ArrayList<>(client.getAllTransactions());
        transactions.sort(TransactionPair.BY_DATE.reversed());
        return EntityJson.envelope(transactions, EntityJson::toJson);
    }
}
