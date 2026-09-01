package name.abuchen.portfolio.online.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityProperty;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.util.WebAccess;

public class MoexISSCommon
{
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //$NON-NLS-1$
    protected String fetch(String issUrl, Security security, LocalDate from, LocalDate to) 
    {
        var secId = extractProperty(security, "moex.secid"); //$NON-NLS-1$
        if (secId == null || secId.isBlank()) secId = security.getTickerSymbol();
        if (secId == null || secId.isBlank()) return null;

        var boardId = extractProperty(security, "moex.boardid"); //$NON-NLS-1$
        if (boardId == null || boardId.isBlank()) boardId = "TQBR"; //$NON-NLS-1$

        var url = String.format(issUrl, secId, from.format(DATE_FMT), to.format(DATE_FMT), boardId); 

        try 
        {
            var json = new WebAccess(url).get();
            return json;
        } 
        catch (Exception e) 
        {
            return null;
        }
    }

    protected List<LatestSecurityPrice> parse(String json, Boolean isBond) 
    {
        List<LatestSecurityPrice> result = new ArrayList<>();
        try
        {
            if (null == json) return result;
            var root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("history")) return result; //$NON-NLS-1$

            var history = root.getAsJsonObject("history"); //$NON-NLS-1$
            var columns = history.getAsJsonArray("columns"); //$NON-NLS-1$
            var data = history.getAsJsonArray("data"); //$NON-NLS-1$

            var idxDate = findColumnIndex(columns, "TRADEDATE"); //$NON-NLS-1$
            var idxPrice = findColumnIndex(columns, "CLOSE"); //$NON-NLS-1$
            var idxLow = findColumnIndex(columns, "LOW"); //$NON-NLS-1$
            var idxHigh = findColumnIndex(columns, "HIGH"); //$NON-NLS-1$
            var idxVolume = findColumnIndex(columns, "VOLUME"); //$NON-NLS-1$
            if (idxDate == -1 || idxPrice == -1) return result;

            for (var rowEl : data) 
            {
                var row = rowEl.getAsJsonArray();
                var dateEl = row.get(idxDate);
                var priceEl = row.get(idxPrice);
                var lowEl = row.get(idxLow);
                var highEl = row.get(idxHigh);
                var volumeEl = row.get(idxVolume);

                if (!dateEl.isJsonNull() && !priceEl.isJsonNull()) 
                {
                    // bond prices return by api as percentage so multiple to 10
                    var bondMultiplier = Boolean.TRUE.equals(isBond) ? 10 : 1;
                    var date = LocalDate.parse(dateEl.getAsString(), DATE_FMT);
                    var price = priceEl.getAsDouble(); 
                    long priceValue = asPrice(price) * bondMultiplier;
                    var lst = new LatestSecurityPrice(date, priceValue);
                    if(!lowEl.isJsonNull())
                    {
                        var low = lowEl.getAsDouble();
                        var lowValue = asPrice(low) * bondMultiplier;
                        lst.setLow(lowValue);
                    }
                    if(!highEl.isJsonNull())
                    {
                        var high = highEl.getAsDouble();
                        var highValue = asPrice(high) * bondMultiplier;
                        lst.setHigh(highValue);
                    }
                    if(!volumeEl.isJsonNull())
                    {
                        var volume = volumeEl.getAsLong();
                        var volumeValue = asVolume(volume);
                        lst.setVolume(volumeValue);
                    }
                    result.add(lst);
                }
            }
        } 
        catch (Exception ignored) 
        {
        }
        return result;
    }

    private long asPrice(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Number n)
            return Values.Quote.factorize(Math.round(n.doubleValue() * 10000) / 10000d);

        throw new IllegalArgumentException(number.getClass().toString());
    }
   
    
    private long asVolume(Object number)
    {
        if (number == null)
            return LatestSecurityPrice.NOT_AVAILABLE;

        if (number instanceof Long n)
            return n.longValue();

        throw new IllegalArgumentException(number.getClass().toString());
    }
    

    
    private int findColumnIndex(JsonArray columns, String name) 
    {
        for (int i = 0; i < columns.size(); i++) 
        {
            if (name.equals(columns.get(i).getAsString())) return i;
        }
        return -1;
    }

    private String extractProperty(Security security, String key) 
    {
        return security.getProperties()
                .filter(p -> key.equals(p.getName()))
                .map(SecurityProperty::getValue)
                .findFirst()
                .orElse(null);
    }

}
