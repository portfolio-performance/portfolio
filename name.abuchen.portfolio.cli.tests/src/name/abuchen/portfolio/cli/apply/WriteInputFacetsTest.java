package name.abuchen.portfolio.cli.apply;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.cli.command.FacetHarness;

/**
 * What <code>apply</code> and <code>restore</code> read besides the client
 * file: the batch of <code>--input</code> and the backup of
 * <code>--backup</code>, each at its facet's interface.
 */
public class WriteInputFacetsTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path input(String text) throws IOException
    {
        return Files.writeString(folder.newFile().toPath(), text, StandardCharsets.UTF_8);
    }

    @Test
    public void theBatchIsTheOperationsOfTheInput() throws IOException
    {
        var batch = input("{\"operations\":[{\"operation\":\"instrument.add\",\"name\":\"ACME\"}]}");

        var result = FacetHarness.run(ApplyCommand.BATCH, null, "--input", batch.toString());

        assertThat(result.value().size(), is(1));
        assertThat(result.value().get(0).getAsJsonObject().get("name").getAsString(), is("ACME"));
    }

    @Test
    public void anInputThatIsNotABatchIsInvalid() throws IOException
    {
        for (var text : new String[] { "not json", "[]", "{}", "{\"operations\":[]}", "{\"operations\":{}}" })
        {
            var result = FacetHarness.run(ApplyCommand.BATCH, null, "--input", input(text).toString());

            assertThat(text, result.code(), is("invalid-input"));
        }
    }

    @Test
    public void anInputThatCannotBeReadIsUnreadable()
    {
        var absent = folder.getRoot().toPath().resolve("absent.json").toString();

        assertThat(FacetHarness.run(ApplyCommand.BATCH, null, "--input", absent).code(), is("input-unreadable"));
    }

    @Test
    public void theBackupIsTheAbsolutePathOfAnExistingFile() throws IOException
    {
        var backup = folder.newFile("client.20261016-120000.pp-cli.xml").toPath();

        var result = FacetHarness.run(RestoreCommand.BACKUP, null, "--backup", backup.toString());

        assertThat(result.value(), is(backup.toAbsolutePath()));
    }

    @Test
    public void aMissingBackupIsNotFound()
    {
        var missing = folder.getRoot().toPath().resolve("nope.xml").toString();

        assertThat(FacetHarness.run(RestoreCommand.BACKUP, null, "--backup", missing).code(),
                        is("backup-not-found"));
    }

    @Test
    public void aBlankBackupIsAnInvalidValue()
    {
        assertThat(FacetHarness.run(RestoreCommand.BACKUP, null, "--backup", " ").code(),
                        is("invalid-option-value"));
    }
}
