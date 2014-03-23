package name.abuchen.portfolio.ui.handlers;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.wizards.datatransfer.ExportWizard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class ExportHandler extends AbstractHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        final IEditorPart editor = page.getActiveEditor();

        if (!(editor instanceof ClientEditor))
            return null;

        Client client = ((ClientEditor) editor).getClient();
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        Dialog dialog = new WizardDialog(shell, new ExportWizard(client));
        dialog.open();

        return null;
    }
}
