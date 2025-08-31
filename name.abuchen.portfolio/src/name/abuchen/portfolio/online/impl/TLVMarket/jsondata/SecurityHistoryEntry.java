package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import java.time.LocalDate;

public class SecurityHistoryEntry
{

    public LocalDate TradeDate;
    public float Change;
    public float BaseRate;
    public float OpenRate;
    public float CloseRate;
    public float HighRate;
    public float LowtRate;
    public int MarketValue;
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
}
