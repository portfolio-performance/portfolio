package name.abuchen.portfolio.snapshot.trades;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.LongStream;

import com.google.common.annotations.VisibleForTesting;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.math.Apportionment;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.security.SnapshotCache;

public class TradeCollector
{
    public static final Comparator<TransactionPair<?>> BY_DATE_AND_TYPE = new ByDateAndType();

    /**
     * Sorts transaction by date and then by type. First come inbound types
     * (purchase, inbound delivery), then transfers, and finally outbound types
     * (sell, outbound delivery) to make sure that trades closed on the same day
     * are matched.<br/>
     * Transfers are sorted into the middle to ensure that purchases are
     * processed before transfers (for example if the users purchases and then
     * transfers on the same day).
     */
    public static final class ByDateAndType implements Comparator<TransactionPair<?>>, Serializable
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(TransactionPair<?> t1, TransactionPair<?> t2)
        {
            var dt1 = t1.getTransaction().getDateTime();
            var dt2 = t2.getTransaction().getDateTime();

            // the date differs, just sort by date (no need to check types)
            if (dt1.getYear() != dt2.getYear() || dt1.getMonth() != dt2.getMonth()
                            || dt1.getDayOfMonth() != dt2.getDayOfMonth())
                return dt1.compareTo(dt2);

            var hasTime1 = dt1.getHour() != 0 || dt1.getMinute() != 0;
            var hasTime2 = dt2.getHour() != 0 || dt2.getMinute() != 0;

            // if both transactions have a time, then sort by time
            if (hasTime1 && hasTime2)
                return dt1.compareTo(dt2);

            // otherwise sort: inbounds, transfers, outbounds
            return getSortOrder(t1) - getSortOrder(t2);
        }

