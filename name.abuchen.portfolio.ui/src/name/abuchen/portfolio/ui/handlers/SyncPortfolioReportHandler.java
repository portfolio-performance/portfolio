package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.online.portfolioreport.PortfolioReportSync;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.ClientInput;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;

public class SyncPortfolioReportHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.PORTFOLIO_REPORT_API_KEY) String portfolioReportApiKey)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.PORTFOLIO_REPORT_API_URL) String portfolioReportApiUrl,
                    @org.eclipse.e4.core.di.annotations.Optional @Preference(value = UIConstants.Preferences.PORTFOLIO_REPORT_API_KEY) String portfolioReportApiKey)
    {

        if (portfolioReportApiUrl == null || portfolioReportApiUrl.isEmpty())
        {
            MessageDialog.openError(shell, Messages.LabelError, Messages.PortfolioReportMissingAPIURL);
            return;
        }

        if (portfolioReportApiKey == null || portfolioReportApiKey.isEmpty())
        {
            MessageDialog.openError(shell, Messages.LabelError, Messages.PortfolioReportMissingAPIKey);
            return;
        }

        Optional<ClientInput> input = MenuHelper.getActiveClientInput(part);
        if (!input.isPresent())
            return;

        ClientInput clientInput = input.get();

        IRunnableWithProgress operation = monitor -> {
            try
            {
                new PortfolioReportSync(portfolioReportApiUrl, portfolioReportApiKey, clientInput.getClient(),
                                clientInput.getLabel()).sync(monitor);
            }
            catch (IOException e)
            {
                PortfolioPlugin.log(e);

                Display.getDefault().asyncExec(
                                () -> MessageDialog.openError(ActiveShell.get(), Messages.LabelError, e.getMessage()));
            }
        };

        try
        {
            new ProgressMonitorDialog(shell).run(true, true, operation);
        }
        catch (InvocationTargetException | InterruptedException e)
        {
            PortfolioPlugin.log(e);
            String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            MessageDialog.openError(shell, Messages.LabelError, message);
        }
    }
}
