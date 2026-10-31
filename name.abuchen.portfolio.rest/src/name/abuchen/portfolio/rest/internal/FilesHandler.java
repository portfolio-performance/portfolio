package name.abuchen.portfolio.rest.internal;

import com.google.gson.JsonArray;

import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.FileAccessRegistry.FileAccess;
import name.abuchen.portfolio.rest.spi.HostApplication;

public class FilesHandler
{
    private final FileAccessRegistry registry;
    private final HostApplication host;

    public FilesHandler(FileAccessRegistry registry, HostApplication host)
    {
        this.registry = registry;
        this.host = host;
    }

    public Response list(Request request)
    {
        var items = new JsonArray();

        for (var file : host.listOpenFiles())
            registry.byPath(file.getPath()) //
                            .filter(FileAccess::enabled) //
                            .ifPresent(access -> items.add(EntityJson.toJson(access, file)));

        return Response.json(200, EntityJson.envelope(items));
    }
}
