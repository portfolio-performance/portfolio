package name.abuchen.portfolio.model;

import java.util.Date;

import name.abuchen.portfolio.model.PortfolioTransaction.Type;

public class BuySellEntry implements CrossEntry
{
    private Portfolio portfolio;
    private PortfolioTransaction portfolioTransaction;
    private Account account;
    private AccountTransaction securityTransaction;
    private AccountTransaction taxTransaction;
    private AccountTransaction feeTransaction;

    public BuySellEntry()
    {}

    public BuySellEntry(Portfolio portfolio, Account account)
    {
        this.portfolio = portfolio;
        this.portfolioTransaction = new PortfolioTransaction();
        this.portfolioTransaction.setCrossEntry(this);

        this.account = account;
        this.securityTransaction = new AccountTransaction();
        this.securityTransaction.setCrossEntry(this);

        this.taxTransaction = new AccountTransaction();
        this.taxTransaction.setCrossEntry(this);
        this.taxTransaction.setType(AccountTransaction.Type.TAXES);

        this.feeTransaction = new AccountTransaction();
        this.feeTransaction.setCrossEntry(this);
        this.feeTransaction.setType(AccountTransaction.Type.FEES);
    }

    public void setDate(Date date)
    {
        this.portfolioTransaction.setDate(date);
        this.securityTransaction.setDate(date);
        this.taxTransaction.setDate(date);
        this.feeTransaction.setDate(date);
    }

    public void setType(Type type)
    {
        this.portfolioTransaction.setType(type);
        this.securityTransaction.setType(AccountTransaction.Type.valueOf(type.name()));
    }

    public void setSecurity(Security security)
    {
        this.portfolioTransaction.setSecurity(security);
        this.securityTransaction.setSecurity(security);
        this.taxTransaction.setSecurity(security);
        this.feeTransaction.setSecurity(security);
    }

    public void setShares(long shares)
    {
        this.portfolioTransaction.setShares(shares);
        this.securityTransaction.setShares(shares);
        this.taxTransaction.setShares(shares);
        this.feeTransaction.setShares(shares);
    }

    public void setAmount(long amount)
    {
        this.portfolioTransaction.setAmount(amount);
        this.securityTransaction.setAmount(amount - taxTransaction.getAmount() - feeTransaction.getAmount());
    }

    public void setTaxes(long taxAmount)
    {
        taxTransaction.setAmount(taxAmount);
    }

    public void setFees(long fees)
    {
        this.portfolioTransaction.setFees(fees + taxTransaction.getAmount());
        this.feeTransaction.setAmount(fees);
    }

    public void insert()
    {
        portfolio.addTransaction(portfolioTransaction);
        account.addTransaction(securityTransaction);
        account.addTransaction(taxTransaction);
        account.addTransaction(feeTransaction);
    }

    @Override
    public void delete()
    {
        portfolio.getTransactions().remove(portfolioTransaction);
        account.getTransactions().remove(securityTransaction);
        account.getTransactions().remove(taxTransaction);
        account.getTransactions().remove(feeTransaction);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t == securityTransaction)
        {
            portfolioTransaction.setDate(securityTransaction.getDate());
            portfolioTransaction.setSecurity(securityTransaction.getSecurity());
            portfolioTransaction.setAmount(securityTransaction.getAmount());
            portfolioTransaction.setType(PortfolioTransaction.Type.valueOf(securityTransaction.getType().name()));
        }
        else if (t == portfolioTransaction)
        {
            securityTransaction.setDate(portfolioTransaction.getDate());
            securityTransaction.setSecurity(portfolioTransaction.getSecurity());
            securityTransaction.setAmount(portfolioTransaction.getAmount());
            securityTransaction.setType(AccountTransaction.Type.valueOf(portfolioTransaction.getType().name()));
        }
        else if (t == feeTransaction || t == taxTransaction)
        {
            portfolioTransaction.setDate(t.getDate());
            portfolioTransaction.setSecurity(t.getSecurity());
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
        else if (t.equals(securityTransaction))
            return account;
        else
            throw new UnsupportedOperationException();

    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return securityTransaction;
        else if (t.equals(securityTransaction))
            return portfolioTransaction;
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public Object getCrossEntity(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return account;
        else if (t.equals(securityTransaction))
            return portfolio;
        else if (t.equals(taxTransaction))
            return portfolio;
        else if (t.equals(feeTransaction))
            return portfolio;
        throw new UnsupportedOperationException();
    }

    public PortfolioTransaction getPortfolioTransaction()
    {
        return portfolioTransaction;
    }

    public AccountTransaction getAccountTransaction()
    {
        return securityTransaction;
    }

    public AccountTransaction getTaxesTransaction()
    {
        return taxTransaction;
    }

    public AccountTransaction getFeeTransaction()
    {
        return feeTransaction;
    }
}
