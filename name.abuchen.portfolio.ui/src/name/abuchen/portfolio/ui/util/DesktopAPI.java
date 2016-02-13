package name.abuchen.portfolio.ui.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Utility class to launch the default browser to open an URI.
 * <p/>
 * Basically, the problem is that {@link java.awt.Desktop} integration doesn't
 * work well on Linux. Based on http://stackoverflow.com/a/18004334/1158146.
 */
public class DesktopAPI
{
    /**
     * Launches the default browser with the given URI.
     */
    public static void browse(String uri)
    {
        try
        {
            URI target = new URI(uri);

            // try Java's desktop API first
            if (Desktop.isDesktopSupported())
            {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE))
                {
                    desktop.browse(target);
                    return;
                }
            }

            // then fallback to using native commands on Linux
            if (Platform.OS_LINUX.equals(Platform.getOS()))
            {
                if (runCommand("sensible-browser", uri)) //$NON-NLS-1$
                    return;
                if (runCommand("kde-open", uri)) //$NON-NLS-1$
                    return;
                if (runCommand("gnome-open", uri)) //$NON-NLS-1$
                    return;
                if (runCommand("xdg-open", uri)) //$NON-NLS-1$
                    return;
            }

            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                            MessageFormat.format(Messages.DesktopAPIErrorOpeningURL, uri));
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                            MessageFormat.format(Messages.DesktopAPIErrorOpeningURL, uri));
        }
        catch (URISyntaxException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                            MessageFormat.format(Messages.DesktopAPIIllegalURL, uri));
        }
    }

    private static boolean runCommand(String... command) throws IOException
    {
        try
        {
            Process p = Runtime.getRuntime().exec(command);
            if (p == null)
                return false;

            try
            {
                p.exitValue();
                return false;
            }
            catch (IllegalThreadStateException itse)
            {
                return true;
            }
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            return false;
        }
    }
}
