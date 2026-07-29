package name.abuchen.portfolio.money.impl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRate;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.ExchangeRateTimeSeries;

@SuppressWarnings("nls")
public class ECBExchangeRateProviderTest
{
    /** a rate seeded by fillInDefaultData, present whenever nothing was loaded */
    private static final LocalDate CHF_DEFAULT_DATE = LocalDate.parse("2015-12-18");

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLookup()
    {
        ExchangeRateProviderFactory factory = new ExchangeRateProviderFactory(new Client());

        assertThat(factory.getTimeSeries("EUR", "CHF"), instanceOf(ExchangeRateTimeSeriesImpl.class));
        assertThat(factory.getTimeSeries("CHF", "EUR"), instanceOf(InverseExchangeRateTimeSeries.class));
        
        assertThat(factory.getTimeSeries("EUR", "XXX"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("XXX", "EUR"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("GBP", "XXX"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("XXX", "GBP"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("XZY", "XXX"), instanceOf(EmptyExchangeRateTimeSeries.class));
        assertThat(factory.getTimeSeries("VND", "EUR"), instanceOf(EmptyExchangeRateTimeSeries.class));

        ExchangeRateTimeSeries timeSeries = factory.getTimeSeries("CHF", "USD");
        assertThat(timeSeries, instanceOf(ChainedExchangeRateTimeSeries.class));
        assertThat(timeSeries.getBaseCurrency(), is("CHF"));
        assertThat(timeSeries.getTermCurrency(), is("USD"));
    }

    @Test
    public void testRatesRoundTripThroughTheStorageLocation() throws Exception
    {
        Path directory = tempFolder.newFolder().toPath();
        LocalDate date = LocalDate.parse("2026-07-28");
        // as delivered by the feed: the HTTP last modified date, in milliseconds
        long lastModified = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        var writing = new ECBExchangeRateProvider(() -> directory);
        var written = writing.getData();
        written.getCurrencyMap().get("CHF").addRate(new ExchangeRate(date, BigDecimal.valueOf(0.9321)));
        written.setLastModified(lastModified);
        // save writes only what is dirty, and only an online update sets that
        written.setDirty(true);

        Path stored = writing.getStorageFile().orElseThrow();
        assertThat(stored.getParent(), is(directory));

        writing.save(new NullProgressMonitor());
        assertThat(stored.toFile().exists(), is(true));

        var reading = new ECBExchangeRateProvider(() -> directory);
        reading.load(new NullProgressMonitor());

        var loaded = eurTo("CHF", reading);
        assertThat(loaded.lookupRate(date).orElseThrow().getValue(), is(BigDecimal.valueOf(0.9321)));
        assertThat(loaded.lookupRate(CHF_DEFAULT_DATE).orElseThrow().getTime(), is(CHF_DEFAULT_DATE));

        // without it, the updater would download the full history every time
        assertThat(reading.getData().getLastModified(), is(lastModified));
    }

    @Test
    public void testMissingFileLeavesTheSeededDefaultsIntact() throws Exception
    {
        Path directory = tempFolder.newFolder().toPath();

        var provider = new ECBExchangeRateProvider(() -> directory);
        provider.load(new NullProgressMonitor());

        assertThat(eurTo("CHF", provider).lookupRate(CHF_DEFAULT_DATE).orElseThrow().getTime(), is(CHF_DEFAULT_DATE));
    }

    /**
     * No instance area - the case a headless launch can produce. Neither
     * operation may propagate; save is called at shutdown.
     */
    @Test
    public void testAbsentStorageLocationDegradesQuietly() throws Exception
    {
        var provider = new ECBExchangeRateProvider(() -> null);
        // save returns early unless there is something to write
        provider.getData().setDirty(true);

        assertThat(provider.getStorageFile(), is(Optional.empty()));

        provider.load(new NullProgressMonitor());
        provider.save(new NullProgressMonitor());

        assertThat(eurTo("CHF", provider).lookupRate(CHF_DEFAULT_DATE).orElseThrow().getTime(), is(CHF_DEFAULT_DATE));
    }

    private static ExchangeRateTimeSeries eurTo(String termCurrency, ECBExchangeRateProvider provider)
    {
        return provider.getAvailableTimeSeries(new Client()).stream()
                        .filter(s -> "EUR".equals(s.getBaseCurrency()) && termCurrency.equals(s.getTermCurrency()))
                        .findFirst().orElseThrow();
    }
}
