package name.abuchen.portfolio.ui.util;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

import com.ibm.icu.text.MessageFormat;

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

    private DesktopAPI()
    {
    }
}
