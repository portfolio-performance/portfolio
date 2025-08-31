package name.abuchen.portfolio.online.impl.TLVMarket.adapters;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class FundsLocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime>
{

    String pattern1 = "yyyy-MM-dd'T'HH:mm:ss"; //$NON-NLS-1$
    String pattern2 = "yyyy-MM-dd'T'HH:mm:SSS"; //$NON-NLS-1$
    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"); //$NON-NLS-1$
    DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"); //$NON-NLS-1$

    private LocalDateTime parseDate(String dateString) throws DateTimeParseException
    {
        if (dateString != null && dateString.trim().length() > 0)
        {
            try
            {
                return LocalDateTime.parse(dateString, formatter1);
            }
            catch (DateTimeParseException pe)
            {
                // dateString = dateString.substring(0, dateString.length() -1);
                return LocalDateTime.parse(dateString, formatter2);
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public JsonElement serialize(LocalDateTime localDate, Type type, JsonSerializationContext jsonSerializationContext)
    {

        String formettedOutput = formatter1.format(localDate);

        return new JsonPrimitive(formettedOutput);

    }

    @Override
    public LocalDateTime deserialize(JsonElement jsonElement, Type type,
                    JsonDeserializationContext jsonDeserializationContext) throws JsonParseException
    {

        return this.parseDate(jsonElement.getAsJsonPrimitive().getAsString());
    }
}
