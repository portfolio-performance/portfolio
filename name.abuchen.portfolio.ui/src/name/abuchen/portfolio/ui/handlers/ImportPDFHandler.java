package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Named;

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
import name.abuchen.portfolio.datatransfer.pdf.PDFImportAssistant;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.FilePathHelper;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

public class ImportPDFHandler
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
        doExecute(part, shell);
    }

    /* package */ void doExecute(MPart part, Shell shell)
    {
        MenuHelper.getActiveClient(part)
                        .ifPresent(client -> runImport((PortfolioPart) part.getObject(), shell, client, null, null));
    }

    private static List<File> unzipFileToTempDir(File zipfile, File tempDir) throws IOException
    {
        List<File> extractedFiles = new ArrayList<>();
        try (FileInputStream zipfs = new FileInputStream(zipfile); ZipInputStream zipin = new ZipInputStream(zipfs))
        {
            ZipEntry entry;
            while ((entry = zipin.getNextEntry()) != null)
            {
                if (entry.isDirectory() || entry.getName() == null || !entry.getName().toLowerCase().endsWith(".pdf")) //$NON-NLS-1$
                    continue;

                Path tempFile = Paths.get(tempDir.getAbsolutePath(), entry.getName());
                Files.createDirectories(tempFile);
                Files.copy(zipin, tempFile, StandardCopyOption.REPLACE_EXISTING);
                extractedFiles.add(tempFile.toFile());
            }
        }
        return extractedFiles;
    }

    public static void runImport(PortfolioPart part, Shell shell, Client client, Account account, Portfolio portfolio)
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

        FilePathHelper helper = new FilePathHelper(part, UIConstants.Preferences.PDF_IMPORT_PATH);

        FileDialog fileDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        fileDialog.setText(Messages.PDFImportWizardAssistant);
        fileDialog.setFilterNames(new String[] { Messages.PDFImportFilterName });
        fileDialog.setFilterExtensions(new String[] { "*.pdf;*.PDF;*.zip;*.ZIP" }); //$NON-NLS-1$
        fileDialog.setFilterPath(helper.getPath());
        fileDialog.open();

        String[] filenames = fileDialog.getFileNames();

        if (filenames.length == 0)
            return;

        helper.savePath(fileDialog.getFilterPath());

        List<File> files = new ArrayList<>();
        List<File> filesToDelete = new ArrayList<>();
        try
        {
            for (String filename : filenames)
            {
                File file = new File(fileDialog.getFilterPath(), filename);
                if (filename.endsWith(".zip")) //$NON-NLS-1$
                {
                    File tempDir = Files.createTempDirectory("portfolio_import_").toFile(); //$NON-NLS-1$
                    filesToDelete.add(tempDir);
                    List<File> extractedFiles = unzipFileToTempDir(file, tempDir);
                    // Place files in front of tempDir in deletion list
                    filesToDelete.addAll(filesToDelete.size() - 1, extractedFiles);
                    files.addAll(extractedFiles);
                }
                else
                {
                    files.add(file);
                }
            }

            runImportWithFiles(part, shell, client, account, portfolio, files);
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            MessageDialog.openError(shell, Messages.LabelError, message);
        }
        finally
        {
            for (File f : filesToDelete)
            {
                boolean deleted = f.delete();
                if (!deleted)
                    f.deleteOnExit();
            }
        }
    }

    public static void runImportWithFiles(PortfolioPart part, Shell shell, Client client, Account account,
                    Portfolio portfolio, List<File> files)
    {
        // sort files to import purchase documents first (that typically create
        // securities with better names)
        files.sort(Comparator.comparing(File::lastModified).thenComparing(File::getPath));

        IPreferenceStore preferences = part.getPreferenceStore();

        try
        {
            Map<File, List<Exception>> errors = new HashMap<>();
            Map<Extractor, List<Extractor.Item>> result = new HashMap<>();

            IRunnableWithProgress operation = monitor -> {
                PDFImportAssistant assistent = new PDFImportAssistant(client, files);
                result.putAll(assistent.run(monitor, errors));
            };

            new ProgressMonitorDialog(shell).run(true, true, operation);

            shell.getDisplay().asyncExec(() -> {
                ImportExtractedItemsWizard wizard = new ImportExtractedItemsWizard(client, preferences, result, errors);
                if (account != null)
                    wizard.setTarget(account);
                if (portfolio != null)
                    wizard.setTarget(portfolio);
                Dialog wizardDialog = new WizardDialog(shell, wizard);
                wizardDialog.open();
            });
        }
        catch (IllegalArgumentException | InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            MessageDialog.openError(shell, Messages.LabelError, message);
        }
    }
}
