package name.abuchen.portfolio.online.portfolioreport;

public class PRPortfolio
{
    private long id;
    private String name;
    private String note;
    private String baseCurrencyCode;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    public String getBaseCurrencyCode()
    {
        return baseCurrencyCode;
    }

    public void setBaseCurrencyCode(String baseCurrencyCode)
    {
        this.baseCurrencyCode = baseCurrencyCode;
    }

}
