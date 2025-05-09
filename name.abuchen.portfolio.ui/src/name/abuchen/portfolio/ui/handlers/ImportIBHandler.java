package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexStatementExtractor;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.FilePathHelper;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

public class ImportIBHandler
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
        MenuHelper.getActiveClient(part).ifPresent(client -> runImport(part, shell, client));
    }

    private void runImport(MPart part, Shell shell, Client client)
    {
        if (client.getAccounts().isEmpty())
        {
            MessageDialog.openError(shell, Messages.LabelError, Messages.MsgErrorAccountNotExist);
            return;
        }

        if (client.getPortfolios().isEmpty())
        {
            MessageDialog.openError(shell, Messages.LabelError, Messages.MsgErrorPortfolioNotExist);
            return;
        }

        try
        {
            Extractor extractor = new IBFlexStatementExtractor(client);

            PortfolioPart portPart = (PortfolioPart) part.getObject();
            FilePathHelper helper = new FilePathHelper(portPart, UIConstants.Preferences.CSV_IMPORT_PATH);

            FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            fileDialog.setText(extractor.getLabel());
            fileDialog.setFilterNames(
                            new String[] { MessageFormat.format("{0} ({1})", extractor.getLabel(), "*.xml") }); //$NON-NLS-1$ //$NON-NLS-2$
            fileDialog.setFilterExtensions(new String[] { "*.xml;*.XML" }); //$NON-NLS-1$
            fileDialog.setFilterPath(helper.getPath());
            fileDialog.open();

            String[] filenames = fileDialog.getFileNames();

            if (filenames.length == 0)
                return;

            List<Extractor.InputFile> files = new ArrayList<>();
            for (String filename : filenames)
                files.add(new Extractor.InputFile(new File(fileDialog.getFilterPath(), filename)));

            ArrayList<Exception> errors = new ArrayList<>();
            List<Extractor.Item> items = extractor.extract(files, errors);

            Map<Extractor, List<Extractor.Item>> result = new HashMap<>();
            result.put(extractor, items);

            Map<File, List<Exception>> e = new HashMap<>();
            if (!errors.isEmpty())
                e.put(files.get(0).getFile(), errors);

            IPreferenceStore preferences = portPart.getPreferenceStore();

            ImportExtractedItemsWizard wizard = new ImportExtractedItemsWizard(client, preferences, result, e);
            Dialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
            dialog.open();
        }
        catch (IllegalArgumentException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, e.getMessage());
        }
    }
}
