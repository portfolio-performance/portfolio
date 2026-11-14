package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.online.QuoteFeed;
import name.abuchen.portfolio.online.impl.YahooFinanceQuoteFeed;
import name.abuchen.portfolio.rest.testsupport.FakeHost;

/**
 * Starting the application's price update from outside the application.
 *
 * Two things these tests hold the handler to. It chooses a scope and hands it
 * over - no job of its own, no provider of its own, and nothing at all for an
 * instrument whose quote the application maintains by hand. And it refuses a
 * scope it cannot carry out before anybody is asked about it: the same rule every
 * other change follows, and the reason {@code describe} resolves the scope too.
 *
 * The update itself is the application's job and runs past the answer, so what a
 * test can reach is which instruments were handed over and what the answer
 * promises about them.
 */
@SuppressWarnings("nls")
public class QuoteUpdateHandlerTest
{
    /** a provider that really exists, so that "a quote could arrive" means what it says */
    private static final String FEED = new YahooFinanceQuoteFeed().getId();

    private static JsonObject body(String json)
    {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static Security instrument(String name, String feed)
    {
        return new Security(name, null, null, feed);
    }

    private static Client clientOf(Security... securities)
    {
        var client = new Client();
        for (var security : securities)
            client.addSecurity(security);
        return client;
    }

    private static FakeHost host()
    {
        return new FakeHost(List.of());
    }

    private static ApiException capture(Runnable call)
    {
        try
        {
            call.run();
        }
        catch (ApiException e)
        {
            return e;
        }
        throw new AssertionError("expected a refusal, but the call went through");
    }

    @Test
    public void testACallThatAsksForNothingRefreshesTheWholeFile()
    {
        var host = host();
        var started = QuoteUpdateHandler.run(clientOf(instrument("Aktiv", FEED), instrument("Von Hand", QuoteFeed.MANUAL)),
                        body("{}"), host);

        // the whole file, so the host is asked for the variant that covers the
        // job's other targets as well - and no list is handed over to do it with
        assertThat(host.quotesRequestedForAll(), is(true));
        assertThat(host.quotesRequested(), is(List.of()));

        assertThat(started.answer().get("scope").getAsString(), is("all"));
        assertThat(started.answer().get("securities").getAsInt(), is(2));
        assertThat(started.answer().get("withQuoteFeed").getAsInt(), is(1));
        assertThat(started.answer().get("started").getAsBoolean(), is(true));
        assertThat(started.answer().get("savedToDisk").getAsBoolean(), is(false));

        // exchange rates are part of this scope whatever the instruments carry
        assertThat(started.mightChange(), is(true));
    }

    @Test
    public void testRetiredInstrumentsAreLeftOutOfTheActiveScope()
    {
        var retired = instrument("Schl\u00E4ft", FEED);
        retired.setRetired(true);
        var awake = instrument("Aktiv", FEED);

        var host = host();
        QuoteUpdateHandler.run(clientOf(awake, retired), body("{\"scope\":\"active\"}"), host);

        assertThat(host.quotesRequestedForAll(), is(false));
        assertThat(host.quotesRequested(), contains(awake));
    }

    @Test
    public void testOneInstrumentOfOneNameAndNothingElse()
    {
        var one = instrument("Einzig", FEED);
        var host = host();
        var started = QuoteUpdateHandler.run(clientOf(one, instrument("Andere", FEED)),
                        body("{\"scope\":\"instrument\",\"instrument\":\"" + one.getUUID() + "\"}"), host);

        assertThat(host.quotesRequested(), contains(one));
        assertThat(started.answer().get("scope").getAsString(), is("instrument"));
        assertThat(started.answer().get("securities").getAsInt(), is(1));
    }

    @Test
    public void testTheInstrumentsOfOneWatchlist()
    {
        var first = instrument("Erste", FEED);
        var second = instrument("Zweite", FEED);
        var other = instrument("Dritte", FEED);

        var client = clientOf(first, second, other);
        var watchlist = new Watchlist();
        watchlist.setName("Beobachtet");
        watchlist.addSecurity(first);
        watchlist.addSecurity(second);
        client.getWatchlists().add(watchlist);

        var host = host();
        var started = QuoteUpdateHandler.run(client, body("{\"scope\":\"watchlist\",\"watchlist\":\"Beobachtet\"}"),
                        host);

        assertThat(host.quotesRequested(), contains(first, second));
        assertThat(started.answer().get("securities").getAsInt(), is(2));
    }

    /**
     * The whole point of the flag: a file whose instruments are all maintained by
     * hand cannot end up holding a new quote, so the answer must not let the
     * caller - or the user - believe an edit is on its way.
     */
    @Test
    public void testNothingCanArriveForInstrumentsWhoseQuotesAreKeptByHand()
    {
        var host = host();
        var started = QuoteUpdateHandler.run(
                        clientOf(instrument("Von Hand", QuoteFeed.MANUAL), instrument("Ohne Feed", null),
                                        instrument("Unbekannter Anbieter", "NO_SUCH_FEED")),
                        body("{\"scope\":\"active\"}"), host);

        assertThat(started.answer().get("securities").getAsInt(), is(3));
        assertThat(started.answer().get("withQuoteFeed").getAsInt(), is(0));
        assertThat(started.mightChange(), is(false));

        // the job was still started: skipping every instrument is its decision,
        // not a refusal invented here
        assertThat(host.quotesRequested().size(), is(3));
    }

    @Test
    public void testThePromptNamesTheThingItIsAboutToStart()
    {
        var one = instrument("Einzig", FEED);
        var facts = QuoteUpdateHandler.describe(clientOf(one, instrument("Andere", FEED)),
                        body("{\"scope\":\"instrument\",\"instrument\":\"" + one.getUUID() + "\"}"),
                        "Richard.portfolio");

        // the operation is stamped by the route, never by the describer
        assertThat(facts.operation(), is(nullValue()));
        assertThat(facts.fileLabel(), is("Richard.portfolio"));
        assertThat(facts.subjectType(), is("quote-update"));
        assertThat(facts.subjectName(), is("Einzig"));
        assertThat(facts.detail(), is("1 instrument"));
    }

    @Test
    public void testAPromptForTheWholeFileSaysHowManyItMeans()
    {
        var facts = QuoteUpdateHandler.describe(clientOf(instrument("A", FEED), instrument("B", FEED)), body("{}"),
                        "kommer.portfolio");

        assertThat(facts.subjectName(), is("all instruments of the file"));
        assertThat(facts.detail(), is("2 instruments"));
    }

    @Test
    public void testAScopeThatIsNotOneOfTheFourIsRefused()
    {
        var refusal = capture(() -> QuoteUpdateHandler.describe(clientOf(instrument("A", FEED)),
                        body("{\"scope\":\"holdings\"}"), "file"));

        assertThat(refusal.getErrors().get(0).field(), is("scope"));
        assertThat(refusal.getErrors().get(0).code(), is("invalid-value"));
        assertThat(refusal.getErrors().get(0).message(), containsString("all, active, instrument, watchlist"));
    }

    @Test
    public void testAnInstrumentScopeWithoutAnInstrumentIsRefused()
    {
        var refusal = capture(() -> QuoteUpdateHandler.run(clientOf(instrument("A", FEED)),
                        body("{\"scope\":\"instrument\"}"), host()));

        assertThat(refusal.getErrors().get(0).field(), is("instrument"));
        assertThat(refusal.getErrors().get(0).code(), is("required"));
    }

    @Test
    public void testAnInstrumentTheFileDoesNotHaveIsRefused()
    {
        var refusal = capture(() -> QuoteUpdateHandler.run(clientOf(instrument("A", FEED)),
                        body("{\"scope\":\"instrument\",\"instrument\":\"00000000-0000-0000-0000-000000000000\"}"),
                        host()));

        assertThat(refusal.getErrors().get(0).field(), is("instrument"));
        assertThat(refusal.getErrors().get(0).code(), is("invalid-value"));
        assertThat(refusal.getErrors().get(0).message(), containsString("pp_list_instruments"));
    }

    @Test
    public void testAWatchlistOfThatNameHasToExist()
    {
        var refusal = capture(() -> QuoteUpdateHandler.run(clientOf(instrument("A", FEED)),
                        body("{\"scope\":\"watchlist\",\"watchlist\":\"Unbekannt\"}"), host()));

        assertThat(refusal.getErrors().get(0).field(), is("watchlist"));
        assertThat(refusal.getErrors().get(0).code(), is("invalid-value"));
        assertThat(refusal.getErrors().get(0).message(), containsString("pp_list_watchlists"));
    }

    @Test
    public void testAFieldThisCallDoesNotReadIsRefused()
    {
        var refusal = capture(() -> QuoteUpdateHandler.describe(clientOf(instrument("A", FEED)),
                        body("{\"scope\":\"all\",\"uuid\":\"1234\"}"), "file"));

        assertThat(refusal.getErrors().get(0).field(), is("uuid"));
        assertThat(refusal.getErrors().get(0).code(), is("unknown-field"));
    }

    /**
     * Refused before anybody is asked, which is the whole order of a write here:
     * the facts are read first, and a scope that cannot be carried out never
     * interrupts the user.
     */
    @Test
    public void testADescribedButImpossibleScopeNeverReachesTheUser()
    {
        var client = clientOf(instrument("A", FEED));
        var host = host();

        capture(() -> QuoteUpdateHandler.describe(client, body("{\"scope\":\"watchlist\"}"), "file"));

        // nothing was started, and nothing would have been: the run resolves the
        // same scope and refuses in the same place
        capture(() -> QuoteUpdateHandler.run(client, body("{\"scope\":\"watchlist\"}"), host));
        assertThat(host.quotesRequested(), is(nullValue()));
    }
}
