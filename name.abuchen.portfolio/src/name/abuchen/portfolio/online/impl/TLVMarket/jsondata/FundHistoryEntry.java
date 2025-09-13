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
    // private float CreationPrice;
    private float DateYield;
    private float Rate;
    // private float ManagmentFee;
    // private float TrusteeFee;
    // private float SuccessFee;
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

    public float getAssetValue()
    {
        return AssetValue;
    }

    public void setAssetValue(float assetValue)
    {
        AssetValue = assetValue;
    }

    public static FundHistoryEntry fromMap(Map<String, Object> map)
    {
        FundHistoryEntry historyentry = new FundHistoryEntry();
        if (map.containsKey("Rate")) //$NON-NLS-1$
        {
            historyentry.setRate(((Double) map.get("Rate")).floatValue()); //$NON-NLS-1$
        }
        if (map.containsKey("FundId")) //$NON-NLS-1$
        {
            historyentry.setFundId((String) map.get("FundId")); //$NON-NLS-1$
        }
        if (map.containsKey("TradeDate")) //$NON-NLS-1$
        {
            historyentry.setTradeDate(LocalDateTime.parse((String) map.get("TradeDate"))); //$NON-NLS-1$
        }
        if (map.containsKey("SellPrice")) //$NON-NLS-1$
        {
            historyentry.setSellPrice(((Double) map.get("SellPrice")).floatValue()); //$NON-NLS-1$
        }
        if (map.containsKey("PurchasePrice")) //$NON-NLS-1$
        {
            historyentry.setPurchasePrice(((Double) map.get("PurchasePrice")).floatValue()); //$NON-NLS-1$
        }
        if (map.containsKey("AssetValue")) //$NON-NLS-1$
        {
            historyentry.setAssetValue(((Double) map.get("AssetValue")).floatValue()); //$NON-NLS-1$
        }
        return historyentry;
    }

}
