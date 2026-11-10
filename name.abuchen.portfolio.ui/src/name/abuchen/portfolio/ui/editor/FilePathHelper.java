package name.abuchen.portfolio.ui.editor;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class FilePathHelper
{
    private final PortfolioPart part;
    private final String key;

    /**
     * Identifies the context to remember the path for, for example the UUID of
     * a portfolio. If null, the path is remembered for the client file only.
     */
    private final String qualifier;

    public FilePathHelper(PortfolioPart part, String key)
    {
        this(part, key, null);
    }

    public FilePathHelper(PortfolioPart part, String key, String qualifier)
    {
        this.part = part;
        this.key = key;
        this.qualifier = qualifier;
    }

    private String getKeyWithQualifier()
    {
        return key + "." + qualifier; //$NON-NLS-1$
    }

    public String getPath()
    {
        // first, qualifier-specific preferences (e.g. per-account/portfolio)
        if (qualifier != null)
        {
            String path = part.getPreferenceStore().getString(getKeyWithQualifier());
            if (isExistingDirectory(path))
                return path;
        }

        // second, check file-specific preferences
        String path = part.getPreferenceStore().getString(key);

        if (!isExistingDirectory(path))
            path = null;

        // third, check application-wide preferences
        if (path == null)
        {
            String p = part.getEclipsePreferences().get(key, null);
            if (isExistingDirectory(p))
                path = p;
        }

        // fourth, fall back to the user directory
        if (path == null)
            path = System.getProperty("user.home"); //$NON-NLS-1$

        return path;
    }

    public void savePath(String path)
    {
        if (qualifier != null)
        {
            part.getPreferenceStore().setValue(getKeyWithQualifier(), path);
        }
        else
        {
            // the preferences without qualifier are the default for the contexts
            // that have none, e.g. File - Import - PDF. Writing them for every
            // depot would overwrite that default constantly
            part.getPreferenceStore().setValue(key, path);
            part.getEclipsePreferences().put(key, path);
        }
    }

    /**
     * Checks whether the stored path still points to a directory. A remembered
     * directory can have been removed meanwhile, and it can be malformed on
     * this platform if the client file was last used on another one.
     */
    private static boolean isExistingDirectory(String path)
    {
        if (path == null || path.isEmpty())
            return false;

        try
        {
            return Files.isDirectory(Paths.get(path));
        }
        catch (InvalidPathException e)
        {
            return false;
        }
    }
}
