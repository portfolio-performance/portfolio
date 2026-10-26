package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class ReferencesTest
{
    private static Security instrument(Client client, String name, String isin, String ticker)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        security.setIsin(isin);
        security.setTickerSymbol(ticker);
        return security;
    }

    private static Map<String, Object> problem(CliException e)
    {
        return e.toProblem();
    }

    @Test
    public void instrumentsResolveByUuidIsinTickerAndName() throws CliException
    {
        var client = new Client();
        var acme = instrument(client, "ACME Corp", "US0000000001", "ACME");
        instrument(client, "Other", "US0000000002", "OTH");

        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", acme.getUUID()), is(sameInstance(acme)));
        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", "US0000000001"), is(sameInstance(acme)));
        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", "ACME"), is(sameInstance(acme)));
        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", "ACME Corp"), is(sameInstance(acme)));
    }

    @Test
    public void comparisonsIgnoreCaseButNothingElse() throws CliException
    {
        var client = new Client();
        var acme = instrument(client, "ACME Corp", "US0000000001", "ACME");

        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", "us0000000001"), is(sameInstance(acme)));
        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", "acme corp"), is(sameInstance(acme)));
        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", acme.getUUID().toUpperCase()), is(sameInstance(acme)));

        assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", " ACME Corp"));
        assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", "ACME Co"));
    }

    @Test
    public void namesThatDifferOnlyByCaseAreAmbiguousNeverGuessed()
    {
        var client = new Client();
        instrument(client, "Acme", null, null);
        instrument(client, "ACME", null, null);

        var e = assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", "acme"));

        assertThat(e.getCode(), is("reference-ambiguous"));
    }

    @Test
    public void aUuidMatchWinsOverAnotherInstrumentsName() throws CliException
    {
        var client = new Client();
        var target = instrument(client, "Target", null, null);
        instrument(client, target.getUUID(), null, null);

        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", target.getUUID()), is(sameInstance(target)));
    }

    @Test
    public void matchesOnDifferentFieldsArePooledWithoutPrecedence()
    {
        var client = new Client();
        var byTicker = instrument(client, "Software AG", null, "SAP");
        var byName = instrument(client, "SAP", null, null);

        var e = assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", "SAP"));

        assertThat(e.getCode(), is("reference-ambiguous"));
        var candidates = (List<?>) problem(e).get("candidates");
        assertThat(candidates.size(), is(2));
        assertThat(((Map<?, ?>) candidates.get(0)).get("uuid"), is(byName.getUUID()));
        assertThat(((Map<?, ?>) candidates.get(0)).get("matchedBy"), is(List.of("name")));
        assertThat(((Map<?, ?>) candidates.get(1)).get("uuid"), is(byTicker.getUUID()));
        assertThat(((Map<?, ?>) candidates.get(1)).get("matchedBy"), is(List.of("tickerSymbol")));
    }

    @Test
    public void anInstrumentMatchingOnSeveralFieldsCountsOnce() throws CliException
    {
        var client = new Client();
        var same = instrument(client, "ACME", "ACME", "ACME");

        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", "acme"), is(sameInstance(same)));
    }

    @Test
    public void retiredInstrumentsTakePartWithoutPreference()
    {
        var client = new Client();
        var active = instrument(client, "Active", "US0000000001", null);
        var retired = instrument(client, "Retired", "US0000000001", null);
        retired.setRetired(true);

        var e = assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", "US0000000001"));

        assertThat(e.getCode(), is("reference-ambiguous"));
        var candidates = (List<?>) problem(e).get("candidates");
        assertThat(((Map<?, ?>) candidates.get(0)).get("uuid"), is(active.getUUID()));
        assertThat(((Map<?, ?>) candidates.get(0)).get("retired"), is(false));
        assertThat(((Map<?, ?>) candidates.get(1)).get("uuid"), is(retired.getUUID()));
        assertThat(((Map<?, ?>) candidates.get(1)).get("retired"), is(true));
    }

    @Test
    public void candidatesCarryTheFieldsAndAreOrderedByNameThenUuid()
    {
        var client = new Client();
        var b = instrument(client, "Same", "XX", null);
        b.setCurrencyCode("USD");
        var a = instrument(client, "same", "XX", "T");
        a.setCurrencyCode("");

        var e = assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", "XX"));

        var candidates = (List<?>) problem(e).get("candidates");
        var expectedFirst = a.getUUID().compareTo(b.getUUID()) < 0 ? a : b;
        assertThat(((Map<?, ?>) candidates.get(0)).get("uuid"), is(expectedFirst.getUUID()));

        var withCurrency = candidates.stream().map(c -> (Map<?, ?>) c)
                        .filter(c -> c.get("uuid").equals(b.getUUID())).findFirst().orElseThrow();
        assertThat(withCurrency.keySet().toString(),
                        is("[uuid, name, retired, matchedBy, isin, currencyCode]"));
        assertThat(withCurrency.get("isin"), is("XX"));
        assertThat(withCurrency.get("currencyCode"), is("USD"));

        var withTicker = candidates.stream().map(c -> (Map<?, ?>) c).filter(c -> c.get("uuid").equals(a.getUUID()))
                        .findFirst().orElseThrow();
        assertThat(withTicker.keySet().toString(), is("[uuid, name, retired, matchedBy, isin, tickerSymbol]"));
    }

    @Test
    public void unknownReferencesAreNotFoundAndNullOrBlankStoredFieldsNeverMatch()
    {
        var client = new Client();
        instrument(client, null, null, "  ");
        instrument(client, "", "", null);

        var e = assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", "nothing"));

        assertThat(e.getCode(), is("reference-not-found"));
        assertThat(problem(e).get("status"), is(404));
        assertThat(problem(e).get("option"), is("instrument"));
        assertThat(problem(e).get("value"), is("nothing"));
        assertThat(e.getExitCode(), is(1));

        assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", " "));
    }

    @Test
    public void aBlankReferenceIsAUsageError()
    {
        var client = new Client();

        var e = assertThrows(CliException.class, () -> References.resolve(References.INSTRUMENT, client, "instrument", "  "));

        assertThat(e.getCode(), is("invalid-option-value"));
        assertThat(e.getExitCode(), is(2));
        assertThat(problem(e).get("option"), is("instrument"));
    }

    @Test
    public void accountsResolveByUuidAndNameEachKindSearchingOnlyItsOwn() throws CliException
    {
        var client = new Client();
        var cash = new AccountBuilder().addTo(client);
        cash.setName("Main");
        var depot = new PortfolioBuilder(cash).addTo(client);
        depot.setName("Broker");
        var security = instrument(client, "Main", "US1", null);

        assertThat(References.resolve(References.CASH_ACCOUNT, client, "account", cash.getUUID()), is(sameInstance(cash)));
        assertThat(References.resolve(References.CASH_ACCOUNT, client, "account", "main"), is(sameInstance(cash)));
        assertThat(References.resolve(References.INVESTMENT_ACCOUNT, client, "account", depot.getUUID()), is(sameInstance(depot)));
        assertThat(References.resolve(References.INVESTMENT_ACCOUNT, client, "account", "BROKER"), is(sameInstance(depot)));

        // a UUID or name of another kind is not found
        assertThat(assertThrows(CliException.class,
                        () -> References.resolve(References.CASH_ACCOUNT, client, "account", depot.getUUID())).getCode(),
                        is("reference-not-found"));
        assertThat(assertThrows(CliException.class,
                        () -> References.resolve(References.INVESTMENT_ACCOUNT, client, "account", "Main")).getCode(),
                        is("reference-not-found"));
        assertThat(assertThrows(CliException.class,
                        () -> References.resolve(References.INSTRUMENT, client, "instrument", cash.getUUID())).getCode(),
                        is("reference-not-found"));
        assertThat(References.resolve(References.INSTRUMENT, client, "instrument", "Main"), is(sameInstance(security)));
    }

    @Test
    public void ambiguousAccountsListTheirCurrencyAndNoIsin()
    {
        var client = new Client();
        var first = new AccountBuilder("USD").addTo(client);
        first.setName("Main");
        var second = new AccountBuilder().addTo(client);
        second.setName("MAIN");

        var e = assertThrows(CliException.class, () -> References.resolve(References.CASH_ACCOUNT, client, "account", "Main"));

        assertThat(e.getCode(), is("reference-ambiguous"));
        assertThat(e.getMessage(), containsString("Main"));
        var candidate = (Map<?, ?>) ((List<?>) problem(e).get("candidates")).stream()
                        .filter(c -> ((Map<?, ?>) c).get("uuid").equals(first.getUUID())).findFirst().orElseThrow();
        assertThat(candidate.keySet().toString(), is("[uuid, name, retired, matchedBy, currencyCode]"));
        assertThat(candidate.get("currencyCode"), is("USD"));
        assertThat(candidate.get("matchedBy"), is(List.of("name")));
    }
}
