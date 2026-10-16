package name.abuchen.portfolio.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import name.abuchen.portfolio.junit.AccountBuilder;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;

/**
 * Paging, ordering and option validation are shared by every list command;
 * they are exercised through <code>cash-accounts</code>.
 */
public class ListPagingTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Client clientWithAccounts(String... names)
    {
        var client = new Client();
        for (var name : names)
        {
            var account = new AccountBuilder().addTo(client);
            account.setName(name);
        }
        return client;
    }

    private CliRun run(Client client, String... options) throws IOException
    {
        var file = ClientFiles.xml(folder.getRoot().toPath(), client);

        var args = new ArrayList<String>(List.of("cash-accounts", "--file", file.getPath()));
        args.addAll(List.of(options));
        return CliRun.run(args.toArray(String[]::new));
    }

    private static JsonObject data(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("data");
    }

    private static JsonObject meta(CliRun run)
    {
        return run.json().getAsJsonObject().getAsJsonObject("meta");
    }

    private static List<String> names(CliRun run)
    {
        var names = new ArrayList<String>();
        for (var item : data(run).getAsJsonArray("items"))
            names.add(item.getAsJsonObject().get("name").getAsString());
        return names;
    }

    @Test
    public void pagesThroughTheListAndOmitsNextOffsetOnTheLastPage() throws IOException
    {
        var client = clientWithAccounts("a", "b", "c", "d", "e");

        var first = run(client, "--limit", "2");
        assertThat(first.exitCode, is(0));
        assertThat(names(first), contains("a", "b"));
        assertThat(data(first).get("total").getAsInt(), is(5));
        assertThat(data(first).get("nextOffset").getAsInt(), is(2));
        assertThat(meta(first).get("limit").getAsInt(), is(2));
        assertThat(meta(first).get("offset").getAsInt(), is(0));

        var second = run(client, "--limit", "2", "--offset", "2");
        assertThat(names(second), contains("c", "d"));
        assertThat(data(second).get("nextOffset").getAsInt(), is(4));
        assertThat(meta(second).get("offset").getAsInt(), is(2));

        var last = run(client, "--limit", "2", "--offset", "4");
        assertThat(names(last), contains("e"));
        assertThat(data(last).get("total").getAsInt(), is(5));
        assertThat(data(last).has("nextOffset"), is(false));
    }

    @Test
    public void pageEndingExactlyAtTheEndHasNoNextOffset() throws IOException
    {
        var client = clientWithAccounts("a", "b", "c", "d");

        var page = run(client, "--limit=2", "--offset=2");

        assertThat(names(page), contains("c", "d"));
        assertThat(data(page).has("nextOffset"), is(false));
    }

    @Test
    public void offsetPastTheEndGivesAnEmptyPage() throws IOException
    {
        var client = clientWithAccounts("a", "b");

        for (var offset : new String[] { "2", "3", "9223372036854775807" })
        {
            var page = run(client, "--offset", offset);

            assertThat(page.exitCode, is(0));
            assertThat(data(page).getAsJsonArray("items").size(), is(0));
            assertThat(data(page).get("total").getAsInt(), is(2));
            assertThat(data(page).has("nextOffset"), is(false));
        }
    }

    @Test
    public void limitCanBeAsLargeAsAThousandAndDefaultsToTwoHundred() throws IOException
    {
        var names = IntStream.range(0, 250).mapToObj(i -> String.format("account-%03d", i)).toArray(String[]::new);
        var client = clientWithAccounts(names);

        var byDefault = run(client);
        assertThat(data(byDefault).getAsJsonArray("items").size(), is(200));
        assertThat(data(byDefault).get("nextOffset").getAsInt(), is(200));

        var maximum = run(client, "--limit", "1000");
        assertThat(maximum.exitCode, is(0));
        assertThat(data(maximum).getAsJsonArray("items").size(), is(250));
        assertThat(data(maximum).has("nextOffset"), is(false));
    }

    @Test
    public void ordersByNameIgnoringCaseAndOrdinally() throws IOException
    {
        // a locale-aware collator would sort the umlaut next to A
        var client = clientWithAccounts("Zebra", "\u00C4rger", "apple", "Banana");

        assertThat(names(run(client)), contains("apple", "Banana", "Zebra", "\u00C4rger"));
    }

    @Test
    public void nullNameSortsFirst() throws IOException
    {
        var client = clientWithAccounts("b", "a");
        client.addAccount(new Account());

        assertThat(names(run(client)), contains("", "a", "b"));
    }

    @Test
    public void equalNamesAreOrderedByUuid() throws IOException
    {
        var client = clientWithAccounts("same", "same", "same", "same");

        var uuids = new ArrayList<String>();
        for (var item : data(run(client)).getAsJsonArray("items"))
            uuids.add(item.getAsJsonObject().get("uuid").getAsString());

        var sorted = new ArrayList<>(uuids);
        sorted.sort(null);
        assertThat(uuids, is(sorted));
    }

    @Test
    public void orderingIsStableAcrossPages() throws IOException
    {
        var client = clientWithAccounts("same", "same", "same", "same", "same");

        var uuids = new ArrayList<String>();
        for (var offset = 0; offset < 5; offset++)
        {
            var page = run(client, "--limit", "1", "--offset", String.valueOf(offset));
            JsonArray items = data(page).getAsJsonArray("items");
            uuids.add(items.get(0).getAsJsonObject().get("uuid").getAsString());
        }

        var sorted = new ArrayList<>(uuids);
        sorted.sort(null);
        assertThat(uuids, is(sorted));
        assertThat(uuids.stream().distinct().count(), is(5L));
    }

    @Test
    public void invalidPagingValuesAreUsageErrors() throws IOException
    {
        var client = clientWithAccounts("a");

        var invalid = List.of(List.of("limit", "0"), List.of("limit", "-1"), List.of("limit", "abc"),
                        List.of("limit", "1.5"), List.of("limit", "1001"), List.of("limit", ""),
                        List.of("offset", "-1"), List.of("offset", "abc"), List.of("offset", "1.5"),
                        List.of("offset", ""), List.of("offset", "99999999999999999999"));

        for (var option : invalid)
        {
            var run = run(client, "--" + option.get(0) + "=" + option.get(1));

            var label = option.toString();
            assertThat(label, run.exitCode, is(2));

            var problem = run.json().getAsJsonObject();
            assertThat(label, problem.get("code").getAsString(), is("invalid-option-value"));
            assertThat(label, problem.get("option").getAsString(), is(option.get(0)));
            assertThat(label, problem.get("value").getAsString(), is(option.get(1)));
        }
    }
}
