package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.model.AttributeFieldType;
import name.abuchen.portfolio.util.ImageUtil;

@SuppressWarnings("nls")
public class AttributeCodecTest
{
    @Test
    public void testWireTypeTokens()
    {
        assertThat(AttributeCodec.wireType(AttributeFieldType.PERCENT), is("percent"));
        assertThat(AttributeCodec.wireType(AttributeFieldType.PERCENTPLAIN), is("number"));
        assertThat(AttributeCodec.wireType(AttributeFieldType.SHARE), is("shares"));
        assertThat(AttributeCodec.wireType(AttributeFieldType.LIMIT_PRICE), is("limit-price"));
    }

    @Test
    public void testReadableAndWritableGatesDifferByOneType()
    {
        assertThat(AttributeCodec.isReadable(AttributeFieldType.PERCENT), is(true));
        assertThat(AttributeCodec.isWritable(AttributeFieldType.PERCENT), is(true));

        // an image travels in and never back out
        assertThat(AttributeCodec.isReadable(AttributeFieldType.IMAGE), is(false));
        assertThat(AttributeCodec.isWritable(AttributeFieldType.IMAGE), is(true));

        assertThat(AttributeCodec.isReadable(AttributeFieldType.BOOKMARK), is(false));
        assertThat(AttributeCodec.isWritable(AttributeFieldType.BOOKMARK), is(false));
        assertThat(AttributeCodec.isReadable(AttributeFieldType.LIMIT_PRICE), is(false));
        assertThat(AttributeCodec.isWritable(AttributeFieldType.LIMIT_PRICE), is(false));
    }

    /** a genuine 32x32 PNG (the application ships it in kommer.xml) */
    private static final String PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAACXBIWXMAAA7EAAAOxAGVKw4bAAAA9klEQVRYw+1VOxKCMBB9oKXjeRibdJ7ADOewRiupHa/BwAUsU+l5PECgQdmE8A9QmDezA8OE3bdv9wHw7/DIvQ8AERcJgNOIRGGcsuzCxdHz8bkl7N3xivwV1R+OQV5d97nE8xqKwJBTkqi61lWYihzYERI0p0+iVlBhNhTlLOV3phoJaYgaASvdNyhxaDqjK2AdXSRsK2DM0UZi2yfBEMQpywBsepCcx4aGPK02tKnAOeKCtx3wgFecsgclRglMXcKg60Cp0r2pY+s27IPZbTiEwOoKLEVC+RfoLlhiDEqN1XeAKrDUDihfQjcCZ0O3A86GbgkdCrigXo85i+2YAAAAAElFTkSuQmCC";
    private static final String DATA_URL = "data:image/png;base64," + PNG_BASE64;

    @Test
    public void testImageDecodesToTheRenderedForm()
    {
        // raw base64 in, the prefixed data-url the application renders from out
        assertThat(AttributeCodec.decode(AttributeFieldType.IMAGE, new JsonPrimitive(PNG_BASE64)), is(DATA_URL));
        // the prefixed form in, the very same bytes out: sending it twice changes nothing
        assertThat(AttributeCodec.decode(AttributeFieldType.IMAGE, new JsonPrimitive(DATA_URL)), is(DATA_URL));
    }

    @Test
    public void testImageRejectsTextThatIsNoPng()
    {
        try
        {
            AttributeCodec.decode(AttributeFieldType.IMAGE, new JsonPrimitive("not a logo"));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected: no base64, no PNG
        }

        try
        {
            // valid base64 ("hello"), but no PNG behind it
            AttributeCodec.decode(AttributeFieldType.IMAGE, new JsonPrimitive("aGVsbG8="));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected
        }
    }

