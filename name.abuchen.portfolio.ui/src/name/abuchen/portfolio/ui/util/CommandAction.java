package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.IParameter;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public class CommandAction extends Action
{
    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    private final String commandId;
    private final String[] parameters;

    private CommandAction(String text, Images image, String commandId, String... parameters)
    {
        super(text);

        if (image != null)
            setImageDescriptor(image.descriptor());

        this.commandId = commandId;
        this.parameters = parameters;
    }

    public static Action forCommand(IEclipseContext context, String text, String commandId, String... parameters)
    {
        CommandAction action = new CommandAction(text, null, commandId, parameters);
        ContextInjectionFactory.inject(action, context);
        return action;
    }

    public static Action forCommand(IEclipseContext context, Images image, String text, String commandId,
                    String... parameters)
    {
        CommandAction action = new CommandAction(text, image, commandId, parameters);
        ContextInjectionFactory.inject(action, context);
        return action;
    }

    @Override
    public void run()
    {
        if (commandService == null || handlerService == null)
            throw new NullPointerException("Command actions have have parameters injected first"); //$NON-NLS-1$

        try
        {
            Command cmd = commandService.getCommand(commandId);

            List<Parameterization> parameterizations = new ArrayList<>();
            if (parameters != null)
            {
                for (int ii = 0; ii < parameters.length; ii = ii + 2)
                {
                    IParameter p = cmd.getParameter(parameters[ii]);
                    parameterizations.add(new Parameterization(p, parameters[ii + 1]));
                }
            }

            ParameterizedCommand pCmd = new ParameterizedCommand(cmd,
                            parameterizations.toArray(new Parameterization[0]));

            if (handlerService.canExecute(pCmd))
                handlerService.executeHandler(pCmd);
        }
        catch (NotDefinedException e)
        {
            PortfolioPlugin.log(e);
            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LabelError, e.getMessage());
        }
    }
}
