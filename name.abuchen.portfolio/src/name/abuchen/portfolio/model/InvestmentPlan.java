package name.abuchen.portfolio.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;

public class InvestmentPlan
{
    private String name;
    private Security security;
    private Portfolio portfolio;
    private Account account;

    private Date start;
    private int interval = 1;

    private long amount;
    private long fees;

    private List<PortfolioTransaction> transactions = new ArrayList<PortfolioTransaction>();

    public InvestmentPlan()
    {

    }

    public InvestmentPlan(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        this.security = security;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }

    public Date getStart()
    {
        return start;
    }

    public void setStart(Date start)
    {
        this.start = start;
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        this.fees = fees;
    }

    public List<PortfolioTransaction> getTransactions()
    {
        return transactions;
    }

    public void removeTransaction(PortfolioTransaction transaction)
    {
        transactions.remove(transaction);
    }

    private DateTime getLastDate()
    {
        DateTime last = null;
        for (PortfolioTransaction t : transactions)
        {
            DateTime date = new DateTime(t.getDate());
            if (last == null || last.isBefore(date))
                last = date;
        }

        return last;
    }

    private DateTime next(DateTime date)
    {
        DateTime next = date.plusMonths(interval);
        next = next.withDayOfMonth(Math.min(next.dayOfMonth().getMaximumValue(), new DateTime(start).getDayOfMonth()));
        return next;
    }

    public List<PortfolioTransaction> generateTransactions()
    {
        DateTime transactionDate = null;

        if (transactions.isEmpty())
            transactionDate = new DateTime(start);
        else
            transactionDate = next(getLastDate());

        List<PortfolioTransaction> newlyCreated = new ArrayList<PortfolioTransaction>();

        DateTime now = DateTime.now();

        while (transactionDate.isBefore(now))
        {
            long price = getSecurity().getSecurityPrice(transactionDate.toDate()).getValue();
            long shares = Math.round(((double) (amount - fees) / (double) price) * Values.Share.factor());

            PortfolioTransaction transaction = null;
            if (account != null)
            {
                BuySellEntry entry = new BuySellEntry(portfolio, account);
                entry.setType(PortfolioTransaction.Type.BUY);
                entry.setDate(transactionDate.toDate());
                entry.setShares(shares);
                entry.setFees(fees);
                entry.setAmount(amount);
                entry.setSecurity(getSecurity());
                entry.insert();

                transaction = entry.getPortfolioTransaction();
            }
            else
            {
                transaction = new PortfolioTransaction(transactionDate.toDate(), security,
                                PortfolioTransaction.Type.DELIVERY_INBOUND, shares, amount, fees);
                portfolio.addTransaction(transaction);

            }
            transactions.add(transaction);
            newlyCreated.add(transaction);

            transactionDate = next(transactionDate);
        }

        return newlyCreated;
    }
}
