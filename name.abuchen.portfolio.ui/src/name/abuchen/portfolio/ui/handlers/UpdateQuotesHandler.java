package name.abuchen.portfolio.ui.handlers;

import java.text.MessageFormat;
import java.util.EnumSet;

import jakarta.inject.Named;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuItem;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.jobs.SyncOnlineSecuritiesJob;
import name.abuchen.portfolio.ui.jobs.UpdateDividendsJob;
import name.abuchen.portfolio.ui.jobs.UpdateQuotesJob;
import name.abuchen.portfolio.ui.selection.SelectionService;

public class UpdateQuotesHandler
{
    public enum FilterType
    {
        SECURITY, ACTIVE
    }

    @CanExecute
    boolean isVisible(@Named(IServiceConstants.ACTIVE_PART) MPart part, MMenuItem menuItem,
                    SelectionService selectionService)
    {
        var clientInput = MenuHelper.getActiveClientInput(part, false);
        if (clientInput.isEmpty())
            return false;

        if (UIConstants.ElementId.MENU_ITEM_UPDATE_QUOTES_SELECTED_SECURITIES.equals(menuItem.getElementId()))
        {
            var selection = selectionService.getSelection(clientInput.get().getClient());

            menuItem.setLabel(MessageFormat.format(Messages.MenuUpdatePricesForSelectedInstruments,
                            selection.isPresent() ? selection.get().getSecurities().size() : 0));

            // disable the menu if there is currently no selection
            return selection.isPresent();
        }

        return true;
    }

    @Execute
    public void execute(@Named(IServiceConstants.ACTIVE_PART) MPart part,
                    @Named(IServiceConstants.ACTIVE_SHELL) Shell shell, SelectionService selectionService,
                    @Named(UIConstants.Parameter.FILTER) @Optional String filter)
    {
        MenuHelper.getActiveClient(part).ifPresent(client -> {

            if (FilterType.SECURITY.name().equalsIgnoreCase(filter))
            {
                selectionService.getSelection(client).ifPresent(s -> {
                    new UpdateQuotesJob(client, s.getSecurities()).schedule();
                    new UpdateDividendsJob(client, s.getSecurities()).schedule(5000);
                });
            }
            else if (FilterType.ACTIVE.name().equalsIgnoreCase(filter))
            {
                new UpdateQuotesJob(client, s -> !s.isRetired(), EnumSet.allOf(UpdateQuotesJob.Target.class))
                                .schedule();
                new UpdateDividendsJob(client, s -> !s.isRetired()).schedule(5000);
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
