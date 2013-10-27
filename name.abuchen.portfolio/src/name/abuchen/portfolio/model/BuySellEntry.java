package name.abuchen.portfolio.model;

import java.util.Date;

import name.abuchen.portfolio.model.PortfolioTransaction.Type;

public class BuySellEntry implements CrossEntry
{
    private Portfolio portfolio;
    private PortfolioTransaction portfolioTransaction;
    private Account account;
    private AccountTransaction accountTransaction;
    private AccountTransaction taxTransaction;

    public BuySellEntry()
    {}

    public BuySellEntry(Portfolio portfolio, Account account)
    {
        this.portfolio = portfolio;
        this.portfolioTransaction = new PortfolioTransaction();
        this.portfolioTransaction.setCrossEntry(this);

        this.account = account;
        this.accountTransaction = new AccountTransaction();
        this.accountTransaction.setCrossEntry(this);
        this.taxTransaction = new AccountTransaction();
        this.taxTransaction.setCrossEntry(this);
        this.taxTransaction.setType(AccountTransaction.Type.TAXES);
    }

    public void setDate(Date date)
    {
        this.portfolioTransaction.setDate(date);
        this.accountTransaction.setDate(date);
        this.taxTransaction.setDate(date);
    }

    public void setType(Type type)
    {
        this.portfolioTransaction.setType(type);
        this.accountTransaction.setType(AccountTransaction.Type.valueOf(type.name()));
    }

    public void setSecurity(Security security)
    {
        this.portfolioTransaction.setSecurity(security);
        this.accountTransaction.setSecurity(security);
        this.taxTransaction.setSecurity(security);
    }

    public void setShares(long shares)
    {
        this.portfolioTransaction.setShares(shares);
        this.accountTransaction.setShares(shares);
        this.taxTransaction.setShares(shares);
    }

    public void setAmount(long amount)
    {
        this.portfolioTransaction.setAmount(amount);
        this.accountTransaction.setAmount(amount - taxTransaction.getAmount());
    }

    public void setTaxes(long taxAmount)
    {
        taxTransaction.setAmount(taxAmount);
    }

    public void setFees(long fees)
    {
        this.portfolioTransaction.setFees(fees + taxTransaction.getAmount());
    }

    public void insert()
    {
        portfolio.addTransaction(portfolioTransaction);
        account.addTransaction(accountTransaction);
        account.addTransaction(taxTransaction);
    }

    @Override
    public void delete()
    {
        portfolio.getTransactions().remove(portfolioTransaction);
        account.getTransactions().remove(accountTransaction);
        account.getTransactions().remove(taxTransaction);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t == accountTransaction)
        {
            portfolioTransaction.setDate(accountTransaction.getDate());
            portfolioTransaction.setSecurity(accountTransaction.getSecurity());
            portfolioTransaction.setAmount(accountTransaction.getAmount());
            portfolioTransaction.setType(PortfolioTransaction.Type.valueOf(accountTransaction.getType().name()));
        }
        else if (t == portfolioTransaction)
        {
            accountTransaction.setDate(portfolioTransaction.getDate());
            accountTransaction.setSecurity(portfolioTransaction.getSecurity());
            accountTransaction.setAmount(portfolioTransaction.getAmount());
            accountTransaction.setType(AccountTransaction.Type.valueOf(portfolioTransaction.getType().name()));
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Object getEntity(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return portfolio;
        else if (t.equals(accountTransaction))
            return account;
        else
            throw new UnsupportedOperationException();

    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return accountTransaction;
        else if (t.equals(accountTransaction))
            return portfolioTransaction;
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public Object getCrossEntity(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return account;
        else if (t.equals(accountTransaction))
            return portfolio;
        else if (t.equals(taxTransaction))
            return portfolio;
        throw new UnsupportedOperationException();
    }

    public PortfolioTransaction getPortfolioTransaction()
    {
        return portfolioTransaction;
    }

    public AccountTransaction getAccountTransaction()
    {
        return accountTransaction;
    }

    public AccountTransaction getTaxesTransaction()
    {
        return taxTransaction;
    }
}
