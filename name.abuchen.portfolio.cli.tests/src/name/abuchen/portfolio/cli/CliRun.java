package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.file.WriteEnvironment;

/**
 * Runs the CLI in-process the way an agent would: arguments, environment and
 * stdin in; stdout, stderr and the exit code out. Every successful run must
 * keep the {@link ResponseContract}, or the test fails right there.
 */
public final class CliRun
{
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    public final int exitCode;
    public final String stdout;
    public final String stderr;

    private CliRun(int exitCode, String stdout, String stderr)
    {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public static CliRun run(String... args)
    {
        return run(Map.of(), args);
    }

    public static CliRun run(Map<String, String> env, String... args)
    {
        return runWithStdin(env, "", args);
    }

    public static CliRun runWithStdin(Map<String, String> env, String stdin, String... args)
    {
        return execute(System.getProperty("os.name", ""), WriteEnvironment.SYSTEM, env, stdin, args);
    }

    /** Runs as if on the operating system with the given <code>os.name</code>. */
    static CliRun runOn(String osName, Map<String, String> env, String... args)
    {
        return execute(osName, WriteEnvironment.SYSTEM, env, "", args);
    }

    /** Runs with a file system and process probe the test controls. */
    public static CliRun runWith(WriteEnvironment environment, String osName, String... args)
    {
        return execute(osName, environment, Map.of(), "", args);
    }

    private static CliRun execute(String osName, WriteEnvironment environment, Map<String, String> env, String stdin,
                    String... args)
    {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int exitCode;
        try (var stdout = new PrintStream(out, true, StandardCharsets.UTF_8);
                        var stderr = new PrintStream(err, true, StandardCharsets.UTF_8))
        {
            exitCode = PpCli.execute(args, new HashMap<>(env),
                            new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)), stdout, stderr, CLOCK,
                            osName, environment);
        }

        var run = new CliRun(exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));

        var violations = ResponseContract.violations(List.of(args), exitCode, run.stdout);
        if (!violations.isEmpty())
            throw new AssertionError("pp-cli " + String.join(" ", args) + " broke the response contract:\n  "
                            + String.join("\n  ", violations) + "\nstdout: " + run.stdout);

        return run;
    }

    JsonElement json()
    {
        return JsonParser.parseString(stdout);
    }

    public JsonObject data()
    {
        return json().getAsJsonObject().getAsJsonObject("data");
    }

    public JsonObject meta()
    {
        return json().getAsJsonObject().getAsJsonObject("meta");
    }

    /** The items of a list response. */
    public JsonArray items()
    {
        return data().getAsJsonArray("items");
    }

    public JsonArray warnings()
    {
        return json().getAsJsonObject().getAsJsonArray("warnings");
    }

    public List<JsonObject> warnings(String code)
    {
        var matching = new ArrayList<JsonObject>();
        for (var warning : warnings())
        {
            if (warning.getAsJsonObject().get("code").getAsString().equals(code))
                matching.add(warning.getAsJsonObject());
        }
        return matching;
    }

    /** Empty for a failed run, which has no warnings. */
    public List<String> warningCodes()
    {
        var codes = new ArrayList<String>();
        var warnings = warnings();
        if (warnings != null)
            warnings.forEach(w -> codes.add(w.getAsJsonObject().get("code").getAsString()));
        return codes;
    }

    /** The problem+json document of a failed run. */
    public JsonObject problem()
    {
        var json = json().getAsJsonObject();
        if (json.has("data"))
            throw new AssertionError("expected a problem document but got a response: " + stdout);
        return json;
    }

    /** The error code of a failed run. */
    public String code()
    {
        return problem().get("code").getAsString();
    }

    /** The field failures of a <code>validation-failed</code> problem. */
    public JsonArray failures()
    {
        return problem().getAsJsonArray("failures");
    }

    public JsonObject failure(int index)
    {
        return failures().get(index).getAsJsonObject();
    }

    /** Each field failure as <code>operationIndex:field:code</code>. */
    public List<String> failureKeys()
    {
        var keys = new ArrayList<String>();
        for (var element : failures())
        {
            var failure = element.getAsJsonObject();
            keys.add(failure.get("operationIndex").getAsInt() + ":" + failure.get("field").getAsString() + ":"
                            + failure.get("code").getAsString());
        }
        return keys;
    }

    /** The result of the operation at the index of an applied batch. */
    public JsonObject result(int index)
    {
        return data().getAsJsonArray("results").get(index).getAsJsonObject();
    }

    /** The record the first operation of an applied batch created. */
    public JsonObject created()
    {
        return first("created");
    }

    /** The record the first operation of an applied batch updated. */
    public JsonObject updated()
    {
        return first("updated");
    }

    /** The record the first operation of an applied batch deleted. */
    public JsonObject deleted()
    {
        return first("deleted");
    }

    private JsonObject first(String change)
    {
        return result(0).getAsJsonArray(change).get(0).getAsJsonObject();
    }

    /** Asserts that the batch was refused for one field failure first. */
    public void assertRefused(String field, String code)
    {
        assertThat(stdout, exitCode, is(1));
        assertThat(code(), is("validation-failed"));
        assertThat(failure(0).get("field").getAsString(), is(field));
        assertThat(failure(0).get("code").getAsString(), is(code));
    }
}
