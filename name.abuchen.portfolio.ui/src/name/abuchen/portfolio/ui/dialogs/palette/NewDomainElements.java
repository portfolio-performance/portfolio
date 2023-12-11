package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.editor.DomainElement;
import name.abuchen.portfolio.ui.util.CommandAction;

public class NewDomainElements implements ElementProvider
{
    private class CommandElement implements Element
    {
        private final String label;
        private final String commandId;
        private final String[] parameters;

        public CommandElement(String label, String commandId, String... parameters)
        {
            this.label = label;
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
            return Images.ADD;
        }

        @Override
        public void execute()
        {
            // execute asynchronously to allow the command palette to close
            Display.getDefault().asyncExec(() -> CommandAction.forCommand(context, label, commandId, parameters).run());
        }
    }

    @Inject
    private IEclipseContext context;

    @Override
    public List<Element> getElements()
    {
        List<Element> elements = new ArrayList<>();

        for (DomainElement element : DomainElement.values())
        {
            elements.add(new CommandElement(element.getPaletteLabel(), UIConstants.Command.NEW_DOMAIN_ELEMENT,
                            UIConstants.Parameter.TYPE, element.name()));
        }

        return elements;
    }
}
