package name.abuchen.portfolio.online.impl.TASE.jsondata;

import name.abuchen.portfolio.online.impl.TASE.utils.TASEHelper.TaseType;

public class IndiceListing
{

    private String Id;
    private String Name;
    private String Smb;
    private String ISIN;
    private int Type;
    private String SubType;
    private String SubTypeDesc;
    private String SubId;
    private String ETFType;
    private TaseType TaseType;

    public String getId()
    {
        return Id;
    }

    public void setId(String id)
    {
        this.Id = id;
    }

    public String getName()
    {
        return Name;
    }

    public void setName(String name)
    {
        this.Name = name;
    }

    public String getISIN()
    {
        return ISIN;
    }

    public void setISIN(String iSIN)
    {
        ISIN = iSIN;
    }

    public int getType()
    {
        return Type;
    }

    public void setType(int type)
    {
        this.Type = type;
    }

    public String getSubType()
    {
        return SubType;
    }

    public void setSubType(String subType)
    {
        this.SubType = subType;
    }

    public String getSubTypeDesc()
    {
        return SubTypeDesc;
    }

    public void setSubTypeDesc(String subTypeDesc)
    {
        this.SubTypeDesc = subTypeDesc;
    }

    public TaseType getTaseType()
    {
        return TaseType;
    }

    public void setTaseType(TaseType taseType)
    {
        TaseType = taseType;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "IndiceListing [id=" + Id + ", name=" + Name + ", smb=" + Smb + ", ISIN=" + ISIN + ", type=" + Type
                        + ", subType=" + SubType + ", subTypeDesc=" + SubTypeDesc + ", subId=" + SubId + ", ETFType="
                        + ETFType + ", TaseType=" + TaseType + "]";
    }

    public String getSmb()
    {
        return Smb == null ? "" : Smb; //$NON-NLS-1$
    }

    public void setSmb(String smb)
    {
        Smb = smb;
    }
}
