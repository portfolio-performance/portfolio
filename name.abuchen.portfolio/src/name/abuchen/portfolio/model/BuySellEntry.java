package name.abuchen.portfolio.model;

import java.util.Date;

import name.abuchen.portfolio.model.PortfolioTransaction.Type;

public class BuySellEntry implements CrossEntry
{
    private Portfolio portfolio;
    private PortfolioTransaction portfolioTransaction;
    private Account account;
    private AccountTransaction accountTransaction;

    public BuySellEntry()
    {}

    public BuySellEntry(Portfolio portfolio, Account account)
    {
        this.portfolio = portfolio;
        this.setPortfolioTransaction(new PortfolioTransaction());
        this.getPortfolioTransaction().setCrossEntry(this);

        this.account = account;
        this.accountTransaction = new AccountTransaction();
        this.accountTransaction.setCrossEntry(this);
    }

    public void setDate(Date date)
    {
        this.getPortfolioTransaction().setDate(date);
        this.accountTransaction.setDate(date);
    }

    public void setType(Type type)
    {
        this.getPortfolioTransaction().setType(type);
        if (!type.equals(PortfolioTransaction.Type.DELIVERY_INBOUND)) {
            this.accountTransaction.setType(AccountTransaction.Type.valueOf(type.name()));
        }
    }

    public void setSecurity(Security security)
    {
        this.getPortfolioTransaction().setSecurity(security);
        this.accountTransaction.setSecurity(security);
    }

    public void setShares(long shares)
    {
        this.getPortfolioTransaction().setShares(shares);
    }

    public void setAmount(long amount)
    {
        this.getPortfolioTransaction().setAmount(amount);
        this.accountTransaction.setAmount(amount);
    }

    public void setFees(long fees)
    {
        this.getPortfolioTransaction().setFees(fees);
    }

    public void insert()
    {
        portfolio.addTransaction(getPortfolioTransaction());
        account.addTransaction(accountTransaction);
    }

    @Override
    public void delete()
    {
        portfolio.getTransactions().remove(getPortfolioTransaction());
        account.getTransactions().remove(accountTransaction);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t == accountTransaction)
        {
            getPortfolioTransaction().setDate(accountTransaction.getDate());
            getPortfolioTransaction().setSecurity(accountTransaction.getSecurity());
            getPortfolioTransaction().setAmount(accountTransaction.getAmount());
            getPortfolioTransaction().setType(PortfolioTransaction.Type.valueOf(accountTransaction.getType().name()));
        }
        else if (t == getPortfolioTransaction())
        {
            accountTransaction.setDate(getPortfolioTransaction().getDate());
            accountTransaction.setSecurity(getPortfolioTransaction().getSecurity());
            accountTransaction.setAmount(getPortfolioTransaction().getAmount());
            accountTransaction.setType(AccountTransaction.Type.valueOf(getPortfolioTransaction().getType().name()));
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(getPortfolioTransaction()))
            return accountTransaction;
        else if (t.equals(accountTransaction))
            return getPortfolioTransaction();
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public Object getCrossEntity(Transaction t)
    {
        if (t.equals(getPortfolioTransaction()))
            return account;
        else if (t.equals(accountTransaction))
            return portfolio;
        else
            throw new UnsupportedOperationException();
    }

    public PortfolioTransaction getPortfolioTransaction()
    {
        return portfolioTransaction;
    }

    public void setPortfolioTransaction(PortfolioTransaction portfolioTransaction)
    {
        this.portfolioTransaction = portfolioTransaction;
    }
}
