package name.abuchen.portfolio.online.portfolioreport;

import java.time.Instant;

public class PRAccount
{
    private String type; 
    private String name;
    private String uuid;
    private String currencyCode;
    private String referenceAccountUuid;
    private boolean active;
    private String note;
    private Instant updatedAt;

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

    public String getReferenceAccountUuid()
    {
        return referenceAccountUuid;
    }

    public void setReferenceAccountUuid(String referenceAccountUuid)
    {
        this.referenceAccountUuid = referenceAccountUuid;
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

    public Instant getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt)
    {
        this.updatedAt = updatedAt;
    }
}
