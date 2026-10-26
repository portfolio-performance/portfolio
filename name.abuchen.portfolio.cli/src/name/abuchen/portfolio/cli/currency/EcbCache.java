package name.abuchen.portfolio.cli.currency;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.protobuf.CodedInputStream;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProvider;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

/**
 * The desktop app's ECB exchange-rate cache, read-only (ADR 0005). Core's
 * generated protobuf classes and its provider's injectable constructor aren't
 * exported, so this parses the three small messages itself:
 *
 * <pre>
 * PECBData               lastModified = 1, series = 2 (repeated)
 * PExchangeRateTimeSeries baseCurrency = 1, termCurrency = 2, exchangeRates = 3 (repeated)
 * PExchangeRate          date = 1 (epoch day), value = 2
 * PDecimalValue          scale = 1, precision = 2, value = 3 (two's-complement big-endian)
 * </pre>
 *
 * Anything unexpected makes the whole cache unavailable rather than producing
 * wrong figures.
 */
public final class EcbCache
{
    /** Names the cache file and wins outright over the default location. */
    public static final String VARIABLE = "PP_ECB_RATES";

    private static final String FILE_NAME = "ecb_exchange_rates.pb";

    // wire tags: (field number << 3) | wire type, with 0 = varint and 2 = length-delimited
    private static final int SERIES = 2 << 3 | 2;
    private static final int BASE_CURRENCY = 1 << 3 | 2;
    private static final int TERM_CURRENCY = 2 << 3 | 2;
    private static final int EXCHANGE_RATE = 3 << 3 | 2;
    private static final int RATE_DATE = 1 << 3;
    private static final int RATE_VALUE = 2 << 3 | 2;
    private static final int DECIMAL_SCALE = 1 << 3;
    private static final int DECIMAL_PRECISION = 2 << 3;
    private static final int DECIMAL_VALUE = 3 << 3 | 2;

    /** Why the cache can't be used. */
    static final class Unavailable extends Exception
    {
        private static final long serialVersionUID = 1L;

        private final String reason;
        private final transient Optional<Path> path;

        Unavailable(String reason, String detail, Optional<Path> path, Throwable cause)
        {
            super(detail, cause);
            this.reason = reason;
            this.path = path;
        }

        /** A stable code: not-configured, not-found, unreadable or unparsable. */
        String reason()
        {
            return reason;
        }

        Optional<Path> path()
        {
            return path;
        }
    }

    /** One currency pair of the cache. */
    static final class Series implements ExchangeRateTimeSeries
    {
        private static final ExchangeRateProvider PROVIDER = new ExchangeRateProvider()
        {
            @Override
            public String getName()
            {
                return "ECB cache of the desktop app";
            }

            @Override
            public List<ExchangeRateTimeSeries> getAvailableTimeSeries(Client client)
            {
                return List.of();
            }
        };

        private final String baseCurrency;
        private final String termCurrency;
        private final List<ExchangeRate> rates;

        Series(String baseCurrency, String termCurrency, List<ExchangeRate> rates)
        {
            this.baseCurrency = baseCurrency;
            this.termCurrency = termCurrency;
            this.rates = new ArrayList<>(rates);
            this.rates.sort(Comparator.comparing(ExchangeRate::getTime));
        }

        @Override
        public String getBaseCurrency()
        {
            return baseCurrency;
        }

        @Override
        public String getTermCurrency()
        {
            return termCurrency;
        }

        @Override
        public Optional<ExchangeRateProvider> getProvider()
        {
            return Optional.of(PROVIDER);
        }

        @Override
        public List<ExchangeRate> getRates()
        {
            return List.copyOf(rates);
        }

        /** Like core's series: the latest rate on or before the day, else the first rate. */
        @Override
        public Optional<ExchangeRate> lookupRate(LocalDate requestedTime)
        {
            if (rates.isEmpty())
                return Optional.empty();

            var index = Collections.binarySearch(rates, new ExchangeRate(requestedTime, BigDecimal.ZERO));
            if (index >= 0)
                return Optional.of(rates.get(index));
            if (index == -1)
                return Optional.of(rates.get(0));
            return Optional.of(rates.get(-index - 2));
        }

        /** The same weight as core's ECB series, so that the same paths win. */
        @Override
        public int getWeight()
        {
            return 2;
        }

        Optional<LocalDate> latestRateDate()
        {
            return rates.isEmpty() ? Optional.empty() : Optional.of(rates.get(rates.size() - 1).getTime());
        }
    }

    /** What was read from one cache file. */
    record Loaded(Path path, List<Series> series)
    {
        /** The newest rate date across all series. */
        Optional<LocalDate> latestRateDate()
        {
            return series.stream().map(Series::latestRateDate).flatMap(Optional::stream)
                            .max(Comparator.naturalOrder());
        }
    }

    private EcbCache()
    {
    }

