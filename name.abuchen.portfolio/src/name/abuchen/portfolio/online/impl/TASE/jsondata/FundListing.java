package name.abuchen.portfolio.online.impl.TASE.jsondata;


import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representation of JSON Fund listing tory returned by TASE API Funds,
 * Securities are returned in a different format by TASE API
 */

public class FundListing
{
    private String FundLongName;
    private String FundShortName;
    private String AssetValue;
    private int FundId;
    private String PurchasePrice;
    private String SellPrice;
    private String UnitValuePrice;
    private LocalDateTime UnitValueValidDate;




    public String getAssetValue()
    {
        return AssetValue;
    }

    public void setAssetValue(String assetValue)
    {
        AssetValue = assetValue;
    }

    public String getPurchasePrice()
    {
        return PurchasePrice;
    }

    public void setPurchasePrice(String purchasePrice)
    {
        PurchasePrice = purchasePrice;
    }

    public String getSellPrice()
    {
        return SellPrice;
    }

    public void setSellPrice(String sellPrice)
    {
        SellPrice = sellPrice;
    }

    public String getUnitValuePrice()
    {
        return UnitValuePrice;
    }

    public void setUnitValuePrice(String unitValuePrice)
    {
        UnitValuePrice = unitValuePrice;
    }

    public LocalDate getUnitValueValidDate()
    {
        return UnitValueValidDate.toLocalDate();
    }

    public void setUnitValueValidDate(LocalDateTime unitValueValidDate)
    {
        UnitValueValidDate = unitValueValidDate;
    }


    public FundListing(String longName, String shortName, String asset)
    {
        this.FundLongName = longName;
        this.FundShortName = shortName;
        this.AssetAsOfDate = asset;
    }
 
}
