package name.abuchen.portfolio.snapshot.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.util.Interval;

/**
 * Caches {@link LazySecurityPerformanceSnapshot} instances for one trade
 * collection run. Open trades share one unfiltered snapshot; trades closed by
 * the same transaction share one truncated snapshot.
 * <p>
 * The cache is captured by the trades and is thread-safe because calculation
 * and evaluation can happen on different threads.
 */
public class SnapshotCache
{
    /**
     * The key of the cache. Client, security and the closing transaction are
     * compared by identity: none of them implements equals/hashCode today.
     * <p>
     * The closing transaction and its security are needed, because the filter
     * truncates the transaction of that security before the closing
     * transaction.
     */
    private static final class Key
    {
        private final Client client;
        private final Security security;
        private final PortfolioTransaction closingTransaction;
        private final String currencyCode;
        private final Interval interval;

        private Key(Client client, Security security, PortfolioTransaction closingTransaction, String currencyCode,
                        Interval interval)
        {
            this.client = client;
            this.security = security;
            this.closingTransaction = closingTransaction;
            this.currencyCode = currencyCode;
            this.interval = interval;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = System.identityHashCode(client);
            result = prime * result + System.identityHashCode(security);
            result = prime * result + System.identityHashCode(closingTransaction);
            result = prime * result + currencyCode.hashCode();
            result = prime * result + interval.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;

            Key other = (Key) obj;
            return client == other.client //
                            && security == other.security //
                            && closingTransaction == other.closingTransaction //
                            && currencyCode.equals(other.currencyCode) //
                            && interval.equals(other.interval);
        }
    }

    private final Map<Key, LazySecurityPerformanceSnapshot> snapshots = new ConcurrentHashMap<>();

    /**
     * Returns the snapshot for the given inputs, creating it with the given
     * supplier if it is not cached yet.
     * <p>
     * A snapshot holds the records of *all* securities of the client, so the
     * security is not part of the input. It is part of the key only because it
     * is what the
     * {@link name.abuchen.portfolio.snapshot.filter.ClientTransactionFilter}
     * truncates - and it is therefore derived from the closing transaction:
     * without a closing transaction there is no filter, the snapshot is created
     * from the unfiltered client, and one snapshot serves the open trades of
     * every security.
     *
     * @param client
     *            the unfiltered client
     * @param closingTransaction
     *            the real (not split) transaction that closed the trade, or
     *            null for open trades
     */
    public LazySecurityPerformanceSnapshot lookup(Client client, PortfolioTransaction closingTransaction,
                    CurrencyConverter converter, Interval interval, Supplier<LazySecurityPerformanceSnapshot> supplier)
    {
        var security = closingTransaction != null ? closingTransaction.getSecurity() : null;

        // the converter itself is not part of the key: all call sites use one
        // exchange rate provider factory per collection run, but each security
        // can have its own term currency
        var key = new Key(client, security, closingTransaction, converter.getTermCurrency(), interval);

        return snapshots.computeIfAbsent(key, k -> supplier.get());
    }
}
