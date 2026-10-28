package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.ClientFiles;
import name.abuchen.portfolio.cli.file.FileFormat;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/**
 * The facets of the plain read commands, each at its own interface through
 * {@link FacetHarness}. Today is 2026-10-16.
 */
public class FacetsTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static final Facet<DateRange> RANGE = DateRange.facet("From.", "To.");

    private static final Sorting.Spec<Security> SORT = Sorting.Spec.of(Security::getUUID, "name",
                    Sorting.Field.of("name", MasterData.byName(Security::getName)));

    private static final ReferenceOption<Security> INSTRUMENT = ReferenceOption.instrument("instrument",
                    "An instrument.");

    private static final Facet<Filters.Reference<Security, Security>> BY_INSTRUMENT = Filters.reference(INSTRUMENT,
                    "instrument", "UUID the instrument filter resolved to.", (security, item) -> security == item);

    @Test
    public void clientFileReportsTheFileVersionAndTheDayTheCommandRan() throws Exception
    {
        var file = ClientFiles.xml(folder.getRoot().toPath());

        var result = FacetHarness.run(ClientFile.READ, null, "--file", file.getPath());

        assertThat(result.value().format(), is(FileFormat.XML));
        assertThat(result.value().asOf(), is(LocalDate.parse("2026-10-16")));
        assertThat(result.meta().get("fileVersion").getAsInt(), is(result.value().client().getFileVersionAfterRead()));
        assertThat(result.meta().get("asOf").getAsString(), is("2026-10-16"));
    }

    @Test
    public void clientFileFailsWithoutAFileOrWithAMissingOne()
    {
        assertThat(FacetHarness.run(ClientFile.READ, null).code(), is("file-required"));

        var missing = folder.getRoot().toPath().resolve("missing.xml").toString();
        assertThat(FacetHarness.run(ClientFile.READ, null, "--file", missing).code(), is("file-not-found"));
    }

    @Test
    public void dateRangeDefaultsToTodayAndLeavesAnAbsentFromOutOfMeta()
    {
        var result = FacetHarness.run(RANGE, null);

        assertThat(result.value().from(), is(Optional.empty()));
        assertThat(result.value().to(), is(LocalDate.parse("2026-10-16")));
        assertThat(result.meta().has("from"), is(false));
        assertThat(result.meta().get("to").getAsString(), is("2026-10-16"));
    }

    @Test
    public void dateRangeEchoesBothDaysAndIsInclusive()
    {
        var result = FacetHarness.run(RANGE, null, "--from", "2026-01-02", "--to", "2026-01-03");

        assertThat(result.meta().get("from").getAsString(), is("2026-01-02"));
        assertThat(result.meta().get("to").getAsString(), is("2026-01-03"));
        assertThat(result.value().contains(LocalDate.parse("2026-01-02")), is(true));
        assertThat(result.value().contains(LocalDate.parse("2026-01-03")), is(true));
        assertThat(result.value().contains(LocalDate.parse("2026-01-04")), is(false));
    }

    @Test
    public void aFromAfterToIsAnInvalidRange()
    {
        var result = FacetHarness.run(RANGE, null, "--from", "2026-02-01", "--to", "2026-01-31");

        assertThat(result.code(), is("invalid-date-range"));
        assertThat(result.failure().getExitCode(), is(2));
        assertThat(result.failure().toProblem().get("status"), is(400));
        assertThat(result.failure().toProblem().get("from"), is("2026-02-01"));
        assertThat(result.failure().toProblem().get("to"), is("2026-01-31"));
    }

    @Test
    public void aFromAfterTodayWithoutToIsAnInvalidRangeAgainstTheDefaultedTo()
    {
        var result = FacetHarness.run(RANGE, null, "--from", "2026-10-17");

        assertThat(result.code(), is("invalid-date-range"));
        assertThat(result.failure().toProblem().get("to"), is("2026-10-16"));
    }

    @Test
    public void dateRangeTakesTheDescriptionsOfItsCommand()
    {
        assertThat(RANGE.declaration().options().stream().map(OptionSpec::description).toList(),
                        is(List.of("From.", "To.")));
    }

    @Test
    public void pagingReportsTheDefaultsOrTheGivenPage()
    {
        var defaults = FacetHarness.run(Paging.FACET, null);
        assertThat(defaults.meta().get("limit").getAsInt(), is(200));
        assertThat(defaults.meta().get("offset").getAsInt(), is(0));

        var given = FacetHarness.run(Paging.FACET, null, "--limit", "2", "--offset", "4");
        assertThat(given.value(), is(new Paging(2, 4)));
        assertThat(given.meta().get("limit").getAsInt(), is(2));
        assertThat(given.meta().get("offset").getAsInt(), is(4));
    }

    @Test
    public void pagingRejectsALimitOutOfRange()
    {
        assertThat(FacetHarness.run(Paging.FACET, null, "--limit", "0").code(), is("invalid-option-value"));
        assertThat(FacetHarness.run(Paging.FACET, null, "--limit", "1001").code(), is("invalid-option-value"));
        assertThat(FacetHarness.run(Paging.FACET, null, "--offset", "-1").code(), is("invalid-option-value"));
    }

    @Test
    public void sortingReportsTheDefaultFieldAscendingOrWhatWasAsked()
    {
        var defaults = FacetHarness.run(SORT, null);
        assertThat(defaults.meta().get("sort").getAsString(), is("name"));
        assertThat(defaults.meta().get("order").getAsString(), is("asc"));

        var descending = FacetHarness.run(SORT, null, "--order", "desc");
        assertThat(descending.value().descending(), is(true));
        assertThat(descending.meta().get("order").getAsString(), is("desc"));
    }

    @Test
    public void sortingRejectsAFieldItDoesNotDeclare()
    {
        assertThat(FacetHarness.run(SORT, null, "--sort", "isin").code(), is("invalid-option-value"));
        assertThat(FacetHarness.run(SORT, null, "--order", "up").code(), is("invalid-option-value"));
    }

    @Test
    public void anAbsentReferenceFilterMatchesEverythingAndIsNotInMeta()
    {
        var client = new Client();
        var acme = instrument(client, "ACME");

        var result = FacetHarness.run(BY_INSTRUMENT, client);

        assertThat(result.value().entity(), is(Optional.empty()));
        assertThat(result.value().test(acme), is(true));
        assertThat(result.meta().has("instrument"), is(false));
    }

    @Test
    public void aGivenReferenceFilterMatchesItsEntityAndEchoesTheUuid()
    {
        var client = new Client();
        var acme = instrument(client, "ACME");
        var other = instrument(client, "Other");

        var result = FacetHarness.run(BY_INSTRUMENT, client, "--instrument", "acme");

        assertThat(result.value().entity().get(), is(sameInstance(acme)));
        assertThat(result.value().test(acme), is(true));
        assertThat(result.value().test(other), is(false));
        assertThat(result.meta().get("instrument").getAsString(), is(acme.getUUID()));
    }

    @Test
    public void aReferenceFilterFailsWhenNothingOrSeveralMatch()
    {
        var client = new Client();
        instrument(client, "Acme");
        instrument(client, "ACME");

        assertThat(FacetHarness.run(BY_INSTRUMENT, client, "--instrument", "Other").code(),
                        is("reference-not-found"));
        assertThat(FacetHarness.run(BY_INSTRUMENT, client, "--instrument", "acme").code(),
                        is("reference-ambiguous"));
    }

    @Test
    public void aLoneOptionResolvesToItsParsedValue()
    {
        var client = new Client();
        var acme = instrument(client, "ACME");

        Facet<Boolean> flag = Facet.option(OptionSpec.flag("include-zero", "A switch."));
        assertThat(FacetHarness.run(flag, null).value(), is(false));
        assertThat(FacetHarness.run(flag, null, "--include-zero").value(), is(true));

        Facet<Optional<LocalDate>> date = Facet.option(OptionSpec.date("date", "A day."));
        assertThat(FacetHarness.run(date, null, "--date", "2026-01-31").value(),
                        is(Optional.of(LocalDate.parse("2026-01-31"))));
        assertThat(FacetHarness.run(date, null, "--date", "yesterday").code(), is("invalid-option-value"));

        var reference = Facet.option(INSTRUMENT);
        assertThat(FacetHarness.run(reference, client, "--instrument", acme.getUUID()).value().get(),
                        is(sameInstance(acme)));
        assertThat(FacetHarness.run(reference, client, "--instrument", "nothing").code(), is("reference-not-found"));
    }

    private static Security instrument(Client client, String name)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        return security;
    }
}
