package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class SecurityHistoryEntry
{

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

    public LocalDate TradeDate;
    public float Change;
    public float BaseRate;
    public float OpenRate;
    public float CloseRate;
    public float HighRate;
    public float LowtRate;
    public float MarketValue;
    public long RegisteredCapital;
    public float TurnOverValueShekel;
    public float OverallTurnOverUnits;
    public int DealsNo;
    public String Exe;
    public String AdjustmentCoefficient;
    public String ExeDesc;
    public float IANS;
    public float IndexAdjustedFreeFloat;
    public LocalDate LastIANSUpdate;
    public String TradeDateEOD;
    public float AdjustmentRate;
    public float BrutoYield;
    public boolean IfTraded;
    public String ShareTradingStatus;
    public boolean IsOfferingPrice;
    public String AdjustmentRateDesc;

    public LocalDate getTradeDate()
    {
        return TradeDate;
    }

    public void setTradeDate(LocalDate tradeDate)
    {
        TradeDate = tradeDate;
    }

    public float getBaseRate()
    {
        return BaseRate;
    }

    public void setBaseRate(float baseRate)
    {
        BaseRate = baseRate;
    }

    public float getCloseRate()
    {
        return CloseRate;
    }

    public void setCloseRate(float closeRate)
    {
        CloseRate = closeRate;
    }

    public float getHighRate()
    {
        return HighRate;
    }

    public void setHighRate(float highRate)
    {
        HighRate = highRate;
    }

    public float getLowRate()
    {
        return LowtRate;
    }

    public void setLowRate(float lowRate)
    {
        LowtRate = lowRate;
    }

    public float getMarketValue()
    {
        return MarketValue;
    }

    public void setMarketValue(float marketValue)
    {
        MarketValue = marketValue;
    }

    public static SecurityHistoryEntry fromMap(Map<String, Object> map)
    {
        SecurityHistoryEntry historyentry = new SecurityHistoryEntry();
        if (map.containsKey("MarketValue")) //$NON-NLS-1$
        {
            historyentry.setMarketValue(((Double) map.get("MarketValue")).floatValue()); //$NON-NLS-1$
        }
        if (map.containsKey("LowRate")) //$NON-NLS-1$
        {
            historyentry.setLowRate(((Double) map.get("LowRate")).floatValue()); //$NON-NLS-1$
        }
        if (map.containsKey("HighRate")) //$NON-NLS-1$
        {
            historyentry.setHighRate(((Double) map.get("HighRate")).floatValue()); //$NON-NLS-1$
        }
        if (map.containsKey("TradeDate")) //$NON-NLS-1$
        {
            historyentry.setTradeDate(LocalDate.parse((String) map.get("TradeDate"), formatter)); //$NON-NLS-1$
        }

        return historyentry;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "SecurityHistoryEntry [TradeDate=" + TradeDate + ", Change=" + Change + ", BaseRate=" + BaseRate
                        + ", OpenRate=" + OpenRate + ", CloseRate=" + CloseRate + ", HighRate=" + HighRate
                        + ", LowtRate=" + LowtRate + ", MarketValue=" + MarketValue + ", RegisteredCapital="
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
