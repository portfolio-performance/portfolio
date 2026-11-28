package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Watchlist;

/**
 * A watchlist created, and instruments moved into and out of one.
 *
 * Most of these tests are about the one property that makes a watchlist unlike
 * everything else in a file: it has no identifier. A name twice is refused not
 * because the model forbids it but because two lists of one name could never be
 * told apart again - and a change that changed nothing must not leave the file
 * waiting to be saved.
 */
@SuppressWarnings("nls")
public class WatchlistWriteHandlerTest
{
    /** counts what the model reports, because that is what makes the change visible */
    private static int[] watch(Client client)
    {
        var dirty = new int[1];
        client.addPropertyChangeListener(e -> {
            if ("dirty".equals(e.getPropertyName()))
                dirty[0]++;
        });
        return dirty;
    }

    private static JsonObject body(String json)
    {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static Watchlist aListed(String name)
    {
        var watchlist = new Watchlist();
        watchlist.setName(name);
        return watchlist;
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

    // ------------------------------------------------------------------ create

    @Test
    public void aNewListIsThereHoldingWhatWasAskedFor()
    {
        var client = new Client();
        var first = new SecurityBuilder().addTo(client);
        var dirty = watch(client);

        var answer = WatchlistWriteHandler.create(client, body("""
                        {"name": "Einzelwerte", "instruments": ["%s"]}""".formatted(first.getUUID())));

        assertThat(client.getWatchlists().size(), is(1));
        assertThat(client.getWatchlists().get(0).getName(), is("Einzelwerte"));
        assertThat(client.getWatchlists().get(0).getSecurities().size(), is(1));

        assertThat(answer.get("name").getAsString(), is("Einzelwerte"));
        assertThat(answer.get("created").getAsBoolean(), is(true));
        // the answer says out loud that there is no uuid to remember
        assertThat(answer.get("hasIdentifier").getAsBoolean(), is(false));
        assertThat(answer.get("instrumentCount").getAsInt(), is(1));
        assertThat(answer.get("added").getAsInt(), is(1));
        assertThat(answer.get("watchlistsBefore").getAsInt(), is(0));
        assertThat(answer.get("watchlistsAfter").getAsInt(), is(1));

        assertThat("one change, one dirty", dirty[0], is(1));
    }

    @Test
    public void aNameAlreadyUsedIsRefusedAndNoSecondListAppears()
    {
        var client = new Client();
        client.addWatchlist(aListed("Tech"));
        var dirty = watch(client);

        var refusal = capture(() -> WatchlistWriteHandler.create(client, body("{\"name\": \"Tech\"}")));

        assertThat(refusal.getStatus(), is(409));
        assertThat(refusal.getType(), is("name-taken"));
        assertThat(refusal.getErrors().get(0).field(), is("name"));
        assertThat(refusal.getDetail(), containsString("Tech"));

        assertThat(client.getWatchlists().size(), is(1));
        assertThat("a refusal leaves nothing unsaved", dirty[0], is(0));
    }

    @Test
    public void aNameThatIsMissingOrOnlySpacesIsRefusedOnItsField()
    {
        var client = new Client();

        var missing = capture(() -> WatchlistWriteHandler.create(client, body("{}")));
        assertThat(missing.getStatus(), is(400));
        assertThat(missing.getErrors().get(0).field(), is("name"));

        var spaces = capture(() -> WatchlistWriteHandler.create(client, body("{\"name\": \"   \"}")));
        assertThat(spaces.getStatus(), is(400));
        assertThat(spaces.getErrors().get(0).field(), is("name"));

        assertThat("nothing was created", client.getWatchlists().size(), is(0));
    }

    @Test
    public void anInstrumentTheFileDoesNotHaveRefusesTheWholeRequest()
    {
        var client = new Client();
        var held = new SecurityBuilder().addTo(client);

        var refusal = capture(() -> WatchlistWriteHandler.create(client, body("""
                        {"name": "Gemischtes", "instruments": ["%s", "fremd-uuid"]}""".formatted(held.getUUID()))));

        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("instruments"));
        assertThat(refusal.getErrors().get(0).message(), containsString("pp_list_instruments"));

        // the point of refusing rather than skipping: no half-filled list left behind
        assertThat(client.getWatchlists().size(), is(0));
        // and nothing was created to fill it either: an instrument is named by uuid
        // here, so a watchlist write cannot be a way to add one to the file
        assertThat(client.getSecurities().size(), is(1));
    }

    // ------------------------------------------------------------------ update

    /**
     * The ordinary case behind the two tools: the instrument is already in the
     * file and a list has to pick it up. Adding it to a list never makes a second
     * instrument, and never edits the one it points at.
     */
    @Test
    public void anInstrumentAlreadyInTheFileIsPutIntoTheListAndNotCreatedAgain()
    {
        var client = new Client();
        var held = new SecurityBuilder().addTo(client);
        client.addWatchlist(aListed("Beobachtet"));

        var answer = WatchlistWriteHandler.update(client, body("""
                        {"name": "Beobachtet", "add": ["%s"]}""".formatted(held.getUUID())));

        assertThat(answer.get("added").getAsInt(), is(1));
        assertThat(answer.get("created").getAsBoolean(), is(false));
        assertThat(client.getSecurities().size(), is(1));
        assertThat(client.getWatchlists().get(0).getSecurities().get(0).getUUID(), is(held.getUUID()));
    }

    @Test
    public void anUnknownInstrumentInAnUpdateRefusesWithoutMakingOne()
    {
        var client = new Client();
        client.addWatchlist(aListed("Beobachtet"));

        var refusal = capture(() -> WatchlistWriteHandler.update(client, body("""
                        {"name": "Beobachtet", "add": ["fremd-uuid"]}""")));

        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("add"));
        assertThat(refusal.getErrors().get(0).message(), containsString("pp_list_instruments"));
        // the file gained no instrument: a watchlist cannot be a way to create one
        assertThat(client.getSecurities().size(), is(0));
        assertThat(client.getWatchlists().get(0).getSecurities().size(), is(0));
    }

