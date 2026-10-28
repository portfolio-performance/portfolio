package name.abuchen.portfolio.cli.currency;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Where the ECB rates behind a {@link ReportingCurrency} come from. It is read
 * lazily, on the first lookup that needs a rate series, and at most once.
 */
public interface RateCache
{
    /** Names the cache file of the desktop adapter and wins outright over the default location. */
    String VARIABLE = EcbCache.VARIABLE;

    /**
     * @throws EcbCache.Unavailable
     *             if the cache can't be used, with the reason why
     */
    EcbCache.Loaded load() throws EcbCache.Unavailable;

    /**
     * The desktop app's cache file (ADR 0005): <code>PP_ECB_RATES</code>, else
     * the default location of the operating system.
     *
     * @param environment
     *            the injected environment
     * @param osName
     *            the <code>os.name</code> of the running system
     */
    static RateCache desktop(Function<String, Optional<String>> environment, String osName)
    {
        return () -> EcbCache.read(EcbCache.locate(environment, osName));
    }

    /** Rates held in memory, as if read from a cache file at a placeholder path. */
    static RateCache of(EcbCache.Series... series)
    {
        var loaded = new EcbCache.Loaded(Path.of("in-memory"), List.of(series));
        return () -> loaded;
    }
}
