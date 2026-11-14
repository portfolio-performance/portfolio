package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/**
 * An instrument that did not exist before, created through the API.
 *
 * Two properties carry most of these tests. The first is that an identifier is a
 * promise: an ISIN the file already carries is refused outright, because two
 * instruments answering to one ISIN would send every later search for it to
 * whichever the file happens to list first. The second is that a creation is
 * judged before anybody is asked about it, exactly as a change is.
 */
@SuppressWarnings("nls")
public class CreateInstrumentTest
{
    /** a valid ISIN taken from a real file; the last digit is its check digit */
    private static final String ISIN = "LU0062624902";

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

    /** the field names of a `fields` array, in the order the caller named them */
    private static List<String> namesOf(JsonArray fields)
    {
        var names = new ArrayList<String>();
        for (var element : fields)
            names.add(element.getAsJsonObject().get("field").getAsString());
        return names;
    }

    // ----------------------------------------------------------------- create

    @Test
    public void anInstrumentArrivesWithWhatWasNamedAndNothingElse()
    {
        var client = new Client();
        var dirty = watch(client);

        var made = SecuritiesHandler.create(client, body("""
                        {"name":"Neue Fonds","currencyCode":"EUR","isin":"%s","wkn":"974587",\
                         "tickerSymbol":"DKDM.F","note":"für die Messung"}""".formatted(ISIN)));
        var answer = made.answer();

        assertThat(client.getSecurities().size(), is(1));
        var created = client.getSecurities().get(0);
        assertThat(created.getName(), is("Neue Fonds"));
        assertThat(created.getCurrencyCode(), is("EUR"));
        assertThat(created.getIsin(), is(ISIN));
        assertThat(created.getWkn(), is("974587"));
        assertThat(created.getTickerSymbol(), is("DKDM.F"));
        assertThat("one change, one dirty", dirty[0], is(1));

        // the answer carries the identifier everything else works with
        assertThat(answer.getAsJsonObject("instrument").get("uuid").getAsString(), is(created.getUUID()));
        assertThat(answer.get("hasQuoteFeed").getAsBoolean(), is(false));
        assertThat(answer.get("hasTransactions").getAsBoolean(), is(false));
        assertThat("the file has nothing like it, so nothing is reported", answer.has("sameNameAs"), is(false));
        assertThat(answer.has("sameTickerAs"), is(false));

        // every field the caller named, counted - and the currency among them,
        // although a new security answers to the file's currency before anything is
        // set: what was asked for, not what differs, is the question here
        var fields = answer.getAsJsonArray("fields");
        assertThat(fields.size(), is(6));
        assertThat(namesOf(fields), contains("name", "currencyCode", "isin", "wkn", "tickerSymbol", "note"));

        // what the Error Log of the desktop user gets is the same list: a line per
        // named field, so the currency is not missing there simply because the model
        // carried it before this call
        assertThat(made.changes().size(), is(6));
        assertThat(made.changes().stream().map(InstrumentChangeLog.Change::field).toList(),
                        contains("name", "currencyCode", "isin", "wkn", "tickerSymbol", "note"));
        assertThat(made.changes().get(1).field(), is("currencyCode"));
        assertThat(made.changes().get(1).from(), is((String) null));
        assertThat(made.changes().get(1).to(), is("EUR"));

        // and it answers to the uuid the answer handed out
        assertThat(SecuritiesHandler.find(client, created.getUUID()), is(created));
    }

    @Test
    public void aNameThatIsMissingOrOnlySpacesIsRefusedOnItsField()
    {
        var client = new Client();

        var blank = capture(() -> SecuritiesHandler.create(client, //
                        body("{\"name\": \"   \", \"currencyCode\": \"EUR\"}")));
        assertThat(blank.getStatus(), is(422));
        assertThat(blank.getErrors().size(), is(1));
        assertThat(blank.getErrors().get(0).field(), is("name"));

        var missing = capture(() -> SecuritiesHandler.create(client, body("{\"currencyCode\": \"EUR\"}")));
        assertThat(missing.getErrors().get(0).field(), is("name"));
        assertThat(missing.getErrors().get(0).code(), is("required"));

        assertThat("nothing was created", client.getSecurities().isEmpty(), is(true));
    }

