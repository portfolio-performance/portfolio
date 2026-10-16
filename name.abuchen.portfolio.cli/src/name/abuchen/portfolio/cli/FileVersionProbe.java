package name.abuchen.portfolio.cli;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

/**
 * Reads the file version straight from the file without loading the model.
 * Core refuses newer XML files with a localized message and does not check
 * unencrypted binary files at all, so the CLI needs the number up front.
 */
final class FileVersionProbe
{
    private static final byte[] BINARY_SIGNATURE = "PPPBV1".getBytes(StandardCharsets.US_ASCII);

    /** Protobuf tag of PClient.version: field 1, varint. */
    private static final int BINARY_VERSION_TAG = 0x08;

    private FileVersionProbe()
    {
    }

    /**
     * @return the version stored in the file, or empty if it cannot be
     *         determined without decrypting or fully parsing the file
     */
    static OptionalInt read(File file, FileFormat format)
    {
        try (var input = new BufferedInputStream(new FileInputStream(file)))
        {
            switch (format)
            {
                case XML, XML_ID_REFERENCES:
                    return readXmlVersion(input);
                case XML_ZIP:
                    return readXmlVersion(firstZipEntry(input));
                case BINARY:
                    return readBinaryVersion(firstZipEntry(input));
                default:
                    return OptionalInt.empty();
            }
        }
        catch (IOException | XMLStreamException | RuntimeException e)
        {
            return OptionalInt.empty();
        }
    }

    private static InputStream firstZipEntry(InputStream input) throws IOException
    {
        var zip = new ZipInputStream(input);
        if (zip.getNextEntry() == null)
            throw new IOException("empty archive"); //$NON-NLS-1$
        return zip;
    }

    private static OptionalInt readXmlVersion(InputStream input) throws XMLStreamException
    {
        var factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        var reader = factory.createXMLStreamReader(input);
        try
        {
            int depth = 0;
            while (reader.hasNext())
            {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT)
                {
                    depth++;
                    if (depth == 2 && "version".equals(reader.getLocalName())) //$NON-NLS-1$
                        return OptionalInt.of(Integer.parseInt(reader.getElementText().trim()));
                }
                else if (event == XMLStreamConstants.END_ELEMENT)
                {
                    depth--;
                }
            }
            return OptionalInt.empty();
        }
        finally
        {
            reader.close();
        }
    }

    private static OptionalInt readBinaryVersion(InputStream input) throws IOException
    {
        var signature = input.readNBytes(BINARY_SIGNATURE.length);
        if (!Arrays.equals(signature, BINARY_SIGNATURE))
            return OptionalInt.empty();

        if (input.read() != BINARY_VERSION_TAG)
            return OptionalInt.empty();

        int value = 0;
        for (int shift = 0; shift < 32; shift += 7)
        {
            int b = input.read();
            if (b < 0)
                return OptionalInt.empty();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0)
                return OptionalInt.of(value);
        }
        return OptionalInt.empty();
    }
}
