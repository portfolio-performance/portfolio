package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Values;

public class SecurityPosition
{
    private Security security;
    private SecurityPrice price;
    private long shares;

    private List<PortfolioTransaction> transactions = new ArrayList<PortfolioTransaction>();

    private transient boolean isDirty = true;
    private transient long marketValue;
    private transient long purchasePrice;
    private transient long purchaseValue;

    public SecurityPosition(Security security)
    {
        this.security = security;
    }

    public Security getSecurity()
    {
        return security;
    }

    public SecurityPrice getPrice()
    {
        return price;
    }

    public void setPrice(SecurityPrice price)
    {
        this.price = price;
        this.isDirty = true;
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
        this.isDirty = true;
    }

    public void addTransaction(PortfolioTransaction t)
    {
        transactions.add(t);
        this.isDirty = true;

        switch (t.getType())
        {
            case BUY:
            case TRANSFER_IN:
            case DELIVERY_INBOUND:
                shares += t.getShares();
                break;
            case SELL:
            case TRANSFER_OUT:
            case DELIVERY_OUTBOUND:
                shares -= t.getShares();
                break;
            default:
                throw new RuntimeException();
        }
    }

    public long calculateValue()
    {
        calculate();
        return marketValue;
    }

    public long getFIFOPurchasePrice()
    {
        calculate();
        return purchasePrice;
    }

    public long getFIFOPurchaseValue()
    {
        calculate();
        return purchaseValue;
    }

    public long getProfitLoss()
    {
        calculate();
        return security != null ? marketValue - purchaseValue : 0;
    }

    private void calculate()
    {
        if (!isDirty)
            return;

        // market value
        long p = price != null ? price.getValue() : 0;
        marketValue = shares * p / Values.Share.factor();

        // purchase value / price
        if (transactions.isEmpty())
        {
            purchasePrice = 0;
            purchaseValue = 0;
        }
        else
        {
            calculatePurchaseValuePrice(filter(transactions));
        }

        isDirty = false;
    }

    /**
     * Remove matching transfer_in / transfer_out transactions
     */
    private static List<PortfolioTransaction> filter(List<PortfolioTransaction> input)
    {
        List<PortfolioTransaction> inbound = new ArrayList<PortfolioTransaction>();
        for (PortfolioTransaction t : input)
            if (t.getType() == Type.TRANSFER_IN)
                inbound.add(t);

        if (inbound.isEmpty())
            return input;

        List<PortfolioTransaction> output = new ArrayList<PortfolioTransaction>(input.size());
        TransactionLoop: for (PortfolioTransaction t : input)
        {
            if (t.getType() == Type.TRANSFER_IN)
            {
                continue;
            }
            else if (t.getType() == Type.TRANSFER_OUT)
            {
                Iterator<PortfolioTransaction> iter = inbound.iterator();
                while (iter.hasNext())
                {
                    PortfolioTransaction t_inbound = iter.next();
                    if (t_inbound.getDate().equals(t.getDate()) && t_inbound.getShares() == t.getShares())
                    {
                        iter.remove();
                        continue TransactionLoop;
                    }
                }
                output.add(t);
            }
            else
            {
                output.add(t);
            }
        }

        output.addAll(inbound);
        Collections.sort(output);
        return output;
    }

    private void calculatePurchaseValuePrice(List<PortfolioTransaction> input)
    {
        // assume: list is sorted, to FIFO
        long sharesSold = 0;
        for (PortfolioTransaction t : input)
        {
            if (t.getType() == Type.TRANSFER_OUT || t.getType() == Type.SELL || t.getType() == Type.DELIVERY_OUTBOUND)
                sharesSold += t.getShares();
        }

        long sharesBought = 0;
        long value = 0;
        long investment = 0;
        for (PortfolioTransaction t : input)
        {
            if (t.getType() == Type.TRANSFER_IN || t.getType() == Type.BUY || t.getType() == Type.DELIVERY_INBOUND)
            {
                long bought = t.getShares();

                if (sharesSold > 0)
                {
                    sharesSold -= bought;

                    if (sharesSold < 0)
                        bought = -sharesSold;
                    else
                        bought = 0;
                }

                if (bought > 0)
                {
                    sharesBought += bought;
                    value += (bought * t.getActualPurchasePrice()) / Values.Share.factor();
                    investment += bought * t.getAmount() / t.getShares();
                }
            }
        }

        this.purchasePrice = sharesBought > 0 ? (value * Values.Share.factor()) / sharesBought : 0;
        this.purchaseValue = investment;
    }

    public static SecurityPosition merge(SecurityPosition p1, SecurityPosition p2)
    {
        if (!p1.getSecurity().equals(p2.getSecurity()))
            throw new UnsupportedOperationException();

        SecurityPosition answer = new SecurityPosition(p1.getSecurity());
        answer.price = p1.price;
        answer.shares = p1.shares + p2.shares;
        answer.transactions.addAll(p1.transactions);
        answer.transactions.addAll(p2.transactions);
        return answer;
    }

    public static SecurityPosition split(SecurityPosition position, int weight)
    {
        SecurityPosition answer = new SecurityPosition(position.getSecurity());
        answer.price = position.price;
        answer.shares = Math.round(position.shares * weight / (double) Classification.ONE_HUNDRED_PERCENT);

        for (PortfolioTransaction t : position.transactions)
        {
            PortfolioTransaction t2 = new PortfolioTransaction();
            t2.setDate(t.getDate());
            t2.setSecurity(t.getSecurity());
            t2.setType(t.getType());

            t2.setAmount(Math.round(t.getAmount() * weight / (double) Classification.ONE_HUNDRED_PERCENT));
            t2.setFees(Math.round(t.getFees() * weight / (double) Classification.ONE_HUNDRED_PERCENT));
            t2.setShares(Math.round(t.getShares() * weight / (double) Classification.ONE_HUNDRED_PERCENT));

            answer.transactions.add(t2);
        }

        return answer;
    }

}
