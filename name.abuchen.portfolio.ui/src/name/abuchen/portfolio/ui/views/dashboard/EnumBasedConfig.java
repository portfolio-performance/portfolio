package name.abuchen.portfolio.ui.views.dashboard;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class EnumBasedConfig<E extends Enum<E>> implements WidgetConfig
{
    private final WidgetDelegate delegate;
    private final Dashboard.Config configurationKey;
    private final String label;
    private final Class<E> type;

    private E value;

    public EnumBasedConfig(WidgetDelegate delegate, String label, Class<E> type, Dashboard.Config configurationKey)
    {
        this.delegate = delegate;
        this.configurationKey = configurationKey;
        this.label = label;
        this.type = type;

        this.value = type.getEnumConstants()[0];

        String code = delegate.getWidget().getConfiguration().get(configurationKey.name());

        if (code != null)
        {
            try
            {
                value = Enum.valueOf(type, code);
            }
            catch (IllegalArgumentException ignore)
            {
                PortfolioPlugin.log(ignore);
            }
        }
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        MenuManager subMenu = new MenuManager(label);
        manager.add(subMenu);

        for (E v : type.getEnumConstants())
            subMenu.add(buildAction(v));
    }

    private Action buildAction(E tableLayout)
    {
        Action action = new SimpleAction(tableLayout.toString(), a -> {
            this.value = tableLayout;
            delegate.getWidget().getConfiguration().put(configurationKey.name(), tableLayout.name());
            delegate.getClient().markDirty();
        });
        action.setChecked(this.value == tableLayout);
        return action;
    }

    @Override
    public String getLabel()
    {
        return label + ": " + value.toString(); //$NON-NLS-1$
    }

    public E getValue()
    {
        return value;
    }
}
