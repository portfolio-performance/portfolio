package name.abuchen.portfolio.ui.views.dashboard;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.ui.views.dataseries.DataSeriesSelectionDialog;

public class DataSeriesConfig implements WidgetConfig
{
    public static class Builder
    {
        private WidgetDelegate<?> delegate;
        private String groupName = Messages.LabelDataSeries;
        private String uuid;
        private boolean supportsBenchmarks = true;

        public Builder(WidgetDelegate<?> delegate)
        {
            this.delegate = delegate;
        }

        public Builder withGroupName(String groupName)
        {
            this.groupName = groupName;
            return this;
        }

        public Builder withDataSeries(String uuid)
        {
            this.uuid = uuid;
            return this;
        }

        public Builder withDefaultDataSeries(DataSeries.Type type)
        {
            this.uuid = delegate.getDashboardData().getDataSeriesSet().getAvailableSeries().stream()
                            .filter(ds -> ds.getType().equals(type)).findAny().map(s -> s.getUUID())
                            .orElseThrow(IllegalArgumentException::new);
            return this;
        }

        public Builder withBenchmarkDataSeries(boolean supportsBenchmarks)
        {
            this.supportsBenchmarks = supportsBenchmarks;
            return this;
        }

        public DataSeriesConfig build()
        {
            Objects.requireNonNull(groupName);
            
            if(uuid == null || uuid.isEmpty())
                withDefaultDataSeries(DataSeries.Type.CLIENT);

            DataSeriesConfig config = new DataSeriesConfig(delegate, uuid);
            config.setGroupName(groupName);
            config.setBenchmarkDataSeries(supportsBenchmarks);
            return config;
        }
    }

    private final WidgetDelegate<?> delegate;
    private String groupName;
    private DataSeries dataSeries;
    private boolean supportsBenchmarks = true;

    public DataSeriesConfig(WidgetDelegate<?> delegate, String uuid)
    {
        this.delegate = delegate;

        String configUUID = delegate.getWidget().getConfiguration().get(Dashboard.Config.DATA_SERIES.name());
        if (configUUID != null && !configUUID.isEmpty())
            dataSeries = delegate.getDashboardData().getDataSeriesSet().lookup(configUUID);
        if (dataSeries == null)
            dataSeries = delegate.getDashboardData().getDataSeriesSet().lookup(uuid);
    }

    public static Builder create(WidgetDelegate<?> delegate)
    {
        return new DataSeriesConfig.Builder(delegate);
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }
    
    public String getGroupName()
    {
        return groupName;
    }

    public void setBenchmarkDataSeries(boolean supportsBenchmarks)
    {
        this.supportsBenchmarks = supportsBenchmarks;
    }
    
    public boolean getSupportsBenchmarkDataSeries()
    {
        return supportsBenchmarks;
    }

    public DataSeries getDataSeries()
    {
        return dataSeries;
    }    

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(dataSeries.getLabel()));

        MenuManager subMenu = new MenuManager(groupName);
        subMenu.add(new LabelOnly(dataSeries.getLabel()));
        subMenu.add(new Separator());
        subMenu.add(new SimpleAction(Messages.MenuSelectDataSeries, a -> doAddSeries(false)));

        if (supportsBenchmarks)
            subMenu.add(new SimpleAction(Messages.MenuSelectBenchmarkDataSeries, a -> doAddSeries(true)));

        manager.add(subMenu);
    }

    private void doAddSeries(boolean showOnlyBenchmark)
    {
        List<DataSeries> list = delegate.getDashboardData().getDataSeriesSet().getAvailableSeries().stream()
                        .filter(ds -> ds.isBenchmark() == showOnlyBenchmark).collect(Collectors.toList());

        DataSeriesSelectionDialog dialog = new DataSeriesSelectionDialog(Display.getDefault().getActiveShell());
        dialog.setElements(list);
        dialog.setMultiSelection(false);

        if (dialog.open() != DataSeriesSelectionDialog.OK)
            return;

        List<DataSeries> result = dialog.getResult();
        if (result.isEmpty())
            return;

        dataSeries = result.get(0);
        delegate.getWidget().getConfiguration().put(Dashboard.Config.DATA_SERIES.name(), dataSeries.getUUID());

        delegate.updateLabel();
        delegate.update();
        delegate.getClient().touch();
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelDataSeries + ": " + dataSeries.getLabel(); //$NON-NLS-1$
    }
}
