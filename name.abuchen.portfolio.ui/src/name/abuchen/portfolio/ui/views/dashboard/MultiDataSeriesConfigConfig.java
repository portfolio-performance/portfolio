package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.List;
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

public class MultiDataSeriesConfigConfig implements WidgetConfig
{
    private final WidgetDelegate<?> delegate;
    private List<DataSeriesConfig> configs = new ArrayList<>();
    
    public MultiDataSeriesConfigConfig(WidgetDelegate<?> delegate)
    {
        this.delegate = delegate;
    }
    
    public void load(List<DataSeriesConfig> dsConfigs)
    {
        String config = delegate.getWidget().getConfiguration().get(Dashboard.Config.DATA_SERIES.name());
        if (config != null && !config.isEmpty())
        {
            for(DataSeriesConfig c : new DataSeriesConfigSerializer().fromString(delegate, config))
                configs.add(c);
        }
        else
        {
            dsConfigs.forEach(p -> configs.add(p));
            update();
        }
    }
    
    public List<DataSeries> getDataSeries()
    {
        return configs.stream().map(c -> c.getDataSeries()).collect(Collectors.toList());
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        for (int aa = 0; aa < configs.size(); aa++)
        {
            final int bb = aa;
            
            manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(configs.get(aa).getDataSeries().getLabel()));

            MenuManager subMenu = new MenuManager(configs.get(aa).getGroupName());
            
            subMenu.add(new LabelOnly(configs.get(aa).getDataSeries().getLabel()));
            subMenu.add(new Separator());
            subMenu.add(new SimpleAction(Messages.MenuSelectDataSeries, a -> doAddSeries(bb, false)));

            if (configs.get(aa).getSupportsBenchmarkDataSeries())
                subMenu.add(new SimpleAction(Messages.MenuSelectBenchmarkDataSeries, a -> doAddSeries(bb, true)));

            manager.add(subMenu);
        }
    }    
    
    private void update()
    {
        delegate.getWidget().getConfiguration().put(Dashboard.Config.DATA_SERIES.name(),
                        new DataSeriesConfigSerializer().toString(configs));
        delegate.updateLabel();
    }
    
    private void doAddSeries(int index, boolean showOnlyBenchmark)
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

        configs.set(index, new DataSeriesConfig.Builder(delegate).withDataSeries(result.get(0).getUUID()).withBenchmarkDataSeries(showOnlyBenchmark).build());
        
        update();        
        delegate.update();
        delegate.getClient().touch();
    }

    @Override
    public String getLabel()
    {
        return null;
    }
}
