package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.List;

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

    public SecurityPosition(Security security)
    {
        this.security = security;
    }

    public SecurityPosition(Security security, SecurityPrice price, long shares)
    {
        this(security);
        this.price = price;
        this.shares = shares;
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
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
    }

    public long calculateValue()
    {
        long p = price != null ? price.getValue() : 0;
        return shares * p / Values.Share.factor();
    }

    public long calculateFIFOPurchasePrice()
    {
        if (transactions.isEmpty())
            return 0;

        // assume: list is sorted, to FIFO
        long sharesSold = 0;
        for (PortfolioTransaction t : transactions)
        {
            if (t.getType() == Type.TRANSFER_OUT || t.getType() == Type.SELL)
                sharesSold += t.getShares();
        }

        long sharesBought = 0;
        long purchasePrice = 0;
        for (PortfolioTransaction t : transactions)
        {
            if (t.getType() == Type.TRANSFER_IN || t.getType() == Type.BUY)
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
                    purchasePrice += (bought * t.getActualPurchasePrice()) / Values.Share.factor();
                }
            }
        }

        return sharesBought > 0 ? (purchasePrice * Values.Share.factor()) / sharesBought : 0;
    }

    public void addTransaction(PortfolioTransaction t)
    {
        transactions.add(t);

        switch (t.getType())
        {
            case BUY:
            case TRANSFER_IN:
                shares += t.getShares();
                break;
            case SELL:
            case TRANSFER_OUT:
                shares -= t.getShares();
                break;
            default:
                throw new RuntimeException();
        }
    }
}
