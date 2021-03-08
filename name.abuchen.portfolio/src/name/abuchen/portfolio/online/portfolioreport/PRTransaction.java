package name.abuchen.portfolio.online.portfolioreport;

import java.time.LocalDateTime;

public class PRTransaction
{
    private long id;
    private String type; 

    private long accountId;
    private LocalDateTime datetime;
    private long partnerTransactionId;
    private PRTransaction partnerTransaction;
    // TODO: private List<TransactionUnit> units; 
    private String shares;
    private long securityId;
    private String note;
    

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }
    
    public String getType()
    {
        return type;  
    }

    public void setType(String type)
    {
        this.type = type;
    }
    
    public long getAccountId()
    {
        return accountId;
    }

    public void setAccountId(long accountId)
    {
        this.accountId = accountId;
    }

    public LocalDateTime getDatetime()
    {
        return datetime;
    }

    public void setDatetime(LocalDateTime datetime)
    {
        this.datetime = datetime;
    }

    public long getPartnerTransactionId()
    {
        return partnerTransactionId;
    }

    public void setPartnerTransactionId(long partnerTransactionId)
    {
        this.partnerTransactionId = partnerTransactionId;
    }

    public PRTransaction getPartnerTransaction()
    {
        return partnerTransaction;
    }

    public void setPartnerTransaction(PRTransaction partnerTransaction)
    {
        this.partnerTransaction = partnerTransaction;
    }

    public String getShares()
    {
        return shares;
    }

    public void setShares(String shares)
    {
        this.shares = shares;
    }

    public long getSecurityId()
    {
        return securityId;
    }

    public void setSecurityId(long securityId)
    {
        this.securityId = securityId;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }
}
