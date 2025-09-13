package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SecurityHistory
{
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$


    public SecurityHistoryEntry[] Items;
    public int TotalRec;
    public LocalDate DateFrom;
    public LocalDate DateTo;
    public LocalDate TradeDateEOD;

    public SecurityHistoryEntry[] getItems()
    {
        return Items;
    }

    public void setItems(SecurityHistoryEntry[] items)
    {
        Items = items;
    }

    public LocalDate getDateFrom()
    {
        return DateFrom;
    }

    public int getTotalRec()
    {
        return TotalRec;
    }

    public void setTotalRec(int value)
    {
        TotalRec = value;
    }
    public void setDateFrom(LocalDate dateFrom)
    {
        DateFrom = dateFrom;
    }

    public LocalDate getDateTo()
    {
        return DateTo;
    }

    public void setDateTo(LocalDate dateTo)
    {
        DateTo = dateTo;
    }

    public static SecurityHistory fromMap(Map<String, Object> map)
    {
        SecurityHistory historyentry = new SecurityHistory();

        if (map.containsKey("DateFrom")) //$NON-NLS-1$
        {
            historyentry.setDateFrom(LocalDate.parse((String) map.get("DateFrom"), formatter)); //$NON-NLS-1$
        }
        if (map.containsKey("DateTo")) //$NON-NLS-1$
        {
            historyentry.setDateTo(LocalDate.parse((String) map.get("DateTo"), formatter)); //$NON-NLS-1$
        }

        if (map.containsKey("Items")) //$NON-NLS-1$
        {
            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) map.get("Items"); //$NON-NLS-1$
            SecurityHistoryEntry[] entries = new SecurityHistoryEntry[rawItems.size()];
            for (int i = 0; i < rawItems.size(); i++)
            {
                entries[i] = SecurityHistoryEntry.fromMap(rawItems.get(i));
            }
            historyentry.setItems(entries);
            historyentry.setTotalRec(entries.length);
        }
        return historyentry;

    }



    public static SecurityHistory fromJson(String json)
    {
        class LocalDateTypeAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate>
        {

            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

            @Override
            public JsonElement serialize(final LocalDate date, final Type typeOfSrc,
                            final JsonSerializationContext context)
            {
                return new JsonPrimitive(date.format(formatter));
            }

            @Override
            public LocalDate deserialize(final JsonElement json, final Type typeOfT,
                            final JsonDeserializationContext context) throws JsonParseException
            {
                return LocalDate.parse(json.getAsString(), formatter);
            }
        }

        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter()).create();

        SecurityHistory historyentry = gson.fromJson(json, SecurityHistory.class);
        return historyentry;

    }



    @Override
    public String toString()
    {
        return "SecurityHistory [Items=" + Arrays.toString(Items) + ", TotalRec=" + TotalRec + ", DateFrom=" + DateFrom //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + ", DateTo=" + DateTo + ", TradeDateEOD=" + TradeDateEOD + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
