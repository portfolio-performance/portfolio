package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import name.abuchen.portfolio.online.impl.TLVMarket.utils.TLVHelper.TLVType;

public class IndiceListing
{

    private String id; // "Id": "1209790",
    private String name; // "Name": "ABOU FAMILY B1",
    private String smb; // "Smb": "ABUF.B1",
    private String ISIN; // "ISIN": "IL0012097908",
    private int type; // "Type": 1,
    private String subType; // "SubType": "5",
    private String subTypeDesc; // "SubTypeDesc": "Corporate Bonds",
    private String subId; // "SubId": "002442",
    private String ETFType; // "ETFType": null
    private TLVType TLVType;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
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
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public String getSubType()
    {
        return subType;
    }

    public void setSubType(String subType)
    {
        this.subType = subType;
    }

    public String getSubTypeDesc()
    {
        return subTypeDesc;
    }

    public void setSubTypeDesc(String subTypeDesc)
    {
        this.subTypeDesc = subTypeDesc;
    }

    public TLVType getTLVType()
    {
        return TLVType;
    }

    public void setTLVType(TLVType tLVType)
    {
        TLVType = tLVType;
    }
}
