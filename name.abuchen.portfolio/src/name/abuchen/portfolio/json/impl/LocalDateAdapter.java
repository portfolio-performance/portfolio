package name.abuchen.portfolio.json.impl;

import java.io.IOException;
import java.time.LocalDate;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class LocalDateAdapter extends TypeAdapter<LocalDate>
{
    @Override
    public void write(final JsonWriter jsonWriter, final LocalDate localDate) throws IOException
    {
        jsonWriter.value(localDate.toString());
    }

    @Override
    public LocalDate read(final JsonReader jsonReader) throws IOException
    {
        // previous versions have written the JSON as
        // <pre>{"year":2021,"month":8,"day":12}</pre>

        if (jsonReader.peek() == JsonToken.BEGIN_OBJECT)
        {
            int year = 0;
            int month = 0;
            int day = 0;
            jsonReader.beginObject();
            while (jsonReader.hasNext())
            {
                String name = jsonReader.nextName();
                if ("year".equals(name)) //$NON-NLS-1$
                    year = jsonReader.nextInt();
                else if ("month".equals(name)) //$NON-NLS-1$
                    month = jsonReader.nextInt();
                else if ("day".equals(name)) //$NON-NLS-1$
                    day = jsonReader.nextInt();
                else
                    jsonReader.skipValue();
            }
            jsonReader.endObject();
            return LocalDate.of(year, month, day);
        }
        else
        {
            return LocalDate.parse(jsonReader.nextString());
        }
    }
}
