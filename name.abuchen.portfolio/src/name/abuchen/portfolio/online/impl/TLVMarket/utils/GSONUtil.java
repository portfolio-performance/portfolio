package name.abuchen.portfolio.online.impl.TLVMarket.utils;


import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.online.impl.TLVMarket.adapters.FundsLocalDateAdapter;
import name.abuchen.portfolio.online.impl.TLVMarket.adapters.FundsLocalDateTimeAdapter;

public final class GSONUtil
{

    private GSONUtil()
    {
    }

    public static Gson createGson()
    {

        // @formatter:off
        return new GsonBuilder()
                .registerTypeAdapter(Map.class, createMapDeserializer())
                .registerTypeAdapter(List.class, createListDeserializer())
                .registerTypeAdapter(LocalDateTime.class, new FundsLocalDateTimeAdapter())
                .registerTypeAdapter(LocalDate.class, new FundsLocalDateAdapter())
                .create();
        // @formatter:on
    }

    private static JsonDeserializer<Map<String, Object>> createMapDeserializer()
    {
        return new JsonDeserializer<Map<String, Object>>()
        {

            @Override
            public Map<String, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                            throws JsonParseException
            {

                return json.getAsJsonObject().entrySet().stream() // stream
                                .collect(Collectors.toMap(Entry::getKey,
                                                (e) -> GSONUtil.deserialize(e.getValue(), context)));
            }
        };
    }

    private static JsonDeserializer<List<Object>> createListDeserializer()
    {
        return new JsonDeserializer<List<Object>>()
        {

            @Override
            public List<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                            throws JsonParseException
            {

                return StreamSupport.stream(json.getAsJsonArray().spliterator(), false) // stream
                                .map((e) -> GSONUtil.deserialize(e, context)).collect(Collectors.toList());
            }
        };
    }

    private static Object deserialize(JsonElement value, JsonDeserializationContext context)
    {

        //@formatter:off
        if (value.isJsonNull())
        { 
            return null; 
        }
        if (value.isJsonObject())
        { 
            return context.deserialize(value, Map.class); 
        }
        if (value.isJsonArray())
        { 
            return context.deserialize(value, List.class); 
        }
        if (value.isJsonPrimitive())
        { 
            return parsePrimitive(value); 
        }
        //@formatter:on
        throw new IllegalStateException("This exception should never be thrown!"); //$NON-NLS-1$
    }

    private static Object parsePrimitive(JsonElement value)
    {

        final JsonPrimitive jsonPrimitive = value.getAsJsonPrimitive();

      //@formatter:off
        if (jsonPrimitive.isString())
        { 
            return jsonPrimitive.getAsString(); 
        }

        if (jsonPrimitive.isBoolean())
        { 
            return jsonPrimitive.getAsBoolean(); 
        }

        if (jsonPrimitive.isNumber())
        { 
            return parseNumber(jsonPrimitive); 
        }
      //@formatter:on

        throw new IllegalStateException("This exception should never be thrown!"); //$NON-NLS-1$
    }

    private static Number parseNumber(JsonPrimitive jsonPrimitive)
    {

        if (isInteger(jsonPrimitive))
        {
            // return jsonPrimitive.getAsLong();
            return jsonPrimitive.getAsInt();
        }

        return jsonPrimitive.getAsDouble();

    }

    private static boolean isInteger(final JsonPrimitive jsonPrimitive)
    {
        return jsonPrimitive.getAsString().matches("[-]?\\d+"); //$NON-NLS-1$
    }
}