package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import name.abuchen.portfolio.datatransfer.FlexQueryImporter;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class IBImportHandler
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
        List<Exception> errors = new ArrayList<Exception>();
        
        Client client = MenuHelper.getActiveClient(part);
        if (client == null)
            return;

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
        fileDialog.setFilterNames(new String[] { Messages.IBImportHandler_LabelXMLFile, Messages.CSVImportLabelFileAll });
        fileDialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
        String fileName = fileDialog.open();

        if (fileName == null)
            return;
        
        FlexQueryImporter importer = new FlexQueryImporter(client, new File(fileName));
        importer.importActivityStatement(errors);
        
        
        ((PortfolioPart) part.getObject()).notifyModelUpdated();
    }
}
