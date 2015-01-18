package name.abuchen.portfolio.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Copied from Apache Commons IO issues backlog
 * "Introduce new filter input stream with replacement facilities"
 * (https://issues.apache.org/jira/browse/IO-218)
 */

/**
 * Stands for the input stream that decorates given input stream and replaces its content during reading
 * according to the specified rules.
 * <p/>
 * This class applies replacement rules in particular order based on replacement <code>'from'</code> data length.
 * I.e. if this class is provided with replacements like
 * <code>({1, 2} -> {3, 4}; {2, 3, 4} -> {5, 6, 7}; {4, 5} - > {7, 8})</code> it guarantees that
 * <code>'{2, 3, 4} -> {5, 6, 7}'</code> replacement is applied before anothers. Processing order of same
 * <code>'from'</code>-length replacements is unspecified.
 * <p/>
 * When particular replacement rule is applied, it's <code>'to'</code> clause is not processed via another
 * replacement rules. I.e. if we have a mappings like <code>'{1, 2, 3} -> {4, 5, 6}'</code> and
 * <code>{5} -> {7}</code>, last rule is not applied to the <code>'5'</code> byte that appeared at the
 * <code>'5, 6, 7'</code> group.
 * <b>Memory overhead</b>
 * This class uses internal buffering similar to the one used by {@link PushbackInputStream}
 * and uses a dedicated buffer for single-byte reads ({@link InputStream#read()}). I.e. it creates two buffers in
 * addition to the given stream. One buffer has a size that is max of replacements <code>'from'</code>
 * and <code>'to'</code> buffers size, another is guaranteed to be not more than size of the buffer used by client
 * for buffered reading ({@link #read(byte[], int, int)}).
 * <p/>
 * <b>CPU overhead</b>
 * Generally speaking this class matches every read byte against configured replacement rules and performs
 * replacement if necessary.
 * <p/>
 * Not thread-safe.
 * <p/>
 * <b>Example</b>
 * <pre>
 *       Map<byte[], byte[]> replacements = new HashMap<byte[],byte[]>();
 *       replacements.put(new byte[] {1, 2}, new byte[] {7, 8});
 *       replacements.put(new byte[] {1}, new byte[] {9});
 *       replacements.put(new byte[] {3, 2}, new byte[0]);
 *       byte[] input = {4, 3, 2, 1, 2, 1, 3};
 *       ReplaceFilterInputStream in = new ReplaceFilterInputStream(new ByteArrayInputStream(input), replacements);
 *       ByteArrayOutputStream out = new ByteArrayOutputStream();
 *       int read;
 *       while ((read = in.read()) >= 0) {
 *           out.write(read);
 *       }
 *       System.out.println(Arrays.toString(out.toByteArray())); // prints [4, 7, 8, 9, 3]
 * </pre>
 *
 * @author Denis Zhdanov
 * @since Aug 31, 2009
 */
public class ReplaceFilterInputStream extends FilterInputStream {

    private enum ReplacementResult {
        REPLACED, NOT_MATCHED, NOT_ENOUGH_DATA
    }

    /**
     * We use exactly {@link TreeMap} here in order to process all replacements in particular order - starting from
     * the replacements which <code>'from'</code> data has a larger length.
     */
    private final Map<byte[], byte[]> replacements = new TreeMap<byte[], byte[]>(new Comparator<byte[]>() {
        @Override
        public int compare(byte[] b1, byte[] b2) {
            if (b1.length != b2.length) {
                return b2.length - b1.length;
            }
            for (int i = 0; i < b1.length; ++i) {
                if (b1[i] != b2[i]) {
                    return b2[i] - b1[i];
                }
            }
            return 0;
        }
    });


    /**
     * Is used for <code>'unreading'</code> data if necessary (e.g. there is a possible situation that the buffer
     * ends with the data that matches to particular replacement start but the data is not enough to understand
     * if replacement should be processed. We push back such ambiguous data then).
     * <p/>
     * Another case is that replacement value is much greater than replacement key and the client performs buffered
     * reading. If there are many replacement 'from' matches at the read data, given client buffer may be not large
     * enough to hold read data with 'replacement to' rule applied. We want to holds unprocessed data at this buffer
     * then.
     * <p/>
     * The data is assumed to be located from the end to the start of the buffer. I.e. if following bytes are pushed
     * to this buffer - '1', '2', '3' they are located at the buffer end - {..., 1, 2, 3}.
     */
    private byte[] pushBackBuffer;

    /**
     * Holds index of the next position to be used for data insertion to the {@link #pushBackBuffer}.
     */
    private int pushBackPosition;

    /**
     * Buffer used during single-byte reads ({@link #read()}).
     */
    private final ByteBuffer singleByteReadBuffer;

    /**
     * {@link ByteBuffer} wrapper for the buffer used during buffered reading ({@link #read(byte[])}
     * and {@link #read(byte[], int, int)}). It is assumed that the client of this class reuses the same raw
     * heap buffer for multiple <code>'read()'</code> calls, so, it's worth to try to reuse {@link ByteBuffer}
     * if possible.
     * <p/>
     * This property holds reference to that reused buffer wrapper.
     */
    private ByteBuffer cachedClientBuffer;

    /**
     * This property holds index of the first buffer position that should not be used.
     * <p/>
     * When the user provides a buffer he or she may specify offset and leength. This property holds that
     * <code>'offset + length'</code> value.
     */
    private int endIndex;

    /**
     * This property defines if particular number of subsequent bytes stored at the underlying input stream
     * should be processed as is, i.e. not processed using current replacement mapppings.
     * <p/>
     * This property may be more than zero if, for example, particular mapping <code>'to'</code> clause overlaps
     * another mapping's <code>'from'</code> clause. Suppose we have the following mappings:
     * <pre>
     * {1, 2, 3} -> {4, 5, 6}
     * {5} -> {7}
     * </pre>
     * and the input '{1, 2, 3, 4, 5, 6, 7}'. There is a possible case that the client uses single byte reading,
     * so, when the first '1' byte is read, input is analyzed and <code>'{1, 2, 3} -> {4, 5, 6}'</code> rule
     * is applied, we need to return <code>'4'</code> byte and push back <code>'5'</code> and <code>'6'</code>.
     * However, that <code>'5'</code> should not be processed by <code>'{5} -> {7}'</code> rule, so, we remember
     * that next two bytes should be skipped.
     */
    private int skip;

    /**
     * This flag is set if any replacement rule is applied.
     * <p/>
     * It's primary purpose is to correctly resolve the situations when, for example, last stream byte matches to
     * particular replacement's <code>'from'</code> clause and that clause size if more than one. So, we read
     * the data from the stream, check that single byte is read and that byte matches to the replacement's
     * <code>'from'</code> first byte. But we don't have enough information to answer is the rule should be applied,
     * so, we push back that byte. Cycle.
     */
    private boolean replacementOccurred;

    /**
     * Holds number of bytes actually read from the underlying stream. Is necessary to understand
     * if end of stream is reached.
     */
    private int readFromStream;

    /**
     * Creates new <code>ReplacementInputStream</code> object.
     *
     * @param in           input stream which content should be replaced if necessary
     * @param replacements replacements to use with the given input stream; may be empty map,
     *                     may not be <code>null</code>
     * @throws IllegalArgumentException if given input stream to process or <code>'replacements'</code> argument
     *                                  is <code>null</code>
     */
    public ReplaceFilterInputStream(InputStream in, Map<byte[], byte[]> replacements) throws IllegalArgumentException {
        super(in);
        if (replacements == null) {
            throw new IllegalArgumentException("Can't create ReplaceFilterInputStream for the 'null' replacements. "
                    + "Given input stream to process: " + in);
        }
        boolean replacementsValid = false;
        for (Map.Entry<byte[], byte[]> entry : replacements.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Can't create ReplaceFilterInputStream. Reason: given "
                        + "replacements holds null as one of keys. Replacements: " + replacements);
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(String.format("Can't create ReplaceFilterInputStream. Reason: given "
                        + "replacements holds null as value of key (%s). Replacements: %s",
                        Arrays.toString(entry.getKey()), replacements));
            }
            if (entry.getKey().length > 0) {
                replacementsValid = true;
            }
        }
        if (!replacementsValid) {
            singleByteReadBuffer = null;
            return;
        }

        this.replacements.putAll(replacements);
        int length = getMaxLength(replacements.keySet(), replacements.values());
        singleByteReadBuffer = ByteBuffer.allocate(length);
        pushBackBuffer = new byte[(2 * length)];
        pushBackPosition = pushBackBuffer.length - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        // Act as a usual stream if replacements are not specified.
        if (replacements.isEmpty()) {
            return super.read();
        }

        replacementOccurred = true;
        int read;
        // There is a possible case that replacement occurred and 'to' has a zero length (i.e. matched data
        // is removed). We want to continue the process then. That's the reason of loop presence.
        do {
            singleByteReadBuffer.clear();
            read = doRead(singleByteReadBuffer, 1);
        } while (read == 0 && (replacementOccurred || readFromStream >= 0));

        // We assume here that if no data is read and no replacement occurred underlying stream doesn't contain
        // the data and the only data available is located at push back buffer.
        if (read == 0 && !replacementOccurred) {
            return pushBackBuffer[++pushBackPosition];
        }

        if (read < 0) {
            return read;
        }

        int toUnread = singleByteReadBuffer.remaining() - 1;
        if (toUnread > 0) {
            unread(singleByteReadBuffer.array(), singleByteReadBuffer.position() + 1, toUnread);
            skip += toUnread;
        }
        return singleByteReadBuffer.get(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        // Act as a usual stream if replacements are not specified.
        if (replacements.isEmpty()) {
            return super.read(b, off, len);
        }

        // Conform to the contract defined by InputStream class.
        if (b == null) {
            throw new NullPointerException(String.format("Can't process ReplaceFilterInputStream.read() for the "
                    + "null buffer reference. Given offset: %d, length: %d", off, len));
        }
        if (off < 0) {
            throw new ArrayIndexOutOfBoundsException("Can't process ReplaceFilterInputStream.read(). "
                    + "Reason: given offset is negative (" + off + ")");
        }
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException("Can't process ReplaceFilterInputStream.read(). "
                    + "Reason: given length is negative (" + len + ")");
        }
        if (len > b.length - off) {
            throw new ArrayIndexOutOfBoundsException(String.format("Can't process ReplaceFilterInputStream.read(). "
                    + "Reason: given length (%d) is more than buffer's max available length "
                    + "(%d, implied by buffer length (%d) - offset (%d))", len, b.length - off, b.length, off));
        }

        ByteBuffer bufferToUse;
        int result;
        // There is a possible case that replacement occurred and 'to' has a zero length (i.e. matched data
        // is removed). We want to continue the process then. That's the reason of loop presence.
        do {
            bufferToUse = wrapForBufferedReading(b, off, len);
            result = doRead(bufferToUse, len);
        } while (result == 0 && (replacementOccurred || readFromStream >= 0));

        // We assume here that if no data is read and no replacement occurred underlying stream doesn't contain
        // the data and the only data available is located at push back buffer.
        if (result == 0 && !replacementOccurred) {
            result = pushBackBuffer.length - pushBackPosition - 1;
            System.arraycopy(pushBackBuffer, pushBackPosition + 1, b, off, result);
            pushBackPosition += result;
            return result;
        }

        if (bufferToUse.array() == b) {
            return result;
        }

        if (result > len) {
            unread(bufferToUse.array(), off + len, result - len);
        }

        System.arraycopy(bufferToUse.array(), off, b, off, len);
        return Math.min(result, len);
    }

    /**
     * This method allows to get {@link ByteBuffer} object to be used for {@link #read(byte[], int, int)} processing.
     * It tries to use {@link #singleByteReadBuffer} if possible or wraps given buffer and caches the reference.
     * <p/>
     * Returned buffer has correctly defined <code>'limit'</code> and <code>'position'</code> properties.
     *
     * @param b   buffer given by client for the reading
     * @param off offset to use within given buffer as specified by the client
     * @param len length to use within given buffer as specified by the client
     * @return {@link ByteBuffer} object to use for buffered reading
     */
    private ByteBuffer wrapForBufferedReading(byte[] b, int off, int len) {
        if (cachedClientBuffer == null || cachedClientBuffer.array() != b) {
            cachedClientBuffer = ByteBuffer.wrap(b);
        }

        if (len < singleByteReadBuffer.capacity()) {
            singleByteReadBuffer.clear();
            return singleByteReadBuffer;
        }
        cachedClientBuffer.position(off);
        cachedClientBuffer.limit(off + len);
        return cachedClientBuffer;
    }

    /**
     * Allows to retrieve the largest length between all given arrays.
     *
     * @param iterables arrays which max length value we are interested in
     * @return largest length between all given arrays
     */
    private static int getMaxLength(Iterable<byte[]>... iterables) {
        int result = -1;
        for (Iterable<byte[]> iterable : iterables) {
            for (byte[] array : iterable) {
                result = Math.max(result, array.length);
            }
        }
        return result;
    }

    /**
     * Performs actual data reading and replacement using given buffer. It is assumed that the buffer has its
     * <code>'position'</code> and <code>'length'</code> specified, i.e. <code>'position'</code> defines offset
     * to be used and <code>'limit' - 'position'</code> defines buffer work length.
     *
     * @param buffer            buffer to use during processing
     * @param targetBytesNumber number of bytes that are enough to stop the processing
     * @return number of bytes read during processing
     * @throws IOException in the case of unexpected I/O exception occurred during processing
     */
    private int doRead(ByteBuffer buffer, int targetBytesNumber) throws IOException {
        replacementOccurred = false;
        buffer.mark();
        endIndex = buffer.limit();
        int initalPosition = buffer.position();
        int read = prepare(buffer);
        if (read < 0) {
            return read;
        }
        boolean continueIteration = true;
        while (continueIteration && buffer.hasRemaining()) {

            // There is a possible case that we have a replacement where 'from' is short and 'to' is long.
            // We assume here that the provided buffer is large enough to hold max 'from' or 'to' bytes sequence.
            // hence, it's possible that the client doesn't need to fill the whole buffer (e.g. when single-byte
            // read is used). So, we stop the processing if necessary number of bytes is retrieved and processed.
            if (buffer.position() - initalPosition >= targetBytesNumber) {
                unread(buffer.array(), buffer.position(), buffer.remaining());
                buffer.limit(buffer.position());
                break;
            }
            boolean processed = false;
            for (Map.Entry<byte[], byte[]> entry : replacements.entrySet()) {
                ReplacementResult replacementResult = tryToReplace(buffer, entry.getKey(), entry.getValue());
                switch (replacementResult) {
                    case NOT_MATCHED:
                        continue;
                    case NOT_ENOUGH_DATA:
                        continueIteration = false;
                }
                processed = true;
                break;
            }
            if (!processed && continueIteration) {
                buffer.position(buffer.position() + 1);
            }
        }
        return buffer.position() - buffer.reset().position();
    }

    /**
     * Reads the data from the input stream, defines buffer <code>'limit'</code> proeprty according to the number
     * of read bytes and returns read bytes number.
     *
     * @param buffer buffer to read the data
     * @return number of read bytes
     * @throws IOException in the case of unexpected I/O exception duirng reading
     */
    private int prepare(ByteBuffer buffer) throws IOException {
        // Read data from the internal buffer if any.
        int pushedInBytes = 0;
        if (pushBackPosition + 1 < pushBackBuffer.length) {
            pushedInBytes = Math.min(buffer.remaining(), pushBackBuffer.length - pushBackPosition - 1);
            System.arraycopy(pushBackBuffer, pushBackPosition + 1, buffer.array(), buffer.position(), pushedInBytes);
            pushBackPosition += pushedInBytes;
        }

        // Read data from the underlying stream.
        readFromStream = in.read(buffer.array(), buffer.position() + pushedInBytes, buffer.remaining() - pushedInBytes);
        if (readFromStream < 0 && pushedInBytes <= 0) {
            return readFromStream;
        }

        // Define total number of bytes read.
        int read = pushedInBytes;
        if (readFromStream > 0) {
            read += readFromStream;
        }
        buffer.limit(buffer.position() + read);
        if (skip > 0) {
            int skipNow = Math.min(skip, read);
            skip -= skipNow;
            buffer.position(buffer.position() + skipNow);
        }
        return read;
    }

    private ReplacementResult tryToReplace(ByteBuffer data, byte[] replacementFrom, byte[] replacementTo) {
        ReplacementResult result = ReplacementResult.REPLACED;
        int position = data.position();
        for (byte b : replacementFrom) {
            if (!data.hasRemaining()) {
                result = ReplacementResult.NOT_ENOUGH_DATA;
                unread(data.array(), position, data.position() - position);
                data.limit(position);
                break;
            }
            if (b != data.get()) {
                result = ReplacementResult.NOT_MATCHED;
                break;
            }
        }
        if (result == ReplacementResult.NOT_MATCHED || result == ReplacementResult.NOT_ENOUGH_DATA) {
            data.position(position);
            return result;
        }

        replacementOccurred = true;

        if (replacementFrom.length >= replacementTo.length) {
            replaceWithReduce(data, position, replacementFrom, replacementTo);
        } else {
            replaceWithExpand(data, position, replacementFrom, replacementTo);
        }

        return result;
    }

    /**
     * Performs given data replacement at the given buffer. I.e. replaces given <code>'replacementFrom'</code> data
     * with the given <code>'replacementTo'</code> at the given buffer starting from the
     * <code>'startPosition'</code> offset. Buffer <code>'position'</code> and <code>'limit'</code> are updated
     * as necessary.
     * <p/>
     * <b>Note:</b> this method supposes that <code>'replacementFrom'</code> length is greater or equal to the
     * <code>'replacementTo'</code> length. It's also assumed that current buffer position points to the index
     * just after <code>'replacementFrom'</code> matched section.
     *
     * @param data            data buffer
     * @param startPosition   start replacement position
     * @param replacementFrom replacement key data
     * @param replacementTo   replacement value data
     */
    private void replaceWithReduce(ByteBuffer data, int startPosition, byte[] replacementFrom, byte[] replacementTo) {
        // Copy 'replacementTo' data.
        if (replacementTo.length > 0) {
            System.arraycopy(replacementTo, 0, data.array(), startPosition, replacementTo.length);
        }
        int newPosition = startPosition + replacementTo.length;

        // Move buffer's tail if 'to' is shorter than 'from'.
        if (data.remaining() > 0) {
            System.arraycopy(data.array(), data.position(), data.array(), newPosition, data.remaining());
        }

        data.position(newPosition);
        data.limit(data.limit() - replacementFrom.length + replacementTo.length);
    }

    /**
     * Performs given data replacement at the given buffer. I.e. replaces given <code>'replacementFrom'</code> data
     * with the given <code>'replacementTo'</code> at the given buffer starting from the
     * <code>'startPosition'</code> offset. Buffer <code>'position'</code> and <code>'limit'</code> are updated
     * as necessary.
     * <p/>
     * <b>Note:</b> this method supposes that <code>'replacementTo'</code> length is greater than
     * <code>'replacementFrom'</code> length. It's also assumed that current buffer position points to the index
     * just after <code>'replacementFrom'</code> matched section.
     *
     * @param data            data buffer
     * @param initialPostiion start replacement position
     * @param replacementFrom replacement key data
     * @param replacementTo   replacement value data
     */
    private void replaceWithExpand(ByteBuffer data, int initialPostiion, byte[] replacementFrom, byte[] replacementTo) {
        int diff = replacementTo.length - replacementFrom.length;
        int bufferFreeSpace = endIndex - data.limit();
        int totalUnread = diff - bufferFreeSpace;
        int unreadBufferSize = Math.min(totalUnread, data.remaining());
        int unread = totalUnread;
        // Unread buffer content that overflows the buffer when 'replacementTo' is copied to it.
        if (unread > 0 && unreadBufferSize > 0) {
            unread(data.array(), data.limit() - unreadBufferSize, unreadBufferSize);
            unread -= unreadBufferSize;
        }

        int replacementLength = replacementTo.length;

        // Unread 'replacementTo' tail if it's too big for the given buffer.
        if (unread > 0) {
            unread(replacementTo, replacementTo.length - unread, unread);
            replacementLength -= unread;
            skip += unread;
        }

        // Move buffer data that is located after 'replacementFrom' ection if necessary.
        int moveLength = data.remaining() - unreadBufferSize;
        if (moveLength > 0) {
            System.arraycopy(data.array(), data.position(), data.array(), initialPostiion + replacementLength, moveLength);

            // There is a possible case that 'replacementTo' has greater length than 'replacementFrom' but the buffer
            // has enough space to hold expanded data. Also there is a possible case that we're proecssing near the
            // end of stream, hence, buffer's limit is lower than its capacity. We want to exapnd buffer's limit then.
            // E.g. we can have a following replacement configured {1} -> {2, 2} and have a buffer of capacity 3
            // and data {1, 3} in it (i.e. it has a capacity 3 and limit 2). We want to apply the replacement rule
            // and move byte '3' to the buffer end and insert {2, 2} instead of '1'. So, we're increasing buffer's limit.
            data.limit(Math.min(data.limit() + moveLength, data.capacity()));
        }

        // Copy necessary 'replacementTo' portion to the buffer.
        System.arraycopy(replacementTo, 0, data.array(), initialPostiion, replacementLength);

        // Update buffer parameters.
        int newPosition = initialPostiion + replacementLength;
        if (newPosition > data.limit() && newPosition <= endIndex) {
            data.limit(newPosition);
        }
        data.position(newPosition);
    }

    /**
     * Stores <code>'length'</code> bytes starting from the given offset from the given buffer at the internal
     * buffer expanding if as necessary.
     *
     * @param buffer buffer which data should be stored
     * @param offset offset to use within the given buffer
     * @param length number of bytes to store
     */
    private void unread(byte[] buffer, int offset, int length) {
        if (pushBackPosition + 1 < length) {
            byte[] newBuffer = new byte[pushBackBuffer.length * 2];
            int newPosition = pushBackBuffer.length + pushBackPosition;
            int bytesToCopy = pushBackBuffer.length - pushBackPosition - 1;
            System.arraycopy(pushBackBuffer, pushBackPosition + 1, newBuffer, newPosition + 1, bytesToCopy);
            pushBackPosition = newPosition;
            pushBackBuffer = newBuffer;
        }
        System.arraycopy(buffer, offset, pushBackBuffer, pushBackPosition - length + 1, length);
        pushBackPosition -= length;
    }
}