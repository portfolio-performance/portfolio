package fileversions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.ClientTestUtilities;

@RunWith(Parameterized.class)
@SuppressWarnings("nls")
public class ReadingHistoricClientFilesTest
{
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> getFiles()
    {
        return Arrays.asList(new Object[][] { // NOSONAR
                        { "client52", 52 }, { "client53", 53 } });
    }

    private Path testDir = Paths.get(new File(
                    ReadingHistoricClientFilesTest.class.getProtectionDomain().getCodeSource().getLocation().getFile())
                                    .toURI());

    private String file;
    private int versionOnDisk;

    public ReadingHistoricClientFilesTest(String file, int versionOnDisk)
    {
        this.file = file;
        this.versionOnDisk = versionOnDisk;
    }

    @Test
    public void compare() throws IOException
    {
        Client xmlClient = ClientFactory.load(find(file + ".xml"), null, new NullProgressMonitor());
        String xml = ClientTestUtilities.toString(xmlClient);
        assertThat(xmlClient.getFileVersionAfterRead(), is(versionOnDisk));

        Client binaryClient = ClientFactory.load(find(file + ".binary.portfolio"), null, new NullProgressMonitor());
        String binary = ClientTestUtilities.toString(binaryClient);
        assertThat(binaryClient.getFileVersionAfterRead(), is(versionOnDisk));

        Client binaryEncryptedClient = ClientFactory.load(find(file + ".binary+pwd.portfolio"), "123456".toCharArray(),
                        new NullProgressMonitor());
        String binaryEncrypted = ClientTestUtilities.toString(binaryEncryptedClient);
        assertThat(binaryEncryptedClient.getFileVersionAfterRead(), is(versionOnDisk));

        Client xmlEncrpytedClient = ClientFactory.load(find(file + ".xml+pwd.portfolio"), "123456".toCharArray(),
                        new NullProgressMonitor());
        String xmlEncrpyted = ClientTestUtilities.toString(xmlEncrpytedClient);
        assertThat(xmlEncrpytedClient.getFileVersionAfterRead(), is(versionOnDisk));

        if (!xml.equals(binary))
        {
            int pos = ClientTestUtilities.indexOfDifference(xml, binary);
            assertThat("binary version is not identical to xml " + pos,
                            binary.substring(pos, Math.min(pos + 100, binary.length())),
                            is(xml.substring(pos, Math.min(pos + 100, xml.length()))));
        }

        if (!xml.equals(binaryEncrypted))
        {
            int pos = ClientTestUtilities.indexOfDifference(xml, binaryEncrypted);
            assertThat("encrypted binary version is not identicdal to xml " + pos,
                            binaryEncrypted.substring(pos, Math.min(pos + 100, binaryEncrypted.length())),
                            is(xml.substring(pos, Math.min(pos + 100, xml.length()))));
        }

        if (!xml.equals(xmlEncrpyted))
        {
            int pos = ClientTestUtilities.indexOfDifference(xml, xmlEncrpyted);
            assertThat("encrypted xml is not identical to xml " + pos,
                            xmlEncrpyted.substring(pos, Math.min(pos + 100, xmlEncrpyted.length())),
                            is(xml.substring(pos, Math.min(pos + 100, xml.length()))));
        }

    }

    private File find(String name) throws IOException
    {
        // depending on the environment (Eclipse, Maven, Infinitest) the files
        // can be located in different paths

        try (Stream<Path> fileWalker = Files.walk(testDir, 10))
        {
            Optional<Path> fullPath = fileWalker.filter(p -> p.getFileName().toString().equals(name)).findAny();

            if (fullPath.isPresent())
                return fullPath.get().toFile();
        }

        throw new IllegalArgumentException();
    }
}
