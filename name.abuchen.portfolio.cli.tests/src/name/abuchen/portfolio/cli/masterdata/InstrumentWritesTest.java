package name.abuchen.portfolio.cli.masterdata;

import static name.abuchen.portfolio.cli.masterdata.Instruments.INSTRUMENTS;
import static name.abuchen.portfolio.cli.masterdata.Writing.ALL_EXISTED;
import static name.abuchen.portfolio.cli.masterdata.Writing.UNCLAIMED;
import static name.abuchen.portfolio.cli.masterdata.Writing.applied;
import static name.abuchen.portfolio.cli.masterdata.Writing.asJson;
import static name.abuchen.portfolio.cli.masterdata.Writing.failures;
import static name.abuchen.portfolio.cli.masterdata.Writing.input;
import static name.abuchen.portfolio.cli.masterdata.Writing.json;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import org.junit.Test;

import name.abuchen.portfolio.cli.command.Claims;
import name.abuchen.portfolio.cli.command.Outcome;
import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

public class InstrumentWritesTest
{
    private static final String APPLE_ISIN = "US0378331005";
    private static final String SAP_ISIN = "DE0007164600";

    private final Client client = new Client();

    private Security instrument(String name)
    {
        var security = new SecurityBuilder("USD").addTo(client);
        security.setName(name);
        return security;
    }

    private Map<String, Object> render(Client in, Security security)
    {
        return INSTRUMENTS.renderer(in).apply(security);
    }

    private Outcome add(String input)
    {
        return INSTRUMENTS.writes(client, ALL_EXISTED).add(json(input));
    }

    private Outcome update(Security security, String fields)
    {
        return INSTRUMENTS.writes(client, ALL_EXISTED)
                        .update(json("{\"uuid\":\"" + security.getUUID() + "\"," + fields + "}"), UNCLAIMED);
    }

    private static Security byUuid(Client in, String uuid)
    {
        return in.getSecurities().stream().filter(s -> s.getUUID().equals(uuid)).findFirst().orElseThrow();
    }

    @Test
    public void renderAddRenderRoundTripsAndUpdatingWithTheRenderChangesNothing()
    {
        var original = instrument("ACME Corp");
        original.setIsin(APPLE_ISIN);
        original.setTickerSymbol("ACME");
        original.setWkn("A12345");
        original.setNote("watch closely");
        original.setRetired(true);
        var rendered = render(client, original);

        var copy = new Client();
        var added = applied(INSTRUMENTS.writes(copy, ALL_EXISTED)
                        .add(input(rendered, "uuid", "attributes", "updatedAt")));

        assertThat(added.kind(), is(Outcome.Entity.INSTRUMENT));
        assertThat(added.before(), is(nullValue()));
        assertThat(added.after(), is(Map.of("name", "ACME Corp", "currencyCode", "USD", "isin", APPLE_ISIN,
                        "tickerSymbol", "ACME", "wkn", "A12345", "note", "watch closely", "retired", true)));
        var reRendered = render(copy, byUuid(copy, added.recordId()));
        assertThat(input(reRendered, "uuid", "updatedAt"), is(input(rendered, "uuid", "updatedAt")));
        assertThat(byUuid(copy, added.recordId()).getUpdatedAt(), is(notNullValue()));

        var unchanged = INSTRUMENTS.writes(client, ALL_EXISTED).update(asJson(rendered), UNCLAIMED);
        assertThat(unchanged, is(new Outcome.Unchanged(original.getUUID())));
    }

    @Test
    public void anAddNeedsOnlyANameAndKeepsTheModelDefaults()
    {
        var added = applied(add("{\"name\":\"Bare\"}"));

        assertThat(added.after(), is(Map.of("name", "Bare", "currencyCode", "EUR", "retired", false)));
        var security = byUuid(client, added.recordId());
        assertThat(security.getIsin(), is(nullValue()));
        assertThat(security.isRetired(), is(false));
    }

    @Test
    public void anUpdateChangesOnlyThePresentMembersAndReportsBeforeAndAfter()
    {
        var security = instrument("Old");
        security.setWkn("W1");
        security.setNote("first");

        var updated = applied(update(security, "\"name\":\"Renamed\",\"note\":null,\"retired\":true,\"wkn\":\"W1\""));

        assertThat(updated.recordId(), is(security.getUUID()));
        assertThat(updated.before().toString(), is("{name=Old, note=first, retired=false}"));
        assertThat(updated.after().toString(), is("{name=Renamed, note=null, retired=true}"));
        assertThat(security.getName(), is("Renamed"));
        assertThat(security.getNote(), is(nullValue()));
        assertThat(security.getWkn(), is("W1"));
    }

    @Test
    public void refusesMissingAndClearedNames()
    {
        var security = instrument("Kept");

        assertThat(failures(add("{\"note\":\"nameless\"}")), is(Set.of("name:required")));
        assertThat(failures(add("{\"name\":\"  \"}")), is(Set.of("name:required")));
        assertThat(failures(update(security, "\"name\":null")), is(Set.of("name:required")));
        assertThat(security.getName(), is("Kept"));
        assertThat(client.getSecurities().size(), is(1));
    }

