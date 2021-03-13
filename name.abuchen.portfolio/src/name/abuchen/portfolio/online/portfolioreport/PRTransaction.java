package name.abuchen.portfolio.online.portfolioreport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PRTransaction
{
    private String uuid;
    private String type;

    private String accountUuid;
    private String datetime;
    private String partnerTransactionUuid;
    private List<PRTransactionUnit> units;
    private String shares;
    private String portfolioSecurityUuid;
    private String note;
    private Instant updatedAt;

    public PRTransaction()
    {
    }

    public PRTransaction(PRTransaction source)
    {
        this.uuid = source.uuid;
        this.type = source.type;
        this.accountUuid = source.accountUuid;
        this.datetime = source.datetime;
        this.partnerTransactionUuid = source.partnerTransactionUuid;
        this.units = source.units != null ? new ArrayList<PRTransactionUnit>(source.units) : null;
        this.shares = source.shares;
        this.portfolioSecurityUuid = source.portfolioSecurityUuid;
        this.note = source.note;
        this.updatedAt = source.updatedAt;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getAccountUuid()
    {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid)
    {
        this.accountUuid = accountUuid;
    }

    public String getDatetime()
    {
        return datetime;
    }

    public void setDatetime(String datetime)
    {
        this.datetime = datetime;
    }

    public void setDatetime(LocalDateTime datetime)
    {
        this.datetime = datetime.atZone(ZoneId.systemDefault()).toOffsetDateTime()
                        .format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public String getPartnerTransactionUuid()
    {
        return partnerTransactionUuid;
    }

    public void setPartnerTransactionUuid(String partnerTransactionUuid)
    {
        this.partnerTransactionUuid = partnerTransactionUuid;
    }

    public String getShares()
    {
        return shares;
    }

    public void setShares(String shares)
    {
        this.shares = shares;
    }

    public void setShares(long shares)
    {
        this.shares = Double.toString(shares / 1.e8);
    }

    public List<PRTransactionUnit> getUnits()
    {
        return units;
    }

    public void setUnits(List<PRTransactionUnit> units)
    {
        this.units = units;
    }

    public void addUnit(PRTransactionUnit unit)
    {
        if (this.units == null)
            units = new ArrayList<PRTransactionUnit>();

        units.add(unit);
    }

    public String getPortfolioSecurityUuid()
    {
        return portfolioSecurityUuid;
    }

    public void setPortfolioSecurityUuid(String portfolioSecurityUuid)
    {
        this.portfolioSecurityUuid = portfolioSecurityUuid;
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
