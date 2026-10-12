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
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesCache;
import name.abuchen.portfolio.util.Interval;

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
        // Widgets that calculate data series can also restrict their accounts.
        if ((config.getClass() == DataSeriesConfig.class || config instanceof MultiDataSeriesConfig)
                        && optionallyGet(ClientFilterConfig.class).isEmpty())
            this.config.add(new ClientFilterConfig(this));
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

    protected DataSeriesCache getDataSeriesCache()
    {
        return data.getDataSeriesCache(widget);
    }

    protected PerformanceIndex calculate(DataSeries series, Interval interval)
    {
        return getDataSeriesCache().lookup(series, interval);
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
        if (DataSeriesConfig.class.isAssignableFrom(type) || type == ClientFilterConfig.class)
        {
            // Construct a label for the selected series and account filter.
            // The user can manually change the label later.
            var label = new StringBuilder(getDefaultLabel());
            optionallyGet(ClientFilterConfig.class).map(ClientFilterConfig::getSelectedItem)
                            .filter(item -> !item.getId().isEmpty())
                            .ifPresent(item -> label.append(", ").append(item.getLabel())); //$NON-NLS-1$
            getWidget().setLabel(label.toString());
        }
    }

    protected String getDefaultLabel()
    {
        String defaultLabel = WidgetFactory.valueOf(getWidget().getType()).getLabel();
        return optionallyGet(DataSeriesConfig.class).map(DataSeriesConfig::getDataSeries)
                        .map(series -> defaultLabel + ", " + series.getLabel()).orElse(defaultLabel); //$NON-NLS-1$
    }

    void updateDefaultLabel()
    {
        // Upgrade existing generated labels after loading all configurations.
        // Custom labels remain editable and are preserved when opening a
        // dashboard.
        String defaultLabel = WidgetFactory.valueOf(getWidget().getType()).getLabel();
        if (defaultLabel.equals(widget.getLabel()) || getDefaultLabel().equals(widget.getLabel()))
            optionallyGet(ClientFilterConfig.class).filter(config -> !config.getSelectedItem().getId().isEmpty())
                            .ifPresent(config -> onWidgetConfigEdited(ClientFilterConfig.class));
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
