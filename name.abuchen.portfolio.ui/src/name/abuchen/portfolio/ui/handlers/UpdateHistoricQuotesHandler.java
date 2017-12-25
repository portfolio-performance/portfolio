package name.abuchen.portfolio.ui.handlers;

import java.util.EnumSet;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.UpdateQuotesJob;

public class UpdateHistoricQuotesHandler
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
        Client client = MenuHelper.getActiveClient(part);
        if (client == null)
            return;

        new UpdateQuotesJob(client, EnumSet.of(UpdateQuotesJob.Target.HISTORIC)).schedule();
    }
}
