package name.abuchen.portfolio.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Runs the CLI in-process the way an agent would: arguments, environment and
 * stdin in; stdout, stderr and the exit code out.
 */
final class CliRun
{
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    final int exitCode;
    final String stdout;
    final String stderr;

    private CliRun(int exitCode, String stdout, String stderr)
    {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    static CliRun run(String... args)
    {
        return run(Map.of(), args);
    }

    static CliRun run(Map<String, String> env, String... args)
    {
        return runWithStdin(env, "", args);
    }

    static CliRun runWithStdin(Map<String, String> env, String stdin, String... args)
    {
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int exitCode;
        try (var stdout = new PrintStream(out, true, StandardCharsets.UTF_8);
                        var stderr = new PrintStream(err, true, StandardCharsets.UTF_8))
        {
            exitCode = PpCli.execute(args, new HashMap<>(env),
                            new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)), stdout, stderr, CLOCK);
        }

        return new CliRun(exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    JsonElement json()
    {
        return JsonParser.parseString(stdout);
    }
}
