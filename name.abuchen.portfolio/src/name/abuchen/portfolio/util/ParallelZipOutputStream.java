package name.abuchen.portfolio.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Writes a ZIP archive with exactly one entry and compresses the data of that
 * entry on multiple cores.
 * <p/>
 * The data is split into blocks which are compressed in parallel. Each block
 * uses the last 32 KB of the previous block as preset dictionary and ends with
 * a sync flush. Concatenated, the blocks form one regular deflate stream (same
 * technique as pigz). Therefore the archive can be read by
 * {@link java.util.zip.ZipInputStream}, {@link java.util.zip.ZipFile} and any
 * other ZIP tool, and the compression ratio is almost identical to a
 * {@link java.util.zip.ZipOutputStream} with the same compression level.
 * <p/>
 * Like {@link java.util.zip.ZipOutputStream}, closing this stream completes the
 * archive and closes the underlying output stream.
 */
public class ParallelZipOutputStream extends OutputStream
{
    /* package */ static final int BLOCK_SIZE = 128 * 1024;
    private static final int DICTIONARY_SIZE = 32 * 1024;

    private static final long LOCAL_HEADER_SIGNATURE = 0x04034b50L;
    private static final long DATA_DESCRIPTOR_SIGNATURE = 0x08074b50L;
    private static final long CENTRAL_HEADER_SIGNATURE = 0x02014b50L;
    private static final long END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50L;
    private static final long ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06064b50L;
    private static final long ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE = 0x07064b50L;

    private static final long ZIP64_MAGIC = 0xFFFFFFFFL;
    private static final int ZIP64_EXTRA_FIELD_ID = 0x0001;
    private static final int ZIP64_EXTRA_FIELD_SIZE = 16;
    private static final int ZIP64_END_OF_CENTRAL_DIRECTORY_SIZE = 44;

    private static final int VERSION = 20;
    private static final int VERSION_ZIP64 = 45;
    private static final int FLAGS = 0x0008 /* data descriptor */ | 0x0800 /* UTF-8 */;
    private static final int METHOD_DEFLATED = 8;

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final ThreadFactory THREAD_FACTORY = runnable -> {
        var thread = new Thread(runnable, "ZIP compression " + THREAD_COUNTER.incrementAndGet()); //$NON-NLS-1$
        thread.setDaemon(true);
        return thread;
    };

    private final OutputStream out;
    private final byte[] entryName;
    private final int level;
    private final int threads;
    private final int maxPendingBlocks;
    private final long dosTime;

    private final CRC32 crc = new CRC32();
    private final Deque<Future<byte[]>> pending = new ArrayDeque<>();
    private ThreadPoolExecutor executor;

    private byte[] block = new byte[BLOCK_SIZE];
    private int blockLength = 0;
    private byte[] previousBlock;

    private long uncompressedSize = 0;
    private long compressedSize = 0;
    private long bytesWritten = 0;
    private boolean closed = false;

    /**
     * Creates the archive and writes the header of the single entry.
     *
     * @param out
     *            the underlying output stream
     * @param entryName
     *            the name of the single entry
     * @param level
     *            the compression level, see {@link Deflater}
     */
    public ParallelZipOutputStream(OutputStream out, String entryName, int level) throws IOException
    {
        this(out, entryName, level, Runtime.getRuntime().availableProcessors());
    }

    /* package */ ParallelZipOutputStream(OutputStream out, String entryName, int level, int threads)
                    throws IOException
    {
        if ((level < Deflater.NO_COMPRESSION || level > Deflater.BEST_COMPRESSION)
                        && level != Deflater.DEFAULT_COMPRESSION)
            throw new IllegalArgumentException("invalid compression level " + level); //$NON-NLS-1$

        this.out = Objects.requireNonNull(out);
        this.entryName = entryName.getBytes(StandardCharsets.UTF_8);
        if (this.entryName.length > 0xFFFF)
            throw new IllegalArgumentException("entry name too long"); //$NON-NLS-1$

        this.level = level;
        this.threads = Math.max(1, threads);
        this.maxPendingBlocks = 2 * this.threads;
        this.dosTime = toDosTime(LocalDateTime.now());

        writeLocalFileHeader();
    }