    @Test
    public void refusesMistypedMembers()
    {
        assertThat(failures(add("{\"name\":5,\"isin\":[],\"retired\":\"yes\"}")),
                        is(Set.of("name:invalid-type", "isin:invalid-type", "retired:invalid-type")));
        assertThat(client.getSecurities().isEmpty(), is(true));
    }

    @Test
    public void refusesAnUnknownCurrency()
    {
        assertThat(failures(add("{\"name\":\"Odd\",\"currencyCode\":\"ZZZ\"}")),
                        is(Set.of("currencyCode:invalid-currency")));
    }

    @Test
    public void theCurrencyCanChangeOnlyBeforeTheInstrumentHasTransactions()
    {
        var idle = instrument("Idle");
        var traded = instrument("Traded");
        new AccountBuilder("USD").dividend("2026-01-02", 100, traded).addTo(client);

        assertThat(failures(update(traded, "\"currencyCode\":\"EUR\"")), is(Set.of("currencyCode:currency-immutable")));
        assertThat(traded.getCurrencyCode(), is("USD"));
        assertThat(applied(update(traded, "\"currencyCode\":\"USD\",\"note\":\"kept\"")).after().keySet(),
                        is(Set.of("note")));

        applied(update(idle, "\"currencyCode\":\"EUR\""));
        assertThat(idle.getCurrencyCode(), is("EUR"));
    }

    @Test
    public void refusesAnInvalidIsinNamingTheCoreCheck()
    {
        var outcome = add("{\"name\":\"Bad\",\"isin\":\"US0378331006\"}");

        assertThat(failures(outcome), is(Set.of("isin:invalid-isin")));
        var failure = ((Outcome.Refused) outcome).failures().get(0);
        assertThat(failure.check(), is("Isin.isValid"));
        assertThat(failure.message(), is("'US0378331006' is not a valid ISIN."));
    }

    @Test
    public void anIsinNamesOneInstrumentOnly()
    {
        var holder = instrument("Holder");
        holder.setIsin(APPLE_ISIN);

        var outcome = add("{\"name\":\"Twin\",\"isin\":\"" + APPLE_ISIN + "\"}");
        assertThat(failures(outcome), is(Set.of("isin:duplicate-isin")));
        assertThat(((Outcome.Refused) outcome).failures().get(0).message(),
                        is("The ISIN is already used by instrument " + holder.getUUID() + "."));

        // an instrument keeps its own ISIN, and one added earlier takes it
        applied(update(holder, "\"isin\":\"" + APPLE_ISIN + "\",\"note\":\"same\""));
        applied(add("{\"name\":\"First\",\"isin\":\"" + SAP_ISIN + "\"}"));
        assertThat(failures(add("{\"name\":\"Second\",\"isin\":\"" + SAP_ISIN + "\"}")),
                        is(Set.of("isin:duplicate-isin")));
    }

    @Test
    public void anUpdateNamesAnExistingInstrumentByUuid()
    {
        var other = new AccountBuilder().addTo(client);
        var added = instrument("Added in this batch");
        var writes = INSTRUMENTS.writes(client, entity -> entity != added);

        assertThat(failures(writes.update(json("{\"note\":\"x\"}"), UNCLAIMED)), is(Set.of("uuid:required")));
        assertThat(failures(writes.update(json("{\"uuid\":7}"), UNCLAIMED)), is(Set.of("uuid:invalid-type")));
        assertThat(failures(writes.update(json("{\"uuid\":\"" + other.getUUID() + "\"}"), UNCLAIMED)),
                        is(Set.of("uuid:reference-not-found")));
        assertThat(failures(writes.update(json("{\"uuid\":\"" + added.getUUID() + "\"}"), UNCLAIMED)),
                        is(Set.of("uuid:reference-not-found")));
    }

    @Test
    public void anUpdateOfAnInstrumentAnEarlierOperationClaimedIsRefused()
    {
        var security = instrument("Claimed");
        var claimedBy2 = (Claims) uuid -> OptionalInt.of(2);

        var outcome = INSTRUMENTS.writes(client, ALL_EXISTED)
                        .update(json("{\"uuid\":\"" + security.getUUID() + "\",\"note\":\"two\"}"), claimedBy2);

        assertThat(failures(outcome), is(Set.of("uuid:duplicate-target")));
        assertThat(((Outcome.Refused) outcome).failures().get(0).message(), is("Operation 2 already changes "
                        + security.getUUID() + "; one batch may change an entity only once."));
        assertThat(security.getNote(), is(nullValue()));
    }

    @Test
    public void anUpdateClaimsTheInstrument()
    {
        var security = instrument("Claimed");
        var claimed = new ArrayList<String>();

        applied(INSTRUMENTS.writes(client, ALL_EXISTED).update(
                        json("{\"uuid\":\"" + security.getUUID() + "\",\"note\":\"one\"}"), uuid -> {
                            claimed.add(uuid);
                            return OptionalInt.empty();
                        }));

        assertThat(claimed, is(List.of(security.getUUID())));
    }
}
