package name.abuchen.portfolio.snapshot.trades;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;

public class TradeCollector
{
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

        Collections.sort(transactions,
                        (p1, p2) -> p1.getTransaction().getDateTime().compareTo(p2.getTransaction().getDateTime()));

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

            switch (t.getType())
            {
                case BUY:
                case DELIVERY_INBOUND:
                    openTransactions.computeIfAbsent(portfolio, p -> new ArrayList<>()).add(pair);
                    break;

                case SELL:
                case DELIVERY_OUTBOUND:
                    trades.add(createNewTradeFromSell(openTransactions, pair));
                    break;

                case TRANSFER_IN:
                    moveOpenTransaction(openTransactions, pair);
                    break;

                case TRANSFER_OUT:
                    // ignore -> handled via TRANSFER_IN
                    break;

                default:
                    throw new IllegalArgumentException();

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

            trades.add(newTrade);
        }

        trades.forEach(t -> t.calculate(converter));

        return trades;
    }

    private Trade createNewTradeFromSell(Map<Portfolio, List<TransactionPair<PortfolioTransaction>>> openTransactions,
                    TransactionPair<PortfolioTransaction> pair) throws TradeCollectorException
    {
        Trade newTrade = new Trade(pair.getTransaction().getSecurity(), (Portfolio) pair.getOwner(),
                        pair.getTransaction().getShares());

        List<TransactionPair<PortfolioTransaction>> open = openTransactions.get(pair.getOwner());

        if (open == null || open.isEmpty())
            throw new TradeCollectorException(MessageFormat.format(Messages.MsgErrorTradeCollector_NoHoldingsForSell,
                            pair.getTransaction().getSecurity(), pair.getOwner(), pair));

        long sharesToDistribute = pair.getTransaction().getShares();

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
        if (candidate.getTransaction().getCrossEntry() instanceof BuySellEntry)
            return splitBuySell((BuySellEntry) candidate.getTransaction().getCrossEntry(), newOwner, weight);
        else if (candidate.getTransaction() instanceof PortfolioTransaction)
            return splitPortfolioTransaction((Portfolio) candidate.getOwner(),
                            (PortfolioTransaction) candidate.getTransaction(), weight);
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
