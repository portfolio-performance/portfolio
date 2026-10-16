package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonObject;

/**
 * Every declared command and operation must show up in <code>describe</code>,
 * and every real response must conform to the schema that <code>describe</code>
 * publishes for it. A later ticket that adds a command gets a failure here
 * until it adds sample arguments and a correct schema.
 */
public class DescribeConsistencyTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Arguments that make the command succeed against a sample file. */
    private static List<String> sampleArguments(String command, Path sampleFile)
    {
        return switch (command)
        {
            case "info" -> List.of("info", "--file", sampleFile.toString());
            case "describe" -> List.of("describe");
            default -> throw new AssertionError("Add sample arguments for command '" + command
                            + "' to DescribeConsistencyTest.sampleArguments");
        };
    }

    private JsonObject describe()
    {
        var run = CliRun.run("describe");
        assertThat(run.exitCode, is(0));
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    @Test
    public void describeListsExactlyTheDeclaredCommandsAndOperations()
    {
        var data = describe();

        var described = new TreeSet<String>();
        data.getAsJsonArray("commands").forEach(e -> described.add(e.getAsJsonObject().get("name").getAsString()));
        var declared = new TreeSet<String>();
        Commands.ALL.forEach(c -> declared.add(c.name()));
        assertThat(described, is(declared));

        var describedOperations = new TreeSet<String>();
        data.getAsJsonArray("operations")
                        .forEach(e -> describedOperations.add(e.getAsJsonObject().get("name").getAsString()));
        var declaredOperations = new TreeSet<String>();
        Commands.OPERATIONS.forEach(o -> declaredOperations.add(o.name()));
        assertThat(describedOperations, is(declaredOperations));
    }

    @Test
    public void everyCommandsRealResponseConformsToItsDeclaredSchemas() throws IOException
    {
        var sampleFile = ClientFiles.xml(folder.getRoot().toPath()).toPath();

        for (var element : describe().getAsJsonArray("commands"))
        {
            var command = element.getAsJsonObject();
            var name = command.get("name").getAsString();

            var run = CliRun.run(sampleArguments(name, sampleFile).toArray(String[]::new));
            assertThat(name + " exit code, stdout: " + run.stdout, run.exitCode, is(0));

            var envelope = run.json().getAsJsonObject();
            assertThat(name + " envelope", envelope.keySet(), is(Set.of("data", "meta", "warnings")));

            var response = command.getAsJsonObject("response");
            assertThat(name + " data", SchemaValidator.validate(envelope.get("data"), response.getAsJsonObject("data")),
                            is(empty()));
            assertThat(name + " meta", SchemaValidator.validate(envelope.get("meta"), response.getAsJsonObject("meta")),
                            is(empty()));
        }
    }

    @Test
    public void everyCommandsDeclaredOptionsAreDescribed()
    {
        var described = describe();

        for (var command : Commands.ALL)
        {
            var options = new TreeSet<String>();
            for (var spec : described.getAsJsonArray("commands"))
            {
                if (spec.getAsJsonObject().get("name").getAsString().equals(command.name()))
                    spec.getAsJsonObject().getAsJsonArray("options")
                                    .forEach(o -> options.add(o.getAsJsonObject().get("name").getAsString()));
            }

            var declared = new TreeSet<String>();
            command.options().forEach(o -> declared.add(o.name()));
            assertThat(command.name(), options, is(declared));
        }
    }
}
