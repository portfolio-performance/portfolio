package name.abuchen.portfolio.ui.views.dashboard;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

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

    /**
     * If not null, display the context menu at this path (and not at the top
     * level of the widget configuration menu).
     */
    private final String pathToMenu;

    private EnumSet<E> values;

    public EnumBasedConfig(WidgetDelegate<?> delegate, String label, Class<E> type, Dashboard.Config configurationKey,
                    Policy policy)
    {
        this(delegate, label, type, configurationKey, policy, null);
    }

    public EnumBasedConfig(WidgetDelegate<?> delegate, String label, Class<E> type, Dashboard.Config configurationKey,
                    Policy policy, String pathToMenu)
    {
        this.delegate = delegate;
        this.configurationKey = configurationKey;
        this.label = label;
        this.type = type;
        this.policy = policy;

        this.pathToMenu = pathToMenu;

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
        for (E v : type.getEnumConstants())
            subMenu.add(buildAction(v));

        if (pathToMenu != null)
        {
            IMenuManager alternative = manager.findMenuUsingPath(pathToMenu);
            alternative.add(new Separator());
            alternative.add(subMenu);
        }
        else
        {
            manager.add(subMenu);
        }
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
                    throw new IllegalArgumentException("unsupported policy " + policy); //$NON-NLS-1$
            }

            delegate.getWidget().getConfiguration().put(configurationKey.name(),
                            values.stream().map(E::name).collect(Collectors.joining(","))); //$NON-NLS-1$

            delegate.update();
            delegate.getClient().touch();
        });
        action.setChecked(this.values.contains(value));
        return action;
    }

    @Override
    public String getLabel()
    {
        return label + ": " + values.stream().map(E::toString).collect(Collectors.joining(", ")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public E getValue()
    {
        if (policy != Policy.EXACTLY_ONE)
            throw new IllegalArgumentException("policy must be EXACTLY_ONE but is " + policy); //$NON-NLS-1$
        return values.iterator().next();
    }

    public Set<E> getValues()
    {
        return values;
    }
}
