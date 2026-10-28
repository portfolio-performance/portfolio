package name.abuchen.portfolio.cli.masterdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import name.abuchen.portfolio.cli.command.Claims;
import name.abuchen.portfolio.cli.command.Outcome;

/** Helpers for the write tests of the master data kinds. */
final class Writing
{
    private static final Gson GSON = new Gson();

    /** Every entity of the model counts as being in the file before the batch. */
    static final Predicate<Object> ALL_EXISTED = entity -> true;

    /** No earlier operation has claimed anything. */
    static final Claims UNCLAIMED = uuid -> OptionalInt.empty();

    private Writing()
    {
    }

    static JsonObject json(String text)
    {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    /** The item as an input, without the members no write takes. */
    static JsonObject input(Map<String, Object> item, String... readOnly)
    {
        var copy = new LinkedHashMap<>(item);
        for (var member : readOnly)
            copy.remove(member);
        return GSON.toJsonTree(copy).getAsJsonObject();
    }

    static JsonObject asJson(Map<String, Object> item)
    {
        return GSON.toJsonTree(item).getAsJsonObject();
    }

    static Outcome.Applied applied(Outcome outcome)
    {
        assertThat(outcome.toString(), outcome, instanceOf(Outcome.Applied.class));
        return (Outcome.Applied) outcome;
    }

    /** The failures as <code>field:code</code>. */
    static Set<String> failures(Outcome outcome)
    {
        assertThat(outcome.toString(), outcome, instanceOf(Outcome.Refused.class));
        var keys = new TreeSet<String>();
        ((Outcome.Refused) outcome).failures().forEach(f -> keys.add(f.field() + ":" + f.code()));
        return keys;
    }
}
