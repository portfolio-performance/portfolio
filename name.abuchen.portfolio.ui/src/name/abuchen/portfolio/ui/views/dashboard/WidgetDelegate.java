package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;

/**
 * Base UI class for a widget. <D> represents the data object which is
 * calculated in the background and passed back into the {@link #update} method.
 */
public abstract class WidgetDelegate<D>
{
    private final Dashboard.Widget widget;
    private final DashboardData data;
    private final List<WidgetConfig> config = new ArrayList<>();

    public WidgetDelegate(Dashboard.Widget widget, DashboardData data)
    {
        this.widget = widget;
        this.data = data;

        addConfig(new LabelConfig(this));
    }

    public final void addConfig(WidgetConfig config)
    {
        this.config.add(config);
    }

    public Client getClient()
    {
        return data.getClient();
    }

    public Dashboard.Widget getWidget()
    {
        return widget;
    }

    public DashboardData getDashboardData()
    {
        return data;
    }

    public <C extends WidgetConfig> C get(Class<C> type)
    {
        return type.cast(config.stream().filter(c -> type.equals(c.getClass())).findAny()
                        .orElseThrow(IllegalArgumentException::new));
    }

    public <C extends WidgetConfig> Optional<C> optionallyGet(Class<C> type)
    {
        return config.stream().filter(c -> type.equals(c.getClass())).findAny().map(type::cast);
    }

    public Stream<WidgetConfig> getWidgetConfigs()
    {
        return config.stream();
    }

    public abstract Composite createControl(Composite parent, DashboardResources resources);

    /**
     * Immediately updates the widget with the data of the update task. Calls
     * first {@link #getUpdateTask} and then {@link #update(D)}. Updates the
     * result cache.
     */
    public final void update()
    {
        D result = getUpdateTask().get();

        data.getResultCache().put(widget, result != null ? result : DashboardData.EMPTY_RESULT);

        update(result);
    }

    public abstract Supplier<D> getUpdateTask();

    public abstract void update(D data);

    /**
     * Returns the title control to which context menu and default tooltip are
     * attached.
     */
    public abstract Control getTitleControl();
}