    @Test
    public void oneRequestAddsAndRemovesAtOnce()
    {
        var client = new Client();
        var kept = new SecurityBuilder().addTo(client);
        var incoming = new SecurityBuilder().addTo(client);
        var going = new SecurityBuilder().addTo(client);
        var watchlist = aListed("Depot");
        watchlist.addSecurity(kept);
        watchlist.addSecurity(going);
        client.addWatchlist(watchlist);
        var dirty = watch(client);

        var answer = WatchlistWriteHandler.update(client, body("""
                        {"name": "Depot", "add": ["%s"], "remove": ["%s"]}""" //
                        .formatted(incoming.getUUID(), going.getUUID())));

        assertThat(watchlist.getSecurities().size(), is(2));
        assertThat(watchlist.getSecurities().contains(kept), is(true));
        assertThat(watchlist.getSecurities().contains(going), is(false));

        assertThat(answer.get("created").getAsBoolean(), is(false));
        assertThat(answer.get("changed").getAsBoolean(), is(true));
        assertThat(answer.get("instrumentCountBefore").getAsInt(), is(2));
        assertThat(answer.get("instrumentCountAfter").getAsInt(), is(2));
        assertThat(answer.get("added").getAsInt(), is(1));
        assertThat(answer.get("removed").getAsInt(), is(1));
        assertThat("one change, one dirty", dirty[0], is(1));
    }

