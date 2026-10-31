package name.abuchen.portfolio.cli;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;

/**
 * A client file in a folder of its own, and the CLI run against it: a command
 * with <code>--file</code>, or <code>apply</code> with a batch of operations,
 * always as if on Linux so that results don't depend on the machine running
 * the tests. Batches are written to a separate folder, so the client folder
 * holds only the file and what the CLI writes next to it (backups, lock).
 */
public final class CliFixture
{
    /** The simplest successful write. */
    public static final String ADD_ACME = "{\"operation\":\"instrument.add\",\"name\":\"ACME\"}";

    private static final String OS_NAME = "Linux";

    private final TemporaryFolder folder;
    private File file;

    /**
     * Can be created as a field next to its {@link TemporaryFolder} rule: the
     * folders are only made on first use, once the rule has run.
     */
    public CliFixture(TemporaryFolder folder)
    {
        this.folder = folder;
    }

    /** Writes the client as plain XML and makes it the file to run against. */
    public CliFixture save(Client client) throws IOException
    {
        file = ClientFiles.xml(clientFolder(), client);
        return this;
    }

    /** Makes a file written elsewhere the file to run against. */
    public CliFixture use(File file)
    {
        this.file = file;
        return this;
    }

    public File file()
    {
        return file;
    }

    public Path clientFolder()
    {
        return subfolder("client");
    }

    public Path inputFolder()
    {
        return subfolder("input");
    }

    private Path subfolder(String name)
    {
        try
        {
            return Files.createDirectories(folder.getRoot().toPath().resolve(name));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public CliRun run(String command, String... options)
    {
        return run(Map.of(), command, options);
    }

    public CliRun run(Map<String, String> env, String command, String... options)
    {
        var args = new ArrayList<>(List.of(command, "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.runOn(OS_NAME, env, args.toArray(String[]::new));
    }

    public CliRun apply(String... operations) throws IOException
    {
        return applyWith(List.of(), operations);
    }

    /** Applies the operations with further options, such as <code>--dry-run</code>. */
    public CliRun applyWith(List<String> options, String... operations) throws IOException
    {
        return applyWith(Map.of(), options, operations);
    }

    /** Applies the operations with an environment, such as <code>PP_PASSWORD</code>, and further options. */
    public CliRun applyWith(Map<String, String> env, List<String> options, String... operations) throws IOException
    {
        var all = new ArrayList<>(List.of("--input", batch(operations).toString()));
        all.addAll(options);
        return run(env, "apply", all.toArray(String[]::new));
    }

    /** Writes the operations as a batch file for <code>apply --input</code>. */
    public Path batch(String... operations) throws IOException
    {
        return input("{\"operations\":[" + String.join(",", operations) + "]}");
    }

    /** Writes any text, well-formed or not, as an input file. */
    public Path input(String text) throws IOException
    {
        var input = Files.createTempFile(inputFolder(), "batch", ".json");
        Files.writeString(input, text, StandardCharsets.UTF_8);
        return input;
    }

    /** Reads the file back as the desktop app would. */
    public Client reload() throws IOException
    {
        return ClientFactory.load(file, null, new NullProgressMonitor());
    }
}
