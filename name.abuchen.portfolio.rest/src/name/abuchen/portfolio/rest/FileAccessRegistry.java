package name.abuchen.portfolio.rest;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Workspace-preferences-backed registry of API-enabled portfolio files, keyed
 * by absolute file path. The identity is deliberately not stored inside the
 * portfolio file: users copy files to experiment, and an in-file identity would
 * duplicate and let a client silently write to the wrong copy.
 */
public class FileAccessRegistry
{
    public record FileAccess(String path, String uuid, String alias, boolean enabled)
    {
    }

    public sealed interface Resolution
    {
        record Match(FileAccess access) implements Resolution
        {
        }

        record Ambiguous() implements Resolution
        {
        }

        record NotFound() implements Resolution
        {
        }
    }

    private static final Pattern ALIAS_PATTERN = Pattern.compile("[a-z0-9-]{1,32}"); //$NON-NLS-1$

    private final IEclipsePreferences node;

    public FileAccessRegistry(IEclipsePreferences node)
    {
        this.node = node;
    }

    public synchronized FileAccess ensureRecord(String path)
    {
        var existing = byPath(path);
        if (existing.isPresent())
            return existing.get();

        var child = node.node(encode(path));
        child.put("uuid", UUID.randomUUID().toString()); //$NON-NLS-1$
        child.putBoolean("enabled", false); //$NON-NLS-1$
        flush();
        return read(path);
    }

    public synchronized Optional<FileAccess> byPath(String path)
    {
        try
        {
            if (!node.nodeExists(encode(path)))
                return Optional.empty();
        }
        catch (BackingStoreException e)
        {
            return Optional.empty();
        }
        return Optional.of(read(path));
    }

    public synchronized List<FileAccess> all()
    {
        try
        {
            var answer = new ArrayList<FileAccess>();
            for (String childName : node.childrenNames())
                answer.add(read(decode(childName)));
            return answer;
        }
        catch (BackingStoreException e)
        {
            return List.of();
        }
    }

    public synchronized void setEnabled(String path, boolean enabled)
    {
        ensureRecord(path);
        node.node(encode(path)).putBoolean("enabled", enabled); //$NON-NLS-1$
        flush();
    }

    public synchronized void setAlias(String path, String alias)
    {
        if (alias != null)
        {
            if (!ALIAS_PATTERN.matcher(alias).matches())
                throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorAliasMustMatchPattern, alias));
            if (parsesAsUuid(alias))
                throw new IllegalArgumentException(
                                MessageFormat.format(Messages.MsgErrorAliasMustNotLookLikeUUID, alias));
            for (FileAccess access : all())
            {
                if (!access.path().equals(path) && access.alias() != null && access.alias().equalsIgnoreCase(alias))
                    throw new IllegalArgumentException(MessageFormat.format(Messages.MsgErrorAliasAlreadyInUse, alias));
            }
        }

        ensureRecord(path);
        var child = node.node(encode(path));
        if (alias == null)
            child.remove("alias"); //$NON-NLS-1$
        else
            child.put("alias", alias); //$NON-NLS-1$
        flush();
    }

    public synchronized void remove(String path)
    {
        try
        {
            if (node.nodeExists(encode(path)))
            {
                node.node(encode(path)).removeNode();
                flush();
            }
        }
        catch (BackingStoreException e)
        {
            // ignore: record already gone
        }
    }

    public synchronized Resolution resolve(String idOrAlias)
    {
        var enabled = all().stream().filter(FileAccess::enabled).toList();

        for (FileAccess access : enabled)
            if (access.uuid().equals(idOrAlias))
                return new Resolution.Match(access);

        var byAlias = enabled.stream().filter(a -> a.alias() != null && a.alias().equalsIgnoreCase(idOrAlias)).toList();
        if (byAlias.size() > 1)
            return new Resolution.Ambiguous();
        if (byAlias.size() == 1)
            return new Resolution.Match(byAlias.get(0));

        return new Resolution.NotFound();
    }

    private FileAccess read(String path)
    {
        var child = node.node(encode(path));
        return new FileAccess(path, child.get("uuid", null), child.get("alias", null), //$NON-NLS-1$ //$NON-NLS-2$
                        child.getBoolean("enabled", false)); //$NON-NLS-1$
    }

    private static boolean parsesAsUuid(String candidate)
    {
        try
        {
            UUID.fromString(candidate);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    private static String encode(String path)
    {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(path.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String childName)
    {
        return new String(Base64.getUrlDecoder().decode(childName), StandardCharsets.UTF_8);
    }

    private void flush()
    {
        try
        {
            node.flush();
        }
        catch (BackingStoreException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
