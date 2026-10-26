package name.abuchen.portfolio.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a successful command: the <code>{data, meta, warnings}</code>
 * envelope.
 */
public record Response(Object data, Map<String, Object> meta, List<Warning> warnings)
{
    public static Response of(Object data, Map<String, Object> meta)
    {
        return new Response(data, meta, new ArrayList<>());
    }

    public Response warn(Warning warning)
    {
        warnings.add(warning);
        return this;
    }

    Map<String, Object> toEnvelope()
    {
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("data", data);
        envelope.put("meta", meta);
        envelope.put("warnings", warnings.stream().map(Warning::toJson).toList());
        return envelope;
    }
}
