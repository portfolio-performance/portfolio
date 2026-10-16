package name.abuchen.portfolio.cli;

import java.util.Map;

/** Field rules shared by the master-data list commands. */
final class MasterData
{
    private MasterData()
    {
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
