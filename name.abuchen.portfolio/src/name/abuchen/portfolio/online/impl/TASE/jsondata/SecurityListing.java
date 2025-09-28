package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

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
    private String OpenRate;
    private String TradeDataLink;
    private String EODTradeDate;
    private String TurnOverValue;
    private String MarketValue;
    private String OverallTurnOverUnits;
    private String LongName;
    private String TradeDate;
    private String LastRate;
    private String SecurityLongName;
    // public String FullBranch;
    // private String CUSIP;

    // private String CompanyName;
    // private String TurnOverValueShekel;
    // private String InDay;
    // private String ShareType;
    // private String RegisteredCapital;
    // private String Exe;
    // private String ExeDesc;
    // private String ForeignMarket;
    // private String MinimumVolume;
    // private String MinimumVolumeBlock;
    // private String DealsNo;

    // private String MonthYield;
    // private String AnnualYield;
    // private String AdjustmentCoefficient;
    // private String BrutoYield;
    // private String RedemptionDate;
    // private String Linkage;
    // private String AnnualInterest;
    // private String KeepStatus;
    // private String KeepStatusDate;
    // private String SuspendStatus;
    // private String SuspendStatusDate;
    // private String IndexNumber;
    // private String IndexCategoryType;
    // private String UAssetName;
    // private String UAssetValue;
    // private String Symbol;
    // private String DealTime;
    // private String PointsChange;
    // private String ISIN_ID;
    // private String DaysUntilRedemption;
    // private String BaseIndices;
    // private String BaseIndicesDate;
    // private String CompanyLogo;
    // private String LastDealTime;
    // public boolean IsForeignETF;
    // private String ExchangeDate;
    // private String LastStrikeDate;
    // private String LinkageType;
    // private String ExcessivePrice;
    // private String ExcessivePriceCurrency;
    // private String CurrentExcessivePrice;
    // private String StrikeShareRate;
    // private String ExchangeShareName;
    // private String ExchangeShareId;
    // private String ExchangeShareISIN;
    // private String StrikeShareName;
    // private String ExchangeShareRate;
    // private String ExchangeRateType;
    // private String ExchangeRelation;
    // private String NoSharesFromOption;
    // public boolean isTrading;

    // private String CorporateNo;
    // private String Classification_Super;
    // private String Classification_Primary;
    // private String Classification_Secondary;
    // private String Directive;
    // private String StockExchanges;
    // private String Currencies;
    // private String Prospectus;
    // private String Appendix;
    // private String AdditionalDocs;
    // private String RepresetativeDetails;
    // public String CompanyId;
    // private String SecuritySubType;
    // private String TradeTime;

    // private String Change;
    // private String TradingStage;
    // private String TradingStageDesc;
    // private String TradingStageMob;
    private Indicator[] GreenIndicators;
    private Indicator[] RedIndicators;
    // private String SecurityTypeInSite;
    // private String ETFTypeInSite;

    // public boolean IsTASEUP;
    // public boolean AllowTasePlus;
    // public boolean HasOfferingPrice;
    // private String BlockMonetaryTurnOver;
    // private String BlockDealTime;

    private class Indicator
    {
        private String Key;
        private boolean Value;
        private String Desc;

        public String getKey()
        {
            return Key;
        }

        public boolean getValue()
        {
            return Value;
        }

        public String getDesc()
        {
            return Desc;
        }
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

    // public Map<String, String> toMap()
    // {
    // Map<String, String> map = parameters(this);
    // return map;
    // }
    //
    // private static Map<String, String> parameters(Object obj)
    // {
    // Map<String, String> map = new HashMap<>();
    // for (Field field : obj.getClass().getDeclaredFields())
    // {
    // field.setAccessible(true);
    // try
    // {
    // map.put(field.getName(), field.get(obj).toString());
    // }
    // catch (Exception e)
    // {
    //
    // }
    // }
    // return map;
    // }

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