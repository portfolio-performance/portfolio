package name.abuchen.portfolio.ui.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

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

import name.abuchen.portfolio.datatransfer.csv.CSVConfig;
import name.abuchen.portfolio.datatransfer.csv.CSVConfigManager;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;

public class CSVConfigurationsMenuContribution
{
    @Inject
    private CSVConfigManager configManager;

    @Inject
    private ECommandService commandService;

    @Inject
    private EModelService modelService;

    private MCommand mCommand = null;
    private MCommandParameter mParameter = null;

    @PostConstruct
    public void init(MApplication app)
    {
        for (MCommand c : app.getCommands())
            if (c.getElementId().equals(UIConstants.Command.IMPORT_CSV))
                mCommand = c;

        if (mCommand == null)
            return;

        for (MCommandParameter param : mCommand.getParameters())
            if (param.getElementId().equals(UIConstants.Parameter.NAME))
                mParameter = param;
    }

    @AboutToShow
    public void aboutToShow(List<MMenuElement> items)
    {
        if (mCommand == null || mParameter == null)
            return;

        // problem: we must pass a string as parameter to the command, but
        // there is no unique identifier for a CSVConfig. Instead of
        // introducing one, we use the index within the list. As the menu is
        // created dynamically, the index should not be different between
        // showing the menu and opening the wizard page.

        int index = 0;

        for (CSVConfig config : configManager.getBuiltInConfigurations())
            createMenu(items, index++, config);

        items.add(MMenuFactory.INSTANCE.createMenuSeparator());

        for (CSVConfig config : configManager.getUserSpecificConfigurations())
            createMenu(items, index++, config);
    }

    private void createMenu(List<MMenuElement> items, int index, CSVConfig config)
    {
        MParameter parameter = modelService.createModelElement(MParameter.class);
        parameter.setName(mParameter.getElementId());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIConstants.Parameter.NAME, String.valueOf(index));
        ParameterizedCommand command = commandService.createCommand(UIConstants.Command.IMPORT_CSV, parameters);

        MHandledMenuItem menuItem = MMenuFactory.INSTANCE.createHandledMenuItem();
        menuItem.setToBeRendered(true);
        menuItem.setLabel(config.getLabel());
        menuItem.setTooltip(""); //$NON-NLS-1$
        menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        menuItem.getParameters().add(parameter);
        menuItem.setWbCommand(command);
        menuItem.setCommand(mCommand);

        items.add(menuItem);
    }
}
