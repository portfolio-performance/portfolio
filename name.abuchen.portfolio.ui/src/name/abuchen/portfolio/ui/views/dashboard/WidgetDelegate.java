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

    protected final void addConfig(WidgetConfig config)
    {
        this.config.add(config);
    }

    protected Client getClient()
    {
        return data.getClient();
    }

    protected Dashboard.Widget getWidget()
    {
        return widget;
    }

    protected DashboardData getDashboardData()
    {
        return data;
    }

    protected <C extends WidgetConfig> C get(Class<C> type)
    {
        return type.cast(config.stream().filter(c -> type.equals(c.getClass())).findAny()
                        .orElseThrow(IllegalArgumentException::new));
    }

    public Stream<WidgetConfig> getWidgetConfigs()
    {
        return config.stream();
    }

    abstract Composite createControl(Composite parent, DashboardResources resources);

    abstract void update();

    /**
     * Returns the title control to which context menu and default tooltip are
     * attached.
     */
    abstract Control getTitleControl();
}
