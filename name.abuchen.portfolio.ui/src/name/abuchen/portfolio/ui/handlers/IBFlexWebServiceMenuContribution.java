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

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.IBFlexConfigurationDialog;
import name.abuchen.portfolio.ui.dialogs.IBFlexConfigurationDialog.Credential;

public class IBFlexWebServiceMenuContribution
{
    @Inject
    private ECommandService commandService;

    @Inject
    private EModelService modelService;

    private MCommand command;
    private MCommandParameter queryIdParameter;

    @PostConstruct
    public void init(MApplication app)
    {
        for (MCommand candidate : app.getCommands())
        {
            if (candidate.getElementId().equals(UIConstants.Command.IMPORT_IB_FLEX_WEBSERVICE))
            {
                command = candidate;
                break;
            }
        }

        if (command == null)
            throw new IllegalStateException("command " + UIConstants.Command.IMPORT_IB_FLEX_WEBSERVICE + " not found");

        for (MCommandParameter parameter : command.getParameters())
        {
            if (parameter.getElementId().equals(UIConstants.Parameter.IBFLEX_QUERY_ID))
            {
                queryIdParameter = parameter;
                break;
            }
        }

        if (queryIdParameter == null)
            throw new IllegalStateException(
                            "parameter " + UIConstants.Parameter.IBFLEX_QUERY_ID + " not found on command");
    }

    @AboutToShow
    public void aboutToShow(List<MMenuElement> items)
    {
        var credentials = IBFlexConfigurationDialog.getCredentials();
        if (credentials.size() <= 1)
            return;

        for (Credential credential : credentials)
            items.add(createMenuItem(credential));
    }

    private MMenuElement createMenuItem(Credential credential)
    {
        MParameter parameter = modelService.createModelElement(MParameter.class);
        parameter.setName(queryIdParameter.getElementId());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIConstants.Parameter.IBFLEX_QUERY_ID, credential.queryId());
        ParameterizedCommand parameterizedCommand = commandService
                        .createCommand(UIConstants.Command.IMPORT_IB_FLEX_WEBSERVICE, parameters);

        MHandledMenuItem menuItem = MMenuFactory.INSTANCE.createHandledMenuItem();
        menuItem.setToBeRendered(true);
        menuItem.setLabel(credential.label());
        menuItem.setTooltip(""); //$NON-NLS-1$
        menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        menuItem.getParameters().add(parameter);
        menuItem.setWbCommand(parameterizedCommand);
        menuItem.setCommand(command);

        return menuItem;
    }
}
