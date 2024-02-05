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
import name.abuchen.portfolio.model.AttributeType.LongConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.LimitPrice.RelationalOperator;
import name.abuchen.portfolio.model.proto.v1.PAnyValue;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.money.ValuesBuilder;

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
        ValuesBuilder.initQuoteValuesDecimalFormat();
        AttributeType.initPatterns();
    }

    @Test
    public void testLongShareConverter_deDE() throws Exception
    {
        Locale.setDefault(Locale.GERMANY);
        ValuesBuilder.initQuoteValuesDecimalFormat();
        AttributeType.initPatterns();
        performLongConverterTest(
                        Values.Share,
                        Arrays.asList("  12.345,67890123  ", "12.345,6789012345", "12.345,678901236", "12345,67890123",
                                        "1.2.345,678901236", "-12.345,67890123"),
                        Arrays.asList(1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L,
                                        -1234567890123L),
                        Arrays.asList("12.345,67890123", "12.345,67890123", "12.345,67890123", "12.345,67890123",
                                        "12.345,67890123", "-12.345,67890123"));
    }

    @Test
    public void testLongShareConverter_deCH() throws Exception
    {
        Locale.setDefault(new Locale("de", "CH"));
        ValuesBuilder.initQuoteValuesDecimalFormat();
        AttributeType.initPatterns();
        performLongConverterTest(
                        Values.Share,
                        Arrays.asList("  12’345.67890123  ", "12’345.6789012345", "12’345.678901236", "12345.67890123",
                                        "1’2’345.678901236"),
                        Arrays.asList(1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L, 1234567890123L),
                        Arrays.asList("12’345.67890123", "12’345.67890123", "12’345.67890123", "12’345.67890123",
                                        "12’345.67890123"));
    }

    @Test
    public void testLimitPriceConverter_deDE()
    {
        Locale.setDefault(Locale.GERMANY);
        ValuesBuilder.initQuoteValuesDecimalFormat();
        AttributeType.initPatterns();

        performLimitPriceTests(
                        Arrays.asList("  <  123,45   ", "<= 123,45", ">= 1.123,45", " > 1.234", " > 1.2.34,5678"),
                        Arrays.asList(new LimitPrice(RelationalOperator.SMALLER, 12345000000L),
                                        new LimitPrice(RelationalOperator.SMALLER_OR_EQUAL, 12345000000L),
                                        new LimitPrice(RelationalOperator.GREATER_OR_EQUAL, 112345000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123400000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123456780000L)),
                        Arrays.asList("< 123,45", "<= 123,45", ">= 1.123,45", "> 1.234,00", "> 1.234,5678"));
    }

    @Test
    public void testLimitPriceConverter_deCH()
    {
        Locale.setDefault(new Locale("de", "CH"));
        ValuesBuilder.initQuoteValuesDecimalFormat();
        AttributeType.initPatterns();

        performLimitPriceTests(
                        Arrays.asList("  <  123.45   ", "<= 123.45", ">= 1’123.45", " > 1’234", " > 1’2’34.5678"),
                        Arrays.asList(new LimitPrice(RelationalOperator.SMALLER, 12345000000L),
                                        new LimitPrice(RelationalOperator.SMALLER_OR_EQUAL, 12345000000L),
                                        new LimitPrice(RelationalOperator.GREATER_OR_EQUAL, 112345000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123400000000L),
                                        new LimitPrice(RelationalOperator.GREATER, 123456780000L)),
                        Arrays.asList("< 123.45", "<= 123.45", ">= 1’123.45", "> 1’234.00", "> 1’234.5678"));
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

    private void performLongConverterTest(Values<Long> values, List<String> validParseTexts, List<Long> parseResults,
                    List<String> toStringResult)
    {
        Iterator<String> it = validParseTexts.iterator();
        Iterator<Long> valIt = parseResults.iterator();
        Iterator<String> toStringIt = toStringResult.iterator();
        while (it.hasNext())
        {
            String validParseText = it.next();
            System.out.println(validParseText);
            Long val = valIt.next();
            LongConverter sc = new LongConverter(ValuesBuilder.createNumberValues(values));
            assertThat(sc.toString(null), is(""));
            assertThat(sc.toString(val), is(toStringIt.next()));
            assertThrows(NullPointerException.class, () -> sc.fromString(null));
            assertNull(sc.fromString("   "));
            assertThat(sc.fromString(validParseText), is(val));
            assertThat(sc.fromString(validParseText), is(values.factorize(val.doubleValue() / values.factor())));
            checkParseException(sc, "notanumber", Messages.MsgNotANumber, false);

            PAnyValue pav;
            pav = sc.toProto(null);
            assertNotNull(pav);
            assertThat(pav.hasNull(), is(true));
            assertNull(sc.fromProto(pav));
            pav = sc.toProto(val);
            assertNotNull(pav);
            assertThat(pav.hasInt64(), is(true));
            assertThat(sc.fromProto(pav), is(val));
        }
    }

    private void performLimitPriceTests(List<String> validParseTexts, List<LimitPrice> parseResults,
                    List<String> toStringResult)
    {
        Iterator<String> it = validParseTexts.iterator();
        Iterator<LimitPrice> resultIt = parseResults.iterator();
        Iterator<String> toStringIt = toStringResult.iterator();
        while (it.hasNext())
        {
            String validParseText = it.next();
            System.out.println(validParseText);
            LimitPrice val = resultIt.next();
            LimitPriceConverter sc = new LimitPriceConverter();
            assertThat(sc.toString(null), is(""));
            assertThat(sc.fromString(validParseText), is(val));
            assertThat(sc.toString(val), is(toStringIt.next()));
            assertThrows(NullPointerException.class, () -> sc.fromString(null));
            assertNull(sc.fromString("   "));
            checkParseException(sc, "notanumber", Messages.MsgNotAComparator, false);

            PAnyValue pav;
            pav = sc.toProto(null);
            assertNotNull(pav);
            assertThat(pav.hasNull(), is(true));
            assertNull(sc.fromProto(pav));
            pav = sc.toProto(val);
            assertNotNull(pav);
            assertThat(pav.hasString(), is(true));
            assertThat(sc.fromProto(pav), is(val));
        }
    }

    private void checkParseException(Converter sc, String toParse, String expectedResourceKey, boolean expectCause)
    {
        IllegalArgumentException iae;
        iae = assertThrows(IllegalArgumentException.class, () -> sc.fromString(toParse));
        assertThat(iae.getMessage(), is(MessageFormat.format(expectedResourceKey, toParse)));
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
