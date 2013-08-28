package name.abuchen.portfolio.model;

public class DividendTransaction extends Transaction
{
//    public enum Type
//    {
//        DEPOSIT, REMOVAL, INTEREST, DIVIDENDS, FEES, TAXES, BUY, SELL, TRANSFER_IN, TRANSFER_OUT;
//
//        private static final ResourceBundle RESOURCES = ResourceBundle.getBundle("name.abuchen.portfolio.model.labels"); //$NON-NLS-1$
//
//        public String toString()
//        {
//            return RESOURCES.getString("account." + name()); //$NON-NLS-1$
//        }
//    }

//    private Type type;

    private long amount;
    private Account account;
    private long shares;
    private long dividendPerShare;
    private boolean isDiv12;
    private int divEventId;

    public DividendTransaction()
    {}

//    public DividendTransaction(Date date, Security security, Type type, long amount)
//    {
//        super(date, security);
//        this.type = type;
//        this.amount = amount;
//    }

//    public Type getType()
//    {
//        return type;
//    }
//
//    public void setType(Type type)
//    {
//        this.type = type;
//    }

    
    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }
    
    @Override
    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
        updateDividendPerShare();
    }

    public long getShares()
    {
        return shares;
    }

    public void setShares(long shares)
    {
        this.shares = shares;
        updateDividendPerShare();
    }
    
    public long getDividendPerShare()
    {
        return dividendPerShare;
    }
    
    public boolean getIsDiv12 ()
    {
        return isDiv12;
    }

    public void setIsDiv12 (boolean isDiv12)
    {
        this.isDiv12 = isDiv12; 
    }
    
    public int getDivEventId ()
    {
        return divEventId;
    }

    public void setDivEventId (int divEventId)
    {
        this.divEventId = divEventId; 
    }
    
    private void updateDividendPerShare()
    {
        this.dividendPerShare = amountPerShare(amount, shares);
            
    }
    
    static public long amountPerShare (long amount, long shares)
    {
        if (shares != 0)
        {
            return Math.round ((double) amount / (double) shares * Values.Share.divider());          
        }
        else
        {
            return 0;            
        }
    }

    static public long amountTimesShares (long price, long shares)
    {
        if (shares != 0)
        {
            return Math.round ((double) price * (double) shares / Values.Share.divider());          
        }
        else
        {
            return 0;            
        }
    }

}
