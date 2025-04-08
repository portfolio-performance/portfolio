package name.abuchen.portfolio.ui.dialogs.palette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.Element;
import name.abuchen.portfolio.ui.dialogs.palette.CommandPalettePopup.ElementProvider;
import name.abuchen.portfolio.ui.editor.Navigation;
import name.abuchen.portfolio.ui.editor.Navigation.Item;
import name.abuchen.portfolio.ui.editor.Navigation.Tag;
import name.abuchen.portfolio.ui.editor.PortfolioPart;

/* package */ class NavigationElements implements ElementProvider
{
    private static class NavigationElement implements Element
    {
        private final PortfolioPart part;
        private final Navigation.Item item;
        private final String subtitle;

        public NavigationElement(PortfolioPart part, Item item, String subtitle)
        {
            this.part = part;
            this.item = item;
            this.subtitle = subtitle;
        }

        @Override
        public String getTitel()
        {
            return item.getLabel();
        }

        @Override
        public String getSubtitle()
        {
            return subtitle;
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

    @Inject
    private PortfolioPart part;

    @Override
    public List<Element> getElements()
    {
        Navigation navigation = part.getClientInput().getNavigation();
        if (navigation == null)
            return Collections.emptyList();

        List<Element> elements = new ArrayList<>();

        navigation.getRoots().forEach(item -> addElement(elements, part, new ArrayList<>(Arrays.asList(item))));

        return elements;
    }

    private void addElement(List<Element> elements, PortfolioPart part, List<Item> path)
    {
        Item leaf = path.get(path.size() - 1);

        if (leaf.contains(Tag.VIEW))
        {
            String subtitle = String.join(" -> ", path.subList(0, path.size() - 1).stream() //$NON-NLS-1$
                            .map(Item::getLabel).toList());
            elements.add(new NavigationElement(part, leaf, subtitle));
        }

        leaf.getChildren().forEach(child -> {
            path.add(child);
            addElement(elements, part, path);
            path.remove(child);
        });
    }

}
