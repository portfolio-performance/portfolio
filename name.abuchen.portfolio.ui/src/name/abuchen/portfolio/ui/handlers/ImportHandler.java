package name.abuchen.portfolio.ui.handlers;

import java.io.File;

import javax.inject.Named;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportWizard;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class ImportHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return Platform.OS_LINUX.equals(Platform.getOS())
                        || (null != part && part.getObject() instanceof PortfolioPart);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        if (part == null || !(part.getObject() instanceof PortfolioPart))
        {
            MessageDialog.openWarning(shell, Messages.MsgNoFileOpen, Messages.MsgNoFileOpenText);
            return;
        }

        PortfolioPart portfolioPart = (PortfolioPart) part.getObject();
        Client client = portfolioPart.getClient();

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
        fileDialog.setFilterNames(new String[] { Messages.CSVImportLabelFileCSV, Messages.CSVImportLabelFileAll });
        fileDialog.setFilterExtensions(new String[] { "*.csv", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        String fileName = fileDialog.open();

        if (fileName == null)
            return;

        Dialog wizwardDialog = new WizardDialog(shell, new ImportWizard(client, new File(fileName)));
        if (wizwardDialog.open() == Dialog.OK)
            portfolioPart.notifyModelUpdated();
    }
}
