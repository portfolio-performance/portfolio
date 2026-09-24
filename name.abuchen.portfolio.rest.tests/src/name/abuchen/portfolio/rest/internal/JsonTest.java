package name.abuchen.portfolio.rest.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;

import name.abuchen.portfolio.money.Values;

@SuppressWarnings("nls")
public class JsonTest
{
    private static Json json(String body)
    {
        return new Json(JsonParser.parseString(body).getAsJsonObject());
    }

    private static String codeOf(Json json)
    {
        assertThat(json.errors().size(), is(1));
        return json.errors().get(0).code();
    }

    private static List<String> codes(Json json)
    {
        return json.errors().stream().map(ApiException.FieldError::code).toList();
    }

    @Test
    public void testDecimalFromNumberKeepsLiteral()
    {
        var json = json("{\"amount\": 12.30}");
        var value = json.decimal("amount", Values.Amount.precision());

        assertThat(value, is(new BigDecimal("12.30")));
        assertThat(json.errors(), is(empty()));
    }

    @Test
    public void testDecimalFromString()
    {
        var json = json("{\"shares\": \"0.12345678\"}");
        assertThat(json.decimal("shares", Values.Share.precision()), is(new BigDecimal("0.12345678")));
        assertThat(json.errors(), is(empty()));
    }

    @Test
    public void testDecimalDoesNotLosePrecisionOfLargeNumbers()
    {
        // a double cannot represent this value exactly
        var json = json("{\"amount\": 1234567890123.01}");
        assertThat(json.decimal("amount", 2), is(new BigDecimal("1234567890123.01")));
    }

    @Test
    public void testDecimalWithTooManyDecimalsIsInvalidValue()
    {
        var json = json("{\"amount\": \"12.345\"}");
        assertThat(json.decimal("amount", Values.Amount.precision()), is(nullValue()));
        assertThat(codeOf(json), is("invalid-value"));
    }

    @Test
    public void testTrailingZerosDoNotCountAsDecimals()
    {
        var json = json("{\"amount\": \"12.3400\"}");
        assertThat(json.decimal("amount", 2), is(new BigDecimal("12.3400")));
        assertThat(json.errors(), is(empty()));
    }

    @Test
    public void testExponentNotationIsRejected()
    {
        var json = json("{\"a\": 1e2, \"b\": \"1E2\"}");
        assertThat(json.decimal("a", 2), is(nullValue()));
        assertThat(json.decimal("b", 2), is(nullValue()));
        assertThat(codes(json), contains("invalid-value", "invalid-value"));
    }

    @Test
    public void testNonNumericDecimalIsRejected()
    {
        var json = json("{\"a\": \"12,34\", \"b\": true, \"c\": {}}");
        json.decimal("a", 2);
        json.decimal("b", 2);
        json.decimal("c", 2);
        assertThat(codes(json), contains("invalid-value", "invalid-type", "invalid-type"));
    }

    @Test
    public void testDecimalTooLargeIsRejected()
    {
        var json = json("{\"amount\": \"999999999999999999\"}");
        assertThat(json.decimal("amount", 2), is(nullValue()));
        assertThat(codeOf(json), is("invalid-value"));
    }

    @Test
    public void testNegativeDecimal()
    {
        var json = json("{\"amount\": -5.5}");
        assertThat(json.decimal("amount", 2), is(new BigDecimal("-5.5")));
    }

    @Test
    public void testRequireDecimalReportsMissing()
    {
        var json = json("{}");
        assertThat(json.requireDecimal("amount", 2), is(nullValue()));
        assertThat(codeOf(json), is("required"));

        json = json("{\"amount\": null}");
        json.requireDecimal("amount", 2);
        assertThat(codeOf(json), is("required"));
    }

    @Test
    public void testStrings()
    {
        var json = json("{\"a\": \"x\", \"b\": 1, \"c\": \"  \", \"d\": \"toolong\"}");

        assertThat(json.optString("a"), is("x"));
        assertThat(json.optString("missing"), is(nullValue()));
        assertThat(json.optString("b"), is(nullValue()));
        assertThat(json.requireString("c"), is(nullValue()));
        assertThat(json.optString("d", 3), is(nullValue()));
        assertThat(json.requireString("missing2"), is(nullValue()));

        assertThat(codes(json), contains("invalid-type", "required", "too-long", "required"));
    }

    @Test
    public void testRequireStringDoesNotDoubleReportInvalidType()
    {
        var json = json("{\"a\": 1}");
        json.requireString("a");
        assertThat(codeOf(json), is("invalid-type"));
    }

    @Test
    public void testDates()
    {
        var json = json("{\"d\": \"2026-03-02\", \"t\": \"2026-03-02T09:30\", \"dt\": \"2026-03-02\", "
                        + "\"bad\": \"2026-02-30\", \"bad2\": \"02.03.2026\"}");

        assertThat(json.date("d"), is(LocalDate.of(2026, 3, 2)));
        assertThat(json.dateTime("t"), is(LocalDateTime.of(2026, 3, 2, 9, 30)));
        assertThat(json.dateTime("dt"), is(LocalDateTime.of(2026, 3, 2, 0, 0)));
        assertThat(json.date("bad"), is(nullValue()));
        assertThat(json.dateTime("bad2"), is(nullValue()));
        assertThat(codes(json), contains("invalid-value", "invalid-value"));

        var missing = json("{}");
        missing.requireDate("d");
        missing.requireDateTime("t");
        assertThat(codes(missing), contains("required", "required"));
    }

    @Test
    public void testBool()
    {
        var json = json("{\"a\": true, \"b\": \"true\"}");
        assertThat(json.bool("a"), is(true));
        assertThat(json.bool("b"), is(nullValue()));
        assertThat(json.bool("c"), is(nullValue()));
        assertThat(codeOf(json), is("invalid-type"));
    }

    @Test
    public void testRejectUnknownFields()
    {
        var json = json("{\"name\": \"x\", \"typo\": 1, \"clientRef\": \"r\"}");
        json.optString("name");
        json.ignore("clientRef");
        json.rejectUnknownFields();

        assertThat(json.errors().size(), is(1));
        assertThat(json.errors().get(0).field(), is("typo"));
        assertThat(json.errors().get(0).code(), is("unknown-field"));
    }

    @Test
    public void testThrowIfErrorsReportsAllAt422()
    {
        var json = json("{}");
        json.requireString("a");
        json.requireDecimal("b", 2);

        try
        {
            json.throwIfErrors();
            Assert.fail("expected ApiException");
        }
        catch (ApiException e)
        {
            assertThat(e.getStatus(), is(422));
            assertThat(e.getErrors().size(), is(2));
        }
    }
}
