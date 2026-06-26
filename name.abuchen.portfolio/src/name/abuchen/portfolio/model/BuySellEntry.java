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
    private boolean readOnly;

    public BuySellEntry()
    {
        this(null, null);
    }

    public BuySellEntry(Type type)
    {
        this(null, null);
        setType(type);
    }

    public BuySellEntry(Portfolio portfolio, Account account)
    {
        this(portfolio, new PortfolioTransaction(), account, new AccountTransaction());
    }

    /* protobuf only */ BuySellEntry(Portfolio portfolio, PortfolioTransaction portfolioTx, Account account,
                    AccountTransaction accountTx)
    {
        this(portfolio, portfolioTx, account, accountTx, false);
    }

    private BuySellEntry(Portfolio portfolio, PortfolioTransaction portfolioTx, Account account,
                    AccountTransaction accountTx, boolean readOnly)
    {
        this.portfolio = portfolio;
        this.portfolioTransaction = portfolioTx;
        this.account = account;
        this.accountTransaction = accountTx;
        this.readOnly = readOnly;

        if (!readOnly)
        {
            this.portfolioTransaction.setCrossEntry(this);
            this.accountTransaction.setCrossEntry(this);
        }
    }

    public static BuySellEntry readOnly(Portfolio portfolio, PortfolioTransaction portfolioTx, Account account,
                    AccountTransaction accountTx)
    {
        return new BuySellEntry(portfolio, portfolioTx, account, accountTx, true);
    }

    public void setPortfolio(Portfolio portfolio)
    {
        assertWritable();

        this.portfolio = portfolio;
    }

    public Portfolio getPortfolio()
    {
        return this.portfolio;
    }

    public void setAccount(Account account)
    {
        assertWritable();

        this.account = account;
    }

    public Account getAccount()
    {
        return this.account;
    }

    public void setDate(LocalDateTime date)
    {
        assertWritable();

        this.portfolioTransaction.setDateTime(date);
        this.accountTransaction.setDateTime(date);
    }

    public void setType(Type type)
    {
        assertWritable();

        this.portfolioTransaction.setType(type);
        this.accountTransaction.setType(AccountTransaction.Type.valueOf(type.name()));
    }

    public void setSecurity(Security security)
    {
        assertWritable();

        this.portfolioTransaction.setSecurity(security);
        this.accountTransaction.setSecurity(security);
    }

    public void setShares(long shares)
    {
        assertWritable();

        this.portfolioTransaction.setShares(shares);
    }

    public void setAmount(long amount)
    {
        assertWritable();

        this.portfolioTransaction.setAmount(amount);
        this.accountTransaction.setAmount(amount);
    }

    public void setCurrencyCode(String currencyCode)
    {
        assertWritable();

        this.portfolioTransaction.setCurrencyCode(currencyCode);
        this.accountTransaction.setCurrencyCode(currencyCode);
    }

    public void setMonetaryAmount(Money amount)
    {
        assertWritable();

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
        assertWritable();

        this.portfolioTransaction.setNote(note);
        this.accountTransaction.setNote(note);
    }

    @Override
    public String getSource()
    {
        return this.portfolioTransaction.getSource();
    }

    @Override
    public void setSource(String source)
    {
        assertWritable();

        this.portfolioTransaction.setSource(source);
        this.accountTransaction.setSource(source);
    }

    @Override
    public void insert()
    {
        assertWritable();

        // add first the account transaction which might fail due to
        // a currency mismatch
        account.addTransaction(accountTransaction);
        portfolio.addTransaction(portfolioTransaction);
    }

    @Override
    public void updateFrom(Transaction t)
    {
        if (t == accountTransaction)
        {
            if (readOnly)
                return;

            portfolioTransaction.setDateTime(accountTransaction.getDateTime());
            portfolioTransaction.setSecurity(accountTransaction.getSecurity());
            portfolioTransaction.setNote(accountTransaction.getNote());
        }
        else if (t == portfolioTransaction)
        {
            if (readOnly)
                return;

            accountTransaction.setDateTime(portfolioTransaction.getDateTime());
            accountTransaction.setSecurity(portfolioTransaction.getSecurity());
            accountTransaction.setNote(portfolioTransaction.getNote());
        }
        else
        {
            throw new UnsupportedOperationException("transaction can't be used for update: " + t); //$NON-NLS-1$
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
            throw new UnsupportedOperationException("unable to get owner for transcation " + t); //$NON-NLS-1$

    }

    @Override
    public void setOwner(Transaction t, TransactionOwner<? extends Transaction> owner)
    {
        assertWritable();

        if (t.equals(portfolioTransaction) && owner instanceof Portfolio p)
            portfolio = p;
        else if (t.equals(accountTransaction) && owner instanceof Account a)
            account = a;
        else
            throw new IllegalArgumentException("unable to set owner for transcation " + t); //$NON-NLS-1$
    }

    @Override
    public Transaction getCrossTransaction(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return accountTransaction;
        else if (t.equals(accountTransaction))
            return portfolioTransaction;
        else
            throw new UnsupportedOperationException("unable to get cross transaction for transcation " + t); //$NON-NLS-1$
    }

    @Override
    public TransactionOwner<? extends Transaction> getCrossOwner(Transaction t)
    {
        if (t.equals(portfolioTransaction))
            return account;
        else if (t.equals(accountTransaction))
            return portfolio;
        else
            throw new UnsupportedOperationException("unable to get cross owner for transcation " + t); //$NON-NLS-1$
    }

    public PortfolioTransaction getPortfolioTransaction()
    {
        return portfolioTransaction;
    }

    public AccountTransaction getAccountTransaction()
    {
        return accountTransaction;
    }

    private void assertWritable()
    {
        if (readOnly)
            throw new UnsupportedOperationException("Ledger-backed buy/sell cross entries are read-only"); //$NON-NLS-1$
    }
}
