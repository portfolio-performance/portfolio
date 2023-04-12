package name.abuchen.portfolio.ui.editor;

import java.nio.file.Files;
import java.nio.file.Paths;

public class FilePathHelper
{
    private PortfolioPart part;
    private String key;

    public FilePathHelper(PortfolioPart part, String key)
    {
        this.part = part;
        this.key = key;
    }

    public String getPath()
    {
        // first, check file-specific preferences
        String path = part.getPreferenceStore().getString(key);

        if (path.isEmpty() || !Files.isDirectory(Paths.get(path)))
            path = null;

        // second, check application-wide preferences
        if (path == null)
        {
            String p = part.getEclipsePreferences().get(key, null);
            if (p != null && Files.isDirectory(Paths.get(p)))
                path = p;
        }

        // third, fall back to the user directory
        if (path == null)
            path = System.getProperty("user.home"); //$NON-NLS-1$

        return path;
    }

    public void savePath(String path)
    {
        part.getPreferenceStore().setValue(key, path);
        part.getEclipsePreferences().put(key, path);
    }
}
