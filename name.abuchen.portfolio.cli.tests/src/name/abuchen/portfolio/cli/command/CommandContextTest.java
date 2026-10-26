package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/**
 * The typed option accessors of the command context, in process: what each
 * returns for an absent, a valid and an invalid value, and that asking for an
 * option as the wrong type is a programming error.
 */
public class CommandContextTest
{
    private static final OptionSpec DATE = OptionSpec.date("date", "A day.");

    private static final OptionSpec LIMIT = OptionSpec.integer("limit", "n", "A count.", 10, 1, 100L);

    private static final OptionSpec ORDER = OptionSpec.choice("order", "order", "An order.", List.of("asc", "desc"),
                    "asc");

    private static final OptionSpec NAME = new OptionSpec("name", "text", "A text.");

    private static final ReferenceOption<Security> INSTRUMENT = ReferenceOption.instrument("instrument",
                    "An instrument.");

    @Test
    public void dateIsEmptyWhenAbsentAndParsedWhenGiven() throws CliException
    {
        assertThat(context(Map.of()).date(DATE), is(Optional.empty()));
        assertThat(context(Map.of("date", "2026-02-28")).date(DATE), is(Optional.of(LocalDate.of(2026, 2, 28))));
    }

    @Test
    public void invalidDateIsAUsageError()
    {
        var e = assertThrows(CliException.class, () -> context(Map.of("date", "2026-02-30")).date(DATE));
        assertThat(e.getCode(), is("invalid-option-value"));
    }

    @Test
    public void integerIsTheDefaultWhenAbsentAndMustBeInRange() throws CliException
    {
        assertThat(context(Map.of()).integer(LIMIT), is(10L));
        assertThat(context(Map.of("limit", "3")).integer(LIMIT), is(3L));

        var e = assertThrows(CliException.class, () -> context(Map.of("limit", "0")).integer(LIMIT));
        assertThat(e.getCode(), is("invalid-option-value"));
    }

    @Test
    public void choiceIsTheDefaultWhenAbsentAndMustBeListed() throws CliException
    {
        assertThat(context(Map.of()).choice(ORDER), is("asc"));
        assertThat(context(Map.of("order", "desc")).choice(ORDER), is("desc"));

        var e = assertThrows(CliException.class, () -> context(Map.of("order", "up")).choice(ORDER));
        assertThat(e.getCode(), is("invalid-option-value"));
    }

    @Test
    public void textIsEmptyWhenAbsentAndAsGivenOtherwise()
    {
        assertThat(context(Map.of()).text(NAME), is(Optional.empty()));
        assertThat(context(Map.of("name", " ACME ")).text(NAME), is(Optional.of(" ACME ")));
    }

    @Test
    public void referenceIsEmptyWhenAbsentAndResolvedWhenGiven() throws CliException
    {
        var client = new Client();
        var acme = instrument(client, "ACME Corp");
        instrument(client, "Other");

        assertThat(context(Map.of()).reference(INSTRUMENT, client), is(Optional.empty()));
        assertThat(context(Map.of("instrument", "acme corp")).reference(INSTRUMENT, client).get(),
                        is(sameInstance(acme)));
    }

    @Test
    public void referenceThatMatchesNoneOrSeveralFails()
    {
        var client = new Client();
        instrument(client, "Acme");
        instrument(client, "ACME");

        var notFound = assertThrows(CliException.class,
                        () -> context(Map.of("instrument", "Other")).reference(INSTRUMENT, client));
        assertThat(notFound.getCode(), is("reference-not-found"));

        var ambiguous = assertThrows(CliException.class,
                        () -> context(Map.of("instrument", "acme")).reference(INSTRUMENT, client));
        assertThat(ambiguous.getCode(), is("reference-ambiguous"));
    }

    @Test
    public void givenSaysWhetherAnOptionIsPresentWithoutReadingItsValue()
    {
        assertThat(context(Map.of()).given(DATE), is(false));
        assertThat(context(Map.of("date", "not a date")).given(DATE), is(true));
    }

    @Test
    public void readingAnOptionAsAnotherTypeIsAProgrammingError()
    {
        var context = context(Map.of("name", "2026-02-28", "date", "2026-02-28"));

        assertThrows(IllegalArgumentException.class, () -> context.date(NAME));
        assertThrows(IllegalArgumentException.class, () -> context.integer(NAME));
        assertThrows(IllegalArgumentException.class, () -> context.choice(NAME));
        assertThrows(IllegalArgumentException.class, () -> context.text(DATE));
        assertThrows(IllegalArgumentException.class, () -> context.text(INSTRUMENT.spec()));
        assertThrows(IllegalArgumentException.class, () -> context.flag(DATE));
        assertThrows(IllegalArgumentException.class,
                        () -> context.reference(new ReferenceOption<>(NAME, References.INSTRUMENT), new Client()));
    }

    private static Security instrument(Client client, String name)
    {
        var security = new SecurityBuilder().addTo(client);
        security.setName(name);
        return security;
    }

    private static CommandContext context(Map<String, String> options)
    {
        return new CommandContext(options, Map.of(), new ByteArrayInputStream(new byte[0]),
                        new PrintStream(new ByteArrayOutputStream()), CliRun.CLOCK, "Linux", null);
    }
}
