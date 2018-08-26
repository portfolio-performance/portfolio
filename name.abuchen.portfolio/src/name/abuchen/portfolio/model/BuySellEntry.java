package name.abuchen.portfolio.model;

import java.time.LocalDateTime;

import name.abuchen.portfolio.model.PortfolioTransaction.Type;
import name.abuchen.portfolio.money.Money;

public class BuySellEntry implements CrossEntry, Annotated
{
    private Portfolio portfolio;
    private PortfolioTransaction portfolioTransaction;
    private Account account;
    private AccountTransaction accountTransaction;

    public BuySellEntry()
    {
        this(null, null);
    }

    public BuySellEntry(Portfolio portfolio, Account account)
    {
        this.portfolio = portfolio;
        this.portfolioTransaction = new PortfolioTransaction();
        this.portfolioTransaction.setCrossEntry(this);

        this.account = account;
        this.accountTransaction = new AccountTransaction();
        this.accountTransaction.setCrossEntry(this);
    }

    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }

    public Portfolio getPortfolio()
    {
        return this.portfolio;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }

    public Account getAccount()
    {
        return this.account;
    }

    public void setDate(LocalDateTime date)
    {
        this.portfolioTransaction.setDateTime(date);
        this.accountTransaction.setDateTime(date);
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
    }

    public void setShares(long shares)
    {
        this.portfolioTransaction.setShares(shares);
    }

    public void setAmount(long amount)
    {
        this.portfolioTransaction.setAmount(amount);
        this.accountTransaction.setAmount(amount);
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.portfolioTransaction.setCurrencyCode(currencyCode);
        this.accountTransaction.setCurrencyCode(currencyCode);
    }

    public void setMonetaryAmount(Money amount)
    {
        this.portfolioTransaction.setMonetaryAmount(amount);
        this.accountTransaction.setMonetaryAmount(amount);
    }

    @Override
    public String getNote()
    {
        return this.portfolioTransaction.getNote();
    }

    @Override
    public void setNote(String note)
    {
        this.portfolioTransaction.setNote(note);
        this.accountTransaction.setNote(note);
    }

    @Override
    public void insert()
    {
        portfolio.addTransaction(portfolioTransaction);
        account.addTransaction(accountTransaction);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t == accountTransaction)
        {
            portfolioTransaction.setDateTime(accountTransaction.getDateTime());
            portfolioTransaction.setSecurity(accountTransaction.getSecurity());
            portfolioTransaction.setNote(accountTransaction.getNote());
        }
        else if (t == portfolioTransaction)
        {
            accountTransaction.setDateTime(portfolioTransaction.getDateTime());
            accountTransaction.setSecurity(portfolioTransaction.getSecurity());
            accountTransaction.setNote(portfolioTransaction.getNote());
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public TransactionOwner<? extends Transaction> getOwner(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return portfolio;
        else if (t.equals(accountTransaction))
            return account;
        else
            throw new UnsupportedOperationException();

    }

    @Override
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        if (t.equals(portfolioTransaction) && owner instanceof Portfolio)
            portfolio = (Portfolio) owner;
        else if (t.equals(accountTransaction) && owner instanceof Account)
            account = (Account) owner;
        else
            throw new IllegalArgumentException();
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
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        if (t.equals(portfolioTransaction))
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

    public AccountTransaction getAccountTransaction()
    {
        return accountTransaction;
    }
}
