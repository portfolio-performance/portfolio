package name.abuchen.portfolio.json.impl;

import java.io.IOException;
import java.time.LocalTime;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class LocalTimeAdapter extends TypeAdapter<LocalTime>
{
    @Override
    public void write(final JsonWriter jsonWriter, final LocalTime localTime) throws IOException
    {
        jsonWriter.value(localTime.toString());
    }

    @Override
    public LocalTime read(final JsonReader jsonReader) throws IOException
    {
        return LocalTime.parse(jsonReader.nextString());
    }
}
