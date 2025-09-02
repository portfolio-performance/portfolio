package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import java.time.LocalDateTime;
import java.util.Map;

public class FundHistoryEntry
{

    private String FundId;
    private LocalDateTime TradeDate;
    private LocalDateTime LastUpdateDate;
    private float PurchasePrice;
    private float SellPrice;
    private float CreationPrice;
    private float DateYield;
    private float Rate;
    private float ManagmentFee;
    private float TrusteeFee;
    private float SuccessFee;
    private float AssetValue;

    public float getPurchasePrice()
    {
        return PurchasePrice;
    }

    public void setPurchasePrice(float purchasePrice)
    {
        PurchasePrice = purchasePrice;
    }

    public float getSellPrice()
    {
        return SellPrice;
    }

    public void setSellPrice(float sellPrice)
    {
        SellPrice = sellPrice;
    }

    public float getDateYield()
    {
        return DateYield;
    }

    public void setDateYield(float dateYield)
    {
        DateYield = dateYield;
    }

    public void setFundId(String fundId)
    {
        FundId = fundId;
    }

    public void setRate(float rate)
    {
        Rate = rate;
    }

    // Getters and Setters
    public float getRate()
    {
        return this.Rate;
    }

    public String getFundId()
    {
        return this.FundId;
    }

    public LocalDateTime getTradeDate()
    {
        return this.TradeDate;
    }

    public void setTradeDate(LocalDateTime date)
    {
        this.TradeDate = date;
    }

    public static FundHistoryEntry fromMap(Map<String, Object> map)
    {
        FundHistoryEntry historyentry = new FundHistoryEntry();
        if (map.containsKey("Rate"))
        {
            historyentry.setRate(((Double) map.get("Rate")).floatValue());
        }
        if (map.containsKey("FundId"))
        {
            historyentry.setFundId((String) map.get("FundId"));
        }
        if (map.containsKey("TradeDate"))
        {
            historyentry.setTradeDate(LocalDateTime.parse((String) map.get("TradeDate")));
        }
        if (map.containsKey("SellPrice"))
        {
            historyentry.setSellPrice(((Double) map.get("SellPrice")).floatValue());
        }
        if (map.containsKey("PurchasePrice"))
        {
            historyentry.setPurchasePrice(((Double) map.get("PurchasePrice")).floatValue());
        }
        return historyentry;
    }

}
