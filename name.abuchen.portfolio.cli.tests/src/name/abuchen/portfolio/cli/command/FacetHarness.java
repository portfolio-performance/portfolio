package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.SchemaValidator;
import name.abuchen.portfolio.cli.file.FileFormat;
import name.abuchen.portfolio.cli.file.LoadedClient;
import name.abuchen.portfolio.model.Client;

/**
 * Runs one {@link Facet} the way a command does, without a file or the rest of
 * a command: resolve, then report. Every run checks the facet against its own
 * declaration: the meta fragment conforms to the declared members, every
 * warning has a declared code, and a failure's code is one the facet can give.
 * Today is 2026-10-16 (see {@link CliRun#CLOCK}).
 */
public final class FacetHarness
{
    /** What a facet resolved to and reported, or the failure it gave. */
    public record Result<T>(T value, JsonObject meta, List<Warning> warnings, CliException failure)
    {
        public String code()
        {
            if (failure == null)
                throw new AssertionError("expected a failure, but the facet resolved to " + value);
            return failure.getCode();
        }

        public T value()
        {
            if (failure != null)
                throw new AssertionError("expected a value, but the facet failed with " + failure.getCode(), failure);
            return value;
        }

        public List<String> warningCodes()
        {
            return warnings.stream().map(Warning::code).toList();
        }
    }

    private FacetHarness()
    {
    }

    /**
     * @param args
     *            options as on the command line, e.g.
     *            <code>"--from", "2026-01-01"</code>; a switch is given alone
     * @param client
     *            the client a facet that needs one gets; null to read the
     *            file of <code>--file</code>
     */
    public static <T> Result<T> run(Facet<T> facet, List<String> args, Client client)
    {
        var resolved = new AtomicReference<T>();
        var probe = CommandSpec.of("probe", "A probe.", Schema.object("Data."), run -> {
            resolved.set(run.get(facet));
            return Map.of();
        }, facet);

        var context = new CommandContext(options(facet, args), Map.of(), new ByteArrayInputStream(new byte[0]),
                        new PrintStream(new ByteArrayOutputStream()), CliRun.CLOCK, "Linux", null);
        if (client != null)
            context.withClient(new LoadedClient(client, FileFormat.XML));

        Response response;
        try
        {
            response = probe.handler().execute(context);
        }
        catch (CliException e)
        {
            var declared = probe.errorCodes().stream().map(ErrorCode::code).toList();
            assertThat("the facet failed with an undeclared code", declared, hasItem(e.getCode()));
            return new Result<>(null, null, List.of(), e);
        }

        var meta = new Gson().toJsonTree(response.meta()).getAsJsonObject();
        var schema = new Gson().toJsonTree(facet.declaration().metaSchema().toJson()).getAsJsonObject();
        assertThat("meta " + meta, SchemaValidator.validate(meta, schema), is(empty()));

        var warningCodes = facet.declaration().warnings().stream().map(WarningSpec::code).toList();
        for (var warning : response.warnings())
            assertThat("the facet emitted an undeclared warning", warningCodes, hasItem(warning.code()));

        return new Result<>(resolved.get(), meta, List.copyOf(response.warnings()), null);
    }

    public static <T> Result<T> run(Facet<T> facet, Client client, String... args)
    {
        return run(facet, List.of(args), client);
    }

    /** Parses the arguments as the argument parser does, for the facet's options only. */
    private static Map<String, String> options(Facet<?> facet, List<String> args)
    {
        var options = new HashMap<String, String>();
        var rest = new ArrayList<>(args);
        while (!rest.isEmpty())
        {
            var arg = rest.remove(0);
            if (!arg.startsWith("--"))
                throw new IllegalArgumentException("not an option: " + arg);
            var name = arg.substring(2);
            var option = facet.declaration().options().stream().filter(o -> o.name().equals(name)).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("the facet has no option " + arg));
            options.put(name, option.isFlag() ? "true" : rest.remove(0));
        }
        return options;
    }
}
