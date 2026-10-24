package name.abuchen.portfolio.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.protobuf.CodedOutputStream;

/**
 * Writes the desktop app's ECB cache file (<code>ecb_exchange_rates.pb</code>)
 * the way core does, field by field, so that tests don't depend on core's
 * unexported generated classes.
 */
final class EcbCacheFiles
{
    /** The rates of one currency pair, by date; the value is the number of term units per base unit. */
    static final class Series
    {
        private final String base;
        private final String term;
        private final Map<LocalDate, BigDecimal> rates = new LinkedHashMap<>();

        Series(String base, String term)
        {
            this.base = base;
            this.term = term;
        }

        Series rate(String date, String value)
        {
            rates.put(LocalDate.parse(date), new BigDecimal(value));
            return this;
        }

        String base()
        {
            return base;
        }

        String term()
        {
            return term;
        }

        Map<LocalDate, BigDecimal> rates()
        {
            return rates;
        }
    }

    private EcbCacheFiles()
    {
    }

    static Series series(String base, String term)
    {
        return new Series(base, term);
    }

    /** Writes the cache to the given file, creating its folders. */
    static Path write(Path file, Series... series) throws IOException
    {
        var out = new ByteArrayOutputStream();
        var message = CodedOutputStream.newInstance(out);
        message.writeInt64(1, 1_700_000_000_000L);
        for (var s : series)
            message.writeByteArray(2, series(s));
        message.flush();

        Files.createDirectories(file.getParent());
        Files.write(file, out.toByteArray());
        return file;
    }

    private static byte[] series(Series series) throws IOException
    {
        var out = new ByteArrayOutputStream();
        var message = CodedOutputStream.newInstance(out);
        message.writeString(1, series.base());
        message.writeString(2, series.term());
        for (var rate : series.rates().entrySet())
            message.writeByteArray(3, rate(rate.getKey(), rate.getValue()));
        message.flush();
        return out.toByteArray();
    }

    private static byte[] rate(LocalDate date, BigDecimal value) throws IOException
    {
        var out = new ByteArrayOutputStream();
        var message = CodedOutputStream.newInstance(out);
        message.writeInt64(1, date.toEpochDay());
        message.writeByteArray(2, decimal(value));
        message.flush();
        return out.toByteArray();
    }

    private static byte[] decimal(BigDecimal value) throws IOException
    {
        var out = new ByteArrayOutputStream();
        var message = CodedOutputStream.newInstance(out);
        message.writeUInt32(1, value.scale());
        message.writeUInt32(2, value.precision());
        message.writeByteArray(3, value.unscaledValue().toByteArray());
        message.flush();
        return out.toByteArray();
    }
}
