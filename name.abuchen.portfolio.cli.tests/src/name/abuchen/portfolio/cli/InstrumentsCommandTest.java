package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class InstrumentsCommandTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private CliRun run(Client client, String... options) throws IOException
    {
        return cli.save(client).run("instruments", options);
    }

    private static Security instrument(Client client, String name)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        return security;
    }

    @Test
    public void listsInstrumentsWithTheDocumentedFields() throws IOException
    {
        var client = new Client();
        var security = instrument(client, "ACME Corp");
        security.setCurrencyCode("USD");
        security.setIsin("US0000000001");
        security.setWkn("A12345");
        security.setTickerSymbol("ACME");
        security.setNote("watch closely");
        security.setRetired(true);

        var run = run(client);

        assertThat(run.exitCode, is(0));
        assertThat(run.data().get("total").getAsInt(), is(1));

        var item = run.items().get(0).getAsJsonObject();
        assertThat(item.keySet(), is(Set.of("uuid", "name", "currencyCode", "isin", "wkn", "tickerSymbol", "note",
                        "retired", "attributes", "updatedAt")));
        assertThat(item.get("uuid").getAsString(), is(security.getUUID()));
        assertThat(item.get("name").getAsString(), is("ACME Corp"));
        assertThat(item.get("currencyCode").getAsString(), is("USD"));
        assertThat(item.get("isin").getAsString(), is("US0000000001"));
        assertThat(item.get("wkn").getAsString(), is("A12345"));
        assertThat(item.get("tickerSymbol").getAsString(), is("ACME"));
        assertThat(item.get("note").getAsString(), is("watch closely"));
        assertThat(item.get("retired").getAsBoolean(), is(true));
        assertThat(item.getAsJsonArray("attributes").size(), is(0));
    }

    @Test
    public void nullAndBlankOptionalStringsAreOmittedButNameRetiredAndAttributesAreNot() throws IOException
    {
        var client = new Client();
        var security = instrument(client, "Bare");
        security.setIsin(null);
        security.setTickerSymbol("  ");
        security.setWkn("");
        security.setNote("\t");
        security.setCurrencyCode("");

        var item = run(client).items().get(0).getAsJsonObject();

        assertThat(item.keySet(), is(Set.of("uuid", "name", "retired", "attributes", "updatedAt")));
        assertThat(item.get("retired").getAsBoolean(), is(false));
    }

    @Test
    public void indicesWithoutCurrencyAreListedWithoutIt() throws IOException
    {
        var client = new Client();
        var index = instrument(client, "DAX");
        index.setCurrencyCode(null);

        var item = run(client).items().get(0).getAsJsonObject();

        assertThat(item.get("name").getAsString(), is("DAX"));
        assertThat(item.has("currencyCode"), is(false));
    }

    @Test
    public void presentValuesAreEmittedUntrimmed() throws IOException
    {
        var client = new Client();
        var security = instrument(client, " padded ");
        security.setNote(" note ");

        var item = run(client).items().get(0).getAsJsonObject();

        assertThat(item.get("name").getAsString(), is(" padded "));
        assertThat(item.get("note").getAsString(), is(" note "));
    }

    @Test
    public void nullNameIsEmittedAsEmptyString() throws IOException
    {
        var client = new Client();
        var security = instrument(client, null);

        var item = run(client).items().get(0).getAsJsonObject();

        assertThat(item.get("uuid").getAsString(), is(security.getUUID()));
        assertThat(item.get("name").getAsString(), is(""));
    }

    @Test
    public void listsEveryInstrumentIncludingRetiredOnes() throws IOException
    {
        var client = new Client();
        instrument(client, "active");
        instrument(client, "retired").setRetired(true);

        var run = run(client);

        var names = new ArrayList<String>();
        run.items().forEach(i -> names.add(i.getAsJsonObject().get("name").getAsString()));
        assertThat(names, contains("active", "retired"));
    }
}
