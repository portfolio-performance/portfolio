package name.abuchen.portfolio.cli.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.Test;

import name.abuchen.portfolio.cli.CliRun;
import name.abuchen.portfolio.cli.Commands;

/**
 * An agent once quoted holdings in ILA (agorot) as if they were ILS. The
 * figures were right; only the field descriptions did not say that an
 * instrument's currency can be a minor currency unit.
 */
public class MinorUnitDescriptionsTest
{
    private static final Set<String> MONEY_FIELDS = Set.of("amount", "currencyCode", "value", "forexAmount",
                    "forexCurrencyCode", "targetAmount", "targetCurrencyCode");

    /** Response fields that can be in the currency of an instrument, as "command:path". */
    private static final Set<String> INSTRUMENT_CURRENCY = Set.of( //
                    "holdings:holdings[].items[].amount", //
                    "holdings:holdings[].items[].currencyCode", //
                    "holdings:holdings[].items[].price.value", //
                    "prices:items[].value", //
                    "prices:latest.value", //
                    "prices:instrument.currencyCode", //
                    "instruments:items[].currencyCode", //
                    "transactions:items[].amount", //
                    "transactions:items[].currencyCode", //
                    "transactions:items[].units[].amount", //
                    "transactions:items[].units[].currencyCode", //
                    "transactions:items[].units[].forexAmount", //
                    "transactions:items[].units[].forexCurrencyCode");

    /**
     * Response fields that are never in an instrument's currency: the
     * reporting currency, or the currency of a cash account (the target of a
     * transfer, a cash account's own currency).
     */
    private static final Set<String> OTHER_CURRENCY = Set.of( //
                    "holdings:holdings[].total.amount", //
                    "holdings:holdings[].total.currencyCode", //
                    "holdings:holdings[].items[].converted.amount", //
                    "holdings:holdings[].items[].converted.currencyCode", //
                    "holdings:cash.items[].amount", //
                    "holdings:cash.items[].currencyCode", //
                    "holdings:cash.items[].converted.amount", //
                    "holdings:cash.items[].converted.currencyCode", //
                    "holdings:cash.total.amount", //
                    "holdings:cash.total.currencyCode", //
                    "cash-accounts:items[].currencyCode", //
                    "transactions:items[].converted.amount", //
                    "transactions:items[].converted.currencyCode", //
                    "transactions:items[].targetAmount", //
                    "transactions:items[].targetCurrencyCode", //
                    "performance:currencyCode", //
                    "allocation:items[].value", //
                    "instruments:items[].attributes[].value");

    @Test
    public void everyFieldThatCanBeInAnInstrumentCurrencyExplainsMinorUnitsAndNamesConverted()
    {
        var problems = new ArrayList<String>();
        var found = new TreeSet<String>();

        for (var command : Commands.ALL)
        {
            if (command.dataSchema() == null)
                continue;
            walk(command.name(), "", command.dataSchema().toJson(), found, problems);
        }

        assertThat(problems, empty());

        var expected = new TreeSet<>(INSTRUMENT_CURRENCY);
        expected.removeAll(found);
        assertThat("declared instrument-currency fields missing from the schemas", expected, empty());
    }

    @Test
    public void helpShowsEveryNoteInFullOnceAndPointsBackToItAfterwards()
    {
        for (var command : List.of("holdings", "transactions", "prices"))
        {
            var help = CliRun.run(command, "--help").stdout;

            assertThat(command, help, containsString("minor currency unit"));
            assertThat(command, help, containsString("ILA (1/100 ILS)"));
            assertThat(command, help, containsString("GBX (1/100 GBP)"));
            for (var note : MinorUnits.NOTES)
                assertThat(command, help.split(Pattern.quote(note), -1).length - 1,
                                lessThanOrEqualTo(1));
        }
        assertThat(CliRun.run("transactions", "--help").stdout, containsString(MinorUnits.AS_ABOVE.trim()));
    }

    @SuppressWarnings("unchecked")
    private static void walk(String command, String path, Map<String, Object> node, Set<String> found,
                    List<String> problems)
    {
        var properties = (Map<String, Map<String, Object>>) node.get("properties");
        if (properties != null)
        {
            properties.forEach((name, child) -> {
                var childPath = path.isEmpty() ? name : path + "." + name;
                if (MONEY_FIELDS.contains(name))
                    check(command + ":" + childPath, child, found, problems);
                walk(command, childPath, child, found, problems);
            });
        }
        var items = (Map<String, Object>) node.get("items");
        if (items != null)
            walk(command, path + "[]", items, found, problems);
    }

    private static void check(String key, Map<String, Object> node, Set<String> found, List<String> problems)
    {
        if (INSTRUMENT_CURRENCY.contains(key))
        {
            found.add(key);
            var description = String.valueOf(node.get("description"));
            for (var needle : List.of("minor currency unit", "ILA", "GBX", "converted"))
                if (!description.contains(needle))
                    problems.add(key + " does not mention '" + needle + "': " + description);
        }
        else if (!OTHER_CURRENCY.contains(key))
        {
            problems.add(key + " is a money field that is neither declared as instrument currency nor as another one");
        }
    }
}
