package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;

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

    /**
     * Checks whether all files selected in a file dialog can be found below
     * its filter path. SWT returns the file names plus one common directory
     * only. The document portal, however, places each file in a directory of
     * its own, so only one of several selected files can be found. Shows an
     * error message if files are missing.
     *
     * @return true if the selected files can be read
     */
    public static boolean checkSelectedFiles(Shell shell, String filterPath, String[] filenames)
    {
        if (filenames.length < 2 || !isPortalPath(filterPath))
            return true;

        for (var filename : filenames)
        {
            if (!new File(filterPath, filename).exists())
            {
                MessageDialog.openError(shell, Messages.LabelError, Messages.MsgErrorMultipleFilesFlatpak);
                return false;
            }
        }

        return true;
    }
}
