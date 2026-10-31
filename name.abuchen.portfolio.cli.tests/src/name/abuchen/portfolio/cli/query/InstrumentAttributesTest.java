package name.abuchen.portfolio.cli.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.cli.CliFixture;
import name.abuchen.portfolio.junit.SecurityBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.AttributeType.AmountConverter;
import name.abuchen.portfolio.model.AttributeType.AmountPlainConverter;
import name.abuchen.portfolio.model.AttributeType.BookmarkConverter;
import name.abuchen.portfolio.model.AttributeType.BooleanConverter;
import name.abuchen.portfolio.model.AttributeType.Converter;
import name.abuchen.portfolio.model.AttributeType.DateConverter;
import name.abuchen.portfolio.model.AttributeType.ImageConverter;
import name.abuchen.portfolio.model.AttributeType.LimitPriceConverter;
import name.abuchen.portfolio.model.AttributeType.PercentConverter;
import name.abuchen.portfolio.model.AttributeType.PercentPlainConverter;
import name.abuchen.portfolio.model.AttributeType.QuoteConverter;
import name.abuchen.portfolio.model.AttributeType.ShareConverter;
import name.abuchen.portfolio.model.AttributeType.StringConverter;
import name.abuchen.portfolio.model.Attributable;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Security;

/** The typed encoding of instrument attributes. */
public class InstrumentAttributesTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final CliFixture cli = new CliFixture(folder);

    private final Client client = new Client();
    private final Security security = new SecurityBuilder().addTo(client);

    private AttributeType addType(String id, String name, Class<?> valueType, Class<? extends Converter> converter,
                    Class<? extends Attributable> target)
    {
        var type = new AttributeType(id);
        type.setName(name);
        type.setColumnLabel(name);
        type.setTarget(target);
        type.setType(valueType);
        type.setConverter(converter);
        client.getSettings().addAttributeType(type);
        return type;
    }

    private AttributeType addType(String id, String name, Class<?> valueType, Class<? extends Converter> converter)
    {
        return addType(id, name, valueType, converter, Security.class);
    }

    private JsonArray attributes() throws IOException
    {
        var run = cli.save(client).run("instruments");
        assertThat(run.stdout, run.exitCode, is(0));

        var items = run.items();
        assertThat(items.size(), is(1));
        return items.get(0).getAsJsonObject().getAsJsonArray("attributes");
    }

    private JsonObject only(JsonArray attributes)
    {
        assertThat(attributes.toString(), attributes.size(), is(1));
        return attributes.get(0).getAsJsonObject();
    }

    private void assertEncoded(JsonObject attribute, String id, String name, String type)
    {
        assertThat(attribute.keySet(), is(Set.of("id", "name", "type", "value")));
        assertThat(attribute.get("id").getAsString(), is(id));
        assertThat(attribute.get("name").getAsString(), is(name));
        assertThat(attribute.get("type").getAsString(), is(type));
    }

    @Test
    public void securityWithoutAttributesGetsAnEmptyArray() throws IOException
    {
        assertThat(attributes().size(), is(0));
    }

    @Test
    public void stringAttribute() throws IOException
    {
        var type = addType("custom-string", "Sector", String.class, StringConverter.class);
        security.getAttributes().put(type, "Technology");

        var attribute = only(attributes());

        assertEncoded(attribute, "custom-string", "Sector", "string");
        assertThat(attribute.get("value").getAsString(), is("Technology"));
    }

    @Test
    public void booleanAttribute() throws IOException
    {
        var type = addType("flag", "Flagged", Boolean.class, BooleanConverter.class);
        security.getAttributes().put(type, Boolean.TRUE);

        var attribute = only(attributes());

        assertEncoded(attribute, "flag", "Flagged", "boolean");
        assertThat(attribute.getAsJsonPrimitive("value").isBoolean(), is(true));
        assertThat(attribute.get("value").getAsBoolean(), is(true));
    }

    @Test
    public void dateAttributeIsIsoDate() throws IOException
    {
        var type = addType("since", "Since", LocalDate.class, DateConverter.class);
        security.getAttributes().put(type, LocalDate.of(2024, 3, 5));

        var attribute = only(attributes());

        assertEncoded(attribute, "since", "Since", "date");
        assertThat(attribute.get("value").getAsString(), is("2024-03-05"));
    }

    @Test
    public void amountAttributesAreDecimalsScaledByTheAmountPrecision() throws IOException
    {
        var amount = addType("a1", "Amount", Long.class, AmountConverter.class);
        var plain = addType("a2", "Plain amount", Long.class, AmountPlainConverter.class);
        security.getAttributes().put(amount, 123456L);
        security.getAttributes().put(plain, 7050L);

        var attributes = attributes();

        assertThat(attributes.size(), is(2));
        assertEncoded(attributes.get(0).getAsJsonObject(), "a1", "Amount", "amount");
        assertThat(attributes.get(0).getAsJsonObject().get("value").getAsBigDecimal().compareTo(new BigDecimal("1234.56")),
                        is(0));
        assertEncoded(attributes.get(1).getAsJsonObject(), "a2", "Plain amount", "amount");
        assertThat(attributes.get(1).getAsJsonObject().get("value").getAsBigDecimal().compareTo(new BigDecimal("70.5")),
                        is(0));
    }

    @Test
    public void quoteAttributeIsScaledByTheQuotePrecision() throws IOException
    {
        var type = addType("q", "Quote", Long.class, QuoteConverter.class);
        security.getAttributes().put(type, 123456789L);

        var attribute = only(attributes());

        assertEncoded(attribute, "q", "Quote", "quote");
        assertThat(attribute.getAsJsonPrimitive("value").isNumber(), is(true));
        assertThat(attribute.get("value").getAsBigDecimal().compareTo(new BigDecimal("1.23456789")), is(0));
    }

    @Test
    public void sharesAttributeIsScaledByTheSharePrecision() throws IOException
    {
        var type = addType("s", "Shares", Long.class, ShareConverter.class);
        security.getAttributes().put(type, 150_000_000L);

        var attribute = only(attributes());

        assertEncoded(attribute, "s", "Shares", "shares");
        assertThat(attribute.get("value").getAsBigDecimal().compareTo(new BigDecimal("1.5")), is(0));
    }

    @Test
    public void percentIsAFractionAndNumberIsUnscaled() throws IOException
    {
        var percent = addType("p", "TER", Double.class, PercentConverter.class);
        var number = addType("n", "Ratio", Double.class, PercentPlainConverter.class);
        security.getAttributes().put(percent, 0.0022);
        security.getAttributes().put(number, 12.5);

        var attributes = attributes();

        assertThat(attributes.size(), is(2));
        assertEncoded(attributes.get(0).getAsJsonObject(), "p", "TER", "percent");
        assertThat(attributes.get(0).getAsJsonObject().get("value").getAsDouble(), is(0.0022));
        assertEncoded(attributes.get(1).getAsJsonObject(), "n", "Ratio", "number");
        assertThat(attributes.get(1).getAsJsonObject().get("value").getAsDouble(), is(12.5));
    }

    @Test
    public void attributesFollowTheOrderOfTheAttributeTypes() throws IOException
    {
        var first = addType("t1", "First", String.class, StringConverter.class);
        var second = addType("t2", "Second", String.class, StringConverter.class);
        var third = addType("t3", "Third", String.class, StringConverter.class);
        security.getAttributes().put(third, "c");
        security.getAttributes().put(first, "a");
        security.getAttributes().put(second, "b");

        var ids = new ArrayList<String>();
        attributes().forEach(a -> ids.add(a.getAsJsonObject().get("id").getAsString()));

        assertThat(ids, contains("t1", "t2", "t3"));
    }

    @Test
    public void unsupportedTypesAreSkipped() throws IOException
    {
        var logo = client.getSettings().getAttributeTypes().filter(t -> "logo".equals(t.getId())
                        && t.supports(Security.class)).findAny().orElseThrow();
        security.getAttributes().put(logo, "data:image/png;base64,AAAA");

        var bookmark = addType("link", "Link", String.class, BookmarkConverter.class);
        security.getAttributes().put(bookmark, "https://example.com");

        var image = addType("img", "Image", String.class, ImageConverter.class);
        security.getAttributes().put(image, "data:image/png;base64,AAAA");

        var limit = addType("lp", "Limit", String.class, LimitPriceConverter.class);
        security.getAttributes().put(limit, "limit");

        var kept = addType("ok", "Kept", String.class, StringConverter.class);
        security.getAttributes().put(kept, "yes");

        assertEncoded(only(attributes()), "ok", "Kept", "string");
    }

    @Test
    public void orphanedValuesAndTypesForOtherEntitiesAreSkipped() throws IOException
    {
        var removed = new AttributeType("removed");
        removed.setName("Removed");
        security.getAttributes().put(removed, "orphan");

        var accountOnly = addType("acc", "Account only", String.class, StringConverter.class, Account.class);
        security.getAttributes().put(accountOnly, "wrong target");

        assertThat(attributes().size(), is(0));
    }

    @Test
    public void unsetBlankAndMismatchedValuesAreSkippedWithoutFailing() throws IOException
    {
        var unset = addType("unset", "Unset", String.class, StringConverter.class);
        security.getAttributes().put(unset, null);

        var blank = addType("blank", "Blank", String.class, StringConverter.class);
        security.getAttributes().put(blank, "  ");

        var stringHoldingNumber = addType("m1", "Mismatch 1", String.class, StringConverter.class);
        security.getAttributes().put(stringHoldingNumber, 5L);

        var amountHoldingString = addType("m2", "Mismatch 2", Long.class, AmountConverter.class);
        security.getAttributes().put(amountHoldingString, "12.50");

        var dateHoldingString = addType("m3", "Mismatch 3", LocalDate.class, DateConverter.class);
        security.getAttributes().put(dateHoldingString, "2024-01-01");

        var booleanHoldingString = addType("m4", "Mismatch 4", Boolean.class, BooleanConverter.class);
        security.getAttributes().put(booleanHoldingString, "true");

        var nan = addType("nan", "Not a number", Double.class, PercentConverter.class);
        security.getAttributes().put(nan, Double.NaN);

        var infinite = addType("inf", "Infinite", Double.class, PercentPlainConverter.class);
        security.getAttributes().put(infinite, Double.POSITIVE_INFINITY);

        var kept = addType("ok", "Kept", String.class, StringConverter.class);
        security.getAttributes().put(kept, "yes");

        assertEncoded(only(attributes()), "ok", "Kept", "string");
    }

    @Test
    public void decimalsAreWrittenAsPlainNumbersNeverInScientificNotation() throws IOException
    {
        var quote = addType("q", "Quote", Long.class, QuoteConverter.class);
        var shares = addType("s", "Shares", Long.class, ShareConverter.class);
        var zero = addType("z", "Zero", Long.class, ShareConverter.class);
        security.getAttributes().put(quote, 1L);
        security.getAttributes().put(shares, 12L);
        security.getAttributes().put(zero, 0L);

        var run = cli.save(client).run("instruments");

        assertThat(run.stdout.contains("\"value\":0.00000001"), is(true));
        assertThat(run.stdout.contains("\"value\":0.00000012"), is(true));
        assertThat(run.stdout.contains("\"value\":0.00000000"), is(true));
        assertThat(run.stdout.contains("E-"), is(false));
    }

    @Test
    public void attributeValuesConformToTheDeclaredSchema() throws IOException
    {
        var type = addType("custom-string", "Sector", String.class, StringConverter.class);
        security.getAttributes().put(type, "Technology");
        var date = addType("since", "Since", LocalDate.class, DateConverter.class);
        security.getAttributes().put(date, LocalDate.of(2024, 3, 5));
        var flag = addType("flag", "Flagged", Boolean.class, BooleanConverter.class);
        security.getAttributes().put(flag, Boolean.TRUE);
        var amount = addType("amount", "Amount", Long.class, AmountConverter.class);
        security.getAttributes().put(amount, 100L);

        var run = cli.save(client).run("instruments");

        // the response contract that CliRun checks includes the declared schema
        assertThat(run.stdout, run.exitCode, is(0));
    }
}