    @Override
    public void write(int b) throws IOException
    {
        ensureOpen();

        block[blockLength++] = (byte) b;
        if (blockLength == BLOCK_SIZE)
            submitBlock(false);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        ensureOpen();
        Objects.checkFromIndexSize(off, len, b.length);

        while (len > 0)
        {
            int n = Math.min(len, BLOCK_SIZE - blockLength);
            System.arraycopy(b, off, block, blockLength, n);
            blockLength += n;
            off += n;
            len -= n;

            if (blockLength == BLOCK_SIZE)
                submitBlock(false);
        }
    }

    /**
     * Flushes the underlying stream. Data of the current block is only written
     * when the block is full or the stream is closed.
     */
    @Override
    public void flush() throws IOException
    {
        ensureOpen();
        out.flush();
    }

    @Override
    public void close() throws IOException
    {
        if (closed)
            return;
        closed = true;

        try
        {
            submitBlock(true);

            while (!pending.isEmpty())
                writeCompressedBlock(pending.removeFirst());

            writeDataDescriptor();
            writeCentralDirectory();
        }
        finally
        {
            pending.forEach(f -> f.cancel(true));
            pending.clear();

            if (executor != null)
                executor.shutdownNow();
        }

        out.close();
    }

    private void ensureOpen() throws IOException
    {
        if (closed)
            throw new IOException("Stream closed"); //$NON-NLS-1$
    }

    private void submitBlock(boolean isLast) throws IOException
    {
        var data = block;
        var length = blockLength;
        var dictionary = previousBlock;

        crc.update(data, 0, length);
        uncompressedSize += length;

        // all blocks except the last one are full, therefore the previous
        // block always has enough bytes for the dictionary
        previousBlock = isLast ? null : data;
        block = isLast ? null : new byte[BLOCK_SIZE];
        blockLength = 0;

        if (isLast && pending.isEmpty())
        {
            // small data: no need to start threads
            writeCompressed(compress(data, length, dictionary, isLast, level));
            return;
        }

        if (executor == null)
        {
            executor = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                            THREAD_FACTORY);
            // let the threads end even if the stream is never closed
            executor.allowCoreThreadTimeOut(true);
        }

        pending.addLast(executor.submit(() -> compress(data, length, dictionary, isLast, level)));