    @Test
    public void takingOutWhatIsNotInIsReportedAndChangesNothing()
    {
        var client = new Client();
        var kept = new SecurityBuilder().addTo(client);
        var elsewhere = new SecurityBuilder().addTo(client);
        var watchlist = aListed("Depot");
        watchlist.addSecurity(kept);
        client.addWatchlist(watchlist);
        var dirty = watch(client);

        var answer = WatchlistWriteHandler.update(client, body("""
                        {"name": "Depot", "remove": ["%s"]}""".formatted(elsewhere.getUUID())));

        assertThat(answer.has("notHeld"), is(true));
        assertThat(answer.get("notHeld").getAsJsonArray().size(), is(1));
        assertThat(answer.get("notHeld").getAsJsonArray().get(0).getAsJsonObject().get("uuid").getAsString(), //
                        is(elsewhere.getUUID()));
        assertThat(answer.get("removed").getAsInt(), is(0));
        assertThat(answer.get("changed").getAsBoolean(), is(false));
        assertThat("a request that changed nothing leaves no unsaved change", dirty[0], is(0));
    }

    @Test
    public void addingWhatTheListAlreadyHoldsAddsNothingAndChangesNothing()
    {
        var client = new Client();
        var kept = new SecurityBuilder().addTo(client);
        var watchlist = aListed("Depot");
        watchlist.addSecurity(kept);
        client.addWatchlist(watchlist);
        var dirty = watch(client);

        var answer = WatchlistWriteHandler.update(client, //
                        body("{\"name\": \"Depot\", \"add\": [\"" + kept.getUUID() + "\"]}"));

        assertThat(watchlist.getSecurities().size(), is(1));
        assertThat(answer.get("added").getAsInt(), is(0));
        assertThat(answer.get("changed").getAsBoolean(), is(false));
        assertThat(dirty[0], is(0));
    }

    @Test
    public void aListNobodyNamedIsRefusedWithTheNamesTheFileHas()
    {
        var client = new Client();
        client.addWatchlist(aListed("Depot"));
        client.addWatchlist(aListed("Tech"));

        var refusal = capture(() -> WatchlistWriteHandler.update(client, //
                        body("{\"name\": \"Krypto\", \"add\": []}")));

        assertThat(refusal.getStatus(), is(409));
        assertThat(refusal.getType(), is("no-such-watchlist"));
        assertThat(refusal.getDetail(), containsString("Depot"));
        assertThat(refusal.getDetail(), containsString("Tech"));
    }