    /**
     * Where the cache is: <code>PP_ECB_RATES</code>, else core's own rule
     * <code>&lt;workspace&gt;/.metadata/.plugins/name.abuchen.portfolio/ecb_exchange_rates.pb</code>
     * with the product's default instance area of the operating system. Home
     * directories come only from the given environment, never from the real
     * process.
     *
     * @param environment
     *            the injected environment
     * @param osName
     *            the <code>os.name</code> of the running system
     * @throws Unavailable
     *             if no location can be determined
     */
    static Path locate(Function<String, Optional<String>> environment, String osName) throws Unavailable
    {
        try
        {
            var explicit = environment.apply(VARIABLE).filter(value -> !value.isBlank());
            if (explicit.isPresent())
                return Path.of(explicit.get());

            var workspace = defaultWorkspace(environment, osName);
            if (workspace.isEmpty())
                throw new Unavailable("not-configured",
                                "No location is known: set " + VARIABLE + " to the ecb_exchange_rates.pb file.",
                                Optional.empty(), null);

            return workspace.get().resolve(".metadata").resolve(".plugins").resolve("name.abuchen.portfolio")
                            .resolve(FILE_NAME);
        }
        catch (InvalidPathException e)
        {
            throw new Unavailable("unreadable", "Not a valid path: " + e.getInput(), Optional.empty(), e);
        }
    }

    private static Optional<Path> defaultWorkspace(Function<String, Optional<String>> environment, String osName)
    {
        var windows = osName.toLowerCase().startsWith("windows");
        var mac = osName.toLowerCase().startsWith("mac");

        var base = environment.apply(windows ? "LOCALAPPDATA" : "HOME").filter(value -> !value.isBlank());
        if (base.isEmpty())
            return Optional.empty();

        if (windows)
            return Optional.of(Path.of(base.get(), "PortfolioPerformance", "workspace"));
        if (mac)
            return Optional.of(Path.of(base.get(), "Library", "Application Support",
                            "name.abuchen.portfolio.product", "workspace"));
        return Optional.of(Path.of(base.get(), ".PortfolioPerformance", "workspace"));
    }

    /**
     * @throws Unavailable
     *             if the file is missing, can't be read or isn't a valid cache
     */
    static Loaded read(Path path) throws Unavailable
    {
        byte[] bytes;
        try
        {
            bytes = Files.readAllBytes(path);
        }
        catch (NoSuchFileException e)
        {
            throw new Unavailable("not-found", "There is no such file.", Optional.of(path), e);
        }
        catch (IOException | SecurityException e)
        {
            throw new Unavailable("unreadable", "The file can't be read: " + e.getMessage(), Optional.of(path), e);
        }

        try
        {
            var series = parse(bytes);
            if (series.isEmpty())
                throw new Unavailable("unparsable", "The file holds no exchange rates.", Optional.of(path), null);
            return new Loaded(path, series);
        }
        catch (IOException | RuntimeException e)
        {
            throw new Unavailable("unparsable", "The file is not a valid cache: " + e.getMessage(), Optional.of(path),
                            e);
        }
    }

    private static List<Series> parse(byte[] bytes) throws IOException
    {
        var in = CodedInputStream.newInstance(bytes);
        var series = new ArrayList<Series>();

        for (var tag = in.readTag(); tag != 0; tag = in.readTag())
        {
            if (tag == SERIES)
                series.add(parseSeries(CodedInputStream.newInstance(in.readByteArray())));
            else
                skip(in, tag);
        }

        return series;
    }

    private static void skip(CodedInputStream in, int tag) throws IOException
    {
        if (!in.skipField(tag))
            throw new IOException("unexpected end of group");
    }

    private static Series parseSeries(CodedInputStream in) throws IOException
    {
        var base = "";
        var term = "";
        var rates = new ArrayList<ExchangeRate>();

        for (var tag = in.readTag(); tag != 0; tag = in.readTag())
        {
            switch (tag)
            {
                case BASE_CURRENCY -> base = in.readString();
                case TERM_CURRENCY -> term = in.readString();
                case EXCHANGE_RATE -> rates.add(parseRate(CodedInputStream.newInstance(in.readByteArray())));
                default -> skip(in, tag);
            }
        }

        return new Series(base, term, rates);
    }

    private static ExchangeRate parseRate(CodedInputStream in) throws IOException
    {
        long epochDay = 0;
        BigDecimal value = null;

        for (var tag = in.readTag(); tag != 0; tag = in.readTag())
        {
            switch (tag)
            {
                case RATE_DATE -> epochDay = in.readInt64();
                case RATE_VALUE -> value = parseDecimal(CodedInputStream.newInstance(in.readByteArray()));
                default -> skip(in, tag);
            }
        }

        if (value == null)
            throw new IOException("a rate without a value");

        return new ExchangeRate(LocalDate.ofEpochDay(epochDay), value);
    }

    /** As core's <code>ProtobufUtil.fromDecimalValue</code> decodes it. */
    private static BigDecimal parseDecimal(CodedInputStream in) throws IOException
    {
        var scale = 0;
        var precision = 0;
        var unscaled = new byte[0];

        for (var tag = in.readTag(); tag != 0; tag = in.readTag())
        {
            switch (tag)
            {
                case DECIMAL_SCALE -> scale = in.readUInt32();
                case DECIMAL_PRECISION -> precision = in.readUInt32();
                case DECIMAL_VALUE -> unscaled = in.readByteArray();
                default -> skip(in, tag);
            }
        }

        return new BigDecimal(new BigInteger(unscaled), scale, new MathContext(precision));
    }
}
