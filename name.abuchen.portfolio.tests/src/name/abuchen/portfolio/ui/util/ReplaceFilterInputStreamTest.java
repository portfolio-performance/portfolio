package name.abuchen.portfolio.ui.util;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Copied from Apache Commons IO issues backlog
 * "Introduce new filter input stream with replacement facilities"
 * (https://issues.apache.org/jira/browse/IO-218)
 */

/**
 * @author Denis Zhdanov
 * @since 08/31/2009
 */
@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed", "SuppressionAnnotation"})
public class ReplaceFilterInputStreamTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullReplacements() {
        new ReplaceFilterInputStream(new ByteArrayInputStream(new byte[]{1, 2}), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullReplacementKey() {
        new ReplaceFilterInputStream(
                new ByteArrayInputStream(new byte[]{1, 2}),
                Collections.<byte[], byte[]>singletonMap(null, new byte[]{3, 4})
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullReplacementValue() {
        new ReplaceFilterInputStream(
                new ByteArrayInputStream(new byte[]{1, 2}),
                Collections.<byte[], byte[]>singletonMap(new byte[]{3, 4}, null)
        );
    }

    @Test
    public void emptyReplacements() throws IOException {
        byte[] input = {1, 3, 4, 6};
        doTest(input, input, new HashMap<byte[], byte[]>());
    }

    @Test
    public void emptyReplacementKey() throws IOException {
        byte[] input = {1, 2, 3, 4};
        doTest(input, input, Collections.singletonMap(new byte[0], new byte[]{5, 6}));
    }

    @Test
    public void fromAndToOfSameSize() throws IOException {
        String input = "ddasfddtredd";
        String from = "dd";
        String to = "pp";
        doTest(input, from, to);
    }

    @Test
    public void multipleFromOfSameSize() throws IOException {
        String input = "aabcc";
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("aa", "zz");
        replacements.put("cc", "yy");
        doTest(input, replacements);
    }

    @Test
    public void fromLongerThanTo() throws IOException {
        String input = "wpoeqwportwpo";
        String from = "wpo";
        String to = "a";
        doTest(input, from, to);
    }

    @Test
    public void toLongerThanFrom() throws IOException {
        String input = "weqwrtw";
        String from = "w";
        String to = "asd";
        doTest(input, from, to);
    }

    @Test
    public void toClashesFrom() throws IOException {
        String input = "abc";
        String from = "a";

        String to = "aaa";
        doTest(input, from, to);

        input = "aaa";
        doTest(input, from, to);
    }

    @Test
    public void removeReplace() throws IOException {
        String input = "cabcabcac";
        String from = "ca";
        String to = "";
        doTest(input, from, to);
    }

    @Test
    public void bufferSizeLessThanReplacementSize() throws IOException {
        byte[] input = {1, 2, 3, 4, 5};
        ReplaceFilterInputStream in = new ReplaceFilterInputStream(
                new ByteArrayInputStream(input),
                Collections.singletonMap(new byte[]{1, 2, 3}, new byte[]{6})
        );

        byte[] buffer = new byte[2];
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        int read;
        while ((read = in.read(buffer)) >= 0) {
            bOut.write(buffer, 0, read);
        }
        assertArrayEquals(new byte[]{6, 4, 5}, bOut.toByteArray());
    }

    @Test
    public void bufferOverflow() throws IOException {
        String input = "abcde";
        Map<String, String> replacements = new HashMap<String, String>();
        replacements.put("a", "111111111");
        replacements.put("b", "ABCDEFGHI");
        replacements.put("c", "333333333");
        replacements.put("d", "444444444");
        replacements.put("e", "555555555");
        doTest(input, replacements);

        input = "aaaaa";
        String from = "a";
        String to = "bbbbbbbbb";
        doTest(input, from, to);

        input = "aaaaaaa";
        to = "bb";
        doTest(input, from, to);
    }
    
    @Test
    public void slowStream() throws IOException {
        final byte[] from = {1, 1, 1, 1, 1};
        final byte[] to = {2};
        InputStream in = new InputStream() {
            private int i;

            @Override
            public int read() throws IOException {
                return i < from.length ? from[i++] : -1;
            }

            @Override
            public int read(byte b[], int off, int len) throws IOException {
                int read = read();
                if (read < 0) {
                    return -1;
                }
                b[off] = (byte) read;
                return 1;
            }
        };
        ReplaceFilterInputStream stream = new ReplaceFilterInputStream(in, Collections.singletonMap(from, to));
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        int read;
        while ((read = stream.read()) >= 0) {
            bOut.write(read);
        }
        in.close();
        bOut.close();
        assertArrayEquals(to, bOut.toByteArray());
    }

    private void doTest(byte[] input, byte[] expected, Map<byte[], byte[]> replacements) throws IOException {
        ReplaceFilterInputStream in = new ReplaceFilterInputStream(new ByteArrayInputStream(input), replacements);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        int read;
        while ((read = in.read()) >= 0) {
            bOut.write(read);
        }
        in.close();
        bOut.close();
        assertArrayEquals(expected, bOut.toByteArray());
    }

    private void doTest(String input, String from, String to) throws IOException {
        doTest(input, Collections.singletonMap(from, to));
    }

    private void doTest(String input, Map<String, String> replacements) throws IOException {
        String expected = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            expected = expected.replace(entry.getKey(), entry.getValue());
        }
        byte[] rawExpected = expected.getBytes();
        testSingleByteRead(rawExpected, input, replacements);
        testBufferedRead(rawExpected, input, replacements);
    }

    private void testSingleByteRead(byte[] expected, String input, Map<String, String> replacements) throws IOException {
        ReplaceFilterInputStream in = getInputStream(input, replacements);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        int read;
        while ((read = in.read()) >= 0) {
            bOut.write(read);
        }
        bOut.close();
        assertArrayEquals(expected, bOut.toByteArray());
    }

    private void testBufferedRead(byte[] expected, String input, Map<String, String> replacements) throws IOException {
        ReplaceFilterInputStream in = getInputStream(input, replacements);
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[3];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            bOut.write(buffer, 0, read);
        }
        bOut.close();
        assertArrayEquals(expected, bOut.toByteArray());
    }

    private ReplaceFilterInputStream getInputStream(String input, Map<String, String> replacements) {
        ByteArrayInputStream bIn = new ByteArrayInputStream(input.getBytes());
        Map<byte[], byte[]> rawReplacements = new HashMap<byte[], byte[]>();
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            rawReplacements.put(entry.getKey().getBytes(), entry.getValue().getBytes());
        }
        return new ReplaceFilterInputStream(bIn, rawReplacements);
    }

}
