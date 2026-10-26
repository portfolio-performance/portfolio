package name.abuchen.portfolio.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import name.abuchen.portfolio.cli.file.ClientLoader;
import name.abuchen.portfolio.cli.file.LoadedClient;
import name.abuchen.portfolio.cli.file.WriteEnvironment;
import name.abuchen.portfolio.model.Client;

/**
 * Everything a command gets from the outside world: parsed options, the
 * environment, stdin, stderr, the clock and the name of the operating system.
 */
public final class CommandContext
{
    public static final String PASSWORD_VARIABLE = "PP_PASSWORD";

    private final Map<String, String> options;
    private final Map<String, String> env;
    private final InputStream stdin;
    private final PrintStream stderr;
    private final Clock clock;
    private final String osName;
    private final WriteEnvironment writeEnvironment;

    public CommandContext(Map<String, String> options, Map<String, String> env, InputStream stdin, PrintStream stderr,
                    Clock clock, String osName, WriteEnvironment writeEnvironment)
    {
        this.options = options;
        this.env = env;
        this.stdin = stdin;
        this.stderr = stderr;
        this.clock = clock;
        this.osName = osName;
        this.writeEnvironment = writeEnvironment;
    }

    /** Whether the option was given, whatever its type; its value is not checked. */
    public boolean given(OptionSpec option)
    {
        return options.containsKey(option.name());
    }

    /**
     * The value of a date option, or empty if it is absent.
     *
     * @throws CliException
     *             a usage error if the value is not a valid ISO date
     */
    Optional<LocalDate> date(OptionSpec option) throws CliException
    {
        return option.parseDate(raw(option, OptionSpec.IsoDate.class));
    }

    /**
     * The value of an integer option: the default when it is absent.
     *
     * @throws CliException
     *             a usage error if the value is not an integer in the range
     */
    public long integer(OptionSpec option) throws CliException
    {
        return option.parseInteger(raw(option, OptionSpec.IntegerDomain.class));
    }

    /**
     * The value of a choice option: the default when it is absent.
     *
     * @throws CliException
     *             a usage error if the value is not one of the choices
     */
    String choice(OptionSpec option) throws CliException
    {
        return option.parseChoice(raw(option, OptionSpec.Choice.class));
    }

    /** The value of a free-form option as given, or empty if it is absent. */
    public Optional<String> text(OptionSpec option)
    {
        return raw(option, OptionSpec.Text.class);
    }

    /**
     * The entity a reference option names, or empty if it is absent.
     *
     * @throws CliException
     *             a usage error if the value is blank, or a failure if no
     *             entity or several match it
     */
    public <T> Optional<T> reference(ReferenceOption<T> option, Client client) throws CliException
    {
        var value = raw(option.spec(), OptionSpec.Reference.class);
        if (value.isEmpty())
            return Optional.empty();
        return Optional.of(References.resolve(option.kind(), client, option.name(), value.get()));
    }

    /** The raw value of an option declared as <code>type</code>. */
    private Optional<String> raw(OptionSpec option, Class<? extends OptionSpec.Type> type)
    {
        if (!type.isInstance(option.type()))
            throw new IllegalArgumentException(
                            "Option '--" + option.name() + "' is not of type " + type.getSimpleName());
        return Optional.ofNullable(options.get(option.name()));
    }

    /** Whether the switch was given. */
    public boolean flag(OptionSpec option)
    {
        return raw(option, OptionSpec.Flag.class).isPresent();
    }

    public Optional<String> env(String name)
    {
        return Optional.ofNullable(env.get(name));
    }

    public InputStream stdin()
    {
        return stdin;
    }

    public Clock clock()
    {
        return clock;
    }

    /** The <code>os.name</code> of the running system. */
    public String osName()
    {
        return osName;
    }

    public WriteEnvironment writeEnvironment()
    {
        return writeEnvironment;
    }

    /** Loads the client file named by <code>--file</code> or PP_FILE. */
    public LoadedClient loadClient() throws CliException
    {
        return ClientLoader.load(clientPath(), this::password);
    }

    /**
     * Checks the client file named by <code>--file</code> or PP_FILE without
     * loading it; the caller closes it.
     */
    public ClientLoader.OpenedFile openClient() throws CliException
    {
        return ClientLoader.open(clientPath(), this::password);
    }

    private String clientPath() throws CliException
    {
        return text(OptionSpec.FILE).filter(p -> !p.isBlank())
                        .or(() -> env("PP_FILE").filter(p -> !p.isBlank()))
                        .orElseThrow(() -> CliException.usage("file-required", "Client file required",
                                        "Pass --file <path> or set the PP_FILE environment variable."));
    }

    /**
     * The password for an encrypted file: PP_PASSWORD, or else one line from
     * stdin. The prompt goes to stderr so stdout stays a single JSON document.
     *
     * @return the password, or null if none was given
     */
    private char[] password()
    {
        var fromEnv = env(PASSWORD_VARIABLE).filter(p -> !p.isEmpty());
        if (fromEnv.isPresent())
            return fromEnv.get().toCharArray();

        stderr.print("Password: ");
        stderr.flush();
        return readLine();
    }

    private char[] readLine()
    {
        try
        {
            var bytes = new ByteArrayOutputStream();
            int b;
            while ((b = stdin.read()) >= 0 && b != '\n')
                bytes.write(b);

            var line = bytes.toString(StandardCharsets.UTF_8);
            if (line.endsWith("\r"))
                line = line.substring(0, line.length() - 1);
            return line.toCharArray();
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
