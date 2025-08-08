package name.abuchen.portfolio.bootstrap.handlers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;

public class OpenBrowserHandler
{
    private static final ResourceBundle URL = ResourceBundle.getBundle("OSGI-INF.l10n.bundle"); //$NON-NLS-1$

    /**
     * Opens the URL in the browser. If the URL is a key, it will be replaced by
     * the corresponding value from the resource bundle.
     * 
     * @param shell
     *            The active shell, injected from the UI.
     * @param url
     *            The URL to open, or a key to look up in the resource bundle.
     */
    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Named("name.abuchen.portfolio.ui.param.url") String url)
    {
        if (url == null || url.isBlank())
            return;

        if (url.startsWith("%") && url.endsWith("url")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            var key = url.substring(1); // e.G. "command.openbrowser.manual.url"

            if (URL.containsKey(key))
                browse(URL.getString(key));
        }
        else
        {
            browse(url);
        }
    }

    private void browse(String uri)
    {
        // we cannot use DesktopAPI class here because the bootstrap bundle must
        // not have a dependency to the UI bundle

        try
        {
            URI target = new URI(uri);
            Program.launch(target.toASCIIString());
        }
        catch (URISyntaxException e)
        {
            // ignore. URL should be valid because we define them in the
            // resource bundle
        }

    }
}
