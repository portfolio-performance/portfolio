package name.abuchen.portfolio.ui.handlers;

import java.io.IOException;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.online.impl.DivvyDiaryUploader;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;
import name.abuchen.portfolio.ui.util.swt.ActiveShell;

public class UploadToDivvyDiaryHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Optional @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        if (divvyDiaryApiKey == null)
        {
            MessageDialog.openInformation(shell, Messages.LabelInfo, Messages.DivvyDiaryMissingAPIKey);
            return;
        }

        if (!MessageDialog.openConfirm(shell, Messages.LabelInfo, Messages.DivvyDiaryConfirmUpload))
            return;

        MenuHelper.getActiveClientInput(part).ifPresent(clientInput -> {
            new AbstractClientJob(clientInput.getClient(), Messages.DivvyDiaryMsgUploading)
            {
                @Override
                protected IStatus run(IProgressMonitor monitor)
                {
                    try
                    {
                        CurrencyConverter converter = new CurrencyConverterImpl(
                                        clientInput.getExchangeRateProviderFacory(),
                                        clientInput.getClient().getBaseCurrency());
                        new DivvyDiaryUploader().upload(clientInput.getClient(), converter, divvyDiaryApiKey);

                        Display.getDefault().asyncExec(() -> MessageDialog.openInformation(ActiveShell.get(),
                                        Messages.LabelInfo, Messages.DivvyDiaryUploadSuccessfulMsg));

                        return Status.OK_STATUS;
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);

                        Display.getDefault().asyncExec(() -> MessageDialog.openError(ActiveShell.get(),
                                        Messages.LabelError, e.getMessage()));

                        return new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e);
                    }
                }
            }.schedule();
        });

    }
}
