package name.abuchen.portfolio.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import name.abuchen.portfolio.Messages;
import name.abuchen.portfolio.model.AttributeType.AmountConverter;
import name.abuchen.portfolio.model.AttributeType.Converter;
import name.abuchen.portfolio.model.AttributeType.LimitPriceConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.ProtoConverter;
import name.abuchen.portfolio.model.AttributeType.ShareConverter;
import name.abuchen.portfolio.model.LimitPrice.RelationalOperator;
import name.abuchen.portfolio.model.proto.v1.PAnyValue;
import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class AttributeTypeTest
{
    private static Locale DEFAULT_LOCALE = Locale.getDefault();

    @Before
    public void setupLocale()
    {
        DEFAULT_LOCALE = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @After
    public void resetLocale()
    {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    public void testLongShareConverter_deDE() throws Exception
    {
        Locale.setDefault(Locale.GERMANY);
        performLongConverterTest(ShareConverter.class, Values.Share,
                        Arrays.asList("  12.345,67890123  ", "12.345,6789012345", "12.345,678901236", "12345,67890123",
                                        "1.2.345,678901236", "-12.345,67890123"),
                        Arrays.asList(1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L,
                                        -1234567890123L));
    }

    @Test
    public void testLongShareConverter_deCH() throws Exception
    {
        Locale.setDefault(Locale.forLanguageTag("de-CH"));
        performLongConverterTest(ShareConverter.class, Values.Share,
                        Arrays.asList("  12’345.67890123  ", "12’345.6789012345", "12’345.678901236", "12345.67890123",
                                        "1’2’345.678901236"),
                        Arrays.asList(1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L));
    }

    @Test
    public void testLimitPriceConverter_deDE()
    {
        Locale.setDefault(Locale.GERMANY);
        performLimitPriceTests(
                        Arrays.asList("  <  123,45   ", "<= 123,45", ">= 1.123,45", " > 1.234", " > 1.2.34,5678"),
                        Arrays.asList(new LimitPrice(RelationalOperator.SMALLER, 12345000000L),
                                        new LimitPrice(RelationalOperator.SMALLER_OR_EQUAL, 12345000000L),
                                        new LimitPrice(RelationalOperator.GREATER_OR_EQUAL, 112345000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123400000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123456780000L)));

        var converter = new LimitPriceConverter();
        assertThrows(IllegalArgumentException.class, () -> converter.fromString("< 0,X"));
        assertThrows(IllegalArgumentException.class, () -> converter.fromString("< -100"));
    }

    @Test
    public void testLimitPriceConverter_deCH()
    {
        Locale.setDefault(Locale.forLanguageTag("de-CH"));
        performLimitPriceTests(
                        Arrays.asList("  <  123.45   ", "<= 123.45", ">= 1’123.45", " > 1’234", " > 1’2’34.5678"),
                        Arrays.asList(new LimitPrice(RelationalOperator.SMALLER, 12345000000L),
                                        new LimitPrice(RelationalOperator.SMALLER_OR_EQUAL, 12345000000L),
                                        new LimitPrice(RelationalOperator.GREATER_OR_EQUAL, 112345000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123400000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123456780000L)));
    }

    @Test
    public void testLongParsing()
    {
        AmountConverter converter = new AmountConverter();
        assertThat(converter.fromString("1.234,56"), is(Values.Amount.factorize(1234.56)));
        assertThat(converter.fromString("1.234,562"), is(Values.Amount.factorize(1234.56)));

        // no rounding, just cut off
        assertThat(converter.fromString("1.234,569"), is(Values.Amount.factorize(1234.56)));
    }

    @Test
    public void testPercentParsing()
    {
        PercentConverter converter = new PercentConverter();
        assertThat(converter.fromString("22%"), is(0.22));
        assertThat(converter.fromString("22"), is(0.22));
        assertThat(converter.fromString("12,34%"), is(0.1234));
        assertThat(converter.fromString("12,34567%"), is(0.1234567));

        assertThat(converter.toString(converter.fromString("22%")), is("22,00%"));
        assertThat(converter.toString(converter.fromString("22")), is("22,00%"));
    }

    private <T extends Converter & ProtoConverter> void performLongConverterTest(Class<T> converterClass,
                    Values<Long> values, List<String> validParseTexts, List<Long> parseResults)
                    throws RuntimeException, ReflectiveOperationException
    {
        assertThat(validParseTexts.size(), is(parseResults.size()));

        var converter = converterClass.getConstructor().newInstance();

        // check error handling

        assertThrows(NullPointerException.class, () -> converter.fromString(null));
        assertThat(converter.toString(null), is(""));
        assertNull(converter.fromString("   "));
        checkParseException(converter, "notanumber", Messages.MsgNotANumber, false);

        PAnyValue pav = converter.toProto(null);
        assertNotNull(pav);
        assertThat(pav.hasNull(), is(true));
        assertNull(converter.fromProto(pav));

        // check given test values

        for (int index = 0; index < validParseTexts.size(); index++)
        {
            String inputText = validParseTexts.get(index);
            Long expectedValue = parseResults.get(index);

            assertThat(converter.fromString(inputText), is(expectedValue));
            assertThat(converter.fromString(inputText),
                            is(values.factorize(expectedValue.doubleValue() / values.factor())));

            var protoValue = converter.toProto(expectedValue);
            assertNotNull(protoValue);
            assertThat(protoValue.hasInt64(), is(true));
            assertThat(converter.fromProto(protoValue), is(expectedValue));
        }
    }

    private void performLimitPriceTests(List<String> validParseTexts, List<LimitPrice> parseResults)
    {
        assertThat(validParseTexts.size(), is(parseResults.size()));

        LimitPriceConverter converter = new LimitPriceConverter();

        // check error handling

        assertThat(converter.toString(null), is(""));
        assertThrows(NullPointerException.class, () -> converter.fromString(null));
        assertNull(converter.fromString("   "));
        checkParseException(converter, "notanumber", Messages.MsgNotAComparator, false);

        PAnyValue pav = converter.toProto(null);
        assertNotNull(pav);
        assertThat(pav.hasNull(), is(true));
        assertNull(converter.fromProto(pav));

        // check given test values

        Iterator<String> it = validParseTexts.iterator();
        Iterator<LimitPrice> resultIt = parseResults.iterator();
        while (it.hasNext())
        {
            String validParseText = it.next();
            LimitPrice val = resultIt.next();
            assertThat(converter.fromString(validParseText), is(val));

            var protoValue = converter.toProto(val);
            assertNotNull(protoValue);
            assertThat(protoValue.hasString(), is(true));
            assertThat(converter.fromProto(protoValue), is(val));
        }
    }

    private void checkParseException(Converter sc, String toParse, String expectedResourceKey, boolean expectCause)
    {
        IllegalArgumentException iae;
        iae = assertThrows(IllegalArgumentException.class, () -> sc.fromString(toParse));

        var hasParameter = expectedResourceKey.contains("{");
        var expectedMessage = hasParameter ? MessageFormat.format(expectedResourceKey, toParse) : expectedResourceKey;

        assertThat(iae.getMessage(), is(expectedMessage));
        if (expectCause)
        {
            assertNotNull(iae.getCause());
            assertThat(iae.getCause().getClass().getName(), is(ParseException.class.getName()));
        }
        else
        {
            assertNull(iae.getCause());
        }
    }
}
