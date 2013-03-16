package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.ConsistencyChecksJob;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

public class RunConsistencyChecksHandler extends AbstractHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        final IEditorPart activeEditor = page.getActiveEditor();

        if (!(activeEditor instanceof ClientEditor))
            return null;

        ClientEditor editor = (ClientEditor) activeEditor;
        final Client client = editor.getClient();

        new ConsistencyChecksJob(editor, client, true).schedule();

        return null;
    }
}
