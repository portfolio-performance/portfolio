package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.UpdateCPIJob;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.handlers.HandlerUtil;

public class UpdateCPIHandler extends AbstractHandler
{

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        final IEditorPart editor = page.getActiveEditor();

        if (!(editor instanceof ClientEditor))
            return null;

        Client client = ((ClientEditor) editor).getClient();

        new UpdateCPIJob(client)
        {
            @Override
            protected void notifyFinished()
            {
                ((ClientEditor) editor).notifyModelUpdated();
            }
        }.schedule();

        return null;
    }
}
