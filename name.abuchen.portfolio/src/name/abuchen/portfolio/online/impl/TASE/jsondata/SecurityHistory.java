package name.abuchen.portfolio.online.impl.TASE.jsondata;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Representation of JSON Security history returned by TASE API
 */
public class SecurityHistory
{


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

    public String toJson()
    {
        Gson gson = new Gson();
        return gson.toJson(this);

    }

    @Override
    public String toString()
    {
        return "SecurityHistory [Items=" + Arrays.toString(Items) + ", TotalRec=" + TotalRec + ", DateFrom=" + DateFrom //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + ", DateTo=" + DateTo + ", TradeDateEOD=" + TradeDateEOD + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

}
