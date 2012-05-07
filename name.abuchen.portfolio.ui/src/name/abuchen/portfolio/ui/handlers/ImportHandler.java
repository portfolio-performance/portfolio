package name.abuchen.portfolio.ui.handlers;

import java.io.File;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.wizards.ImportWizard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class ImportHandler extends AbstractHandler
{
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        final IEditorPart editor = page.getActiveEditor();

        if (!(editor instanceof ClientEditor))
            return null;

        Client client = ((ClientEditor) editor).getClient();
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
        fileDialog.setFilterNames(new String[] { "Comma-separated Values (*.csv)", "All Files (*.*)" });
        fileDialog.setFilterExtensions(new String[] { "csv", "*" }); //$NON-NLS-1$ //$NON-NLS-2$
        String fileName = fileDialog.open();

        if (fileName == null)
            return null;

        Dialog wizwardDialog = new WizardDialog(shell, new ImportWizard(client, new File(fileName)));
        if (wizwardDialog.open() == Dialog.OK)
            ((ClientEditor) editor).notifyModelUpdated();

        return null;
    }
}
