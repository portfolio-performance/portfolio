package name.abuchen.portfolio.online.impl.TLVMarket.adapters;


import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class FundsLocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate>
{

    String pattern1 = "yyyy-MM-dd'T'HH:mm:ss"; //$NON-NLS-1$
    String pattern2 = "yyyy-MM-dd'T'HH:mm:SSS"; //$NON-NLS-1$
    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd/MM/yyyy"); //$NON-NLS-1$

    private LocalDate parseDate(String dateString) throws DateTimeParseException
    {
        if (dateString != null && dateString.trim().length() > 0)
        {
            return LocalDate.parse(dateString, formatter1);
        }
        else
        {
            return null;
        }
    }

    @Override
    public JsonElement serialize(LocalDate localDate, Type type, JsonSerializationContext jsonSerializationContext)
    {

        String formettedOutput = formatter1.format(localDate);

        return new JsonPrimitive(formettedOutput);

    }

    @Override
    public LocalDate deserialize(JsonElement jsonElement, Type type,
                    JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
    {

        return this.parseDate(jsonElement.getAsJsonPrimitive().getAsString());
    }
}
