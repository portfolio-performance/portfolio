package name.abuchen.portfolio.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.junit.PortfolioBuilder;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ClientFactory;
import name.abuchen.portfolio.model.SaveFlag;

/**
 * Builds small client files in the format under test. Every call writes a new
 * file, so tests never share fixtures.
 */
final class ClientFiles
{
    static final int NEWER_VERSION = Client.CURRENT_VERSION + 29;

    /** Core treats encrypted headers further ahead than 20 as a wrong password. */
    static final int NEWER_ENCRYPTED_VERSION = Client.CURRENT_VERSION + 5;

    private static final byte[] BINARY_SIGNATURE = "PPPBV1".getBytes(StandardCharsets.US_ASCII);

    private ClientFiles()
    {
    }

    /** One instrument, one investment account, one cash account, two records. */
    static Client sampleClient()
    {
        var client = new Client();

        var security = new SecurityBuilder().addTo(client);
        new SecurityBuilder().addTo(client);

        var account = new AccountBuilder().deposit_("2020-01-01", PortfolioBuilder.amountOf(1000)).addTo(client);
        new PortfolioBuilder(account).buyPrice(security, "2020-02-01", 5, 100).addTo(client);

        return client;
    }

    static File write(Path folder, String name, Set<SaveFlag> flags) throws IOException
    {
        var file = folder.resolve(name).toFile();
        ClientFactory.saveAs(sampleClient(), file, null, EnumSet.copyOf(flags));
        return file;
    }

    /** Writes the given client as plain XML. */
    static File xml(Path folder, Client client) throws IOException
    {
        var file = folder.resolve("client.xml").toFile();
        ClientFactory.saveAs(client, file, null, EnumSet.of(SaveFlag.XML));
        return file;
    }

    static File xml(Path folder) throws IOException
    {
        return write(folder, "client.xml", EnumSet.of(SaveFlag.XML));
    }

    static File xmlWithIdReferences(Path folder) throws IOException
    {
        return write(folder, "client.xml", EnumSet.of(SaveFlag.XML, SaveFlag.ID_REFERENCES));
    }

    static File xmlZip(Path folder) throws IOException
    {
        return write(folder, "client.zip", EnumSet.of(SaveFlag.XML, SaveFlag.COMPRESSED));
    }

    static File binary(Path folder) throws IOException
    {
        return write(folder, "client.portfolio", EnumSet.of(SaveFlag.BINARY, SaveFlag.COMPRESSED));
    }

    static File encrypted(Path folder, String password, SaveFlag content, SaveFlag aes) throws IOException
    {
        var file = folder.resolve("client.portfolio").toFile();
        ClientFactory.saveAs(sampleClient(), file, password.toCharArray(),
                        EnumSet.of(content, SaveFlag.ENCRYPTED, aes));
        return file;
    }

    static File xmlAes256(Path folder, String password) throws IOException
    {
        return encrypted(folder, password, SaveFlag.XML, SaveFlag.AES256);
    }

    static File xmlAes128(Path folder, String password) throws IOException
    {
        return encrypted(folder, password, SaveFlag.XML, SaveFlag.AES128);
    }

    static File binaryAes256(Path folder, String password) throws IOException
    {
        return encrypted(folder, password, SaveFlag.BINARY, SaveFlag.AES256);
    }

    /** Core writes the version from the client, which only core can set. */
    static File newerEncrypted(Path folder, String password) throws IOException
    {
        var client = sampleClient();
        try
        {
            var setVersion = Client.class.getDeclaredMethod("setVersion", int.class);
            setVersion.setAccessible(true);
            setVersion.invoke(client, NEWER_ENCRYPTED_VERSION);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException("cannot set the client version", e);
        }

        var file = folder.resolve("client.portfolio").toFile();
        ClientFactory.saveAs(client, file, password.toCharArray(),
                        EnumSet.of(SaveFlag.XML, SaveFlag.ENCRYPTED, SaveFlag.AES256));
        return file;
    }

    static File newerXml(Path folder) throws IOException
    {
        var file = xml(folder);
        var content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        var current = "<version>" + Client.CURRENT_VERSION + "</version>";
        if (!content.contains(current))
            throw new IllegalStateException("version element not found in XML fixture");
        Files.writeString(file.toPath(), content.replace(current, "<version>" + NEWER_VERSION + "</version>"),
                        StandardCharsets.UTF_8);
        return file;
    }

    /** Core has no version ceiling for unencrypted binary files. */
    static File newerBinary(Path folder) throws IOException
    {
        var file = binary(folder);

        byte[] data;
        String entryName;
        try (var zip = new ZipInputStream(Files.newInputStream(file.toPath())))
        {
            ZipEntry entry = zip.getNextEntry();
            entryName = entry.getName();
            data = zip.readAllBytes();
        }

        // PClient writes its version as the first field: tag 0x08, then a
        // varint
        int offset = BINARY_SIGNATURE.length;
        if (!Arrays.equals(Arrays.copyOf(data, offset), BINARY_SIGNATURE) || data[offset] != 0x08
                        || data[offset + 1] != Client.CURRENT_VERSION)
            throw new IllegalStateException("unexpected binary layout in fixture");
        data[offset + 1] = (byte) NEWER_VERSION;

        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes))
        {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(data);
            zip.closeEntry();
        }
        Files.write(file.toPath(), bytes.toByteArray());
        return file;
    }
}
