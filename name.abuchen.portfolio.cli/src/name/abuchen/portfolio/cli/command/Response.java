package name.abuchen.portfolio.cli.command;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a successful command: the <code>{data, meta, warnings}</code>
 * envelope. Immutable, and built only by {@link CommandSpec} from the
 * handler's data and its facets' reports.
 */
public final class Response
{
    private final Object data;
    private final Map<String, Object> meta;
    private final List<Warning> warnings;

    Response(Object data, Map<String, Object> meta, List<Warning> warnings)
    {
        this.data = data;
        // a meta member may be null, which Map.copyOf would reject
        this.meta = Collections.unmodifiableMap(new LinkedHashMap<>(meta));
        this.warnings = List.copyOf(warnings);
    }

    public Object data()
    {
        return data;
    }

    /** Unmodifiable, in schema order. */
    public Map<String, Object> meta()
    {
        return meta;
    }

    /** Unmodifiable. */
    public List<Warning> warnings()
    {
        return warnings;
    }

    public Map<String, Object> toEnvelope()
    {
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("data", data);
        envelope.put("meta", meta);
        envelope.put("warnings", warnings.stream().map(Warning::toJson).toList());
        return envelope;
    }
}
