package name.abuchen.portfolio.ui.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

/**
 * Utility class to launch the default browser to open an URI.
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
            Program.launch(target.toASCIIString());
        }
        catch (URISyntaxException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                            MessageFormat.format(Messages.DesktopAPIIllegalURL, uri));
        }
    }

    /**
     * Opens the given file with the operating system's default application.
     */
    public static void open(File file)
    {
        boolean launched = Program.launch(file.getAbsolutePath());
        if (!launched)
        {
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError,
                            MessageFormat.format(Messages.MsgErrorOpeningFile, file.getAbsolutePath()));
        }
    }

    private DesktopAPI()
    {
    }
}
