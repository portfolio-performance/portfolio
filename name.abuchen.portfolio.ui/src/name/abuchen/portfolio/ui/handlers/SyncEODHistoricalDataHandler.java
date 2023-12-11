package name.abuchen.portfolio.ui.handlers;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.jobs.SyncEODHistoricalDataJob;

public class SyncEODHistoricalDataHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Preference(value = UIConstants.Preferences.EOD_HISTORICAL_DATA_API_KEY) String apiToken)
    {

        if (apiToken == null || apiToken.isEmpty())
        {
            MessageDialog.openError(shell, Messages.LabelError, Messages.EODHistoricalDataMissingAPIKey);
            return;
        }

        MenuHelper.getActiveClientInput(part).ifPresent(
                        clientInput -> new SyncEODHistoricalDataJob(clientInput.getClient(), apiToken).schedule());
    }
}
