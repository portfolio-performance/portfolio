package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;

/**
 * The two accounts a file is built from, made through the API: money in one
 * currency, and the depot that holds papers against a reference account.
 *
 * Most of the depot's tests are about one field. A depot without settled money
 * is how a file ends up with two brokers and nobody knowing whose cash paid -
 * so the reference is either named, or the file's one obvious account, and a
 * file that leaves the choice open is refused rather than guessed at.
 */
@SuppressWarnings("nls")
public class AccountWriteHandlerTest
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

    private static Account cash(Client client, String name, String currency)
    {
        var account = new Account(name);
        account.setCurrencyCode(currency);
        client.addAccount(account);
        return account;
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

    // ------------------------------------------------------------------- cash

    @Test
    public void aNewAccountTakesTheFilesOwnCurrencyWhenNoCurrencyIsGiven()
    {
        var client = new Client();
        var dirty = watch(client);

        var json = AccountWriteHandler.createCash(client, body("{\"name\": \"Flatex\"}"));

        assertThat(json.get("created").getAsBoolean(), is(true));
        assertThat(json.get("name").getAsString(), is("Flatex"));
        assertThat(json.get("currencyCode").getAsString(), is(client.getBaseCurrency()));
        assertThat(json.get("uuid").getAsString(), is(notNullValue()));

        assertThat(client.getAccounts().size(), is(1));
        assertThat(client.getAccounts().get(0).getName(), is("Flatex"));
        assertThat(dirty[0], is(1));
    }

    @Test
    public void aCurrencyTheFileDoesNotMoveInIsRefusedNamingTheOnesItDoes()
    {
        var client = new Client();
        var dirty = watch(client);

        var refusal = capture(() -> AccountWriteHandler.createCash(client, //
                        body("{\"name\": \"US Broker\", \"currencyCode\": \"USD\"}")));

        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("currencyCode"));
        assertThat(refusal.getErrors().get(0).code(), is("unknown-currency"));
        assertThat(refusal.getErrors().get(0).message(), containsString("EUR"));
        assertThat(dirty[0], is(0));

        // once the file does move in it - some instrument carries it - it works
        new SecurityBuilder("USD").addTo(client);
        var json = AccountWriteHandler.createCash(client, //
                        body("{\"name\": \"US Broker\", \"currencyCode\": \"USD\"}"));
        assertThat(json.get("currencyCode").getAsString(), is("USD"));
    }

    @Test
    public void aNameTwiceInTheSameCurrencyIsRefusedAndInAnotherIsOrdinary()
    {
        var client = new Client();
        new SecurityBuilder("USD").addTo(client);
        AccountWriteHandler.createCash(client, body("{\"name\": \"Flatex\"}"));

        var twice = capture(() -> AccountWriteHandler.createCash(client, body("{\"name\": \"Flatex\"}")));
        assertThat(twice.getStatus(), is(409));
        assertThat(twice.getType(), is("name-taken"));
        assertThat(client.getAccounts().size(), is(1));

        // another currency makes another account - that pair is ordinary
        var usd = AccountWriteHandler.createCash(client, //
                        body("{\"name\": \"Flatex\", \"currencyCode\": \"USD\"}"));
        assertThat(usd.get("currencyCode").getAsString(), is("USD"));
        assertThat(client.getAccounts().size(), is(2));
    }

    @Test
    public void aMissingNameIsRefusedAndANoteTravelsWhereItBelongs()
    {
        var client = new Client();

        var noName = capture(() -> AccountWriteHandler.createCash(client, body("{}")));
        assertThat(noName.getStatus(), is(400));
        assertThat(noName.getErrors().get(0).field(), is("name"));

        var json = AccountWriteHandler.createCash(client, //
                        body("{\"name\": \"Depot-Verwahrung\", \"note\": \"gebuehrenfrei seit 2024\"}"));
        assertThat(json.get("note").getAsString(), is("gebuehrenfrei seit 2024"));
    }

    // ------------------------------------------------------------- investment

    @Test
    public void aNewDepotPointsAtTheCashAccountItWasGiven()
    {
        var client = new Client();
        var money = cash(client, "Geld", "EUR");
        var dirty = watch(client);

        var json = AccountWriteHandler.createInvestment(client, body(
                        "{\"name\": \"Depot\", \"referenceCashAccount\": \"" + money.getUUID() + "\"}"));

        assertThat(json.get("created").getAsBoolean(), is(true));
        assertThat(json.get("referenceCashAccount").getAsString(), is(money.getUUID()));

        assertThat(client.getPortfolios().size(), is(1));
        assertThat(client.getPortfolios().get(0).getReferenceAccount(), is(money));
        assertThat(dirty[0], is(1));
    }

    @Test
    public void theReferenceLeftOutIsTheFilesOneAccountInItsBaseCurrency()
    {
        var client = new Client();
        var money = cash(client, "Geld", "EUR");
        new SecurityBuilder("USD").addTo(client);
        cash(client, "Dollar-Konto", "USD");

        var json = AccountWriteHandler.createInvestment(client, body("{\"name\": \"Depot\"}"));

        assertThat(json.get("referenceCashAccount").getAsString(), is(money.getUUID()));
    }

    @Test
    public void aFileThatLeavesTheMoneyOpenIsRefusedRatherThanGuessedAt()
    {
        var client = new Client();
        var dirty = watch(client);

        // nothing to point at
        var none = capture(() -> AccountWriteHandler.createInvestment(client, body("{\"name\": \"Depot\"}")));
        assertThat(none.getStatus(), is(400));
        assertThat(none.getErrors().get(0).field(), is("referenceCashAccount"));

        // two to point at, and the API will not pick one
        cash(client, "Bezl", "EUR");
        cash(client, "Banki", "EUR");
        var several = capture(() -> AccountWriteHandler.createInvestment(client, body("{\"name\": \"Depot\"}")));
        assertThat(several.getStatus(), is(400));
        assertThat(dirty[0], is(0));

        // named, it is a depot at once - even if the name was the ambiguous one
        var json = AccountWriteHandler.createInvestment(client, body(
                        "{\"name\": \"Depot\", \"referenceCashAccount\": \"" + client.getAccounts().get(1).getUUID()
                                        + "\"}"));
        assertThat(json.get("referenceCashAccount").getAsString(), is(client.getAccounts().get(1).getUUID()));
    }

    @Test
    public void aReferenceTheFileDoesNotHoldIsRefusedAsAnInvalidValue()
    {
        var client = new Client();
        cash(client, "Geld", "EUR");

        var refusal = capture(() -> AccountWriteHandler.createInvestment(client, //
                        body("{\"name\": \"Depot\", \"referenceCashAccount\": \"no-such-uuid\"}")));

        assertThat(refusal.getStatus(), is(400));
        assertThat(refusal.getErrors().get(0).field(), is("referenceCashAccount"));
        assertThat(refusal.getErrors().get(0).code(), is("invalid-value"));
    }

    @Test
    public void aDepotNameTwiceIsRefusedAndThePromptNamesTheMoneyItPointsAt()
    {
        var client = new Client();
        var money = cash(client, "Geld", "EUR");
        client.addPortfolio(new Portfolio("Depot"));

        var twice = capture(() -> AccountWriteHandler.createInvestment(client, //
                        body("{\"name\": \"Depot\", \"referenceCashAccount\": \"" + money.getUUID() + "\"}")));
        assertThat(twice.getStatus(), is(409));
        assertThat(twice.getType(), is("name-taken"));

        var facts = AccountWriteHandler.describeInvestment(client, "My File", //
                        body("{\"name\": \"Zweitdepot\", \"referenceCashAccount\": \"" + money.getUUID() + "\"}"));
        assertThat(facts.subjectType(), is("investment-account"));
        assertThat(facts.subjectName(), is("Zweitdepot"));
        assertThat(facts.targetName(), is("Geld"));
    }

    @Test
    public void creatingEitherAccountDoesNotTouchWhatIsAlreadyThere()
    {
        var client = new Client();
        var tenThousand = new AccountBuilder("EUR").deposit_("2024-01-01", 10_00_00L).addTo(client);

        AccountWriteHandler.createCash(client, body("{\"name\": \"Zweitkonto\"}"));
        AccountWriteHandler.createInvestment(client, body(
                        "{\"name\": \"Depot\", \"referenceCashAccount\": \"" + tenThousand.getUUID() + "\"}"));

        assertThat(client.getAccounts().size(), is(2));
        assertThat(client.getAccounts().get(0).getUUID(), is(tenThousand.getUUID()));
        assertThat(client.getAccounts().get(0).getTransactions().size(), is(1));
        assertThat(client.getPortfolios().get(0).getReferenceAccount().getUUID(), is(tenThousand.getUUID()));
    }
}
