package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.file.FileFormat;
import name.abuchen.portfolio.cli.file.LoadedClient;
import name.abuchen.portfolio.model.Client;

/**
 * How {@link CommandSpec#of} composes facets: what it derives, what it rejects
 * at class-init, the order in which facets resolve and report, and the
 * programming errors it turns into {@link IllegalStateException}s.
 */
public class CommandSpecTest
{
    private static final Schema DATA = Schema.object("Data.");

    private static final OptionSpec NAME = new OptionSpec("name", "text", "A text.");

    private static final Facet<Void> DECLARES_A = Facet.of(Declaration.empty().meta("a", Schema.integer("A.")),
                    run -> null, (value, out) -> out.meta("a", 1));

    private static final WarningSpec ODD = new WarningSpec("odd", "Something odd.");

    @Test
    public void twoFacetsDeclaringTheSameOptionAreRejected()
    {
        var first = Facet.option(NAME);
        var second = Facet.option(new OptionSpec("name", "other", "Another text."));

        assertThrows(IllegalStateException.class, () -> CommandSpec.of("probe", "A probe.", DATA, run -> null,
                        first, second));
    }

    @Test
    public void twoFacetsDeclaringTheSameMetaMemberAreRejected()
    {
        var other = Facet.of(Declaration.empty().optionalMeta("a", Schema.string("A.")), run -> null);

        assertThrows(IllegalStateException.class,
                        () -> CommandSpec.of("probe", "A probe.", DATA, run -> null, DECLARES_A, other));
    }

    @Test
    public void aCommandHasAtMostOneClientFileFacet()
    {
        var second = Facet.of(Declaration.empty().in(Declaration.Slot.ENVELOPE), run -> null);

        assertThrows(IllegalStateException.class,
                        () -> CommandSpec.of("probe", "A probe.", DATA, run -> null, ClientFile.READ, second));
    }

    @Test
    public void optionsFollowFacetOrderWithTheSharedOnesLast()
    {
        var spec = CommandSpec.of("probe", "A probe.", DATA, run -> null, ClientFile.READ, Facet.option(NAME),
                        Paging.FACET);

        assertThat(spec.options().stream().map(OptionSpec::name).toList(),
                        is(List.of("name", "limit", "offset", "file", "format", "verbose")));
    }

    @Test
    public void metaIsTheEnvelopeThenPagingThenSortingThenTheOtherFacets() throws CliException
    {
        var sort = Sorting.Spec.of((String s) -> s, "name", Sorting.Field.of("name", String::compareTo));
        var spec = CommandSpec.of("probe", "A probe.", DATA, run -> null, DECLARES_A, sort, Paging.FACET,
                        ClientFile.READ);

        assertThat(spec.metaSchema().memberNames(true),
                        is(List.of("fileVersion", "asOf", "limit", "offset", "sort", "order", "a")));

        var response = run(spec, Map.of());
        assertThat(List.copyOf(response.meta().keySet()),
                        is(List.of("fileVersion", "asOf", "limit", "offset", "sort", "order", "a")));
    }

    @Test
    public void errorCodesAreThoseOfEveryCommandTheOptionTypesAndTheFacets()
    {
        var withRange = CommandSpec.of("probe", "A probe.", DATA, run -> null, DateRange.facet("From.", "To."));
        var codes = withRange.errorCodes();
        assertThat(codes, hasItems(ErrorCode.INVALID_DATE_RANGE, ErrorCode.UNKNOWN_OPTION, ErrorCode.INTERNAL_ERROR));
        assertThat(codes, not(hasItem(ErrorCode.FILE_REQUIRED)));
        assertThat(codes, not(hasItem(ErrorCode.REFERENCE_NOT_FOUND)));

        var withReference = CommandSpec.of("probe", "A probe.", DATA, run -> null, ClientFile.READ,
                        Facet.option(ReferenceOption.instrument("instrument", "An instrument.").asRequired()));
        assertThat(withReference.errorCodes(), hasItems(ErrorCode.FILE_REQUIRED, ErrorCode.REFERENCE_NOT_FOUND,
                        ErrorCode.REFERENCE_AMBIGUOUS, ErrorCode.OPTION_REQUIRED));
        assertThat(withReference.errorCodes(), not(hasItem(ErrorCode.INVALID_DATE_RANGE)));
    }

