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

import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.DomainElement;

public class NewDomainElementMenuContribution
{
    @Inject
    private EModelService modelService;

    @Inject
    private ECommandService commandService;

    private MCommand mCommand = null;
    private MCommandParameter mParameter = null;

    @PostConstruct
    public void init(MApplication app)
    {
        for (MCommand c : app.getCommands())
            if (c.getElementId().equals(UIConstants.Command.NEW_DOMAIN_ELEMENT))
                mCommand = c;

        if (mCommand == null)
            return;

        for (MCommandParameter param : mCommand.getParameters())
            if (param.getElementId().equals(UIConstants.Parameter.TYPE))
                mParameter = param;
    }

    @AboutToShow
    public void aboutToShow(@Named(IServiceConstants.ACTIVE_PART) MPart part, List<MMenuElement> items,
                    IEclipseContext context)
    {
        if (!MenuHelper.getActiveClientInput(part, false).isPresent())
            return;

        if (mCommand == null || mParameter == null)
            return;

        for (DomainElement item : DomainElement.values())
            items.add(createMenu(item));
    }

    private MMenuElement createMenu(DomainElement element)
    {
        MParameter parameter = modelService.createModelElement(MParameter.class);
        parameter.setName(mParameter.getElementId());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(UIConstants.Parameter.TYPE, element.name());
        ParameterizedCommand command = commandService.createCommand(UIConstants.Command.NEW_DOMAIN_ELEMENT,
                        parameters);

        MHandledMenuItem menuItem = MMenuFactory.INSTANCE.createHandledMenuItem();
        menuItem.setToBeRendered(true);
        menuItem.setLabel(element.getMenuLabel());
        menuItem.setTooltip(""); //$NON-NLS-1$
        menuItem.setContributorURI("platform:/plugin/" + PortfolioPlugin.PLUGIN_ID); //$NON-NLS-1$
        menuItem.getParameters().add(parameter);
        menuItem.setWbCommand(command);
        menuItem.setCommand(mCommand);

        return menuItem;
    }

}
