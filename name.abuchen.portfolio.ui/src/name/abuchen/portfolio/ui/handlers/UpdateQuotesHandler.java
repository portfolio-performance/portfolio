package name.abuchen.portfolio.ui.handlers;

import java.util.EnumSet;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

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
                    @Named("name.abuchen.portfolio.ui.param.only-current-security") @Optional String onlyCurrentSecurity)
    {
        MenuHelper.getActiveClient(part).ifPresent(client -> {
            if (Boolean.parseBoolean(onlyCurrentSecurity))
            {
                selectionService.getSelection(client)
                                .ifPresent(s -> new UpdateQuotesJob(client, s.getSecurity()).schedule());
            }
            else
            {
                new UpdateQuotesJob(client, EnumSet.allOf(UpdateQuotesJob.Target.class)).schedule();
            }
        });
    }
}
