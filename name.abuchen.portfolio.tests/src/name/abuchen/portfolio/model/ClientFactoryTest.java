package name.abuchen.portfolio.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import name.abuchen.portfolio.money.CurrencyUnit;

@SuppressWarnings("nls")
public class ClientFactoryTest
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGetFlagsForZipFile() throws IOException
    {
        var zipFile = tempFolder.newFile("test.zip");
        var flags = ClientFactory.getFlags(zipFile);

        assertEquals(EnumSet.of(SaveFlag.XML, SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForZipFileCaseInsensitive() throws IOException
    {
        var zipFile = tempFolder.newFile("test.ZIP");
        var flags = ClientFactory.getFlags(zipFile);

        assertEquals(EnumSet.of(SaveFlag.XML, SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForEncryptedPortfolio() throws IOException
    {
        var encryptedFile = tempFolder.newFile("test.portfolio");

        // Write encrypted signature (PORTFOLIO in bytes)
        try (var out = new FileOutputStream(encryptedFile))
        {
            out.write(new byte[] { 80, 79, 82, 84, 70, 79, 76, 73, 79 });
        }

        var flags = ClientFactory.getFlags(encryptedFile);
        assertEquals(EnumSet.of(SaveFlag.ENCRYPTED), flags);
    }

    @Test
    public void testGetFlagsForCompressedPortfolio() throws IOException
    {
        var compressedFile = tempFolder.newFile("test.portfolio");

        // Write ZIP signature (PK\x03\x04)
        try (var out = new FileOutputStream(compressedFile))
        {
            out.write(new byte[] { 80, 75, 3, 4 });
        }

        var flags = ClientFactory.getFlags(compressedFile);
        assertEquals(EnumSet.of(SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForAribitraryPortfolio() throws IOException
    {
        var compressedFile = tempFolder.newFile("arbitrary.portfolio");

        // Write ZIP signature (PK\x03\x04)
        try (var out = new FileOutputStream(compressedFile))
        {
            out.write(new byte[] { 44, 45, 43, 46 });
        }

        var flags = ClientFactory.getFlags(compressedFile);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForXmlFileWithoutIdReferences() throws IOException
    {
        var xmlFile = tempFolder.newFile("test.xml");

        try (var out = new FileOutputStream(xmlFile))
        {
            out.write("<client><version>123</version>".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(xmlFile);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForXmlFileWithIdReferences() throws IOException
    {
        var xmlFile = tempFolder.newFile("test.xml");

        try (var out = new FileOutputStream(xmlFile))
        {
            out.write("<client id=\"123\">".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(xmlFile);
        assertTrue(flags.contains(SaveFlag.XML));
        assertTrue(flags.contains(SaveFlag.ID_REFERENCES));
    }

    @Test
    public void testGetFlagsForEmptyFile() throws IOException
    {
        var emptyFile = tempFolder.newFile("empty.dat");

        var flags = ClientFactory.getFlags(emptyFile);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForFileWithoutExtension() throws IOException
    {
        var file = tempFolder.newFile("noextension");

        try (var out = new FileOutputStream(file))
        {
            out.write("<client><version>123</version>".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForEncryptedFileWithoutExtension() throws IOException
    {
        var file = tempFolder.newFile("encrypted");

        // Write encrypted signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 80, 79, 82, 84, 70, 79, 76, 73, 79 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.ENCRYPTED), flags);
    }

    @Test
    public void testGetFlagsForCompressedFileWithoutExtension() throws IOException
    {
        var file = tempFolder.newFile("compressed");

        // Write ZIP signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 80, 75, 3, 4 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.COMPRESSED), flags);
    }

    @Test
    public void testGetFlagsForUnknownFileContent() throws IOException
    {
        var file = tempFolder.newFile("unknown.dat");

        // Write some random content that doesn't match any signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 1, 2, 3, 4, 5 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForUnknownShortFileContent() throws IOException
    {
        var file = tempFolder.newFile("unknown.dat");

        // Write some random content that doesn't match any signature
        try (var out = new FileOutputStream(file))
        {
            out.write(new byte[] { 1 });
        }

        var flags = ClientFactory.getFlags(file);
        assertEquals(EnumSet.of(SaveFlag.XML), flags);
    }

    @Test
    public void testGetFlagsForXmlFileWithIdReferencesInFallback() throws IOException
    {
        var file = tempFolder.newFile("unknown.dat");

        try (var out = new FileOutputStream(file))
        {
            out.write("<client id=\"1\"><version>123</version>".getBytes(StandardCharsets.UTF_8));
        }

        var flags = ClientFactory.getFlags(file);
        assertTrue(flags.contains(SaveFlag.XML));
        assertTrue(flags.contains(SaveFlag.ID_REFERENCES));
    }

    @Test
    public void testSaveAndLoadCompressedBinaryFile() throws IOException
    {
        assertSaveAndLoad("test.portfolio", null, EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
    }

    @Test
    public void testSaveAndLoadCompressedXmlFile() throws IOException
    {
        assertSaveAndLoad("test.zip", null, EnumSet.of(SaveFlag.XML, SaveFlag.COMPRESSED));
    }

    @Test
    public void testSaveAndLoadEncryptedBinaryFile() throws IOException
    {
        assertSaveAndLoad("test.portfolio", "secret".toCharArray(),
                        EnumSet.of(SaveFlag.BINARY, SaveFlag.ENCRYPTED, SaveFlag.AES256));
    }

    @Test
    public void testSaveAndLoadEncryptedXmlFile() throws IOException
    {
        assertSaveAndLoad("test.portfolio", "secret".toCharArray(),
                        EnumSet.of(SaveFlag.XML, SaveFlag.ENCRYPTED, SaveFlag.AES256));
    }

    private void assertSaveAndLoad(String filename, char[] password, Set<SaveFlag> flags) throws IOException
    {
        var client = ClientFactory.load(ClientFactoryTest.class.getResourceAsStream("/scenarios/protobuf.xml"));

        // add enough historical prices so that the compressed data spans
        // multiple blocks of the parallel compression
        var security = new Security("Security with many prices", CurrencyUnit.EUR);
        var date = LocalDate.of(1900, 1, 1);
        for (int ii = 0; ii < 60_000; ii++)
            security.addPrice(new SecurityPrice(date.plusDays(ii), 1_000_000_000L + ii * 12_345L));
        // the binary format requires the timestamp of the last modification
        security.setUpdatedAt(Instant.parse("2026-10-21T08:00:00Z"));
        client.addSecurity(security);

        var expected = ClientTestUtilities.toString(client);

        var file = tempFolder.newFile(filename);
        ClientFactory.saveAs(client, file, password, EnumSet.copyOf(flags));

        var loaded = ClientFactory.load(file, password, new NullProgressMonitor());
        var actual = ClientTestUtilities.toString(loaded);

        assertTrue("difference starting at character " + ClientTestUtilities.indexOfDifference(expected, actual),
                        expected.equals(actual));
    }
}
