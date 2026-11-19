package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Named;

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
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexDateRange;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexStatementExtractor;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient.IBFlexException;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.money.Values;
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

        // Fetch everything since the last import
        final String finalToken = credential.token();
        final String finalQueryId = credential.queryId();
        LocalDateTime lastImportDate = IBFlexConfiguration.getLastImportDate(client, finalQueryId);
        var range = IBFlexDateRange.sinceLastImport(lastImportDate, IBFlexDateRange.today());

        var webClient = new IBFlexWebServiceClient();
        var canceled = new AtomicBoolean();

        try
        {
            IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);
            List<Extractor.Item> items = new ArrayList<>();
            List<Exception> errors = new ArrayList<>();

            var workerThread = new AtomicReference<Thread>();
            var dialog = new ProgressMonitorDialog(shell)
            {
                @Override
                protected void cancelPressed()
                {
                    super.cancelPressed();
                    canceled.set(true);
                    // abort the request in flight; the interrupt wakes up the
                    // worker while waiting between retries
                    webClient.cancel();
                    var t = workerThread.get();
                    if (t != null)
                        t.interrupt();
                }
            };
            dialog.run(true, true, monitor -> {
                workerThread.set(Thread.currentThread());
                // one unit of work per request to IB plus one for extracting
                int requestCount = range.split().size();
                monitor.beginTask(Messages.IBFlexFetching, requestCount + 1);

                var tempFiles = new ArrayList<File>();
                try
                {
                    var statements = webClient.fetchStatements(finalToken, finalQueryId, range,
                                    (current, total, chunk) -> {
                                        if (current > 1)
                                            monitor.worked(1);
                                        var from = Values.Date.format(chunk.from());
                                        var to = Values.Date.format(chunk.to());
                                        monitor.subTask(total > 1
                                                        ? MessageFormat.format(Messages.IBFlexFetchingRangePart,
                                                                        from, to, current, total)
                                                        : MessageFormat.format(Messages.IBFlexFetchingRange, from,
                                                                        to));
                                    });
                    monitor.worked(1);
                    monitor.subTask("");

                    var inputFiles = new ArrayList<Extractor.InputFile>();
                    for (var xml : statements)
                    {
                        var tempFile = Files.createTempFile("ibflex_", ".xml").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
                        tempFiles.add(tempFile);
                        Files.writeString(tempFile.toPath(), xml, StandardCharsets.UTF_8);
                        inputFiles.add(new Extractor.InputFile(tempFile));
                    }

                    items.addAll(extractor.extract(inputFiles, errors));

                    // do not open the import wizard if canceled while extracting
                    if (canceled.get())
                        throw new InterruptedException();
                }
                catch (IBFlexException | IOException e)
                {
                    throw new InvocationTargetException(e);
                }
                finally
                {
                    monitor.done();
                    tempFiles.forEach(this::deleteTempFile);
                }
            });

            // Check if any items were extracted
            if (items.isEmpty() && errors.isEmpty())
            {
                MessageDialog.openInformation(shell, Messages.LabelInfo,
                                MessageFormat.format(Messages.IBFlexNoItemsExtracted, Values.Date.format(range.from()),
                                                Values.Date.format(range.to())));
                return;
            }

            // Show import wizard
            IPreferenceStore preferences = part.getPreferenceStore();
            ImportExtractedItemsWizard wizard = new ImportExtractedItemsWizard(client, preferences,
                            Map.of(extractor, items),
                            errors.isEmpty() ? Map.of() : Map.of(new File("IBFlex Web Service"), errors), //$NON-NLS-1$
                            new HashMap<>(), part);
            if (new WizardDialog(shell, wizard).open() == WizardDialog.OK)
            {
                // Remember the latest extracted transaction, whether or not the
                // user imported it: items the user did not import are not
                // offered again. The next import starts on that day, so items
                // IB reports later for that day are not missed.
                items.stream() //
                                .map(Extractor.Item::getDate) //
                                .filter(Objects::nonNull) //
                                .max(LocalDateTime::compareTo) //
                                .filter(date -> lastImportDate == null || date.isAfter(lastImportDate))
                                .ifPresent(date -> IBFlexConfiguration.setLastImportDate(client, finalQueryId, date));
            }
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();

            // If operation was cancelled by user, silently return. Other errors
            // after cancel (e.g. writing the temp file while interrupted) are
            // a consequence of the cancel, too.
            if (canceled.get() || cause instanceof IBFlexException ibException && ibException.isCanceled())
                return;

            // log the actual error, not the wrapper without a message
            Throwable error = cause != null ? cause : e;
            PortfolioPlugin.log(formatLogMessage(error), error);
            MessageDialog.openError(shell, Messages.LabelError, formatErrorMessage(error));
        }
        catch (InterruptedException e)
        {
            if (canceled.get())
                return;

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

    static String formatLogMessage(Throwable error)
    {
        if (error instanceof IBFlexException ibException && ibException.getErrorCode() > 0)
            return MessageFormat.format("IB Flex Web Service error {0}: {1}", //
                            String.valueOf(ibException.getErrorCode()), ibException.getMessage());

        return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
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
