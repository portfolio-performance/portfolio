package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import name.abuchen.portfolio.datatransfer.ComdirectPDFExtractor;
import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.INGDibaPDFExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

public class ImportPDFHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return Platform.OS_LINUX.equals(Platform.getOS())
                        || (null != part && part.getObject() instanceof PortfolioPart);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Named("name.abuchen.portfolio.ui.param.pdf-type") String type) throws IOException
    {
        if (part == null || !(part.getObject() instanceof PortfolioPart))
            return;

        PortfolioPart portfolioPart = (PortfolioPart) part.getObject();
        Client client = portfolioPart.getClient();

        // determine extractor class

        Extractor extractor = null;
        if ("ingdiba".equals(type)) //$NON-NLS-1$
            extractor = new INGDibaPDFExtractor(client);
        else if ("comdirect".equals(type)) //$NON-NLS-1$
            extractor = new ComdirectPDFExtractor(client);
        else
            throw new UnsupportedOperationException("Unknown pdf type: " + type); //$NON-NLS-1$

        // open file dialog to pick pdf files

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        fileDialog.setText(extractor.getLabel());
        fileDialog.setFilterNames(new String[] { MessageFormat.format("{0} ({1})", //$NON-NLS-1$
                        extractor.getLabel(), extractor.getFilterExtension()) });
        fileDialog.setFilterExtensions(new String[] { extractor.getFilterExtension() });
        fileDialog.open();

        String[] fileNames = fileDialog.getFileNames();

        if (fileNames.length == 0)
            return;

        List<File> files = new ArrayList<File>();
        for (String file : fileNames)
            files.add(new File(fileDialog.getFilterPath(), file));

        // open wizard dialog

        Dialog wizwardDialog = new WizardDialog(shell, new ImportExtractedItemsWizard(client, extractor, files));
        wizwardDialog.open();
    }
}