    @Test
    public void aCreationInsistsOnACurrency()
    {
        var client = new Client();

        var missing = capture(() -> SecuritiesHandler.create(client, body("{\"name\": \"Neue Fonds\"}")));
        assertThat(missing.getErrors().get(0).field(), is("currencyCode"));
        assertThat(missing.getErrors().get(0).code(), is("required"));

        var unknown = capture(() -> SecuritiesHandler.create(client, //
                        body("{\"name\": \"Neue Fonds\", \"currencyCode\": \"EURO\"}")));
        assertThat(unknown.getErrors().get(0).field(), is("currencyCode"));
        assertThat(unknown.getErrors().get(0).code(), is("unknown-currency"));

        assertThat(client.getSecurities().isEmpty(), is(true));
    }

    @Test
    public void anIsinWithAWrongCheckDigitIsRefused()
    {
        var client = new Client();

        // only one of the ten endings can be right, and this is not it
        var refusal = capture(() -> SecuritiesHandler.create(client, body("""
                        {"name": "Neue Fonds", "currencyCode": "EUR", "isin": "LU0062624901"}""")));

        assertThat(refusal.getErrors().get(0).field(), is("isin"));
        assertThat(refusal.getErrors().get(0).code(), is("invalid-isin"));
        assertThat(client.getSecurities().isEmpty(), is(true));
    }

    @Test
    public void anIsinTheFileAlreadyCarriesCreatesNothingAndSaysWhoseItIs()
    {
        var client = new Client();
        var other = new SecurityBuilder().addTo(client);
        other.setName("DekaLux");
        other.setIsin(ISIN);
        var dirty = watch(client);

        var refusal = capture(() -> SecuritiesHandler.create(client, body("""
                        {"name": "Zweite", "currencyCode": "EUR", "isin": "%s"}""".formatted(ISIN))));

        assertThat(refusal.getStatus(), is(422));
        assertThat(refusal.getErrors().get(0).field(), is("isin"));
        assertThat(refusal.getErrors().get(0).code(), is("duplicate-identifier"));
        assertThat(refusal.getErrors().get(0).message(), containsString("DekaLux"));
        assertThat(refusal.getErrors().get(0).message(), containsString(other.getUUID()));

        assertThat("nothing was created", client.getSecurities().size(), is(1));
        assertThat("and nothing was left unsaved", dirty[0], is(0));
    }

    @Test
    public void aWknBelongsToOneInstrumentToo()
    {
        var client = new Client();
        var other = new SecurityBuilder().addTo(client);
        other.setName("DekaLux");
        other.setWkn("974587");

        var refusal = capture(() -> SecuritiesHandler.create(client, //
                        body("{\"name\": \"Zweite\", \"currencyCode\": \"EUR\", \"wkn\": \"974587\"}")));

        assertThat(refusal.getErrors().get(0).field(), is("wkn"));
        assertThat(refusal.getErrors().get(0).code(), is("duplicate-identifier"));
        assertThat(client.getSecurities().size(), is(1));
    }

    @Test
    public void anAttributeOfTheFileCanBeSetWhileCreating()
    {
        var client = new Client();
        var rating = new AttributeType("rating");
        rating.setName("Rating");
        rating.setTarget(Security.class);
        rating.setType(String.class);
        rating.setConverter(AttributeType.StringConverter.class);
        client.getSettings().addAttributeType(rating);

        SecuritiesHandler.create(client, body("""
                        {"name": "Neue Fonds", "currencyCode": "EUR", "attributes": {"rating": "solide"}}"""));

        assertThat(client.getSecurities().get(0).getAttributes().get(rating), is("solide"));
    }

    @Test
    public void anAttributeNobodyDefinedRefusesTheCreationAndCreatesNothing()
    {
        var client = new Client();

        var refusal = capture(() -> SecuritiesHandler.create(client, //
                        body("{\"name\": \"Neue Fonds\", \"currencyCode\": \"EUR\", \"attributes\": {\"defekt\": \"x\"}}")));

        assertThat(refusal.getErrors().get(0).field(), is("attributes.defekt"));
        assertThat(refusal.getErrors().get(0).code(), is("unknown-attribute"));
        assertThat(client.getSecurities().isEmpty(), is(true));
    }

