package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class FundHistoryEntry
{

    private String FundId;
    private LocalDateTime TradeDate;
    // private LocalDateTime LastUpdateDate;
    private String PurchasePrice;
    private String SellPrice;
    // private float CreationPrice;
    private String DateYield;
    private String Rate;
    // private float ManagmentFee;
    // private float TrusteeFee;
    // private float SuccessFee;
    private String AssetValue;

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

    public String getDateYield()
    {
        return DateYield;
    }

    public void setDateYield(String dateYield)
    {
        DateYield = dateYield;
    }

    public void setFundId(String fundId)
    {
        FundId = fundId;
    }

    public void setRate(String rate)
    {
        Rate = rate;
    }

    public String getRate()
    {
        return this.Rate;
    }

    public String getFundId()
    {
        return this.FundId;
    }

    public LocalDate getTradeDate()
    {
        return this.TradeDate.toLocalDate();
    }

    public void setTradeDate(LocalDateTime date)
    {
        this.TradeDate = date;
    }

    public String getAssetValue()
    {
        return AssetValue;
    }

    public void setAssetValue(String assetValue)
    {
        AssetValue = assetValue;
    }



    public static FundHistoryEntry fromJson(String json)
    {

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss");//$NON-NLS-1$

        class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime>
        {

            @Override
            public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
                            final JsonSerializationContext context)
            {
                return new JsonPrimitive(date.format(formatter));
            }

            @Override
            public LocalDateTime deserialize(final JsonElement json, final Type typeOfT,
                            final JsonDeserializationContext context) throws JsonParseException
            {
                // return LocalDateTime.parse(json.getAsString(), formatter);
                return LocalDateTime.parse(json.getAsString(), formatter);
            }

        }
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).create();

        FundHistoryEntry historyentry = gson.fromJson(json, FundHistoryEntry.class);
        return historyentry;
    }
}
