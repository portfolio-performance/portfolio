package name.abuchen.portfolio.ui.handlers;

import java.util.EnumSet;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.jobs.SyncOnlineSecuritiesJob;
import name.abuchen.portfolio.ui.jobs.UpdateDividendsJob;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.selection.SelectionService;

public class UpdateQuotesHandler
{
    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part)
    {
        return MenuHelper.isClientPartActive(part);
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, SelectionService selectionService,
                    @Named("name.abuchen.portfolio.ui.param.filter") @Optional String filter)
    {
        MenuHelper.getActiveClient(part).ifPresent(client -> {

            if ("security".equals(filter)) //$NON-NLS-1$
            {
                selectionService.getSelection(client)
                                .ifPresent(s -> new UpdateQuotesJob(client, s.getSecurity()).schedule());
            }
            else if ("active".equals(filter)) //$NON-NLS-1$
            {
                new UpdateQuotesJob(client, s -> !s.isRetired(), EnumSet.allOf(UpdateQuotesJob.Target.class))
                                .schedule();
            }
            else
            {
                new UpdateQuotesJob(client, EnumSet.allOf(UpdateQuotesJob.Target.class)).schedule();
                new SyncOnlineSecuritiesJob(client).schedule(2000);
                new UpdateDividendsJob(client).schedule(5000);
            }
        });
    }
}
