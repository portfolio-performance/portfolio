package name.abuchen.portfolio.snapshot.trades;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;

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

    private Client client;
    private CurrencyConverter converter;

    public TradeCollector(Client client, CurrencyConverter converter)
    {
        this.client = client;
        this.converter = converter;
    }

    public List<Trade> collect(Security security) throws TradeCollectorException
    {
        List<TransactionPair<?>> transactions = security.getTransactions(client);

        Collections.sort(transactions, BY_DATE_AND_TYPE);

        List<Trade> trades = new ArrayList<>();
        Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> openTransactions = new HashMap<>();
        Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> allTransactions = new HashMap<>();

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
                    allTransactions.computeIfAbsent(portfolio, p -> new ArrayList<>()).add(pair);
                    if (openList.isEmpty() || openList.get(0).getTransaction().getType().isPurchase() == type.isPurchase())
                        openList.add(pair);
                    else
                        trades.add(createNewTrade(openTransactions, pair, allTransactions));
                    break;

                case TRANSFER_IN:
                    moveOpenTransaction(openTransactions, pair);
                    moveOpenTransaction(allTransactions, pair);
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

            long shares = position.stream().mapToLong(p -> p.getTransaction().getShares()).sum();

            Trade newTrade = new Trade(security, entry.getKey(), shares);
            newTrade.setStart(position.get(0).getTransaction().getDateTime());
            newTrade.getTransactions().addAll(position);
            newTrade.getTransactionsMovingAverage().addAll(allTransactions.get(entry.getKey()));

            trades.add(newTrade);
        }

        trades.forEach(t -> t.calculate(converter));

        return trades;
    }

    private Trade createNewTrade(Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> openTransactions,
                    TransactionPair<PortfolioTransaction> pair,
                    Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> allTransactions)
                    throws TradeCollectorException
    {
        Trade newTrade = new Trade(pair.getTransaction().getSecurity(), (Portfolio) pair.getOwner(),
                        pair.getTransaction().getShares());

        List<TransactionPair<PortfolioTransaction>> open = openTransactions.get(pair.getOwner());

        if (open == null || open.isEmpty())
            throw new TradeCollectorException(MessageFormat.format(Messages.MsgErrorTradeCollector_NoHoldingsForSell,
                            pair.getTransaction().getSecurity(), pair.getOwner(), pair));

        long sharesToDistribute = pair.getTransaction().getShares();

        // sort open to get fifo
        Collections.sort(open, BY_DATE_AND_TYPE);

        for (TransactionPair<PortfolioTransaction> candidate : new ArrayList<>(open))
        {
            if (sharesToDistribute == 0)
                break;

            if (newTrade.getStart() == null)
                newTrade.setStart(candidate.getTransaction().getDateTime());

            if (sharesToDistribute >= candidate.getTransaction().getShares())
            {
                newTrade.getTransactions().add(candidate);
                open.remove(candidate);
                sharesToDistribute -= candidate.getTransaction().getShares();
            }
            else if (sharesToDistribute < candidate.getTransaction().getShares())
            {
                newTrade.getTransactions().add(split(candidate, (Portfolio) pair.getOwner(),
                                sharesToDistribute / (double) candidate.getTransaction().getShares()));
                open.set(open.indexOf(candidate),
                                split(candidate, (Portfolio) pair.getOwner(),
                                                (candidate.getTransaction().getShares() - sharesToDistribute)
                                                                / (double) candidate.getTransaction().getShares()));

                sharesToDistribute = 0;
            }
        }

        if (sharesToDistribute > 0)
        {
            throw new TradeCollectorException(MessageFormat.format(
                            Messages.MsgErrorTradeCollector_MissingHoldingsForSell, pair.getTransaction().getSecurity(),
                            pair.getOwner(), Values.Share.format(sharesToDistribute), pair));
        }

        newTrade.getTransactionsMovingAverage().addAll(allTransactions.get(pair.getOwner()));
        newTrade.getTransactions().add(pair);
        newTrade.setEnd(pair.getTransaction().getDateTime());

        return newTrade;
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
                long remainingShares = candidate.getTransaction().getShares() - sharesToTransfer;

                positions.set(positions.indexOf(candidate), split(candidate, outbound,
                                remainingShares / (double) candidate.getTransaction().getShares()));
                target.add(split(candidate, inbound,
                                sharesToTransfer / (double) candidate.getTransaction().getShares()));

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

    private TransactionPair<PortfolioTransaction> split(TransactionPair<PortfolioTransaction> candidate,
                    Portfolio newOwner, double weight)
    {
        if (candidate.getTransaction().getCrossEntry() instanceof BuySellEntry entry)
            return splitBuySell(entry, newOwner, weight);
        else if (candidate.getTransaction() instanceof PortfolioTransaction)
            return splitPortfolioTransaction((Portfolio) candidate.getOwner(), candidate.getTransaction(), weight);
        else
            throw new UnsupportedOperationException();
    }

    private TransactionPair<PortfolioTransaction> splitBuySell(BuySellEntry entry, Portfolio portfolio, double weight)
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

        copy.setShares(Math.round(t.getShares() * weight));
        copy.setAmount(Math.round(t.getAmount() * weight));

        t.getUnits().forEach(unit -> copy.getPortfolioTransaction().addUnit(unit.split(weight)));

        return new TransactionPair<>(entry.getPortfolio(), copy.getPortfolioTransaction());
    }

    private TransactionPair<PortfolioTransaction> splitPortfolioTransaction(Portfolio portfolio,
                    PortfolioTransaction transaction, double weight)
    {
        PortfolioTransaction newTransaction = new PortfolioTransaction();
        newTransaction.setType(transaction.getType());
        newTransaction.setDateTime(transaction.getDateTime());
        newTransaction.setSecurity(transaction.getSecurity());
        newTransaction.setCurrencyCode(transaction.getCurrencyCode());
        newTransaction.setNote(transaction.getNote());

        newTransaction.setShares(Math.round(transaction.getShares() * weight));
        newTransaction.setAmount(Math.round(transaction.getAmount() * weight));

        transaction.getUnits().forEach(unit -> newTransaction.addUnit(unit.split(weight)));

        return new TransactionPair<>(portfolio, newTransaction);
    }

}
