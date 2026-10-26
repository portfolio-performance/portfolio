package name.abuchen.portfolio.cli;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import name.abuchen.portfolio.cli.file.WriteEnvironment;

/**
 * The AI-agent command line. {@link #execute} is the single in-process entry
 * point; {@link #main} only adapts the process to it.
 */
public final class PpCli
{
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    private PpCli()
    {
    }

    public static void main(String[] args)
    {
        var stdout = new PrintStream(System.out, false, StandardCharsets.UTF_8);
        var stderr = new PrintStream(System.err, true, StandardCharsets.UTF_8);

        int exitCode = execute(args, System.getenv(), System.in, stdout, stderr, Clock.systemDefaultZone(),
                        System.getProperty("os.name", ""));

        stdout.flush();
        System.exit(exitCode);
    }

    /**
     * Runs one command. Stdout receives exactly one JSON document, either the
     * response envelope or a problem+json error, unless <code>--format
     * table</code> asks for a table of a successful list, or <code>--help</code>
     * (or no command at all) asks for plain-text help, which needs no client
     * file; everything else, including core's own log output, goes to stderr.
     * Core's INFO and OK status lines are left out unless <code>--verbose</code>
     * is given.
     *
     * @return the process exit code: 0 on success, 2 for usage errors, 1 for
     *         other failures
     */
    public static int execute(String[] args, Map<String, String> env, InputStream stdin, PrintStream stdout,
                    PrintStream stderr, Clock clock, String osName)
    {
        return execute(args, env, stdin, stdout, stderr, clock, osName, WriteEnvironment.SYSTEM);
    }

    /** As above, with a file system and process probe other than the real ones; for tests. */
    static int execute(String[] args, Map<String, String> env, InputStream stdin, PrintStream stdout,
                    PrintStream stderr, Clock clock, String osName, WriteEnvironment writeEnvironment)
    {
        var originalOut = System.out; // NOSONAR
        var originalErr = System.err; // NOSONAR
        PrintStream quietErr = null;

        // core logs to the console; keep it away from the JSON on stdout
        System.setOut(stderr);
        System.setErr(stderr);
        try
        {
            var help = HelpText.render(Arrays.asList(args));
            if (help.isPresent())
            {
                stdout.print(help.get());
                stdout.flush();
                return 0;
            }

            var parsed = ArgumentParser.parse(Arrays.asList(args));
            if (!parsed.options().containsKey(OptionSpec.VERBOSE.name()))
            {
                quietErr = new PrintStream(new QuietStatusFilter(stderr), true, StandardCharsets.UTF_8);
                System.setErr(quietErr);
            }

            var format = OutputFormat.parse(parsed.command(), parsed.options());
            var context = new CommandContext(parsed.options(), env, stdin, stderr, clock, osName, writeEnvironment);
            var response = parsed.command().handler().execute(context);

            if (format == OutputFormat.TABLE)
                TableFormat.print(stdout, stderr, response, parsed.command().tableColumns());
            else
                print(stdout, response.toEnvelope());
            return 0;
        }
        catch (CliException e)
        {
            print(stdout, e.toProblem());
            return e.getExitCode();
        }
        catch (RuntimeException | LinkageError e)
        {
            e.printStackTrace(stderr);
            print(stdout, CliException.failure("internal-error", "Internal error", String.valueOf(e), 500)
                            .toProblem());
            return CliException.EXIT_FAILURE;
        }
        finally
        {
            if (quietErr != null)
                quietErr.close();
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static void print(PrintStream stdout, Map<String, Object> document)
    {
        stdout.print(GSON.toJson(document));
        stdout.print('\n');
        stdout.flush();
    }
}
