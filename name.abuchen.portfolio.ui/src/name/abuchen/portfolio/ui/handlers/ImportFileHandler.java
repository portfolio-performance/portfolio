package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.FileExtractorService;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPart;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

public class ImportFileHandler
{

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    FileExtractorService fileExtractorService,
                    @Named("name.abuchen.portfolio.ui.param.extractor-type") String extractorType)
    {
        doExecute(part, shell, fileExtractorService, extractorType, false);
    }

    /* package */ void doExecute(MPart part, Shell shell, FileExtractorService fileExtractorService,
                    String extractorType, boolean isLegacyMode)
    {
        Client client = MenuHelper.getActiveClient(part);
        if (client == null)
            return;

        Map<String, Extractor> extractors = fileExtractorService.getExtractors(extractorType);

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        fileDialog.setText(Messages.bind(Messages.FileImportWizardAssistant, extractorType.toUpperCase()));
        fileDialog.setFilterNames(new String[] {
                        Messages.bind(Messages.FileImportFilterName, extractorType.toUpperCase(), extractorType.toLowerCase()) });
        fileDialog.setFilterExtensions(new String[] { "*." + extractorType.toLowerCase() });
        fileDialog.open();

        String[] fileNames = fileDialog.getFileNames();

        if (fileNames.length == 0)
            return;

        List<Extractor.InputFile> files = new ArrayList<>();
        for (String fileName : fileNames)
        {
            switch (extractorType.toLowerCase())
            {
                case "pdf":
                    files.add(new PDFInputFile(new File(fileDialog.getFilterPath(), fileName)));
                    break;
                default:
                    files.add(new Extractor.InputFile(new File(fileDialog.getFilterPath(), fileName)));
                    break;
            }
        }

        IPreferenceStore preferences = ((PortfolioPart) part.getObject()).getPreferenceStore();
        try
        {

            IRunnableWithProgress operation = monitor -> {
                monitor.beginTask(Messages.FileImportWizardMsgExtracting, files.size());

                for (Extractor.InputFile inputFile : files)
                {
                    monitor.setTaskName(inputFile.getName());

                    try
                    {
                        inputFile.parse();
                    }
                    catch (IOException e)
                    {
                        throw new IllegalArgumentException(MessageFormat.format(Messages.FileImportErrorParsingDocument,
                                        inputFile.getName()), e);
                    }

                    monitor.worked(1);
                }

                // if we just run this async, then the main window on macOS does
                // not regain focus and the menus are not usable
                new Job("") //$NON-NLS-1$
                {
                    @Override
                    protected IStatus run(IProgressMonitor monitor)
                    {
                        shell.getDisplay().asyncExec(() -> openWizard(shell, client, files, extractors.values(),
                                        preferences, isLegacyMode));
                        return Status.OK_STATUS;
                    }
                }.schedule(50);

            };

            new ProgressMonitorDialog(shell).run(true, true, operation);
        }
        catch (IllegalArgumentException | InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            MessageDialog.openError(shell, Messages.LabelError, message);
        }
    }

    protected void openWizard(Shell shell, Client client, List<Extractor.InputFile> files,
                    Collection<Extractor> extractors, IPreferenceStore preferences, boolean isLegacyMode)
    {
        ImportExtractedItemsWizard wizard = new ImportExtractedItemsWizard(client, extractors, preferences, files);
        wizard.setLegacyMode(isLegacyMode);
        Dialog wizwardDialog = new WizardDialog(shell, wizard);
        wizwardDialog.open();
    }
}
