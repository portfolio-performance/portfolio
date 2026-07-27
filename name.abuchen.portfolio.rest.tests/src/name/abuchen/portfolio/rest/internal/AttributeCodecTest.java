package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import name.abuchen.portfolio.model.AttributeFieldType;

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
    public void testSupportedExcludesCompound()
    {
        assertThat(AttributeCodec.isSupported(AttributeFieldType.PERCENT), is(true));
        assertThat(AttributeCodec.isSupported(AttributeFieldType.IMAGE), is(false));
        assertThat(AttributeCodec.isSupported(AttributeFieldType.BOOKMARK), is(false));
        assertThat(AttributeCodec.isSupported(AttributeFieldType.LIMIT_PRICE), is(false));
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
