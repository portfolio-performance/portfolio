package name.abuchen.portfolio.snapshot.trades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransferEntry;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;

public class TradeCollector
{
    private Client client;

    public TradeCollector(Client client)
    {
        this.client = client;
    }

    public List<Trade> collect(Security security)
    {
        List<TransactionPair<?>> transactions = security.getTransactions(client);

        Collections.sort(transactions,
                        (p1, p2) -> p1.getTransaction().getDateTime().compareTo(p2.getTransaction().getDateTime()));

        List<Trade> trades = new ArrayList<>();
        Map<Portfolio, List<TransactionPair<?>>> openTransactions = new HashMap<>();

        for (TransactionPair<?> pair : transactions)
        {
            if (pair.getTransaction() instanceof PortfolioTransaction)
            {
                Portfolio portfolio = (Portfolio) pair.getOwner();
                PortfolioTransaction t = (PortfolioTransaction) pair.getTransaction();

                switch (t.getType())
                {
                    case BUY:
                    case DELIVERY_INBOUND:
                        openTransactions.computeIfAbsent(portfolio, p -> new ArrayList<>()).add(pair);
                        break;

                    case SELL:
                    case DELIVERY_OUTBOUND:
                        trades.add(createNewTradeFromSell(security, openTransactions, pair));
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
        }

        // create open trades out of the remaining

        for (List<TransactionPair<?>> position : openTransactions.values())
        {
            if (position.isEmpty())
                continue;

            long shares = position.stream().mapToLong(p -> p.getTransaction().getShares()).sum();

            Trade newTrade = new Trade(security, shares);
            newTrade.setStart(position.get(0).getTransaction().getDateTime());
            newTrade.getTransactions().addAll(position);

            trades.add(newTrade);
        }

        return trades;
    }

    private Trade createNewTradeFromSell(Security security, Map<Portfolio, List<TransactionPair<?>>> openTransactions,
                    TransactionPair<?> pair)
    {
        Trade newTrade = new Trade(security, pair.getTransaction().getShares());

        List<TransactionPair<?>> open = openTransactions.get(pair.getOwner());

        if (open == null || open.isEmpty())
            throw new IllegalArgumentException();

        long sharesToDistribute = pair.getTransaction().getShares();

        for (TransactionPair<?> candidate : new ArrayList<>(open))
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
                newTrade.getTransactions().add(
                                split(candidate, sharesToDistribute / (double) candidate.getTransaction().getShares()));
                open.set(open.indexOf(candidate),
                                split(candidate, (candidate.getTransaction().getShares() - sharesToDistribute)
                                                / (double) candidate.getTransaction().getShares()));
                
                
                sharesToDistribute = 0;
            }
        }

        if (sharesToDistribute > 0)
            throw new IllegalArgumentException();

        newTrade.getTransactions().add(pair);
        newTrade.setEnd(pair.getTransaction().getDateTime());

        return newTrade;
    }

    private TransactionPair<?> split(TransactionPair<?> candidate, double weight)
    {
        if (candidate.getTransaction().getCrossEntry() instanceof BuySellEntry)
            return splitBuySell((BuySellEntry) candidate.getTransaction().getCrossEntry(), weight);
        else if (candidate.getTransaction() instanceof PortfolioTransaction)
            return splitPT((Portfolio) candidate.getOwner(), (PortfolioTransaction) candidate.getTransaction(), weight);
        else
            throw new UnsupportedOperationException();
    }

    private TransactionPair<?> splitBuySell(BuySellEntry entry, double weight)
    {
        PortfolioTransaction t = entry.getPortfolioTransaction();

        BuySellEntry copy = new BuySellEntry();
        copy.setDate(t.getDateTime());
        copy.setCurrencyCode(t.getCurrencyCode());
        copy.setSecurity(t.getSecurity());
        copy.setType(t.getType());
        copy.setNote(t.getNote());

        copy.setShares(Math.round(t.getShares() * weight));
        copy.setAmount(Math.round(t.getAmount() * weight));

        copyUnits(t, copy.getPortfolioTransaction(), weight);

        return new TransactionPair<>(entry.getPortfolio(), copy.getPortfolioTransaction());
    }

    private TransactionPair<?> splitPT(Portfolio portfolio, PortfolioTransaction transaction, double weight)
    {
        PortfolioTransaction newTransaction = new PortfolioTransaction();
        newTransaction.setType(transaction.getType());
        newTransaction.setDateTime(transaction.getDateTime());
        newTransaction.setSecurity(transaction.getSecurity());
        newTransaction.setCurrencyCode(transaction.getCurrencyCode());
        newTransaction.setNote(transaction.getNote());

        newTransaction.setShares(Math.round(transaction.getShares() * weight));
        newTransaction.setAmount(Math.round(transaction.getAmount() * weight));

        copyUnits(transaction, newTransaction, weight);

        return new TransactionPair<>(portfolio, newTransaction);
    }

    private void copyUnits(Transaction source, Transaction target, double weight)
    {
        source.getUnits().forEach(unit -> {
            if (unit.getForex() == null)
                target.addUnit(new Unit(unit.getType(), Money.of(unit.getAmount().getCurrencyCode(),
                                Math.round(unit.getAmount().getAmount() * weight))));
            else
                target.addUnit(new Unit(unit.getType(), //
                                Money.of(unit.getAmount().getCurrencyCode(),
                                                Math.round(unit.getAmount().getAmount() * weight)),
                                Money.of(unit.getForex().getCurrencyCode(),
                                                Math.round(unit.getForex().getAmount() * weight)),
                                unit.getExchangeRate()));
        });
    }

    private void moveOpenTransaction(Map<Portfolio, List<TransactionPair<?>>> openTransactions, TransactionPair<?> pair)
    {
        PortfolioTransferEntry transfer = (PortfolioTransferEntry) pair.getTransaction().getCrossEntry();
        Portfolio outbound = (Portfolio) transfer.getOwner(transfer.getSourceTransaction());
        Portfolio inbound = (Portfolio) transfer.getOwner(transfer.getTargetTransaction());

        // remove from outbound portfolio

        List<TransactionPair<?>> target = openTransactions.computeIfAbsent(inbound, p -> new ArrayList<>());

        List<TransactionPair<?>> positions = openTransactions.get(outbound);
        if (positions == null || positions.isEmpty())
            throw new IllegalArgumentException();

        long sharesToTransfer = pair.getTransaction().getShares();

        for (TransactionPair<?> candidate : new ArrayList<>(positions))
        {
            if (sharesToTransfer >= candidate.getTransaction().getShares())
            {
                target.add(candidate);
                positions.remove(candidate);
                sharesToTransfer -= candidate.getTransaction().getShares();
                break;
            }
            else if (sharesToTransfer < candidate.getTransaction().getShares())
            {
                // FIXME split transaction

                sharesToTransfer -= candidate.getTransaction().getShares();
                break;
            }
        }

        if (sharesToTransfer > 0)
            throw new IllegalArgumentException();

    }

}
