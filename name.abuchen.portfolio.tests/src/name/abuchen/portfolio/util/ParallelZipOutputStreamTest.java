package name.abuchen.portfolio.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class ParallelZipOutputStreamTest
{
    private static final int BLOCK = ParallelZipOutputStream.BLOCK_SIZE;

    private static final int[] SIZES = { 0, 1, 1000, BLOCK - 1, BLOCK, BLOCK + 1, 3 * BLOCK, 7 * BLOCK + 123 };

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testReadWithZipInputStream() throws IOException
    {
        for (int threads : new int[] { 1, 4 })
        {
            for (int size : SIZES)
            {
                var data = sampleData(size);
                var zip = zip(data, "data.portfolio", threads);

                try (var in = new ZipInputStream(new ByteArrayInputStream(zip)))
                {
                    ZipEntry entry = in.getNextEntry();
                    assertThat(entry, is(notNullValue()));
                    assertThat(entry.getName(), is("data.portfolio"));
                    assertArrayEquals("size " + size + ", threads " + threads, data, in.readAllBytes());

                    // values of the data descriptor are available after
                    // reading the entry
                    assertThat(entry.getSize(), is((long) size));
                    assertThat(entry.getCrc(), is(crc(data)));

                    assertThat(in.getNextEntry(), is(nullValue()));
                }
            }
        }
    }

    @Test
    public void testReadWithZipFile() throws IOException
    {
        for (int size : SIZES)
        {
            var data = sampleData(size);
            var zip = zip(data, "data.xml", 4);

            var file = tempFolder.newFile("test" + size + ".zip");
            try (var out = new FileOutputStream(file))
            {
                out.write(zip);
            }

            try (var zipFile = new ZipFile(file))
            {
                assertThat(zipFile.size(), is(1));

                var entry = zipFile.getEntry("data.xml");
                assertThat(entry, is(notNullValue()));
                assertThat(entry.getMethod(), is(ZipEntry.DEFLATED));
                assertThat(entry.getSize(), is((long) size));
                assertThat(entry.getCrc(), is(crc(data)));

                try (var in = zipFile.getInputStream(entry))
                {
                    assertArrayEquals("size " + size, data, in.readAllBytes());
                }
            }
        }
    }

    @Test
    public void testSingleByteWritesCreateSameContent() throws IOException
    {
        var data = sampleData(2 * BLOCK + 17);

        var buffer = new ByteArrayOutputStream();
        try (var out = new ParallelZipOutputStream(buffer, "data", Deflater.BEST_COMPRESSION, 2))
        {
            for (byte b : data)
                out.write(b);
        }

        assertArrayEquals(data, unzip(buffer.toByteArray()));
    }

    @Test
    public void testCompressionRatioIsCloseToZipOutputStream() throws IOException
    {
        var data = sampleData(20 * BLOCK);

        var reference = new ByteArrayOutputStream();
        try (var out = new ZipOutputStream(reference))
        {
            out.setLevel(Deflater.BEST_COMPRESSION);
            out.putNextEntry(new ZipEntry("data"));
            out.write(data);
            out.closeEntry();
        }

        var parallel = zip(data, "data", 4);

        // the dictionary of the previous block keeps the loss below 1 percent
        assertThat((long) parallel.length, lessThanOrEqualTo(reference.size() * 101L / 100L));
    }

    @Test
    public void testCloseCompletesArchiveOnlyOnceAndClosesUnderlyingStream() throws IOException
    {
        var target = new ClosingAwareOutputStream();

        var out = new ParallelZipOutputStream(target, "data", Deflater.BEST_COMPRESSION, 2);
        out.write(sampleData(BLOCK + 1));
        out.close();

        int size = target.size();
        out.close();

        assertThat(target.closed, is(true));
        assertThat(target.size(), is(size));
        assertArrayEquals(sampleData(BLOCK + 1), unzip(target.toByteArray()));
    }

    @Test
    public void testCloseClosesUnderlyingStreamIfArchiveCannotBeCompleted() throws IOException
    {
        var target = new ClosingAwareOutputStream();

        var out = new ParallelZipOutputStream(target, "data", Deflater.BEST_COMPRESSION, 2);
        out.write(sampleData(BLOCK + 1));

        target.failWrites = true;

        var exception = assertThrows(IOException.class, out::close);
        assertThat(exception.getMessage(), is("disk full"));
        assertThat(target.closed, is(true));
    }

    @Test
    public void testWriteAfterCloseFails() throws IOException
    {
        var out = new ParallelZipOutputStream(new ByteArrayOutputStream(), "data", Deflater.BEST_COMPRESSION, 2);
        out.close();

        assertThrows(IOException.class, () -> out.write(1));
        assertThrows(IOException.class, () -> out.write(new byte[10], 0, 10));
    }

    @Test
    public void testInvalidCompressionLevel()
    {
        assertThrows(IllegalArgumentException.class,
                        () -> new ParallelZipOutputStream(new ByteArrayOutputStream(), "data", 10));
    }

    private static byte[] zip(byte[] data, String name, int threads) throws IOException
    {
        var buffer = new ByteArrayOutputStream();
        try (var out = new ParallelZipOutputStream(buffer, name, Deflater.BEST_COMPRESSION, threads))
        {
            // write in chunks which do not match the block size
            int offset = 0;
            while (offset < data.length)
            {
                int length = Math.min(5000, data.length - offset);
                out.write(data, offset, length);
                offset += length;
            }
        }
        return buffer.toByteArray();
    }

    private static byte[] unzip(byte[] zip) throws IOException
    {
        try (var in = new ZipInputStream(new ByteArrayInputStream(zip)))
        {
            assertThat(in.getNextEntry(), is(notNullValue()));
            return in.readAllBytes();
        }
    }

    private static long crc(byte[] data)
    {
        var crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    /**
     * Creates compressible data with repeating patterns similar to serialized
     * client files.
     */
    private static byte[] sampleData(int size)
    {
        var words = new String[] { "<price t=\"", "\" v=\"", "\"/>\n", "security", "account", "portfolio", "EUR",
                        "USD", "transaction" };
        var random = new Random(42);
        var data = new byte[size];
        int pos = 0;
        while (pos < size)
        {
            var token = random.nextInt(4) == 0 ? String.valueOf(random.nextInt(100000))
                            : words[random.nextInt(words.length)];
            for (int ii = 0; ii < token.length() && pos < size; ii++)
                data[pos++] = (byte) token.charAt(ii);
        }
        return data;
    }

    private static class ClosingAwareOutputStream extends OutputStream
    {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean closed = false;
        private boolean failWrites = false;

        @Override
        public void write(int b) throws IOException
        {
            if (failWrites)
                throw new IOException("disk full");
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException
        {
            if (failWrites)
                throw new IOException("disk full");
            buffer.write(b, off, len);
        }

        @Override
        public void close()
        {
            closed = true;
        }

        int size()
        {
            return buffer.size();
        }

        byte[] toByteArray()
        {
            return buffer.toByteArray();
        }
    }
}
