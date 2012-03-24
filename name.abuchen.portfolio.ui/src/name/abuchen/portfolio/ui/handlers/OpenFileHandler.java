package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.ClientEditor.ClientEditorInput;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class OpenFileHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        try
        {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

            FileDialog dialog = new FileDialog(shell, SWT.OPEN);
            dialog.setFilterExtensions(new String[] { "*.xml" }); //$NON-NLS-1$
            dialog.setFilterNames(new String[] { Messages.LabelPortfolioPerformanceFile });
            String fileSelected = dialog.open();

            if (fileSelected != null)
            {
                IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
                IWorkbenchPage page = window.getActivePage();
                page.openEditor(new ClientEditorInput(new Path(fileSelected)), "name.abuchen.portfolio.ui.editor"); //$NON-NLS-1$
            }

            return null;
        }
        catch (PartInitException e)
        {
            throw new ExecutionException(Messages.MsgErrorOpeningEditor, e);
        }
    }
}
