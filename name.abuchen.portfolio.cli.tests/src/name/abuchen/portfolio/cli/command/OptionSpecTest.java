package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * The option types that check their value as the command line is parsed:
 * a multi-choice and a currency code, each through a lone option facet.
 */
public class OptionSpecTest
{
    private static final OptionSpec TYPE = OptionSpec.choices("type", "types", "Some types.",
                    List.of("BUY", "SELL", "DIVIDENDS"));

    private static final Facet<List<String>> TYPES = Facet.option(TYPE);

    private static final Facet<Optional<String>> CURRENCY = Facet.option(OptionSpec.currency("currency", "A currency."));

    @Test
    public void anAbsentMultiChoiceIsEmpty()
    {
        assertThat(FacetHarness.run(TYPES, null).value(), is(List.of()));
    }

    @Test
    public void aMultiChoiceIsCaseInsensitiveDeduplicatedAndInPublishedOrder()
    {
        assertThat(FacetHarness.run(TYPES, null, "--type", "dividends, Buy,BUY").value(),
                        is(List.of("BUY", "DIVIDENDS")));
    }

    @Test
    public void anUnknownOrEmptyChoiceIsAnInvalidValue()
    {
        for (var value : List.of("BUY,REFUND", "BUY,", "", " "))
        {
            var result = FacetHarness.run(TYPES, null, "--type", value);
            assertThat(value, result.code(), is("invalid-option-value"));
            assertThat(result.failure().toProblem().get("option"), is("type"));
            assertThat(result.failure().toProblem().get("value"), is(value));
        }
    }

    @Test
    public void aCurrencyKnownToCoreIsTakenAsGiven()
    {
        assertThat(FacetHarness.run(CURRENCY, null).value(), is(Optional.empty()));
        assertThat(FacetHarness.run(CURRENCY, null, "--currency", "USD").value(), is(Optional.of("USD")));
    }

    @Test
    public void aCurrencyUnknownToCoreIsAnInvalidValue()
    {
        assertThat(FacetHarness.run(CURRENCY, null, "--currency", "XYZ").code(), is("invalid-option-value"));
    }
}
