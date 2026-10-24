package name.abuchen.portfolio.cli;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

/** Field rules shared by the master-data list commands. */
final class MasterData
{
    static final Schema UPDATED_AT = Schema.string(
                    "When the entity last changed in the file, as an ISO-8601 UTC instant to the second, for example 2026-10-24T15:04:57Z. Absent if the file has none.");

    private MasterData()
    {
    }

    /** The name is always emitted; a null name becomes the empty string. */
    static String orEmpty(String value)
    {
        return value == null ? "" : value;
    }

    /** Name, case-insensitive and ordinal. */
    static <T> Comparator<T> byName(Function<T, String> name)
    {
        return Comparator.comparing((T entity) -> orEmpty(name.apply(entity)), String.CASE_INSENSITIVE_ORDER);
    }

    /** Name (case-insensitive, ordinal), then UUID. */
    static <T> Comparator<T> byNameThenUuid(Function<T, String> name, Function<T, String> uuid)
    {
        return byName(name).thenComparing(entity -> orEmpty(uuid.apply(entity)));
    }

    /** Adds <code>updatedAt</code>, the last modified time, unless core has none. */
    static void putUpdatedAt(Map<String, Object> json, Instant updatedAt)
    {
        Sorting.lastModified(updatedAt).ifPresent(instant -> json.put("updatedAt", instant.toString()));
    }

    /** The uuid and name every reference to an entity carries. */
    static void putIdentity(Map<String, Object> json, String uuid, String name)
    {
        json.put("uuid", uuid);
        json.put("name", orEmpty(name));
    }

    static boolean isSet(String value)
    {
        return value != null && !value.isBlank();
    }

    /** Adds the field unless the value is null or blank. Values are emitted untrimmed. */
    static void putIfSet(Map<String, Object> json, String name, String value)
    {
        if (isSet(value))
            json.put(name, value);
    }
}
