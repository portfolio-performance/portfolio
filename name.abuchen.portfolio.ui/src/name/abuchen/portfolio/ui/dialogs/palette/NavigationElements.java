package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.editor.Navigation;
import name.abuchen.portfolio.ui.editor.Navigation.Item;
import name.abuchen.portfolio.ui.editor.Navigation.Tag;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

public class NavigationElements
{
    private static class NavigationElement implements Element
    {
        private final PortfolioPart part;
        private final Navigation.Item item;

        public NavigationElement(PortfolioPart part, Item item)
        {
            this.part = part;
            this.item = item;
        }

        @Override
        public String getTitel()
        {
            return item.getLabel();
        }

        @Override
        public Images getImage()
        {
            return item.getImage() != null ? item.getImage() : Images.VIEW;
        }

        @Override
        public void execute()
        {
            part.activateView(item);
        }
    }

    private NavigationElements()
    {
    }

    public static List<Element> createFor(PortfolioPart part)
    {
        Navigation navigation = part.getClientInput().getNavigation();
        if (navigation == null)
            return Collections.emptyList();

        return navigation.findAll(Tag.VIEW).map(item -> new NavigationElement(part, item)).collect(Collectors.toList());
    }

}
