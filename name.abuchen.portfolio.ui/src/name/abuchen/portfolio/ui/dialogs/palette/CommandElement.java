package name.abuchen.portfolio.ui.dialogs.palette;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.util.CommandAction;

class CommandElement implements Element
{
    private final IEclipseContext context;
    private final String label;
    private final Images image;
    private final String commandId;
    private final String[] parameters;

    public CommandElement(IEclipseContext context, String label, Images image, String commandId, String... parameters)
    {
        this.context = context;
        this.label = label;
        this.image = image;
        this.commandId = commandId;
        this.parameters = parameters;
    }

    @Override
    public String getTitel()
    {
        return label;
    }

    @Override
    public String getSubtitle()
    {
        return null;
    }

    @Override
    public Images getImage()
    {
        return image;
    }

    @Override
    public void execute()
    {
        // execute asynchronously to allow the command palette to close
        Display.getDefault().asyncExec(() -> CommandAction.forCommand(context, label, commandId, parameters).run());
    }
}