package name.abuchen.portfolio.rest.internal;

import java.util.List;

import name.abuchen.portfolio.rest.FileAccessRegistry;
import name.abuchen.portfolio.rest.spi.HostApplication;
import name.abuchen.portfolio.rest.spi.OpenFile;

/**
 * Resolves the {file} path segment (UUID or alias) to an open, API-enabled
 * portfolio file. Unknown and disabled files are deliberately
 * indistinguishable (404) so a token holder cannot enumerate unshared files.
 */
public class FileResolver
{
    public record ResolvedFile(FileAccessRegistry.FileAccess access, OpenFile file)
    {
    }

    private final FileAccessRegistry registry;
    private final HostApplication host;

    public FileResolver(FileAccessRegistry registry, HostApplication host)
    {
        this.registry = registry;
        this.host = host;
    }

    public ResolvedFile resolve(String idOrAlias)
    {
        var resolution = registry.resolve(idOrAlias);

        if (resolution instanceof FileAccessRegistry.Resolution.Ambiguous)
            throw ApiException.conflict("ambiguous-alias", "Alias is ambiguous, use the file UUID", null, List.of()); //$NON-NLS-1$ //$NON-NLS-2$

        if (!(resolution instanceof FileAccessRegistry.Resolution.Match match))
            throw ApiException.notFound();

        var open = host.listOpenFiles().stream() //
                        .filter(file -> file.getPath().equals(match.access().path())) //
                        .findFirst();

        if (open.isEmpty())
            throw ApiException.conflict("file-not-open", "File is not open in Portfolio Performance", //$NON-NLS-1$ //$NON-NLS-2$
                            match.access().path(), List.of());

        return new ResolvedFile(match.access(), open.get());
    }
}
