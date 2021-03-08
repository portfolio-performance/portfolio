package name.abuchen.portfolio.online.portfolioreport;

public class PRAccount
{
    private long id;
    private String type; 
    private String name;
    private String uuid;
    private String currencyCode;
    private long referenceAccountId;
    private boolean active;
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
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getCurrencyCode()
    {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode)
    {
        this.currencyCode = currencyCode;
    }

    public long getReferenceAccountId()
    {
        return referenceAccountId;
    }

    public void setReferenceAccountId(long referenceAccountId)
    {
        this.referenceAccountId = referenceAccountId;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
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
