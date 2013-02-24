package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.ui.ClientEditorInput;
import name.abuchen.portfolio.ui.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.intro.IIntroManager;

public class NewFileHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        try
        {
            IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
            IWorkbenchPage page = window.getActivePage();
            page.openEditor(new ClientEditorInput(), "name.abuchen.portfolio.ui.editor"); //$NON-NLS-1$

            IIntroManager introManager = PlatformUI.getWorkbench().getIntroManager();
            if (introManager.getIntro() != null)
                introManager.closeIntro(introManager.getIntro());

            return null;
        }
        catch (PartInitException e)
        {
            throw new ExecutionException(Messages.MsgErrorOpeningEditor, e);
        }
    }
}
