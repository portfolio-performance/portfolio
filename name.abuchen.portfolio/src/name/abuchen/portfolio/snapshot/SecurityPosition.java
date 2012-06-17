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

    private transient boolean isDirty = true;
    private transient long marketValue;
    private transient long purchasePrice;
    private transient long purchaseValue;

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

    public long getDelta()
    {
        calculate();
        return marketValue - purchaseValue;
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
            calculatePurchaseValuePrice();
        }

        isDirty = false;
    }

    private void calculatePurchaseValuePrice()
    {
        // assume: list is sorted, to FIFO
        long sharesSold = 0;
        for (PortfolioTransaction t : transactions)
        {
            if (t.getType() == Type.TRANSFER_OUT || t.getType() == Type.SELL)
                sharesSold += t.getShares();
        }

        long sharesBought = 0;
        long value = 0;
        long investment = 0;
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
                    value += (bought * t.getActualPurchasePrice()) / Values.Share.factor();
                    investment += bought * t.getAmount() / t.getShares();
                }
            }
        }

        this.purchasePrice = sharesBought > 0 ? (value * Values.Share.factor()) / sharesBought : 0;
        this.purchaseValue = investment;
    }
}
