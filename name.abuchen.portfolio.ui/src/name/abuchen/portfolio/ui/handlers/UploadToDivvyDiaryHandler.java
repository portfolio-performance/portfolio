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
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.online.impl.DivvyDiaryUploader;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.jobs.AbstractClientJob;

@SuppressWarnings("restriction")
public class UploadToDivvyDiaryHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Optional @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        return MenuHelper.isClientPartActive(part) && divvyDiaryApiKey != null;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell,
                    @Optional @Preference(value = UIConstants.Preferences.DIVVYDIARY_API_KEY) String divvyDiaryApiKey)
    {
        if (divvyDiaryApiKey == null)
            return;

        MenuHelper.getActiveClientInput(part).ifPresent(clientInput -> {
            new AbstractClientJob(clientInput.getClient(), "Uploading holdings to DivvyDiary")
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
                        return Status.OK_STATUS;
                    }
                    catch (IOException e)
                    {
                        PortfolioPlugin.log(e);
                        return new Status(Status.ERROR, PortfolioPlugin.PLUGIN_ID, e.getMessage(), e);
                    }
                }
            }.schedule();
        });
    }
}
