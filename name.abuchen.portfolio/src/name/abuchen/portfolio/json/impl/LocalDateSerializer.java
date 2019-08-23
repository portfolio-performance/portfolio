package name.abuchen.portfolio.json.impl;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LocalDateSerializer implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate>
{
    @Override
    public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context)
    {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    {
        try
        {
            return LocalDate.parse(json.getAsJsonPrimitive().getAsString());
        }
        catch (DateTimeParseException e)
        {
            throw new JsonParseException(e);
        }
    }
}
