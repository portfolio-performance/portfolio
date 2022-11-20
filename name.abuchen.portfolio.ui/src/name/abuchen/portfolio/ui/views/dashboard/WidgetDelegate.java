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

    protected WidgetDelegate(Dashboard.Widget widget, DashboardData data)
    {
        this.widget = widget;
        this.data = data;

        addConfig(new LabelConfig(this));
    }

    public final void addConfig(WidgetConfig config)
    {
        this.config.add(config);
    }

    public final void addConfigAfter(Class<? extends WidgetConfig> type, WidgetConfig config)
    {
        for (int ii = 0; ii < this.config.size(); ii++)
        {
            if (this.config.get(ii).getClass().isAssignableFrom(type))
            {
                this.config.add(ii + 1, config);
                return;
            }
        }

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

    public void onWidgetConfigEdited(Class<? extends WidgetConfig> type)
    {
        if (type == DataSeriesConfig.class)
        {
            // construct label to indicate the data series (user can manually
            // change the label later)
            getWidget().setLabel(WidgetFactory.valueOf(getWidget().getType()).getLabel() + ", " //$NON-NLS-1$
                            + get(DataSeriesConfig.class).getDataSeries().getLabel());
        }
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

    /**
     * Generates the container CSS class names by using the label text of the
     * widget. Therefore all non-alphanumeric characters are stripped and the
     * result is capped to 30 characters.
     */
    protected String getContainerCssClassNames()
    {
        String cssClassName = widget.getLabel().replaceAll("[^a-zA-Z0-9]", "").toLowerCase(); //$NON-NLS-1$ //$NON-NLS-2$
        return "dashboard-widget " + cssClassName.substring(0, Math.min(cssClassName.length(), 30)); //$NON-NLS-1$
    }
}
