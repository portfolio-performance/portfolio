package name.abuchen.portfolio.ui.handlers;

import java.net.URL;

import name.abuchen.portfolio.ui.ClientEditorInput;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenSampleFileHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        try
        {
            URL installURL = Platform.getInstallLocation().getURL();
            IPath p = new Path(installURL.getFile()).append("kommer.xml"); //$NON-NLS-1$

            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
            IWorkbenchPage page = window.getActivePage();
            page.openEditor(new ClientEditorInput(p), "name.abuchen.portfolio.ui.editor"); //$NON-NLS-1$

            return null;
        }
        catch (PartInitException e)
        {
            throw new ExecutionException(Messages.MsgErrorOpeningEditor, e);
        }
    }
}
