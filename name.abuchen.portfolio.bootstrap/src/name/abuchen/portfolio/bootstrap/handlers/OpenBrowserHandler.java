package name.abuchen.portfolio.bootstrap.handlers;

import java.util.ResourceBundle;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.DesktopAPI;

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
                    @Named(UIConstants.Parameter.URL) String url)
    {
        if (url != null && url.startsWith("%") && url.endsWith("url")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            var key = url.substring(1); // e.G. "command.openbrowser.manual.url"

            if (URL.containsKey(key))
                url = URL.getString(key);
            else
                return;
        }

        if (url != null && !url.isBlank())
        {
            DesktopAPI.browse(url);
        }
    }
}
