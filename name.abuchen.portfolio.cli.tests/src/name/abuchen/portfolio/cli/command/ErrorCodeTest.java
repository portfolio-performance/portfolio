package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import name.abuchen.portfolio.cli.performance.PerformanceCommand;

/**
 * Which error codes a command's help lists, in process: the codes that follow
 * from its declared options apply to any command that declares such an
 * option, including one no hand-written list names.
 */
public class ErrorCodeTest
{
    private static final OptionSpec TEXT = new OptionSpec("name", "text", "A text.");

    @Test
    public void aProblemDocumentCarriesThePublishedCode()
    {
        var problem = CliException.usage(ErrorCode.INVALID_DATE_RANGE, "Invalid date range", "Detail.").toProblem();

        assertThat(problem.get("code"), is("invalid-date-range"));
        assertThat(problem.get("type"), is("urn:pp-cli:error:invalid-date-range"));
    }

    @Test
    public void aCommandWithAReferenceOptionCanFailToResolveIt()
    {
        var withReference = probe(ReferenceOption.instrument("instrument", "An instrument.").spec());

        assertThat(codes(withReference), hasItems("reference-not-found", "reference-ambiguous"));
        assertThat(codes(probe(TEXT)), not(hasItem("reference-not-found")));
        assertThat(codes(probe(TEXT)), not(hasItem("reference-ambiguous")));
    }

    @Test
    public void aCommandWithARequiredOptionCanFailWithoutIt()
    {
        assertThat(codes(probe(TEXT.asRequired())), hasItem("option-required"));
        assertThat(codes(probe(TEXT)), not(hasItem("option-required")));
    }

    @Test
    public void performanceCanFailWithOptionRequiredThoughNoneOfItsOptionsIsRequired()
    {
        assertThat(PerformanceCommand.SPEC.options().stream().anyMatch(OptionSpec::required),
                        is(false));
        assertThat(PerformanceCommand.SPEC.errorCodes(), hasItem(ErrorCode.OPTION_REQUIRED));
    }

    /** A command no hand-written list can name. */
    private static CommandSpec probe(OptionSpec option)
    {
        return new CommandSpec("probe", "A probe.", true, List.of(option), Schema.object("Data."),
                        Schema.object("Meta."), context -> null);
    }

    private static List<String> codes(CommandSpec command)
    {
        return Arrays.stream(ErrorCode.values()).filter(e -> e.appliesTo(command)).map(ErrorCode::code).toList();
    }
}
