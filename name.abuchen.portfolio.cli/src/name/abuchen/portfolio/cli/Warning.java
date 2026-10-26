package name.abuchen.portfolio.cli;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A condition worth knowing about that doesn't fail the command. It has the
 * same <code>code</code> and <code>detail</code> members as an error, plus
 * machine-readable extensions, and is reported in the <code>warnings</code> of
 * the response envelope.
 */
public final class Warning
{
    private final String code;
    private final String detail;
    private final Map<String, Object> extensions = new LinkedHashMap<>();

    public Warning(String code, String detail)
    {
        this.code = code;
        this.detail = detail;
    }

    /** Adds a machine-readable field next to <code>code</code> and <code>detail</code>. */
    public Warning with(String name, Object value)
    {
        extensions.put(name, value);
        return this;
    }

    String code()
    {
        return code;
    }

    String detail()
    {
        return detail;
    }

    Map<String, Object> toJson()
    {
        var json = new LinkedHashMap<String, Object>();
        json.put("code", code);
        json.put("detail", detail);
        json.putAll(extensions);
        return json;
    }
}
