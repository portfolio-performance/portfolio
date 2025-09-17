package name.abuchen.portfolio.online.impl.TLVMarket.jsondata;


import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

public class FundHistory
{

    // private static DateTimeFormatter formatter =
    // DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$
    private static final Gson gson = new Gson();

    public FundHistoryEntry[] Table;
    public int Total;
    private LocalDateTime StartDate;
    private LocalDateTime EndDate;

    public LocalDate getDateFrom()
    {
        return StartDate.toLocalDate();
    }

    public void setDateFrom(LocalDateTime startDate)
    {
        StartDate = startDate;
    }

    public LocalDate getDateTo()
    {
        return EndDate.toLocalDate();
    }

    public void setDateTo(LocalDateTime endDate)
    {
        EndDate = endDate;
    }

    public void setTotalRecs(int total)
    {
        Total = total;
    }

    public int getTotalRecs()
    {
        return Total;
    }

    public FundHistoryEntry[] getItems()
    {
        return Table;
    }

    public void setItems(FundHistoryEntry[] items)
    {
        Table = items;
    }

    public static FundHistory fromMap(Map<String, Object> map)
    {
        FundHistory historyentry = new FundHistory();

        if (map.containsKey("StartDate")) //$NON-NLS-1$
        {
            historyentry.setDateFrom(LocalDateTime.parse((String) map.get("StartDate"))); //$NON-NLS-1$
        }
        if (map.containsKey("EndDate")) //$NON-NLS-1$
        {
            historyentry.setDateTo(LocalDateTime.parse((String) map.get("EndDate"))); //$NON-NLS-1$
        }
        if (map.containsKey("Table")) //$NON-NLS-1$
        {

            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) map.get("Table"); //$NON-NLS-1$
            FundHistoryEntry[] entries = new FundHistoryEntry[rawItems.size()];
            for (int i = 0; i < rawItems.size(); i++)
            {
                entries[i] = FundHistoryEntry.fromMap(rawItems.get(i));
            }
            historyentry.setItems(entries);
            historyentry.setTotalRecs(entries.length);

        }
        return historyentry;
    }

    public static FundHistory fromJson(String json)
    {
        
        class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime>
        {

            private final DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm"); //$NON-NLS-1$
            private final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"); //$NON-NLS-1$
            @Override
            public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
                            final JsonSerializationContext context)
            {
                try
                {
                    return new JsonPrimitive(date.format(formatter1));
                }
                catch (DateTimeParseException e)
                {
                    return new JsonPrimitive(date.format(formatter2));
                }

            }

            @Override
            public LocalDateTime deserialize(final JsonElement json, final Type typeOfT,
                            final JsonDeserializationContext context) throws JsonParseException
            {
                try
                {
                    // System.out.println("Trying to prase: " +
                    // json.getAsString());
                    LocalDateTime d = LocalDateTime.parse(json.getAsString()); // ,
                                                                               // formatter2);
                    return d;
                }
                catch (DateTimeParseException e)
                {
                    try
                    {
                        // System.out.println(e.getMessage());
                        // System.out.println("Trying to prase: " +
                        // json.getAsString());
                        LocalDateTime d = LocalDateTime.parse(json.getAsString()); // ,
                                                                                   // formatter1);
                        // System.out.println("Parsed: " + d);
                        return d;
                    }
                    catch (DateTimeParseException f)
                    {
                        System.out.println(f.getMessage());
                        return LocalDateTime.now();
                    }
                }
            }

        }
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).create();

        FundHistory historyentry = gson.fromJson(json, FundHistory.class);
        return historyentry;
    }

    @Override
    public String toString()
    {
        return "FundHistory [Table=" + Arrays.toString(Table) + ", Total=" + Total + ", StartDate=" + StartDate //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        + ", EndDate=" + EndDate + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public String toJson()
    {
        return gson.toJson(this);

    }
}
