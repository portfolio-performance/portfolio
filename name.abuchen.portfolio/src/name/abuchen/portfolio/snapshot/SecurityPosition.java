package name.abuchen.portfolio.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.model.Transaction;
import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.money.Money;

public class SecurityPosition
{
    private final InvestmentVehicle investment;
    private final SecurityPrice price;
    private final long shares;
    private final List<PortfolioTransaction> transactions;

    private transient boolean isDirty = true;
    private transient long marketValue;
    private transient long purchasePrice;
    private transient long purchaseValue;

    private SecurityPosition(InvestmentVehicle investment, SecurityPrice price, long shares,
                    List<PortfolioTransaction> transactions)
    {
        this.investment = investment;
        this.price = price;
        this.shares = shares;
        this.transactions = transactions;
    }

    public SecurityPosition(AccountSnapshot snapshot)
    {
        Objects.requireNonNull(snapshot);
        Objects.requireNonNull(snapshot.getAccount());

        this.investment = snapshot.getAccount();
        this.price = new SecurityPrice(snapshot.getTime(), snapshot.getUnconvertedFunds().getAmount());
        this.shares = Values.Share.factor();
        this.transactions = new ArrayList<PortfolioTransaction>();
    }

    public SecurityPosition(Security security, SecurityPrice price, List<PortfolioTransaction> transactions)
    {
        Objects.requireNonNull(security);
        Objects.requireNonNull(price);

        this.investment = security;
        this.price = price;
        this.shares = transactions.stream().mapToLong(t -> {
            switch (t.getType())
            {
                case BUY:
                case TRANSFER_IN:
                case DELIVERY_INBOUND:
                    return t.getShares();
                case SELL:
                case TRANSFER_OUT:
                case DELIVERY_OUTBOUND:
                    return -t.getShares();
                default:
                    throw new UnsupportedOperationException();
            }
        }).sum();
        this.transactions = new ArrayList<PortfolioTransaction>(transactions);
    }

    public Security getSecurity()
    {
        return investment instanceof Security ? (Security) investment : null;
    }

    public InvestmentVehicle getInvestmentVehicle()
    {
        return investment;
    }

    public SecurityPrice getPrice()
    {
        return price;
    }

    public long getShares()
    {
        return shares;
    }

    public Money calculateValue()
    {
        calculate();
        return Money.of(investment.getCurrencyCode(), marketValue);
    }

    public Money getFIFOPurchasePrice()
    {
        calculate();
        return Money.of(investment.getCurrencyCode(), purchasePrice);
    }

    public Money getFIFOPurchaseValue()
    {
        calculate();
        return Money.of(investment.getCurrencyCode(), purchaseValue);
    }

    public Money getProfitLoss()
    {
        calculate();
        return Money.of(investment.getCurrencyCode(), investment instanceof Security ? marketValue - purchaseValue : 0);
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
        return output;
    }

    private void calculatePurchaseValuePrice(List<PortfolioTransaction> input)
    {
        Collections.sort(input, new Transaction.ByDate());

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

        List<PortfolioTransaction> allTransactions = new ArrayList<PortfolioTransaction>();
        allTransactions.addAll(p1.transactions);
        allTransactions.addAll(p2.transactions);

        return new SecurityPosition(p1.getSecurity(), p1.price, p1.shares + p2.shares, allTransactions);
    }

    public static SecurityPosition split(SecurityPosition position, int weight)
    {
        List<PortfolioTransaction> splitTransactions = new ArrayList<PortfolioTransaction>(position.transactions.size());

        for (PortfolioTransaction t : position.transactions)
        {
            PortfolioTransaction t2 = new PortfolioTransaction();
            t2.setDate(t.getDate());
            t2.setSecurity(t.getSecurity());
            t2.setType(t.getType());

            t2.setAmount(Math.round(t.getAmount() * weight / (double) Classification.ONE_HUNDRED_PERCENT));
            t2.setFees(Math.round(t.getFees() * weight / (double) Classification.ONE_HUNDRED_PERCENT));
            t2.setTaxes(Math.round(t.getTaxes() * weight / (double) Classification.ONE_HUNDRED_PERCENT));
            t2.setShares(Math.round(t.getShares() * weight / (double) Classification.ONE_HUNDRED_PERCENT));

            splitTransactions.add(t2);
        }

        return new SecurityPosition(position.investment, position.price, Math.round(position.shares * weight
                        / (double) Classification.ONE_HUNDRED_PERCENT), splitTransactions);
    }
}
