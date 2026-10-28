package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Values;

/**
 * The two calculations that loop over instruments report how far they have
 * come, and stop between two instruments when the caller gave up - the only
 * points at which a calculation can be stopped at all.
 */
@SuppressWarnings("nls")
public class ProgressTest
{
    /** records the steps, and gives up after the given number */
    private static final class Recording implements Progress
    {
        private final List<String> steps = new CopyOnWriteArrayList<>();
        private final int cancelAfter;

        private Recording(int cancelAfter)
        {
            this.cancelAfter = cancelAfter;
        }

        @Override
        public void phase(String message)
        {
            // not recorded
        }

        @Override
        public void step(int done, int total, String message)
        {
            steps.add(done + "/" + total + " " + message);
        }

        @Override
        public boolean isCancelled()
        {
            return steps.size() >= cancelAfter;
        }
    }

    @Test
    public void testOutsideACalculationThereIsNoProgress()
    {
        assertThat(Progress.current(), is(Progress.NONE));
    }

    @Test
    public void testTheInstrumentPerformanceReportsEveryInstrument() throws Exception
    {
        var client = threeTradedInstruments();
        var progress = new Recording(Integer.MAX_VALUE);

        Progress.runWith(progress, () -> InstrumentPerformanceHandler.list(client,
                        new ExchangeRateProviderFactory(client), "2024-01-01", "2024-12-31", null, null, null, null));

        assertThat(progress.steps, is(List.of("0/3 instrument 1 of 3", "1/3 instrument 2 of 3",
                        "2/3 instrument 3 of 3")));
        // and the thread does not keep it
        assertThat(Progress.current(), is(Progress.NONE));
    }

    @Test
    public void testTheInstrumentPerformanceStopsWhenCancelled() throws Exception
    {
        var client = threeTradedInstruments();
        var progress = new Recording(2);

        try
        {
            Progress.runWith(progress, () -> InstrumentPerformanceHandler.list(client,
                            new ExchangeRateProviderFactory(client), "2024-01-01", "2024-12-31", null, null, null,
                            null));
            fail("expected the calculation to stop");
        }
        catch (CancellationException e)
        {
            // two instruments reported, the third never started
            assertThat(progress.steps.size(), is(2));
        }
    }

    @Test
    public void testTheTradesStopWhenCancelled() throws Exception
    {
        var client = threeTradedInstruments();
        var progress = new Recording(1);

        try
        {
            Progress.runWith(progress, () -> TradesHandler.list(client, new ExchangeRateProviderFactory(client), null,
                            null, null, null, null, null, null));
            fail("expected the collection to stop");
        }
        catch (CancellationException e)
        {
            assertThat(progress.steps.size(), is(1));
        }
    }

    private static Client threeTradedInstruments()
    {
        var client = new Client();
        var account = new AccountBuilder().addTo(client);
        for (int ii = 0; ii < 3; ii++)
        {
            var security = new SecurityBuilder().addPrice("2024-06-01", Values.Quote.factorize(100 + ii)).addTo(client);
            security.setName("Instrument " + ii);
            new PortfolioBuilder(account) //
                            .buy(security, "2024-02-01", Values.Share.factorize(10), Values.Amount.factorize(1000)) //
                            .addTo(client);
        }
        return client;
    }
}
