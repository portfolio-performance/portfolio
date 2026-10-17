package name.abuchen.portfolio.online.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
            var idxFaceValue = findColumnIndex(columns, "FACEVALUE"); //$NON-NLS-1$
            if (idxDate == -1 || idxPrice == -1) return result;
            if (Boolean.TRUE.equals(isBond) && idxFaceValue == -1) return result;

            for (var rowEl : data) 
            {
                try
                {
                    var row = rowEl.getAsJsonArray();
                    var dateEl = getCell(row, idxDate);
                    var priceEl = getCell(row, idxPrice);
                    if (dateEl == null || priceEl == null) continue;

                    var faceValue = Boolean.TRUE.equals(isBond) ? getDecimal(getCell(row, idxFaceValue)) : null;
                    if (Boolean.TRUE.equals(isBond) && (faceValue == null || faceValue.signum() <= 0)) continue;

                    var date = LocalDate.parse(dateEl.getAsString(), DATE_FMT);
                    var price = getDecimal(priceEl);
                    if (price == null) continue;

                    // Bond CLOSE/LOW/HIGH are percentages of FACEVALUE. The resulting clean quote is in FACEUNIT
                    // currency, which must match the security's currency. ACCINT is intentionally not included.
                    var lst = new LatestSecurityPrice(date, asPrice(price, faceValue),
                                    LatestSecurityPrice.NOT_AVAILABLE, LatestSecurityPrice.NOT_AVAILABLE,
                                    LatestSecurityPrice.NOT_AVAILABLE);
                    var low = getDecimal(getCell(row, idxLow));
                    if (low != null)
                    {
                        lst.setLow(asPrice(low, faceValue));
                    }
                    var high = getDecimal(getCell(row, idxHigh));
                    if (high != null)
                    {
                        lst.setHigh(asPrice(high, faceValue));
                    }
                    var volumeEl = getCell(row, idxVolume);
                    if (volumeEl != null)
                    {
                        var volume = volumeEl.getAsLong();
                        var volumeValue = asVolume(volume);
                        lst.setVolume(volumeValue);
                    }
                    result.add(lst);
                }
                catch (Exception ignored)
                {
                    // Ignore malformed rows while continuing to parse subsequent history entries.
                }
            }
        } 
        catch (Exception ignored) 
        {
        }
        return result;
    }

    private long asPrice(BigDecimal price, BigDecimal faceValue)
    {
        var amount = faceValue == null ? price : price.multiply(faceValue).movePointLeft(2);
        return amount.movePointRight(Values.Quote.precision()).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private BigDecimal getDecimal(JsonElement element)
    {
        return element == null || element.isJsonNull() ? null : element.getAsBigDecimal();
    }

    private JsonElement getCell(JsonArray row, int index)
    {
        if (index < 0 || index >= row.size()) return null;
        var cell = row.get(index);
        return cell.isJsonNull() ? null : cell;
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