    /**
     * A PNG is not only its magic. The header that follows says how much the
     * renderer will be asked to lay out, and the application reads these bytes
     * back with {@code ImageIO} - which allocates by that claim, not by the
     * bytes that were sent.
     */
    @Test
    public void testImageRefusesAHeaderADecoderWouldRegret()
    {
        // the magic of a PNG, and a header that stops mid-way through it
        try
        {
            decodeImage(Arrays.copyOf(pngClaiming(1, 1), 20));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            assertThat(e.getMessage(), is("the bytes are not a PNG image"));
        }

        // a well-formed header, a few bytes behind it, claiming an image no
        // desktop is meant to hold
        try
        {
            decodeImage(pngClaiming(100_000, 100_000));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            assertThat(e.getMessage(), containsString("100000"));
        }

        // and a header of a size the renderer will lay out without a second
        // thought: accepted, even though hardly any bytes follow it
        assertThat(decodeImage(pngClaiming(64, 64)), startsWith(ImageUtil.BASE64PREFIX));
    }

    /** the smallest PNG header a decoder reads, declaring the given size */
    private static byte[] pngClaiming(int width, int height)
    {
        var header = ByteBuffer.allocate(33);
        header.put(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        header.putInt(13); // the IHDR chunk holds thirteen bytes of header
        header.put(new byte[] {'I', 'H', 'D', 'R'});
        header.putInt(width).putInt(height);
        header.put(new byte[] {8, 6, 0, 0, 0}); // depth, colour, compression, filter, interlace
        header.putInt(0); // and its CRC, whatever it happens to be
        return header.array();
    }

    private static String decodeImage(byte[] bytes)
    {
        return (String) AttributeCodec.decode(AttributeFieldType.IMAGE,
                        new JsonPrimitive(Base64.getEncoder().encodeToString(bytes)));
    }

    @Test
    public void testImageRejectsMoreThanTheCap()
    {
        var padded = new byte[AttributeCodec.MAX_IMAGE_BYTES + 1];
        var magic = new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        System.arraycopy(magic, 0, padded, 0, magic.length);

        try
        {
            AttributeCodec.decode(AttributeFieldType.IMAGE,
                            new JsonPrimitive(java.util.Base64.getEncoder().encodeToString(padded)));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected
        }
    }

    @Test
    public void testEncodeAmountIsPlainDecimal()
    {
        // Amount stores cents (precision 2): 1234 -> 12.34
        assertThat(AttributeCodec.encode(AttributeFieldType.AMOUNT, Long.valueOf(1234L)).getAsString(), is("12.34"));
    }

    @Test
    public void testDecodeAmountScalesToLong()
    {
        var value = JsonParser.parseString("12.34");
        assertThat(AttributeCodec.decode(AttributeFieldType.AMOUNT, value), is(Long.valueOf(1234L)));
    }

    @Test
    public void testDecodeAmountRoundsHalfUp()
    {
        // 12.999 * 100 = 1299.9 -> HALF_UP -> 1300
        var value = JsonParser.parseString("12.999");
        assertThat(AttributeCodec.decode(AttributeFieldType.AMOUNT, value), is(Long.valueOf(1300L)));
    }

    @Test
    public void testPercentRoundTripsAsStoredFraction()
    {
        // a 0.7% TER is stored as the fraction 0.007 and emitted as-is
        assertThat(AttributeCodec.encode(AttributeFieldType.PERCENT, Double.valueOf(0.007d)).getAsString(), is("0.007"));
        var value = JsonParser.parseString("0.007");
        assertThat(AttributeCodec.decode(AttributeFieldType.PERCENT, value), is(Double.valueOf(0.007d)));
    }

    @Test
    public void testDateRoundTripsAsIso()
    {
        assertThat(AttributeCodec.encode(AttributeFieldType.DATE, LocalDate.of(2026, 7, 22)).getAsString(),
                        is("2026-07-22"));
        var value = new JsonPrimitive("2026-07-22");
        assertThat(AttributeCodec.decode(AttributeFieldType.DATE, value), is(LocalDate.of(2026, 7, 22)));
    }

    @Test
    public void testDecodeRejectsWrongType()
    {
        try
        {
            AttributeCodec.decode(AttributeFieldType.AMOUNT, new JsonPrimitive("not-a-number"));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected
        }
    }

    @Test
    public void testQuoteRoundTripsAtPrecisionEight()
    {
        // Quote stores 8 decimal places: 12345678 -> 0.12345678
        assertThat(AttributeCodec.encode(AttributeFieldType.QUOTE, Long.valueOf(12345678L)).getAsString(),
                        is("0.12345678"));
        var value = JsonParser.parseString("0.12345678");
        assertThat(AttributeCodec.decode(AttributeFieldType.QUOTE, value), is(Long.valueOf(12345678L)));
    }

    @Test
    public void testShareRoundTripsAtPrecisionEight()
    {
        // Share stores 8 decimal places: 100000000 -> 1
        assertThat(AttributeCodec.encode(AttributeFieldType.SHARE, Long.valueOf(100000000L)).getAsString(), is("1"));
        var value = JsonParser.parseString("1");
        assertThat(AttributeCodec.decode(AttributeFieldType.SHARE, value), is(Long.valueOf(100000000L)));
    }

    @Test
    public void testAmountPlainIsIndistinguishableFromAmount()
    {
        assertThat(AttributeCodec.wireType(AttributeFieldType.AMOUNTPLAIN),
                        is(AttributeCodec.wireType(AttributeFieldType.AMOUNT)));
    }

    @Test
    public void testAmountPlainRoundTripsAtPrecisionTwo()
    {
        var value = JsonParser.parseString("12.34");
        assertThat(AttributeCodec.decode(AttributeFieldType.AMOUNTPLAIN, value), is(Long.valueOf(1234L)));
        assertThat(AttributeCodec.encode(AttributeFieldType.AMOUNTPLAIN, Long.valueOf(1234L)).getAsString(),
                        is("12.34"));
    }

    @Test
    public void testStringRoundTrips()
    {
        assertThat(AttributeCodec.encode(AttributeFieldType.STRING, "hello world").getAsString(), is("hello world"));
        var value = new JsonPrimitive("hello world");
        assertThat(AttributeCodec.decode(AttributeFieldType.STRING, value), is("hello world"));
    }

    @Test
    public void testBooleanRoundTrips()
    {
        assertThat(AttributeCodec.encode(AttributeFieldType.BOOLEAN, Boolean.TRUE).getAsBoolean(), is(true));
        var value = new JsonPrimitive(Boolean.TRUE);
        assertThat(AttributeCodec.decode(AttributeFieldType.BOOLEAN, value), is(Boolean.TRUE));
    }

    @Test
    public void testDecodeRejectsOutOfRangeNumber()
    {
        try
        {
            AttributeCodec.decode(AttributeFieldType.AMOUNT, JsonParser.parseString("1e30"));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected
        }
    }

    @Test
    public void testDecodeRejectsNumericStringForAmount()
    {
        try
        {
            // a JSON string, even if it looks numeric, is not a JSON number
            AttributeCodec.decode(AttributeFieldType.AMOUNT, new JsonPrimitive("12.34"));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected
        }
    }

    @Test
    public void testDecodeRejectsPercentThatWouldOverflowToInfinity()
    {
        try
        {
            // 1e400 exceeds Double's range -> BigDecimal#doubleValue rounds to Infinity
            AttributeCodec.decode(AttributeFieldType.PERCENT, JsonParser.parseString("1e400"));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected
        }
    }

    @Test
    public void testDecodeRejectsPercentPlainThatWouldOverflowToInfinity()
    {
        try
        {
            AttributeCodec.decode(AttributeFieldType.PERCENTPLAIN, JsonParser.parseString("1e400"));
            Assert.fail("expected InvalidValueException");
        }
        catch (AttributeCodec.InvalidValueException e)
        {
            // expected
        }
    }
}
