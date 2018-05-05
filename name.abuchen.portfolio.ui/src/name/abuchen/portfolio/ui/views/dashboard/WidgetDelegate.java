package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Dashboard;

public abstract class WidgetDelegate
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

    public Stream<WidgetConfig> getWidgetConfigs()
    {
        return config.stream();
    }

    public abstract Composite createControl(Composite parent, DashboardResources resources);

    public abstract void update();

    /**
     * Returns the title control to which context menu and default tooltip are
     * attached.
     */
    public abstract Control getTitleControl();
}
