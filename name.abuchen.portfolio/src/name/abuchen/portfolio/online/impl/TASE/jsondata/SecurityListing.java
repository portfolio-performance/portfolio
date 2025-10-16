package name.abuchen.portfolio.online.impl.TASE.jsondata;

/**
 * Representation of JSON Security Listing returned by TASE API
 */
public class SecurityListing
{

    private String Id;
    private String Name;
    private String Smb;
    private String ISIN;
    private String Type;
    private String SubType;
    private String SubTypeDesc;
    private String SubId;
    private String ETFType;
    private String BaseRate;
    private String HighRate;
    private String LowRate;
    private String MarketValue;
    private String OverallTurnOverUnits;
    private String LongName;
    private String TradeDate;
    private String LastRate;
    private String SecurityLongName;



    private class Indicator
    {
        private String Key;
        private boolean Value;
        private String Desc;

    }

    public SecurityListing(String Name, String Smb, String ISIN, String Type, String SubType)
    {
        this.Name = Name;
        this.Smb = Smb;
        this.ISIN = ISIN;
        this.Type = Type;
        this.SubType = SubType;
    }

    public String getId()
    {
        return this.Id;
    }

    public String getType()
    {
        return this.Type;
    }

    public String getSubType()
    {
        return this.SubType;
    }

    public String getSubTypeDesc()
    {
        return this.SubTypeDesc;
    }

    public String getOrDefault(String var1, String var2)
    {
        return ((var1 == null) || (var1.length() == 0)) ? var2 : ""; //$NON-NLS-1$
    }


    @Override
    public String toString()
    {
        String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
        String listing = "Id: " + Id + newLine; //$NON-NLS-1$
        listing += "Name: " + Name + newLine; //$NON-NLS-1$
        listing += "Smb: " + Smb + newLine; //$NON-NLS-1$
        listing += "ISIN " + ISIN + newLine; //$NON-NLS-1$
        listing += "Type " + Type + newLine; //$NON-NLS-1$
        listing += "SubType " + SubType + newLine; //$NON-NLS-1$
        listing += "SubTypeDesc " + SubTypeDesc + newLine; //$NON-NLS-1$
        listing += "SubId " + SubId + newLine; //$NON-NLS-1$
        listing += "ETFType " + ETFType + newLine; //$NON-NLS-1$
        listing += "Long Name " + LongName + newLine; //$NON-NLS-1$
        listing += "Security Long Name " + SecurityLongName + newLine; //$NON-NLS-1$

        return listing;
    }

    public String getHighRate()
    {
        return HighRate;
    }

    public void setHighRate(String highRate)
    {
        HighRate = highRate;
    }

    public String getLowRate()
    {
        return LowRate;
    }

    public void setLowRate(String lowRate)
    {
        LowRate = lowRate;
    }

    public String getOverallTurnOverUnits()
    {
        return OverallTurnOverUnits;
    }

    public void setOverallTurnOverUnits(String overallTurnOverUnits)
    {
        OverallTurnOverUnits = overallTurnOverUnits;
    }

    public String getLastRate()
    {
        return LastRate;
    }

    public void setLastRate(String lastRate)
    {
        LastRate = lastRate;
    }

    public String getTradeDate()
    {
        return TradeDate;
    }

    public void setTradeDate(String tradeDate)
    {
        TradeDate = tradeDate;
    }
}