    @Test
    public void neitherAddingNorRemovingIsRefusedTellingTheCallerSo()
    {
        var client = new Client();
        client.addWatchlist(aListed("Depot"));

        var refusal = capture(() -> WatchlistWriteHandler.update(client, body("{\"name\": \"Depot\"}")));

        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("add"));
        assertThat(refusal.getErrors().get(0).code(), is("required"));
    }

    // ------------------------------------------------------- the prompt's facts

    @Test
    public void thePromptNamesTheListAndTheRouteStampsTheOperation()
    {
        var facts = WatchlistWriteHandler.describe(new Client(), "Richard.portfolio", //
                        body("{\"name\": \"Einzelwerte\"}"), true);

        assertThat(facts.fileLabel(), is("Richard.portfolio"));
        assertThat(facts.subjectType(), is("watchlist"));
        assertThat(facts.subjectName(), is("Einzelwerte"));
        // the operation is the key a session grant is remembered under, and the
        // describer is not the one who decides it
        assertThat(facts.operation(), is(nullValue()));
    }

    @Test
    public void aChangeThatCouldNotBeMadeIsRefusedBeforeAnyoneIsAsked()
    {
        var client = new Client();
        client.addWatchlist(aListed("Tech"));

        var creation = capture(() -> WatchlistWriteHandler.describe(client, "x", //
                        body("{\"name\": \"Tech\"}"), true));
        assertThat(creation.getStatus(), is(409));

        var update = capture(() -> WatchlistWriteHandler.describe(client, "x", //
                        body("{\"name\": \"Fehlend\", \"add\": []}"), false));
        assertThat(update.getStatus(), is(409));

        // found live: the name was settled before the prompt while the instruments
        // were not, so the user was offered a change that could never have been
        // made and it failed only afterwards
        var foreignInstruments = capture(() -> WatchlistWriteHandler.describe(client, "x", //
                        body("{\"name\": \"Neu\", \"instruments\": [\"nicht-da\"]}"), true));
        assertThat(foreignInstruments.getStatus(), is(400));
        assertThat(foreignInstruments.getErrors().get(0).field(), is("instruments"));

        var foreignRemoval = capture(() -> WatchlistWriteHandler.describe(client, "x", //
                        body("{\"name\": \"Tech\", \"remove\": [\"nicht-da\"]}"), false));
        assertThat(foreignRemoval.getStatus(), is(400));
        assertThat(foreignRemoval.getErrors().get(0).field(), is("remove"));

        var neitherDirection = capture(() -> WatchlistWriteHandler.describe(client, "x", //
                        body("{\"name\": \"Tech\"}"), false));
        assertThat(neitherDirection.getStatus(), is(400));
        assertThat(neitherDirection.getErrors().get(0).field(), is("add"));
        assertThat(neitherDirection.getErrors().get(0).code(), is("required"));
    }

    @Test
    public void aMissingNameIsRefusedBeforeAnyoneIsAsked()
    {
        var refusal = capture(() -> WatchlistWriteHandler.describe(new Client(), "x", body("{}"), true));

        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("name"));
    }

    // ------------------------------------------------------------------ delete

    @Test
    public void aListThatIsRemovedIsGoneFromTheFile()
    {
        var client = new Client();
        var tech = aListed("Tech");
        client.addWatchlist(tech);
        var dirty = watch(client);

        var json = WatchlistWriteHandler.remove(client, body("{\"name\": \"Tech\"}"));

        assertThat(json.get("name").getAsString(), is("Tech"));
        assertThat(json.get("deleted").getAsBoolean(), is(true));
        assertThat(json.get("instrumentCountBefore").getAsInt(), is(0));
        assertThat(client.getWatchlists().size(), is(0));
        assertThat(dirty[0], is(1));
    }

    @Test
    public void deletingAListTakesAwayTheSelectionAndNotTheInstruments()
    {
        var client = new Client();
        var sas = new SecurityBuilder().addTo(client);
        sas.setName("SAP SE");

        var tech = aListed("Tech");
        tech.addSecurity(sas);
        client.addWatchlist(tech);

        var json = WatchlistWriteHandler.remove(client, body("{\"name\": \"Tech\"}"));

        // the instruments the list pointed at stay in the file, untouched
        assertThat(json.get("instrumentCountBefore").getAsInt(), is(1));
        assertThat(client.getWatchlists().size(), is(0));
        assertThat(client.getSecurities().size(), is(1));
    }

    @Test
    public void aListTheFileDoesNotHaveIsRefusedAndSaysWhatItHas()
    {
        var client = new Client();
        client.addWatchlist(aListed("Tech"));

        // the list is there once - and after this it is not
        WatchlistWriteHandler.remove(client, body("{\"name\": \"Tech\"}"));

        var refusal = capture(() -> WatchlistWriteHandler.remove(client, body("{\"name\": \"Tech\"}")));
        assertThat(refusal.getStatus(), is(409));
        assertThat(refusal.getType(), is("no-such-watchlist"));

        // a second sending cannot succeed, and describing it refuses too
        var again = capture(() -> WatchlistWriteHandler.describeDeletion(client, "f", body("{\"name\": \"Tech\"}")));
        assertThat(again.getStatus(), is(409));
    }

    @Test
    public void deletingADifferentListStillWorksAfterARetusal()
    {
        var client = new Client();
        client.addWatchlist(aListed("Tech"));
        client.addWatchlist(aListed("Bonds"));
        var dirty = watch(client);

        capture(() -> WatchlistWriteHandler.remove(client, body("{\"name\": \"Zombie\"}")));
        WatchlistWriteHandler.remove(client, body("{\"name\": \"Tech\"}"));

        // the refusal touched nothing; the name is matched exactly, spaces gone
        assertThat(client.getWatchlists().size(), is(1));
        assertThat(client.getWatchlists().get(0).getName(), is("Bonds"));
        assertThat(dirty[0], is(1));
    }
}