        /**
         * Returns 1 for inbound types (purchase, inbound delivery), 2 for
         * transfers, and 3 for outbound types
         */
        private int getSortOrder(TransactionPair<?> pair)
        {
            if (pair.getTransaction() instanceof PortfolioTransaction tx)
            {
                if (tx.getType() == PortfolioTransaction.Type.TRANSFER_IN
                                || tx.getType() == PortfolioTransaction.Type.TRANSFER_OUT)
                    return 2;

                return tx.getType().isPurchase() ? 1 : 3;
            }
            else if (pair.getTransaction() instanceof AccountTransaction tx)
            {
                if (tx.getType() == AccountTransaction.Type.TRANSFER_IN
                                || tx.getType() == AccountTransaction.Type.TRANSFER_OUT)
                    return 2;

                return tx.getType().isDebit() ? 1 : 3;
            }
            else
            {
                throw new IllegalArgumentException(pair.getTransaction().getClass().getName());
            }
        }
    }

    private final Client client;
    private final CurrencyConverter converter;
    private final TradeGrouping grouping;
    private final SnapshotCache snapshotCache;

    public TradeCollector(Client client, CurrencyConverter converter, TradeGrouping grouping,
                    SnapshotCache snapshotCache)
    {
        this.client = client;
        this.converter = converter;
        this.grouping = grouping;
        this.snapshotCache = snapshotCache;
    }

    /**
     * Creates a collector for tests and for the one-off collection of trades:
     * it groups trades as {@link TradeGrouping#COMBINED} and uses a private
     * {@link SnapshotCache} that is shared with nobody.
     */
    @VisibleForTesting
    public TradeCollector(Client client, CurrencyConverter converter)
    {
        this(client, converter, TradeGrouping.COMBINED, new SnapshotCache());
    }

    public List<Trade> collect(Security security) throws TradeCollectorException
    {
        List<TransactionPair<?>> transactions = security.getTransactions(client);

        Collections.sort(transactions, BY_DATE_AND_TYPE);

        List<Trade> trades = new ArrayList<>();
        Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> openTransactions = new HashMap<>();

        for (TransactionPair<?> txp : transactions)
        {
            if (!(txp.getTransaction() instanceof PortfolioTransaction))
                continue;

            @SuppressWarnings("unchecked")
            TransactionPair<PortfolioTransaction> pair = (TransactionPair<PortfolioTransaction>) txp;

            Portfolio portfolio = (Portfolio) txp.getOwner();
            PortfolioTransaction t = (PortfolioTransaction) txp.getTransaction();
            List<TransactionPair<PortfolioTransaction>> openList = openTransactions.computeIfAbsent(portfolio, p -> new ArrayList<>());

            Type type = t.getType();
            switch (type)
            {
                case BUY, DELIVERY_INBOUND:
                case SELL, DELIVERY_OUTBOUND:
                    // If fifo is empty or contains the same type of transactions
                    // as incoming one, add it to the fifo. Otherwise, we create
                    // a new trade. (Note: it's an invariant that fifo contains
                    // transaction of the same type (purchase vs !purchase), so
                    // we test 0th element in fifo).
                    if (openList.isEmpty() || openList.get(0).getTransaction().getType().isPurchase() == type.isPurchase())
                        openList.add(pair);
                    else
                        trades.addAll(createNewTrades(openTransactions, pair));
                    break;

                case TRANSFER_IN:
                    moveOpenTransaction(openTransactions, pair);
                    break;

                case TRANSFER_OUT:
                    // ignore -> handled via TRANSFER_IN
                    break;

                default:
                    throw new IllegalArgumentException("unsupported type " + type); //$NON-NLS-1$

            }
        }

        // create open trades out of the remaining

        for (Entry<Portfolio, List<TransactionPair<PortfolioTransaction>>> entry : openTransactions.entrySet())
        {
            List<TransactionPair<PortfolioTransaction>> position = entry.getValue();

            if (position.isEmpty())
                continue;

            if (grouping == TradeGrouping.PER_LOT)
            {
                // one trade per open lot. Note: the portfolio is taken from the
                // key of the map, i.e. the portfolio whose open positions are
                // drained, and *not* from the transaction pair: after a
                // transfer, the pair still reports the portfolio the lot has
                // been transferred away from

                for (var lot : position)
                {
                    var newTrade = new Trade(security, entry.getKey(), lot.getTransaction().getShares());
                    newTrade.setStart(lot.getTransaction().getDateTime());
                    newTrade.getTransactions().add(lot);

                    trades.add(newTrade);
                }
            }
            else
            {
                long shares = position.stream().mapToLong(p -> p.getTransaction().getShares()).sum();

                Trade newTrade = new Trade(security, entry.getKey(), shares);
                newTrade.setStart(position.get(0).getTransaction().getDateTime());
                newTrade.getTransactions().addAll(position);

                trades.add(newTrade);
            }
        }

        trades.forEach(t -> t.calculate(client, converter, snapshotCache));

        return trades;
    }

    /**
     * Matches the given closing transaction against the open lots of the
     * portfolio and creates the resulting trades: one trade covering all
     * consumed lots ({@link TradeGrouping#COMBINED}) or one trade per consumed
     * lot ({@link TradeGrouping#PER_LOT}).
     */
    private List<Trade> createNewTrades(Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> openTransactions,
                    TransactionPair<PortfolioTransaction> pair) throws TradeCollectorException
    {
        List<TransactionPair<PortfolioTransaction>> open = openTransactions.get(pair.getOwner());

        if (open == null || open.isEmpty())
            throw new TradeCollectorException(MessageFormat.format(Messages.MsgErrorTradeCollector_NoHoldingsForSell,
                            pair.getTransaction().getSecurity(), pair.getOwner(), pair));

        long sharesToDistribute = pair.getTransaction().getShares();

        // sort open to get fifo
        Collections.sort(open, BY_DATE_AND_TYPE);

        // the open lots consumed by this closing transaction. If a lot is
        // consumed only partially, the element is the split off part of it
        List<TransactionPair<PortfolioTransaction>> consumed = new ArrayList<>();

        for (TransactionPair<PortfolioTransaction> candidate : new ArrayList<>(open))
        {
            if (sharesToDistribute == 0)
                break;

            if (sharesToDistribute >= candidate.getTransaction().getShares())
            {
                consumed.add(candidate);
                open.remove(candidate);
                sharesToDistribute -= candidate.getTransaction().getShares();
            }
            else if (sharesToDistribute < candidate.getTransaction().getShares())
            {
                var owner = (Portfolio) pair.getOwner();
                var lotShares = candidate.getTransaction().getShares();

                var pieces = split(candidate, List.of(owner, owner),
                                new long[] { sharesToDistribute, lotShares - sharesToDistribute });

                consumed.add(pieces.get(0));
                open.set(open.indexOf(candidate), pieces.get(1));

                sharesToDistribute = 0;
            }
        }

        if (sharesToDistribute > 0)
        {
            throw new TradeCollectorException(MessageFormat.format(
                            Messages.MsgErrorTradeCollector_MissingHoldingsForSell, pair.getTransaction().getSecurity(),
                            pair.getOwner(), Values.Share.format(sharesToDistribute), pair));
        }

        // if no lot has been consumed at all (a closing transaction without
        // shares), there is nothing to distribute and both groupings create the
        // same single trade
        if (grouping == TradeGrouping.PER_LOT && !consumed.isEmpty())
            return createTradePerLot(pair, consumed);
        else
            return List.of(createCombinedTrade(pair, consumed));
    }

    /**
     * Creates one trade holding all consumed lots and the closing transaction.
     */
    private Trade createCombinedTrade(TransactionPair<PortfolioTransaction> pair,
                    List<TransactionPair<PortfolioTransaction>> consumed)
    {
        var newTrade = new Trade(pair.getTransaction().getSecurity(), (Portfolio) pair.getOwner(),
                        pair.getTransaction().getShares());

        if (!consumed.isEmpty())
            newTrade.setStart(consumed.get(0).getTransaction().getDateTime());

        newTrade.getTransactions().addAll(consumed);
        newTrade.getTransactions().add(pair);
        newTrade.setEnd(pair.getTransaction().getDateTime());
        newTrade.setRealClosingTransaction(pair.getTransaction());

        return newTrade;
    }

    /**
     * Creates one trade per consumed lot. Every trade holds its lot and its own
     * split copy of the closing transaction.
     */
    private List<Trade> createTradePerLot(TransactionPair<PortfolioTransaction> pair,
                    List<TransactionPair<PortfolioTransaction>> consumed)
    {
        // Split the closing transaction by the shares contributed by each
        // consumed lot.

        List<TransactionPair<PortfolioTransaction>> closingPieces = consumed.size() == 1 ? null
                        : split(pair, Collections.nCopies(consumed.size(), (Portfolio) pair.getOwner()),
                                        consumed.stream().mapToLong(lot -> lot.getTransaction().getShares()).toArray());

        var answer = new ArrayList<Trade>();

        for (int ii = 0; ii < consumed.size(); ii++)
        {
            TransactionPair<PortfolioTransaction> lot = consumed.get(ii);

            var newTrade = new Trade(pair.getTransaction().getSecurity(), (Portfolio) pair.getOwner(),
                            lot.getTransaction().getShares());
            newTrade.setStart(lot.getTransaction().getDateTime());
            newTrade.getTransactions().add(lot);

            newTrade.getTransactions().add(closingPieces == null ? pair : closingPieces.get(ii));

            newTrade.setEnd(pair.getTransaction().getDateTime());

            // the trade must keep the real closing transaction because the
            // ClientTransactionFilter identifies it by reference; a split copy
            // exists nowhere in the client and would be silently ignored
            newTrade.setRealClosingTransaction(pair.getTransaction());

            answer.add(newTrade);
        }

        return answer;
    }

    private void moveOpenTransaction(Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> openTransactions,
                    TransactionPair<PortfolioTransaction> pair) throws TradeCollectorException
    {
        PortfolioTransferEntry transfer = (PortfolioTransferEntry) pair.getTransaction().getCrossEntry();
        Portfolio outbound = (Portfolio) transfer.getOwner(transfer.getSourceTransaction());
        Portfolio inbound = (Portfolio) transfer.getOwner(transfer.getTargetTransaction());

        // remove from outbound portfolio

        List<TransactionPair<PortfolioTransaction>> target = openTransactions.computeIfAbsent(inbound,
                        p -> new ArrayList<>());

        List<TransactionPair<PortfolioTransaction>> positions = openTransactions.get(outbound);
        if (positions == null || positions.isEmpty())
            throw new TradeCollectorException(
                            MessageFormat.format(Messages.MsgErrorTradeCollector_NoHoldingsForTransfer,
                                            pair.getTransaction().getSecurity(), outbound, inbound, pair));

        long sharesToTransfer = pair.getTransaction().getShares();

        // sort positions to get fifo
        Collections.sort(positions, BY_DATE_AND_TYPE);

        for (TransactionPair<PortfolioTransaction> candidate : new ArrayList<>(positions))
        {
            if (sharesToTransfer == 0)
                break;

            if (sharesToTransfer >= candidate.getTransaction().getShares())
            {
                positions.remove(candidate);
                target.add(candidate);
                sharesToTransfer -= candidate.getTransaction().getShares();
            }
            else if (sharesToTransfer < candidate.getTransaction().getShares())
            {
                long lotShares = candidate.getTransaction().getShares();

                var pieces = split(candidate, List.of(inbound, outbound),
                                new long[] { sharesToTransfer, lotShares - sharesToTransfer });

                target.add(pieces.get(0));
                positions.set(positions.indexOf(candidate), pieces.get(1));

                sharesToTransfer = 0;
            }
        }

        if (sharesToTransfer > 0)
        {
            throw new TradeCollectorException(
                            MessageFormat.format(Messages.MsgErrorTradeCollector_MissingHoldingsForTransfer,
                                            pair.getTransaction().getSecurity(), outbound, inbound,
                                            Values.Share.format(sharesToTransfer), pair));
        }
    }

    /**
     * Splits a transaction by share weights. Shares are exact; amount, unit
     * amount and unit forex amount are apportioned so all pieces reconcile.
     *
     * @param newOwners
     *            one owner per piece; only copied {@link BuySellEntry}
     *            instances use it directly
     */
    private List<TransactionPair<PortfolioTransaction>> split(TransactionPair<PortfolioTransaction> candidate,
                    List<Portfolio> newOwners, long[] weights)
    {
        PortfolioTransaction t = candidate.getTransaction();

        if (newOwners.size() != weights.length)
            throw new IllegalArgumentException("one owner required per weight"); //$NON-NLS-1$

        if (LongStream.of(weights).sum() != t.getShares())
            throw new IllegalArgumentException(
                            MessageFormat.format("weights {0} do not add up to the {1} shares of {2}", //$NON-NLS-1$
                                            Arrays.toString(weights), t.getShares(), t));

        long[] amounts = Apportionment.distribute(t.getAmount(), weights);

        // Apportion each unit object independently; do not derive GROSS_VALUE
        // from amount, fees and taxes because imported transactions need not
        // reconcile.

        List<Unit> units = t.getUnits().toList();
        List<long[]> unitAmounts = new ArrayList<>();
        List<long[]> unitForexAmounts = new ArrayList<>();

        for (Unit unit : units)
        {
            unitAmounts.add(Apportionment.distribute(unit.getAmount().getAmount(), weights));
            unitForexAmounts.add(unit.getForex() != null
                            ? Apportionment.distribute(unit.getForex().getAmount(), weights)
                            : new long[weights.length]);
        }

        var answer = new ArrayList<TransactionPair<PortfolioTransaction>>();

        for (int ii = 0; ii < weights.length; ii++)
        {
            TransactionPair<PortfolioTransaction> piece;

            if (t.getCrossEntry() instanceof BuySellEntry entry)
                piece = createBuySellPiece(entry, newOwners.get(ii), weights[ii], amounts[ii]);
            else
                // a transaction without a cross entry is copied with the owner
                // of the candidate: it has no portfolio of its own to set, so
                // the new owner is deliberately ignored here
                piece = createPortfolioTransactionPiece((Portfolio) candidate.getOwner(), t, weights[ii], amounts[ii]);

            for (int jj = 0; jj < units.size(); jj++)
                piece.getTransaction()
                                .addUnit(units.get(jj).piece(unitAmounts.get(jj)[ii], unitForexAmounts.get(jj)[ii]));

            answer.add(piece);
        }

        return answer;
    }

    private TransactionPair<PortfolioTransaction> createBuySellPiece(BuySellEntry entry, Portfolio portfolio,
                    long shares, long amount)
    {
        PortfolioTransaction t = entry.getPortfolioTransaction();

        BuySellEntry copy = new BuySellEntry();
        copy.setPortfolio(portfolio);
        copy.setAccount(entry.getAccount());

        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setType(t.getType());
        copy.setNote(t.getNote());

        copy.setShares(shares);
        copy.setAmount(amount);

        return new TransactionPair<>(entry.getPortfolio(), copy.getPortfolioTransaction());
    }

    private TransactionPair<PortfolioTransaction> createPortfolioTransactionPiece(Portfolio portfolio,
                    PortfolioTransaction transaction, long shares, long amount)
    {
        PortfolioTransaction newTransaction = new PortfolioTransaction();
        newTransaction.setType(transaction.getType());
        newTransaction.setDateTime(transaction.getDateTime());
        newTransaction.setSecurity(transaction.getSecurity());
        newTransaction.setCurrencyCode(transaction.getCurrencyCode());
        newTransaction.setNote(transaction.getNote());

        newTransaction.setShares(shares);
        newTransaction.setAmount(amount);

        return new TransactionPair<>(portfolio, newTransaction);
    }

}
