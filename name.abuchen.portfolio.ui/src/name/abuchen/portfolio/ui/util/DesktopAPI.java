package name.abuchen.portfolio.ui.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Utility class to launch the default browser to open an URL.
 * <p/>
 * Basically, the problem is that {@link java.awt.Desktop} integration doesn't
 * work well on Linux. Based on http://stackoverflow.com/a/18004334/1158146.
 */
public class DesktopAPI
{
    public static void browse(String uri)
    {
        try
        {
            URI target = new URI(uri);

            // try using native commands on Linux first
            if (Platform.OS_LINUX.equals(Platform.getOS()))
            {
                if (runCommand("kde-open", uri)) //$NON-NLS-1$
                    return;
                if (runCommand("gnome-open", uri)) //$NON-NLS-1$
                    return;
                if (runCommand("xdg-open", uri)) //$NON-NLS-1$
                    return;
            }

            // fall back to Java Desktop
            if (!Desktop.isDesktopSupported())
                throw new IOException(Messages.DesktopAPIPlatformNotSupported);

            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE))
                throw new IOException(Messages.DesktopAPIBrowserActionNotSupported);

            desktop.browse(target);
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
