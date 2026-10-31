package name.abuchen.portfolio.cli.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import name.abuchen.portfolio.model.ClientFactory;

/**
 * The normalized file formats reported to agents. Core keeps the matching save
 * flags package-private, so the CLI detects the format itself from the file
 * extension and magic bytes, mirroring core's own rules.
 */
public enum FileFormat
{
    XML("xml"), XML_ID_REFERENCES("xml-id-references"), XML_ZIP("xml-zip"), BINARY("binary"), XML_AES256(
                    "xml-aes256"), XML_AES128("xml-aes128"), BINARY_AES256("binary-aes256"), BINARY_AES128(
                                    "binary-aes128");

    private static final byte[] ZIP_SIGNATURE = { 'P', 'K', 3, 4 };
    private static final String ID_REFERENCES_MARKER = "<client id=";

    private final String id;

    FileFormat(String id)
    {
        this.id = id;
    }

    public String id()
    {
        return id;
    }

    /**
     * The format of an encrypted file. Whether it holds XML or binary data is
     * only known after decrypting the header.
     */
    static FileFormat encrypted(boolean binary, int keyBits)
    {
        if (keyBits == 256)
            return binary ? BINARY_AES256 : XML_AES256;
        return binary ? BINARY_AES128 : XML_AES128;
    }

    /** Delegates to core, which recognizes encrypted files by content, never by name. */
    static boolean isEncrypted(File file)
    {
        return ClientFactory.isEncrypted(file);
    }

    /** @param file an unencrypted client file; see {@link #isEncrypted} */
    static FileFormat detect(File file) throws IOException
    {
        var name = file.getName().toLowerCase(Locale.US);

        if (name.endsWith(".zip"))
            return XML_ZIP;
        if (name.endsWith(".portfolio"))
            return detectByContent(file);
        if (name.endsWith(".xml"))
            return xmlFormat(readHead(file));

        return detectByContent(file);
    }

    private static FileFormat detectByContent(File file) throws IOException
    {
        var head = readHead(file);

        if (startsWith(head, ZIP_SIGNATURE))
            return containsBinary(file) ? BINARY : XML_ZIP;
        if (head.length > 0 && head[0] == '<')
            return xmlFormat(head);

        return XML;
    }

    private static FileFormat xmlFormat(byte[] head)
    {
        return new String(head, StandardCharsets.UTF_8).contains(ID_REFERENCES_MARKER) ? XML_ID_REFERENCES : XML;
    }

    /** A ZIP holds binary data if its first entry is the protobuf payload. */
    private static boolean containsBinary(File file) throws IOException
    {
        try (var zip = new ZipInputStream(new BufferedInputStream(new FileInputStream(file))))
        {
            ZipEntry entry = zip.getNextEntry();
            return entry != null && entry.getName().endsWith(".portfolio");
        }
    }

    private static byte[] readHead(File file) throws IOException
    {
        try (var input = new FileInputStream(file))
        {
            return input.readNBytes(100);
        }
    }

    private static boolean startsWith(byte[] actual, byte[] expected)
    {
        if (actual.length < expected.length)
            return false;
        for (int ii = 0; ii < expected.length; ii++)
            if (actual[ii] != expected[ii])
                return false;
        return true;
    }
}
