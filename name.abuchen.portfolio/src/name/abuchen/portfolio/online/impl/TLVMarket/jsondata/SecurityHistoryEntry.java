package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import java.time.format.DateTimeFormatter;

public class SecurityHistoryEntry
{

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

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

    // public static SecurityHistoryEntry fromMap(Map<String, Object> map)
    // {
    // SecurityHistoryEntry historyentry = new SecurityHistoryEntry();
    // if (map.containsKey("MarketValue")) //$NON-NLS-1$
    // {
    // historyentry.setMarketValue(((Double)
    // map.get("MarketValue")).floatValue()); //$NON-NLS-1$
    // }
    // if (map.containsKey("LowRate")) //$NON-NLS-1$
    // {
    // historyentry.setLowRate(((Double) map.get("LowRate")).floatValue());
    // //$NON-NLS-1$
    // }
    // if (map.containsKey("HighRate")) //$NON-NLS-1$
    // {
    // historyentry.setHighRate(((Double) map.get("HighRate")).floatValue());
    // //$NON-NLS-1$
    // }
    // if (map.containsKey("TradeDate")) //$NON-NLS-1$
    // {
    // historyentry.setTradeDate(LocalDate.parse((String) map.get("TradeDate"),
    // formatter)); //$NON-NLS-1$
    // }
    //
    // return historyentry;
    // }

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

    public String getOverallTurnOverUnits()
    {
        return OverallTurnOverUnits;
    }

    public void setOverallTurnOverUnits(String overallTurnOverUnits)
    {
        OverallTurnOverUnits = overallTurnOverUnits;
    }
}
