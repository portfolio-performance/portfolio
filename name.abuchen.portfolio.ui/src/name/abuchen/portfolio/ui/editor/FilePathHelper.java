package name.abuchen.portfolio.ui.editor;

import java.nio.file.Files;
import java.nio.file.Paths;

import name.abuchen.portfolio.model.TransactionOwner;

public class FilePathHelper
{
    private PortfolioPart part;
    private String key;
    private String postfix;

    public FilePathHelper(PortfolioPart part, String key)
    {
        this(part, key, (TransactionOwner<?>) null);
    }

    public FilePathHelper(PortfolioPart part, String key, TransactionOwner<?> owner)
    {
        this.part = part;
        this.key = key;
        this.postfix = owner != null ? owner.getUUID() : null;
    }

    private String getKeyWithPostfix()
    {
        return key + "/" + postfix; //$NON-NLS-1$
    }

    public String getPath()
    {
        // postfix-specific preferences (e.g. per-account/portfolio
        // path)
        if (postfix != null)
        {
            String path = part.getPreferenceStore().getString(getKeyWithPostfix());
            if (!path.isEmpty() && Files.isDirectory(Paths.get(path)))
                return path;
        }

        // second, check file-specific preferences
        String path = part.getPreferenceStore().getString(key);

        if (path.isEmpty() || !Files.isDirectory(Paths.get(path)))
            path = null;

        // third, check application-wide preferences
        if (path == null)
        {
            String p = part.getEclipsePreferences().get(key, null);
            if (p != null && Files.isDirectory(Paths.get(p)))
                path = p;
        }

        // fourth, fall back to the user directory
        if (path == null)
            path = System.getProperty("user.home"); //$NON-NLS-1$

        return path;
    }

    public void savePath(String path)
    {
        part.getPreferenceStore().setValue(key, path);
        part.getEclipsePreferences().put(key, path);

        if (postfix != null)
            part.getPreferenceStore().setValue(getKeyWithPostfix(), path);
    }
}