    @Test
    public void facetsWithoutTheClientResolveFirstSoUsageErrorsComeBeforeFileErrors()
    {
        var resolved = new ArrayList<String>();
        var needsClient = Facet.of(Declaration.empty().needingClient(), run -> {
            resolved.add("client");
            return null;
        });
        var plain = Facet.of(Declaration.empty(), run -> {
            resolved.add("plain");
            return null;
        });
        var spec = CommandSpec.of("probe", "A probe.", DATA, run -> null, ClientFile.READ, needsClient, plain,
                        DateRange.facet("From.", "To."));

        // no file at all: the range is checked first
        var e = assertThrows(CliException.class,
                        () -> spec.handler().execute(context(Map.of("from", "2026-02-01", "to", "2026-01-31"))));
        assertThat(e.getCode(), is("invalid-date-range"));
        assertThat(resolved, is(List.of("plain")));
    }

    @Test
    public void theHandlerReturnsDataAndItsWarningsComeBeforeTheFacetsOnes() throws CliException
    {
        var facetWarning = new WarningSpec("late", "Reported by a facet.");
        var reporting = Facet.of(Declaration.empty().warnings(facetWarning), run -> null,
                        (value, out) -> out.warn(new Warning("late", "Late.")));
        var declaring = Facet.of(Declaration.empty().warnings(ODD), run -> null);
        var spec = CommandSpec.of("probe", "A probe.", DATA, run -> {
            run.warn(new Warning("odd", "Odd."));
            return Map.of("x", 1);
        }, reporting, declaring);

        var response = run(spec, Map.of());

        assertThat(response.data(), is(Map.of("x", 1)));
        assertThat(response.warnings().stream().map(Warning::code).toList(), is(List.of("odd", "late")));
        assertThat(spec.warnings(), is(List.of(facetWarning, ODD)));
    }

    @Test
    public void aWarningNoFacetDeclaresIsAProgrammingError()
    {
        var spec = CommandSpec.of("probe", "A probe.", DATA, run -> {
            run.warn(new Warning("odd", "Odd."));
            return null;
        });

        assertThrows(IllegalStateException.class, () -> run(spec, Map.of()));
    }

    @Test
    public void aFacetEmittingAnUndeclaredMemberOrWarningIsAProgrammingError()
    {
        var member = Facet.of(Declaration.empty(), run -> null, (value, out) -> out.meta("b", 2));
        assertThrows(IllegalStateException.class,
                        () -> run(CommandSpec.of("probe", "A probe.", DATA, run -> null, member), Map.of()));

        var warning = Facet.of(Declaration.empty(), run -> null, (value, out) -> out.warn(new Warning("odd", "Odd.")));
        assertThrows(IllegalStateException.class,
                        () -> run(CommandSpec.of("probe", "A probe.", DATA, run -> null, warning), Map.of()));
    }

    @Test
    public void aFacetLeavingOutARequiredMemberIsAProgrammingError()
    {
        var silent = Facet.of(Declaration.empty().meta("a", Schema.integer("A.")), run -> null);

        assertThrows(IllegalStateException.class,
                        () -> run(CommandSpec.of("probe", "A probe.", DATA, run -> null, silent), Map.of()));
    }

    @Test
    public void readingAnUndeclaredOptionIsAProgrammingError()
    {
        var name = Facet.option(NAME);
        var sneaky = Facet.of(Declaration.empty(), run -> run.text(NAME));
        assertThrows(IllegalStateException.class,
                        () -> run(CommandSpec.of("probe", "A probe.", DATA, run -> null, name, sneaky),
                                        Map.of("name", "x")));

        var fromHandler = CommandSpec.of("probe", "A probe.", DATA, run -> run.text(NAME), name);
        assertThrows(IllegalStateException.class, () -> run(fromHandler, Map.of("name", "x")));
    }

    @Test
    public void askingForTheClientFromAFacetThatDoesNotNeedItIsAProgrammingError()
    {
        var eager = Facet.of(Declaration.empty(), CommandContext::client);

        assertThrows(IllegalStateException.class,
                        () -> run(CommandSpec.of("probe", "A probe.", DATA, run -> null, ClientFile.READ, eager),
                                        Map.of()));
    }

    @Test
    public void aFacetOfAnotherCommandHasNoValue()
    {
        var spec = CommandSpec.of("probe", "A probe.", DATA, run -> run.get(Paging.FACET), DECLARES_A);

        assertThrows(IllegalStateException.class, () -> run(spec, Map.of()));
    }

    private static Response run(CommandSpec spec, Map<String, String> options) throws CliException
    {
        return spec.handler().execute(context(options).withClient(new LoadedClient(new Client(), FileFormat.XML)));
    }

    private static CommandContext context(Map<String, String> options)
    {
        return new CommandContext(options, Map.of(), new ByteArrayInputStream(new byte[0]),
                        new PrintStream(new ByteArrayOutputStream()), CliRun.CLOCK, "Linux", null);
    }
}
