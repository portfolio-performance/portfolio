package name.abuchen.portfolio.ui.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.ui.di.AboutToShow;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MCommandParameter;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuFactory;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.RecentFilesCache;

public class RecentFilesMenuContribution
{
    @Inject
    private RecentFilesCache recentFiles;

    @Inject
    private ECommandService commandService;

    @Inject
    private EModelService modelService;

    MCommand mCommand = null;
    MCommandParameter mParameter = null;

    @PostConstruct
    public void init(MApplication app)
    {
        for (MCommand c : app.getCommands())
            if (c.getElementId().equals(UIConstants.Command.OPEN_RECENT_FILE))
                mCommand = c;

        if (mCommand == null)
            return;

        for (MCommandParameter param : mCommand.getParameters())
            if (param.getElementId().equals(UIConstants.Parameter.FILE))
                mParameter = param;
    }

    @AboutToShow
    public void aboutToShow(List<MMenuElement> items)
    {
        if (mCommand == null || mParameter == null)
            return;

        for (String file : recentFiles.getRecentFiles())
        {
            MParameter parameter = modelService.createModelElement(MParameter.class);
            parameter.setName(mParameter.getElementId());

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(UIConstants.Parameter.FILE, file);
            ParameterizedCommand command = commandService.createCommand(UIConstants.Command.OPEN_RECENT_FILE,
                            parameters);

            MHandledMenuItem menuItem = MMenuFactory.INSTANCE.createHandledMenuItem();
            menuItem.setToBeRendered(true);
            menuItem.setLabel(file);
            menuItem.setTooltip(""); //$NON-NLS-1$
            menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
            menuItem.getParameters().add(parameter);
            menuItem.setWbCommand(command);
            menuItem.setCommand(mCommand);

            items.add(menuItem);
        }
    }
}
