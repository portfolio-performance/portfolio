package name.abuchen.portfolio.ui.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandParameter;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import name.abuchen.portfolio.model.Watchlist;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class WatchlistPriceUpdateMenuContribution
{
    private EModelService modelService;
    private ECommandService commandService;

    private MCommand mCommand = null;
    private MCommandParameter filterParameter = null;
    private MCommandParameter watchlistParameter = null;

    @Inject
    public WatchlistPriceUpdateMenuContribution(EModelService modelService, ECommandService commandService)
    {
        this.modelService = modelService;
        this.commandService = commandService;
    }

    @PostConstruct
    public void init(MApplication app)
    {
        for (MCommand c : app.getCommands())
            if (c.getElementId().equals(UIConstants.Command.UPDATE_QUOTES))
                mCommand = c;

        if (mCommand == null)
            return;

        for (MCommandParameter param : mCommand.getParameters())
        {
            if (param.getElementId().equals(UIConstants.Parameter.FILTER))
                filterParameter = param;
            else if (param.getElementId().equals(UIConstants.Parameter.WATCHLIST))
                watchlistParameter = param;
        }
    }

    @AboutToShow
    public void aboutToShow(@Named(IServiceConstants.ACTIVE_PART) MPart part, List<MMenuElement> items,
                    IEclipseContext context)
    {
        var clientInput = MenuHelper.getActiveClientInput(part, false);
        if (clientInput.isEmpty())
            return;

        if (mCommand == null || filterParameter == null || watchlistParameter == null)
            return;

        var client = clientInput.get().getClient();
        var watchlists = client.getWatchlists();

        if (watchlists.isEmpty())
            return;

        for (Watchlist watchlist : watchlists)
            items.add(createMenu(watchlist));
    }

    private MMenuElement createMenu(Watchlist watchlist)
    {
        MParameter filterParam = modelService.createModelElement(MParameter.class);
        filterParam.setName(filterParameter.getName());

        MParameter watchlistParam = modelService.createModelElement(MParameter.class);
        watchlistParam.setName(watchlistParameter.getName());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIConstants.Parameter.FILTER, UpdateQuotesHandler.FilterType.WATCHLIST.name());
        parameters.put(UIConstants.Parameter.WATCHLIST, watchlist.getName());
        ParameterizedCommand command = commandService.createCommand(UIConstants.Command.UPDATE_QUOTES, parameters);

        MHandledMenuItem menuItem = MMenuFactory.INSTANCE.createHandledMenuItem();
        menuItem.setToBeRendered(true);
        menuItem.setLabel(watchlist.getName());
        menuItem.setTooltip(""); //$NON-NLS-1$
        menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        menuItem.getParameters().add(filterParam);
        menuItem.getParameters().add(watchlistParam);
        menuItem.setWbCommand(command);
        menuItem.setCommand(mCommand);

        return menuItem;
    }
}
