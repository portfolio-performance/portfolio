package name.abuchen.portfolio.cli.masterdata;

import static name.abuchen.portfolio.cli.masterdata.Instruments.INSTRUMENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.CliException;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class InstrumentsTest
{
    private final Client client = new Client();

    private Security instrument(String name, String isin, String ticker)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        security.setIsin(isin);
        security.setTickerSymbol(ticker);
        return security;
    }

    private Map<String, Object> render(Security security)
    {
        return INSTRUMENTS.renderer(client).apply(security);
    }

    private Security resolve(String value) throws CliException
    {
        return INSTRUMENTS.reference().resolve(client, "instrument", value);
    }

    private CliException failure(String value)
    {
        return assertThrows(CliException.class, () -> resolve(value));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> candidates(CliException e)
    {
        return (List<Map<String, Object>>) e.toProblem().get("candidates");
    }

    @Test
    public void rendersEveryMemberInDeclarationOrder()
    {
        var security = instrument("ACME Corp", "US0000000001", "ACME");
        security.setCurrencyCode("USD");
        security.setWkn("A12345");
        security.setNote("watch closely");
        security.setRetired(true);
        security.setUpdatedAt(Instant.parse("2026-10-24T15:04:57.123Z"));

        var item = render(security);

        assertThat(item.keySet().toString(), is(
                        "[uuid, name, currencyCode, isin, tickerSymbol, wkn, note, retired, attributes, updatedAt]"));
        assertThat(item.get("uuid"), is(security.getUUID()));
        assertThat(item.get("name"), is("ACME Corp"));
        assertThat(item.get("currencyCode"), is("USD"));
        assertThat(item.get("isin"), is("US0000000001"));
        assertThat(item.get("wkn"), is("A12345"));
        assertThat(item.get("tickerSymbol"), is("ACME"));
        assertThat(item.get("note"), is("watch closely"));
        assertThat(item.get("retired"), is(true));
        assertThat(item.get("attributes"), is(List.of()));
        assertThat(item.get("updatedAt"), is("2026-10-24T15:04:57Z"));
    }

    @Test
    public void nullAndBlankOptionalStringsAreOmittedButNameRetiredAndAttributesAreNot()
    {
        var security = instrument(null, null, "  ");
        security.setWkn("");
        security.setNote("\t");
        security.setCurrencyCode(null);
        security.setUpdatedAt(null);

        var item = render(security);

        assertThat(item.keySet().toString(), is("[uuid, name, retired, attributes]"));
        assertThat(item.get("name"), is(""));
        assertThat(item.get("retired"), is(false));
    }

    @Test
    public void presentValuesAreRenderedUntrimmed()
    {
        var security = instrument(" padded ", null, null);
        security.setNote(" note ");

        var item = render(security);

        assertThat(item.get("name"), is(" padded "));
        assertThat(item.get("note"), is(" note "));
    }

    @Test
    public void resolvesByUuidIsinTickerAndNameIgnoringCaseOnly() throws CliException
    {
        var acme = instrument("ACME Corp", "US0000000001", "ACME");
        instrument("Other", "US0000000002", "OTH");

        assertThat(resolve(acme.getUUID()), is(sameInstance(acme)));
        assertThat(resolve(acme.getUUID().toUpperCase()), is(sameInstance(acme)));
        assertThat(resolve("us0000000001"), is(sameInstance(acme)));
        assertThat(resolve("acme"), is(sameInstance(acme)));
        assertThat(resolve("ACME CORP"), is(sameInstance(acme)));

        assertThat(failure(" ACME Corp").getCode(), is("reference-not-found"));
        assertThat(failure("ACME Co").getCode(), is("reference-not-found"));
    }

    @Test
    public void aUuidMatchWinsOverAnotherInstrumentsName() throws CliException
    {
        var target = instrument("Target", null, null);
        instrument(target.getUUID(), null, null);

        assertThat(resolve(target.getUUID()), is(sameInstance(target)));
    }

    @Test
    public void anInstrumentMatchingOnSeveralKeysCountsOnce() throws CliException
    {
        var same = instrument("ACME", "ACME", "ACME");

        assertThat(resolve("acme"), is(sameInstance(same)));
    }

    @Test
    public void matchesOnDifferentKeysArePooledAndNeverGuessed()
    {
        var byTicker = instrument("Software AG", null, "SAP");
        var byName = instrument("SAP", null, null);

        var e = failure("SAP");

        assertThat(e.getCode(), is("reference-ambiguous"));
        assertThat(e.toProblem().get("status"), is(409));
        var candidates = candidates(e);
        assertThat(candidates.size(), is(2));
        assertThat(candidates.get(0).get("uuid"), is(byName.getUUID()));
        assertThat(candidates.get(0).get("matchedBy"), is(List.of("name")));
        assertThat(candidates.get(1).get("uuid"), is(byTicker.getUUID()));
        assertThat(candidates.get(1).get("matchedBy"), is(List.of("tickerSymbol")));
    }

    @Test
    public void namesThatDifferOnlyByCaseAreAmbiguous()
    {
        instrument("Acme", null, null);
        instrument("ACME", null, null);

        assertThat(failure("acme").getCode(), is("reference-ambiguous"));
    }

    @Test
    public void retiredInstrumentsTakePartAndCandidatesCarryTheFlag()
    {
        var active = instrument("Active", "US0000000001", null);
        var retired = instrument("Retired", "US0000000001", null);
        retired.setRetired(true);

        var candidates = candidates(failure("US0000000001"));

        assertThat(candidates.get(0).get("uuid"), is(active.getUUID()));
        assertThat(candidates.get(0).get("retired"), is(false));
        assertThat(candidates.get(1).get("uuid"), is(retired.getUUID()));
        assertThat(candidates.get(1).get("retired"), is(true));
    }

    @Test
    public void candidatesListTheSetDetailsAndAreOrderedByNameThenUuid()
    {
        var b = instrument("Same", "XX", null);
        b.setCurrencyCode("USD");
        var a = instrument("same", "XX", "T");
        a.setCurrencyCode("");

        var candidates = candidates(failure("XX"));

        var first = a.getUUID().compareTo(b.getUUID()) < 0 ? a : b;
        assertThat(candidates.get(0).get("uuid"), is(first.getUUID()));

        var withCurrency = candidates.stream().filter(c -> c.get("uuid").equals(b.getUUID())).findFirst()
                        .orElseThrow();
        assertThat(withCurrency.keySet().toString(), is("[uuid, name, retired, matchedBy, isin, currencyCode]"));
        assertThat(withCurrency.get("isin"), is("XX"));
        assertThat(withCurrency.get("currencyCode"), is("USD"));

        var withTicker = candidates.stream().filter(c -> c.get("uuid").equals(a.getUUID())).findFirst()
                        .orElseThrow();
        assertThat(withTicker.keySet().toString(), is("[uuid, name, retired, matchedBy, isin, tickerSymbol]"));
    }

    @Test
    public void unknownReferencesAreNotFoundAndBlankStoredKeysNeverMatch()
    {
        instrument(null, null, "  ");
        instrument("", "", null);

        var e = failure("nothing");

        assertThat(e.getCode(), is("reference-not-found"));
        assertThat(e.toProblem().get("status"), is(404));
        assertThat(e.toProblem().get("option"), is("instrument"));
        assertThat(e.toProblem().get("value"), is("nothing"));
        assertThat(e.getExitCode(), is(1));
    }

    @Test
    public void aBlankReferenceIsAUsageError()
    {
        var e = failure("  ");

        assertThat(e.getCode(), is("invalid-option-value"));
        assertThat(e.getExitCode(), is(2));
        assertThat(e.toProblem().get("option"), is("instrument"));
    }
}
