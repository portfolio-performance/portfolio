package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.TLVType;

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
    private TLVType TLVType;

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

    public TLVType getTLVType()
    {
        return TLVType;
    }

    public void setTLVType(TLVType tLVType)
    {
        TLVType = tLVType;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "IndiceListing [id=" + Id + ", name=" + Name + ", smb=" + Smb + ", ISIN=" + ISIN + ", type=" + Type
                        + ", subType=" + SubType + ", subTypeDesc=" + SubTypeDesc + ", subId=" + SubId + ", ETFType="
                        + ETFType + ", TLVType=" + TLVType + "]";
    }

    public String getSmb()
    {
        return Smb;
    }

    public void setSmb(String smb)
    {
        Smb = smb;
    }
}