    // ----------------------------------------------------------- reported, not refused

    @Test
    public void aTickerTheFileAlreadyUsesIsReportedRatherThanRefused()
    {
        var client = new Client();
        var other = new SecurityBuilder().addTo(client);
        other.setName("Andere Anteilsklasse");
        other.setTickerSymbol("R3L");

        var answer = SecuritiesHandler.create(client, //
                        body("{\"name\": \"Neue Anteilsklasse\", \"currencyCode\": \"EUR\", \"tickerSymbol\": \"r3l\"}")) //
                        .answer();

        // the same ticker across share classes and venues is ordinary, so this is a
        // piece of news rather than a refusal
        assertThat(client.getSecurities().size(), is(2));
        assertThat(answer.has("sameTickerAs"), is(true));
        assertThat(answer.getAsJsonArray("sameTickerAs").size(), is(1));
        assertThat(answer.getAsJsonArray("sameTickerAs").get(0).getAsJsonObject().get("uuid").getAsString(), //
                        is(other.getUUID()));
        assertThat(answer.has("sameNameAs"), is(false));
    }

    @Test
    public void aNameTheFileAlreadyHasIsReportedAndKeepsBothInstruments()
    {
        var client = new Client();
        var other = new SecurityBuilder().addTo(client);
        other.setName("Streuer Fonds");

        var answer = SecuritiesHandler.create(client, //
                        body("{\"name\": \"Streuer Fonds\", \"currencyCode\": \"EUR\"}")) //
                        .answer();

        assertThat(client.getSecurities().size(), is(2));
        assertThat(answer.getAsJsonArray("sameNameAs").size(), is(1));
        assertThat(answer.getAsJsonArray("sameNameAs").get(0).getAsJsonObject().get("uuid").getAsString(), //
                        is(other.getUUID()));
    }

    // ------------------------------------------------------------------ undo

    @Test
    public void whatWasCreatedWhileNothingUsedItIsRemovableAgain()
    {
        var client = new Client();
        SecuritiesHandler.create(client, body("{\"name\": \"Weg wieder\", \"currencyCode\": \"EUR\"}"));
        var uuid = client.getSecurities().get(0).getUUID();

        SecuritiesHandler.delete(client, uuid);

        assertThat("the delete tool is this creation's undo", client.getSecurities().isEmpty(), is(true));
    }

    // ----------------------------------------------------------------- prompt

    @Test
    public void thePromptNamesTheInstrumentThatWouldBeMade()
    {
        var facts = SecuritiesHandler.describeCreation(new Client(), //
                        body("{\"name\": \"  Neue Fonds  \", \"currencyCode\": \"EUR\"}"), "datei.portfolio");

        assertThat(facts.fileLabel(), is("datei.portfolio"));
        assertThat(facts.subjectType(), is("instrument"));
        assertThat(facts.subjectName(), is("Neue Fonds"));
        // the operation is the key a session grant is remembered under, and the
        // describer is not the one who decides it
        assertThat(facts.operation(), is(nullValue()));
    }

    @Test
    public void aCreationThatCouldNotBeMadeIsRefusedBeforeAnyoneIsAsked()
    {
        var client = new Client();
        var other = new SecurityBuilder().addTo(client);
        other.setName("DekaLux");
        other.setWkn("974587");

        var taken = capture(() -> SecuritiesHandler.describeCreation(client, //
                        body("{\"name\": \"Zweite\", \"currencyCode\": \"EUR\", \"wkn\": \"974587\"}"), "x"));
        assertThat(taken.getStatus(), is(422));
        assertThat(taken.getErrors().get(0).code(), is("duplicate-identifier"));

        var noCurrency = capture(() -> SecuritiesHandler.describeCreation(client, //
                        body("{\"name\": \"Zweite\"}"), "x"));
        assertThat(noCurrency.getErrors().get(0).field(), is("currencyCode"));

        var badIsin = capture(() -> SecuritiesHandler.describeCreation(client, //
                        body("{\"name\": \"Zweite\", \"currencyCode\": \"EUR\", \"isin\": \"LU0062624901\"}"), "x"));
        assertThat(badIsin.getErrors().get(0).code(), is("invalid-isin"));
    }
}
