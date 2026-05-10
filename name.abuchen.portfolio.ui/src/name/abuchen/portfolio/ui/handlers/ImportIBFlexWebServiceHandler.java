package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexStatementExtractor;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient.IBFlexException;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
import name.abuchen.portfolio.ui.preferences.IBFlexConfiguration;
import name.abuchen.portfolio.ui.preferences.IBFlexConfiguration.Credential;
import name.abuchen.portfolio.ui.wizards.datatransfer.ImportExtractedItemsWizard;

public class ImportIBFlexWebServiceHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Named(UIConstants.Parameter.IBFLEX_QUERY_ID) String queryId)
    {
        MenuHelper.getActiveClient(part)
                        .ifPresent(client -> runImport((PortfolioPart) part.getObject(), shell, client, queryId));
    }

    private void runImport(PortfolioPart part, Shell shell, Client client, String selectedQueryId)
    {
        // Check prerequisites
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

        // Get credentials from preferences
        var credentials = IBFlexConfiguration.getCredentials();

        if (credentials.isEmpty())
        {
            MessageDialog.openError(shell, Messages.LabelError, Messages.IBFlexCredentialsNotConfigured);
            return;
        }

        Credential credential = resolveCredential(shell, credentials, selectedQueryId);
        if (credential == null)
            return;

        // Fetch and import
        final String finalToken = credential.token();
        final String finalQueryId = credential.queryId();
        LocalDateTime lastImportDate = IBFlexConfiguration.getLastImportDate(client, finalQueryId);

        try
        {
            IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client, lastImportDate);
            List<Extractor.Item> items = new ArrayList<>();
            List<Exception> errors = new ArrayList<>();

            var workerThread = new AtomicReference<Thread>();
            var dialog = new ProgressMonitorDialog(shell)
            {
                @Override
                protected void cancelPressed()
                {
                    super.cancelPressed();
                    var t = workerThread.get();
                    if (t != null)
                        t.interrupt();
                }
            };
            dialog.run(true, true, monitor -> {
                workerThread.set(Thread.currentThread());
                monitor.beginTask(Messages.IBFlexFetching, IProgressMonitor.UNKNOWN);

                File tempFile = null;
                try
                {
                    var webClient = new IBFlexWebServiceClient();
                    var xml = webClient.fetchStatement(finalToken, finalQueryId);

                    tempFile = Files.createTempFile("ibflex_", ".xml").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
                    Files.writeString(tempFile.toPath(), xml, StandardCharsets.UTF_8);

                    items.addAll(extractor.extract(List.of(new Extractor.InputFile(tempFile)), errors));
                }
                catch (IBFlexException | IOException e)
                {
                    throw new InvocationTargetException(e);
                }
                finally
                {
                    monitor.done();
                    deleteTempFile(tempFile);
                }
            });

            // Check if any items were extracted
            if (items.isEmpty() && errors.isEmpty())
            {
                String message = lastImportDate != null
                                ? MessageFormat.format(Messages.IBFlexNoNewItemsExtracted, lastImportDate)
                                : Messages.IBFlexNoItemsExtracted;
                MessageDialog.openInformation(shell, Messages.LabelInfo, message);
                return;
            }

            // Find max date among filtered items for later storage
            LocalDateTime maxDate = items.stream() //
                            .map(Extractor.Item::getDate) //
                            .filter(d -> d != null) //
                            .max(LocalDateTime::compareTo).orElse(null);

            // Show import wizard
            IPreferenceStore preferences = part.getPreferenceStore();
            ImportExtractedItemsWizard wizard = new ImportExtractedItemsWizard(client, preferences,
                            Map.of(extractor, items),
                            errors.isEmpty() ? Map.of() : Map.of(new File("IBFlex Web Service"), errors)); //$NON-NLS-1$
            if (new WizardDialog(shell, wizard).open() == WizardDialog.OK && maxDate != null)
            {
                IBFlexConfiguration.setLastImportDate(client, finalQueryId, maxDate);
            }
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();

            // If operation was cancelled by user, silently return
            if (cause instanceof IBFlexException ibException && ibException.isCanceled())
                return;

            PortfolioPlugin.log(e);
            MessageDialog.openError(shell, Messages.LabelError, formatErrorMessage(cause != null ? cause : e));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            PortfolioPlugin.log(e);
        }
    }

    static String formatErrorMessage(Throwable error)
    {
        if (error instanceof IBFlexException ibException)
        {
            if (ibException.isTokenExpired())
                return MessageFormat.format(Messages.IBFlexTokenExpired, ibException.getMessage());

            if (ibException.isRequestInvalid())
                return MessageFormat.format(Messages.IBFlexRequestInvalid, ibException.getMessage());
        }

        return error != null ? error.getMessage() : null;
    }

    private Credential resolveCredential(Shell shell, List<Credential> credentials, String selectedQueryId)
    {
        if (selectedQueryId != null && !selectedQueryId.isBlank())
        {
            String normalizedQueryId = selectedQueryId.trim();

            for (Credential candidate : credentials)
            {
                if (candidate.queryId().equals(normalizedQueryId))
                    return candidate;
            }
        }

        if (credentials.size() == 1)
            return credentials.get(0);

        return selectCredential(shell, credentials);
    }

    private Credential selectCredential(Shell shell, List<Credential> credentials)
    {
        var labels = credentials.stream().map(Credential::label).toArray(String[]::new);

        var dialog = new MessageDialog(shell, Messages.IBFlexFetching, null, Messages.IBFlexQueryId,
                        MessageDialog.QUESTION, 0, labels);

        int index = dialog.open();
        if (index == SWT.DEFAULT || index < 0 || index >= credentials.size())
            return null;

        return credentials.get(index);
    }

    private void deleteTempFile(File file)
    {
        if (file == null)
            return;

        try
        {
            Files.deleteIfExists(file.toPath());
        }
        catch (IOException e)
        {
            // Best effort - log but don't fail
            PortfolioPlugin.log(e);
        }
    }
}
