package name.abuchen.portfolio.cli;

import java.io.InputStream;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/**
 * Everything a command gets from the outside world: parsed options, the
 * environment, stdin and the clock.
 */
final class CommandContext
{
    private final Map<String, String> options;
    private final Map<String, String> env;
    private final InputStream stdin;
    private final Clock clock;

    CommandContext(Map<String, String> options, Map<String, String> env, InputStream stdin, Clock clock)
    {
        this.options = options;
        this.env = env;
        this.stdin = stdin;
        this.clock = clock;
    }

    Optional<String> option(String name)
    {
        return Optional.ofNullable(options.get(name));
    }

    Optional<String> env(String name)
    {
        return Optional.ofNullable(env.get(name));
    }

    InputStream stdin()
    {
        return stdin;
    }

    Clock clock()
    {
        return clock;
    }

    /** Loads the client file named by <code>--file</code> or PP_FILE. */
    LoadedClient loadClient() throws CliException
    {
        var path = option(OptionSpec.FILE.name()).filter(p -> !p.isBlank())
                        .or(() -> env("PP_FILE").filter(p -> !p.isBlank()))
                        .orElseThrow(() -> CliException.usage("file-required", "Client file required",
                                        "Pass --file <path> or set the PP_FILE environment variable."));

        return ClientLoader.load(path);
    }
}
