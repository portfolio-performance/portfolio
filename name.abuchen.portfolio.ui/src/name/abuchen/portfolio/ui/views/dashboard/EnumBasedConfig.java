package name.abuchen.portfolio.ui.views.dashboard;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.SimpleAction;

public class EnumBasedConfig<E extends Enum<E>> implements WidgetConfig
{
    public enum Policy
    {
        EXACTLY_ONE, MULTIPLE
    }

    private final Policy policy;
    private final WidgetDelegate<?> delegate;
    private final Dashboard.Config configurationKey;
    private final String label;
    private final Class<E> type;

    private EnumSet<E> values;

    public EnumBasedConfig(WidgetDelegate<?> delegate, String label, Class<E> type, Dashboard.Config configurationKey,
                    Policy policy)
    {
        this.delegate = delegate;
        this.configurationKey = configurationKey;
        this.label = label;
        this.type = type;
        this.policy = policy;

        this.values = EnumSet.noneOf(type);

        String code = delegate.getWidget().getConfiguration().get(configurationKey.name());

        if (code != null && !code.isEmpty())
        {
            try
            {
                String[] codes = code.split(","); //$NON-NLS-1$
                for (String c : codes)
                        values.add(Enum.valueOf(type, c));
            }
            catch (IllegalArgumentException ignore)
            {
                PortfolioPlugin.log(ignore);
            }
        }

        if (values.isEmpty() && policy == Policy.EXACTLY_ONE)
            this.values.add(type.getEnumConstants()[0]);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        MenuManager subMenu = new MenuManager(label);
        manager.add(subMenu);

        for (E v : type.getEnumConstants())
            subMenu.add(buildAction(v));
    }

    private Action buildAction(E value)
    {
        Action action = new SimpleAction(value.toString(), a -> {

            switch (policy)
            {
                case EXACTLY_ONE:
                    this.values.clear();
                    this.values.add(value);
                    break;
                case MULTIPLE:
                    boolean isActive = this.values.contains(value);
                    if (isActive)
                        this.values.remove(value);
                    else
                        this.values.add(value);
                    break;
                default:
                    throw new IllegalArgumentException();
            }

            delegate.getWidget().getConfiguration().put(configurationKey.name(),
                            String.join(",", values.stream().map(E::name).collect(Collectors.toList()))); //$NON-NLS-1$

            delegate.update();
            delegate.getClient().touch();
        });
        action.setChecked(this.values.contains(value));
        return action;
    }

    @Override
    public String getLabel()
    {
        return label + ": " + String.join(", ", values.stream().map(E::toString).collect(Collectors.toList())); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public E getValue()
    {
        if (policy != Policy.EXACTLY_ONE)
            throw new IllegalArgumentException();
        return values.iterator().next();
    }

    public Set<E> getValues()
    {
        return values;
    }
}