        // write completed blocks in order and limit the number of blocks held
        // in memory
        while (!pending.isEmpty() && (pending.size() > maxPendingBlocks || pending.peekFirst().isDone()))
            writeCompressedBlock(pending.removeFirst());
    }

    private static byte[] compress(byte[] data, int length, byte[] dictionary, boolean isLast, int level)
    {
        var deflater = new Deflater(level, true);
        try
        {
            if (dictionary != null)
                deflater.setDictionary(dictionary, dictionary.length - DICTIONARY_SIZE, DICTIONARY_SIZE);

            deflater.setInput(data, 0, length);

            var result = new ByteArrayOutputStream(length / 2 + 64);
            var buffer = new byte[64 * 1024];

            if (isLast)
            {
                // final block of the deflate stream
                deflater.finish();
                while (!deflater.finished())
                {
                    int n = deflater.deflate(buffer);
                    result.write(buffer, 0, n);
                }
            }
            else
            {
                // sync flush: the block ends at a byte boundary and the next
                // block can be appended
                int n;
                do
                {
                    n = deflater.deflate(buffer, 0, buffer.length, Deflater.SYNC_FLUSH);
                    result.write(buffer, 0, n);
                }
                while (n == buffer.length);
            }

            return result.toByteArray();
        }
        finally
        {
            deflater.end();
        }
    }

    private void writeCompressedBlock(Future<byte[]> future) throws IOException
    {
        try
        {
            writeCompressed(future.get());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(e.getMessage());
        }
        catch (ExecutionException e)
        {
            throw new IOException(e.getCause());
        }
    }

    private void writeCompressed(byte[] data) throws IOException
    {
        writeBytes(data);
        compressedSize += data.length;
    }

    private boolean isZip64Entry()
    {
        return compressedSize >= ZIP64_MAGIC || uncompressedSize >= ZIP64_MAGIC;
    }

    private void writeLocalFileHeader() throws IOException
    {
        // CRC and sizes are unknown yet and follow in the data descriptor
        writeInt(LOCAL_HEADER_SIGNATURE);
        writeShort(VERSION);
        writeShort(FLAGS);
        writeShort(METHOD_DEFLATED);
        writeInt(dosTime);
        writeInt(0); // CRC-32
        writeInt(0); // compressed size
        writeInt(0); // uncompressed size
        writeShort(entryName.length);
        writeShort(0); // extra field length
        writeBytes(entryName);
    }

    private void writeDataDescriptor() throws IOException
    {
        writeInt(DATA_DESCRIPTOR_SIGNATURE);
        writeInt(crc.getValue());
        if (isZip64Entry())
        {
            writeLong(compressedSize);
            writeLong(uncompressedSize);
        }
        else
        {
            writeInt(compressedSize);
            writeInt(uncompressedSize);
        }
    }

    private void writeCentralDirectory() throws IOException
    {
        boolean zip64Entry = isZip64Entry();
        long centralDirectoryOffset = bytesWritten;

        writeInt(CENTRAL_HEADER_SIGNATURE);
        writeShort(zip64Entry ? VERSION_ZIP64 : VERSION); // version made by
        writeShort(zip64Entry ? VERSION_ZIP64 : VERSION); // version needed
        writeShort(FLAGS);
        writeShort(METHOD_DEFLATED);
        writeInt(dosTime);
        writeInt(crc.getValue());
        writeInt(zip64Entry ? ZIP64_MAGIC : compressedSize);
        writeInt(zip64Entry ? ZIP64_MAGIC : uncompressedSize);
        writeShort(entryName.length);
        writeShort(zip64Entry ? 4 + ZIP64_EXTRA_FIELD_SIZE : 0); // extra field
        writeShort(0); // comment length
        writeShort(0); // disk number start
        writeShort(0); // internal file attributes
        writeInt(0); // external file attributes
        writeInt(0); // offset of local header
        writeBytes(entryName);

        if (zip64Entry)
        {
            writeShort(ZIP64_EXTRA_FIELD_ID);
            writeShort(ZIP64_EXTRA_FIELD_SIZE);
            writeLong(uncompressedSize);
            writeLong(compressedSize);
        }

        long centralDirectorySize = bytesWritten - centralDirectoryOffset;
        boolean zip64End = centralDirectoryOffset >= ZIP64_MAGIC;

        if (zip64End)
        {
            long zip64EndOffset = bytesWritten;

            writeInt(ZIP64_END_OF_CENTRAL_DIRECTORY_SIGNATURE);
            writeLong(ZIP64_END_OF_CENTRAL_DIRECTORY_SIZE);
            writeShort(VERSION_ZIP64); // version made by
            writeShort(VERSION_ZIP64); // version needed
            writeInt(0); // number of this disk
            writeInt(0); // disk with central directory
            writeLong(1); // entries on this disk
            writeLong(1); // total entries
            writeLong(centralDirectorySize);
            writeLong(centralDirectoryOffset);

            writeInt(ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_SIGNATURE);
            writeInt(0); // disk with zip64 end of central directory
            writeLong(zip64EndOffset);
            writeInt(1); // total number of disks
        }

        writeInt(END_OF_CENTRAL_DIRECTORY_SIGNATURE);
        writeShort(0); // number of this disk
        writeShort(0); // disk with central directory
        writeShort(1); // entries on this disk
        writeShort(1); // total entries
        writeInt(centralDirectorySize);
        writeInt(zip64End ? ZIP64_MAGIC : centralDirectoryOffset);
        writeShort(0); // comment length
    }

    private void writeShort(int value) throws IOException
    {
        out.write(value & 0xFF);
        out.write((value >>> 8) & 0xFF);
        bytesWritten += 2;
    }

    private void writeInt(long value) throws IOException
    {
        writeShort((int) (value & 0xFFFF));
        writeShort((int) ((value >>> 16) & 0xFFFF));
    }

    private void writeLong(long value) throws IOException
    {
        writeInt(value & 0xFFFFFFFFL);
        writeInt(value >>> 32);
    }

    private void writeBytes(byte[] data) throws IOException
    {
        out.write(data);
        bytesWritten += data.length;
    }

    private static long toDosTime(LocalDateTime time)
    {
        if (time.getYear() < 1980)
            return (1 << 21) | (1 << 16); // 1980-01-01 00:00

        return ((long) (time.getYear() - 1980) << 25) | (time.getMonthValue() << 21) | (time.getDayOfMonth() << 16)
                        | (time.getHour() << 11) | (time.getMinute() << 5) | (time.getSecond() >> 1);
    }
}
