package name.abuchen.portfolio.ui.handlers;

import java.io.File;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.datatransfer.CSVImportWizard;

public class ImportCSVHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        Client client = MenuHelper.getActiveClient(part);
        if (client == null)
            return;

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
        fileDialog.setFilterNames(new String[] { Messages.CSVImportLabelFileCSV, Messages.CSVImportLabelFileAll });
        fileDialog.setFilterExtensions(new String[] { "*.csv", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        String fileName = fileDialog.open();

        if (fileName == null)
            return;

        IPreferenceStore preferences = ((PortfolioPart) part.getObject()).getPreferenceStore();
        Dialog wizwardDialog = new WizardDialog(shell, new CSVImportWizard(client, preferences, new File(fileName)));
        wizwardDialog.open();
    }
}
