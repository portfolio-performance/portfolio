package name.abuchen.portfolio.ui.views.dashboard;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final WidgetDelegate<?> delegate;
    private final boolean supportsBenchmarks;
    private final String label;
    private final Dashboard.Config configurationKey;
    private final Predicate<DataSeries> predicate;
    private final int index; // indicates the index among all DataSeriesConfigs for this widget.

    private DataSeries dataSeries;

    // Use only when this is the only DataSeriesConfig for the widget
    public DataSeriesConfig(WidgetDelegate<?> delegate, boolean supportsBenchmarks)
    {
        this(delegate, 0, supportsBenchmarks, false, null, Messages.LabelDataSeries, Dashboard.Config.DATA_SERIES);
    }

    // Use only when this is the only DataSeriesConfig for the widget
    public DataSeriesConfig(WidgetDelegate<?> delegate, boolean supportsBenchmarks, Predicate<DataSeries> predicate)
    {
        this(delegate, 0, supportsBenchmarks, false, predicate, Messages.LabelDataSeries, Dashboard.Config.DATA_SERIES);
    }

    public DataSeriesConfig(WidgetDelegate<?> delegate, int index, boolean supportsBenchmarks, boolean supportsEmptyDataSeries,
                    Predicate<DataSeries> predicate, String label)
    {
        this(delegate, index, supportsBenchmarks, supportsEmptyDataSeries, predicate, label, Dashboard.Config.DATA_SERIES);
    }

    protected DataSeriesConfig(WidgetDelegate<?> delegate,int index, boolean supportsBenchmarks, boolean supportsEmptyDataSeries,
                    Predicate<DataSeries> predicate, String label, Dashboard.Config configurationKey)
    {
        this.delegate = delegate;
        this.supportsBenchmarks = supportsBenchmarks;
        this.label = label;
        this.configurationKey = configurationKey;
        this.predicate = predicate;
        this.index = index;

        String uuid = delegate.getWidget().getConfiguration().get(configurationKey.name() + indexToUUIDSuffix(index));
        if (uuid != null && !uuid.isEmpty())
            dataSeries = delegate.getDashboardData().getDataSeriesSet().lookup(uuid);
        if (dataSeries == null && !supportsEmptyDataSeries)
            dataSeries = delegate.getDashboardData().getDataSeriesSet().getAvailableSeries().stream()
                            .filter(ds -> ds.getType().equals(DataSeries.Type.CLIENT)).findAny()
                            .orElseThrow(IllegalArgumentException::new);
    }

    public DataSeries getDataSeries()
    {
        return dataSeries;
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

        // use configurationKey as contribution id to allow other context menus
        // to attach to this menu manager later
        MenuManager subMenu = new MenuManager(label, configurationKey.name());
        subMenu.add(new LabelOnly(dataSeries != null ? dataSeries.getLabel() : "-")); //$NON-NLS-1$
        subMenu.add(new Separator());
        subMenu.add(new SimpleAction(Messages.MenuSelectDataSeries, a -> doAddSeries(false)));

        if (supportsBenchmarks)
            subMenu.add(new SimpleAction(Messages.MenuSelectBenchmarkDataSeries, a -> doAddSeries(true)));

        manager.add(subMenu);
    }

    private void doAddSeries(boolean showOnlyBenchmark)
    {
        Stream<DataSeries> stream = delegate.getDashboardData().getDataSeriesSet().getAvailableSeries().stream()
                        .filter(ds -> ds.isBenchmark() == showOnlyBenchmark);
        if (predicate != null)
            stream = stream.filter(predicate);

        List<DataSeries> list = stream.collect(Collectors.toList());

        DataSeriesSelectionDialog dialog = new DataSeriesSelectionDialog(Display.getDefault().getActiveShell());
        dialog.setElements(list);
        dialog.setMultiSelection(false);

        if (dialog.open() != DataSeriesSelectionDialog.OK) // NOSONAR
            return;

        List<DataSeries> result = dialog.getResult();
        if (result.isEmpty())
            return;

        dataSeries = result.get(0);
        delegate.getWidget().getConfiguration().put(configurationKey.name() + indexToUUIDSuffix(index), dataSeries.getUUID());

        delegate.update();
        delegate.getClient().touch();
    }
    
    public int getIndex()
    {
        return index;
    }

    @Override
    public String getLabel()
    {
        return label + ": " + (dataSeries != null ? dataSeries.getLabel() : "-"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    private String indexToUUIDSuffix(int index)
    {
        // Converts "0" to the empty string, necessary for backwards compatibility.
        if (index == 0)
        {
            return ""; //$NON-NLS-1$
        }
        else
        {
            return Integer.toString(index);
        }
    }
}
