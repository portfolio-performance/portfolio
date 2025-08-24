package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.editor.DomainElement;

public class NewDomainElements implements ElementProvider
{
    @Inject
    private IEclipseContext context;

    @Override
    public List<Element> getElements()
    {
        List<Element> elements = new ArrayList<>();

        for (DomainElement element : DomainElement.values())
        {
            elements.add(new CommandElement(context, element.getPaletteLabel(), Images.ADD,
                            UIConstants.Command.NEW_DOMAIN_ELEMENT, UIConstants.Parameter.TYPE, element.name()));
        }

        return elements;
    }
}
