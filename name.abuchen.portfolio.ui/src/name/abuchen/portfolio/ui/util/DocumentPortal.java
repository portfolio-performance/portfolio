package name.abuchen.portfolio.ui.util;

import java.util.regex.Pattern;

/**
 * Handles paths that the Flatpak document portal hands over.
 * <p>
 * Inside the Flatpak sandbox, the file dialog does not return the real path of
 * a selected file. The document portal instead exposes each selected file in a
 * directory of its own, for example
 * <code>/run/user/1000/doc/1a2b3c4d/statement.pdf</code>. Such a directory
 * belongs to a single document and is removed by the portal later on.
 */
public final class DocumentPortal
{
    private static final Pattern PORTAL_PATH = Pattern.compile("^/run/(user/\\d+|flatpak)/doc(/.*)?$"); //$NON-NLS-1$

    private DocumentPortal()
    {
    }

    /**
     * Returns true if the path is a directory or file of the Flatpak document
     * portal. Such a path must not be remembered as the starting directory of a
     * file dialog because it points to a single document only.
     */
    public static boolean isPortalPath(String path)
    {
        return path != null && PORTAL_PATH.matcher(path).matches();
    }
}
