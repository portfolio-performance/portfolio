package name.abuchen.portfolio.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.datatransfer.Extractor;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexStatementExtractor;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient;
import name.abuchen.portfolio.datatransfer.ibflex.IBFlexWebServiceClient.IBFlexException;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.dialogs.IBFlexConfigurationDialog;
import name.abuchen.portfolio.ui.dialogs.IBFlexConfigurationDialog.IBFlexModel;
import name.abuchen.portfolio.ui.editor.PortfolioPart;
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
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell)
    {
        MenuHelper.getActiveClient(part).ifPresent(client -> runImport((PortfolioPart) part.getObject(), shell, client));
    }

    private void runImport(PortfolioPart part, Shell shell, Client client)
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

        // Check if credentials are configured
        String token = IBFlexModel.getToken(client);
        String queryId = IBFlexModel.getQueryId(client);

        if (token == null || token.isBlank() || queryId == null || queryId.isBlank())
        {
            // Show configuration dialog
            var dialog = new IBFlexConfigurationDialog(shell, client);
            if (dialog.open() != Dialog.OK)
                return;

            token = IBFlexModel.getToken(client);
            queryId = IBFlexModel.getQueryId(client);

            if (token == null || token.isBlank() || queryId == null || queryId.isBlank())
                return;
        }

        // Fetch and import
        final String finalToken = token;
        final String finalQueryId = queryId;

        try
        {
            IBFlexStatementExtractor extractor = new IBFlexStatementExtractor(client);
            List<Extractor.Item> items = new ArrayList<>();
            List<Exception> errors = new ArrayList<>();

            new ProgressMonitorDialog(shell).run(true, true, monitor -> {
                monitor.beginTask(Messages.IBFlexFetching, IProgressMonitor.UNKNOWN);

                File tempFile = null;
                try
                {
                    IBFlexWebServiceClient webClient = new IBFlexWebServiceClient();
                    String xml = webClient.fetchStatement(finalToken, finalQueryId, monitor::isCanceled);

                    if (monitor.isCanceled())
                        throw new InterruptedException();

                    tempFile = Files.createTempFile("ibflex_", ".xml").toFile(); //$NON-NLS-1$ //$NON-NLS-2$
                    Files.writeString(tempFile.toPath(), xml, StandardCharsets.UTF_8);

                    items.addAll(extractor.extract(List.of(new Extractor.InputFile(tempFile)), errors));
                }
                catch (IBFlexException e)
                {
                    throw new InvocationTargetException(e);
                }
                catch (IOException e)
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
                MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.IBFlexNoItemsExtracted);
                return;
            }

            // Show import wizard
            IPreferenceStore preferences = part.getPreferenceStore();
            ImportExtractedItemsWizard wizard = new ImportExtractedItemsWizard(client, preferences,
                            Map.of(extractor, items),
                            errors.isEmpty() ? Map.of() : Map.of(new File("IBFlex Web Service"), errors)); //$NON-NLS-1$
            new WizardDialog(shell, wizard).open();
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();

            // If operation was cancelled by user, silently return
            if (cause instanceof IBFlexException ibException && ibException.isCanceled())
                return;

            PortfolioPlugin.log(e);

            if (cause instanceof IBFlexException ibException && ibException.isTokenExpired())
            {
                MessageDialog.openError(shell, Messages.LabelError,
                                MessageFormat.format(Messages.IBFlexTokenExpired, cause.getMessage()));
            }
            else
            {
                String message = cause != null ? cause.getMessage() : e.getMessage();
                MessageDialog.openError(shell, Messages.LabelError, message);
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            PortfolioPlugin.log(e);
        }
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
