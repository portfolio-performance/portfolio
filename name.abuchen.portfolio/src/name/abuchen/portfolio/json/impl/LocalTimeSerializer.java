package name.abuchen.portfolio.json.impl;

import java.lang.reflect.Type;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class LocalTimeSerializer implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime>
{
    @Override
    public JsonElement serialize(LocalTime src, Type typeOfSrc, JsonSerializationContext context)
    {
        return new JsonPrimitive(src.withNano(0).withSecond(0).toString());
    }

    @Override
    public LocalTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    {
        try
        {
            return LocalTime.parse(json.getAsJsonPrimitive().getAsString());
        }
        catch (DateTimeParseException e)
        {
            throw new JsonParseException(e);
        }
    }
}
