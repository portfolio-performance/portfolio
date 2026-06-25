package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.pdf.PDFImportAssistant;
import name.abuchen.portfolio.datatransfer.pdf.PDFInputFile;
import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.FilePathHelper;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportWizardDialog;

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
        Path tempDirPath = tempDir.toPath().normalize();
        try (FileInputStream zipfs = new FileInputStream(zipfile); ZipInputStream zipin = new ZipInputStream(zipfs))
        {
            ZipEntry entry;
            while ((entry = zipin.getNextEntry()) != null)
            {
                if (entry.isDirectory() || entry.getName() == null || !entry.getName().toLowerCase().endsWith(".pdf")) //$NON-NLS-1$
                    continue;

                Path tempFile = tempDirPath.resolve(entry.getName()).normalize();
                if (!tempFile.startsWith(tempDirPath))
                    throw new IOException("Invalid zip entry: " + entry.getName()); //$NON-NLS-1$

                Files.createDirectories(tempFile.getParent());
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
                if (filename.toLowerCase(Locale.ROOT).endsWith(".zip")) //$NON-NLS-1$
                {
                    File tempDir = Files.createTempDirectory("portfolio_import_").toFile(); //$NON-NLS-1$
                    filesToDelete.add(tempDir);
                    files.addAll(unzipFileToTempDir(file, tempDir));
                }
                else
                {
                    files.add(file);
                }
            }

            List<File> filesToDeleteAfterImport = List.copyOf(filesToDelete);
            runImportWithFiles(part, shell, client, account, portfolio, files,
                            () -> deleteFiles(filesToDeleteAfterImport));
            filesToDelete.clear();
        }
        catch (IOException e)
        {
            PortfolioPlugin.log(e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            MessageDialog.openError(shell, Messages.LabelError, message);
        }
        finally
        {
            deleteFiles(filesToDelete);
        }
    }

    public static void runImportWithFiles(PortfolioPart part, Shell shell, Client client, Account account,
                    Portfolio portfolio, List<File> files)
    {
        runImportWithFiles(part, shell, client, account, portfolio, files, () -> {
        });
    }

    private static void runImportWithFiles(PortfolioPart part, Shell shell, Client client, Account account,
                    Portfolio portfolio, List<File> files, Runnable afterImportDialog)
    {
        // sort files to import purchase documents first (that typically create
        // securities with better names)
        files.sort(Comparator.comparing(File::lastModified).thenComparing(File::getPath));

        IPreferenceStore preferences = part.getPreferenceStore();

        try
        {
            Map<File, List<Exception>> errors = new HashMap<>();
            Map<Extractor, List<Extractor.Item>> result = new HashMap<>();
            Map<File, PDFInputFile> failedFiles = new HashMap<>();

            IRunnableWithProgress operation = monitor -> {
                PDFImportAssistant assistent = new PDFImportAssistant(client, files);
                result.putAll(assistent.run(monitor, errors));
                failedFiles.putAll(assistent.getFailedInputFiles());
            };

            new ProgressMonitorDialog(shell).run(true, true, operation);

            Display.getDefault().asyncExec(() -> {
                try
                {
                    var wizard = new ImportExtractedItemsWizard(client, preferences, result, errors, failedFiles, part);
                    part.inject(wizard);
                    if (account != null)
                        wizard.setTarget(account);
                    if (portfolio != null)
                        wizard.setTarget(portfolio);
                    Dialog wizardDialog = new ImportWizardDialog(shell, wizard);
                    wizardDialog.open();
                }
                finally
                {
                    afterImportDialog.run();
                }
            });
        }
        catch (IllegalArgumentException | InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            MessageDialog.openError(shell, Messages.LabelError, message);
        }
    }

    private static void deleteFiles(List<File> files)
    {
        for (File f : files)
            deleteFile(f);
    }

    private static void deleteFile(File file)
    {
        if (!file.exists())
            return;

        if (file.isDirectory())
        {
            File[] children = file.listFiles();
            if (children != null)
            {
                for (File child : children)
                    deleteFile(child);
            }
        }

        boolean deleted = file.delete();
        if (!deleted)
            file.deleteOnExit();
    }
}
