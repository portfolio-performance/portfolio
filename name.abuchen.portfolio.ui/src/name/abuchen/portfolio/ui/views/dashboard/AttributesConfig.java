package name.abuchen.portfolio.ui.views.dashboard;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class AttributesConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;

    private final List<AttributeType> availableTypes;
    private AttributeType selectedType;

    public AttributesConfig(WidgetDelegate<?> delegate, Predicate<AttributeType> predicate)
    {
        this.delegate = delegate;

        this.availableTypes = delegate.getClient().getSettings().getAttributeTypes().filter(predicate)
                        .collect(Collectors.toList());

        String uuid = delegate.getWidget().getConfiguration().get(Dashboard.Config.ATTRIBUTE_UUID.name());

        if (uuid != null && !uuid.isEmpty())
            selectedType = availableTypes.stream().filter(t -> uuid.equals(t.getId())).findAny().orElse(null);
    }

    public List<AttributeType> getTypes()
    {
        return selectedType != null ? Arrays.asList(selectedType) : availableTypes;
    }

    public boolean hasTypes()
    {
        return !availableTypes.isEmpty();
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

        MenuManager subMenu = new MenuManager(Messages.GroupLabelAttributes);

        Action any = new SimpleAction(Messages.LabelAllAttributes, a -> {
            selectedType = null;
            delegate.getWidget().getConfiguration().remove(Dashboard.Config.ATTRIBUTE_UUID.name());
            delegate.update();
            delegate.getClient().touch();
        });
        any.setChecked(selectedType == null);
        subMenu.add(any);

        for (AttributeType type : availableTypes)
        {
            Action action = new SimpleAction(type.getName(), a -> {
                selectedType = type;
                delegate.getWidget().getConfiguration().put(Dashboard.Config.ATTRIBUTE_UUID.name(), type.getId());
                delegate.update();
                delegate.getClient().touch();

            });

            if (type.equals(selectedType))
                action.setChecked(true);

            subMenu.add(action);
        }

        manager.add(subMenu);
    }

    @Override
    public String getLabel()
    {
        return Messages.GroupLabelAttributes + ": " + (selectedType != null ? selectedType.getName() : "-"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
