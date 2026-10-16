package name.abuchen.portfolio.cli;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

/** Ordering and field rules shared by the master-data list commands. */
final class MasterData
{
    private MasterData()
    {
    }

    /** Name (case-insensitive, ordinal; null counts as empty), then UUID. */
    static <T> Comparator<T> byNameThenUuid(Function<T, String> name, Function<T, String> uuid)
    {
        return Comparator.comparing((T entity) -> orEmpty(name.apply(entity)), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entity -> orEmpty(uuid.apply(entity)));
    }

    /** The name is always emitted; a null name becomes the empty string. */
    static String orEmpty(String value)
    {
        return value == null ? "" : value;
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
