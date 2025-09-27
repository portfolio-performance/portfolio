package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

public class SecurityHistoryEntry
{


    public String TradeDate;
    public float Change;
    public String BaseRate;
    public String OpenRate;
    public String CloseRate;
    public String HighRate;
    public String LowRate;
    public String MarketValue;
    public long RegisteredCapital;
    public float TurnOverValueShekel;
    public String OverallTurnOverUnits;
    public int DealsNo;
    public String Exe;
    public String AdjustmentCoefficient;
    public String ExeDesc;
    public float IANS;
    public float IndexAdjustedFreeFloat;
    public String LastIANSUpdate;
    public String TradeDateEOD;
    public float AdjustmentRate;
    public float BrutoYield;
    public boolean IfTraded;
    public String ShareTradingStatus;
    public boolean IsOfferingPrice;
    public String AdjustmentRateDesc;

    public String getTradeDate()
    {
        return TradeDate;
    }

    public void setTradeDate(String tradeDate)
    {
        TradeDate = tradeDate;
    }

    public String getBaseRate()
    {
        return BaseRate;
    }

    public void setBaseRate(String baseRate)
    {
        BaseRate = baseRate;
    }

    public String getCloseRate()
    {
        return CloseRate;
    }

    public void setCloseRate(String closeRate)
    {
        CloseRate = closeRate;
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

    public String getMarketValue()
    {
        return MarketValue;
    }

    public void setMarketValue(String marketValue)
    {
        MarketValue = marketValue;
    }

    public String getOverallTurnOverUnits()
    {
        return OverallTurnOverUnits;
    }

    public void setOverallTurnOverUnits(String overallTurnOverUnits)
    {
        OverallTurnOverUnits = overallTurnOverUnits;
    }


    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "SecurityHistoryEntry [TradeDate=" + TradeDate + ", Change=" + Change + ", BaseRate=" + BaseRate
                        + ", OpenRate=" + OpenRate + ", CloseRate=" + CloseRate + ", HighRate=" + HighRate
                        + ", LowtRate=" + LowRate + ", MarketValue=" + MarketValue + ", RegisteredCapital="
                        + RegisteredCapital + ", TurnOverValueShekel=" + TurnOverValueShekel + ", OverallTurnOverUnits="
                        + OverallTurnOverUnits + ", DealsNo=" + DealsNo + ", Exe=" + Exe + ", AdjustmentCoefficient="
                        + AdjustmentCoefficient + ", ExeDesc=" + ExeDesc + ", IANS=" + IANS
                        + ", IndexAdjustedFreeFloat=" + IndexAdjustedFreeFloat + ", LastIANSUpdate=" + LastIANSUpdate
                        + ", TradeDateEOD=" + TradeDateEOD + ", AdjustmentRate=" + AdjustmentRate + ", BrutoYield="
                        + BrutoYield + ", IfTraded=" + IfTraded + ", ShareTradingStatus=" + ShareTradingStatus
                        + ", IsOfferingPrice=" + IsOfferingPrice + ", AdjustmentRateDesc=" + AdjustmentRateDesc + "]"
        ;
    }


}
