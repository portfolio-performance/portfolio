package name.abuchen.portfolio.ui.views.dashboard;

import java.util.Arrays;
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
    public static class DataSeriesConfigElement
    {
        private final String message;
        private final boolean isBenchmark;
        private final Predicate<DataSeries> predicate;
        private final boolean isEmptyAllowed;
        
        public DataSeriesConfigElement(String message, boolean isBenchmark, Predicate<DataSeries> predicate, boolean isEmptyAllowed)
        {
            this.message = message;
            this.isBenchmark = isBenchmark;
            this.predicate = predicate;
            this.isEmptyAllowed = isEmptyAllowed;
        }
    }
    
    private final WidgetDelegate<?> delegate;
    private final DataSeriesConfigElement[] dataSeriesConfigElements;
    private final String label;
    private final Dashboard.Config configurationKey;

    private DataSeries[] dataSeries;

    public DataSeriesConfig(WidgetDelegate<?> delegate, boolean supportsBenchmarks)
    {
        this(delegate, supportsBenchmarks, false, null, Messages.LabelDataSeries, Dashboard.Config.DATA_SERIES);
    }

    public DataSeriesConfig(WidgetDelegate<?> delegate, boolean supportsBenchmarks, Predicate<DataSeries> predicate)
    {
        this(delegate, supportsBenchmarks, false, predicate, Messages.LabelDataSeries, Dashboard.Config.DATA_SERIES);
    }
    
    protected DataSeriesConfig(WidgetDelegate<?> delegate, boolean supportsBenchmark, boolean supportsEmptyDataSeries,
                    Predicate<DataSeries> predicate, String label, Dashboard.Config configurationKey)
    {
        this(delegate, getSimpleDataSeriesConfigElements(supportsBenchmark, predicate), label, configurationKey);
    }
    
    private static DataSeriesConfigElement[] getSimpleDataSeriesConfigElements(boolean supportsBenchmark, Predicate<DataSeries> predicate)
    {

        DataSeriesConfigElement[] dataSeriesConfigElements = new DataSeriesConfigElement[supportsBenchmark? 2 : 1];
        dataSeriesConfigElements[0] = new DataSeriesConfigElement(Messages.MenuSelectDataSeries, false, predicate, false);
        if(supportsBenchmark)
            dataSeriesConfigElements[1] = new DataSeriesConfigElement(Messages.MenuSelectBenchmarkDataSeries, true, predicate, true);
        return dataSeriesConfigElements;
    }

    public DataSeriesConfig(WidgetDelegate<?> delegate,  DataSeriesConfigElement[] dataSeriesConfigElements)
    {
        this(delegate, dataSeriesConfigElements, Messages.LabelDataSeries, Dashboard.Config.DATA_SERIES);
    }

    protected DataSeriesConfig(WidgetDelegate<?> delegate, DataSeriesConfigElement[] dataSeriesConfigElements,
                    String label, Dashboard.Config configurationKey)
    {
        this.delegate = delegate;
        this.label = label;
        this.configurationKey = configurationKey;
        this.dataSeriesConfigElements = dataSeriesConfigElements;

        dataSeries = new DataSeries[dataSeriesConfigElements.length];
        
        for(int i = 0; i < dataSeriesConfigElements.length; i++)
        {
            String uuid = delegate.getWidget().getConfiguration().get(configurationKey.name()
                            + indexToUUIDSuffix(i));
            if (uuid != null && !uuid.isEmpty())
                dataSeries[i] = delegate.getDashboardData().getDataSeriesSet().lookup(uuid);
            if (dataSeries[i] == null && !dataSeriesConfigElements[i].isEmptyAllowed)
                dataSeries[i] = delegate.getDashboardData().getDataSeriesSet().getAvailableSeries().stream()
                                .filter(ds -> ds.getType().equals(DataSeries.Type.CLIENT)).findAny()
                                .orElseThrow(IllegalArgumentException::new);
        }
    }

    public DataSeries getDataSeries()
    {
        return dataSeries[0];
    }

    public DataSeries getDataSeries(int index)
    {
        return dataSeries[index];
    }

    public DataSeries[] getAllDataSeries()
    {
        return Arrays.copyOf(dataSeries, dataSeries.length);
    }

    @Override
    public void menuAboutToShow(IMenuManager manager)
    {
        manager.appendToGroup(DashboardView.INFO_MENU_GROUP_NAME, new LabelOnly(getLabel()));

        // use configurationKey as contribution id to allow other context menus
        // to attach to this menu manager later
        MenuManager subMenu = new MenuManager(label, configurationKey.name());
        for(int i = 0; i < dataSeriesConfigElements.length; i++)
            subMenu.add(new LabelOnly(dataSeries[i] != null ? dataSeries[i].getLabel() : "-")); //$NON-NLS-1$
        subMenu.add(new Separator());
        for(int i = 0; i < dataSeriesConfigElements.length; i++)
        {
            int iFinal = i; 
            subMenu.add(new SimpleAction(dataSeriesConfigElements[i].message, a ->
                doAddSeries(iFinal, dataSeriesConfigElements[iFinal].isBenchmark)));
        }

        manager.add(subMenu);
    }

    private void doAddSeries(int index, boolean showOnlyBenchmark)
    {
        Stream<DataSeries> stream = delegate.getDashboardData().getDataSeriesSet().getAvailableSeries().stream()
                        .filter(ds -> ds.isBenchmark() == showOnlyBenchmark);
        if (dataSeriesConfigElements[index].predicate != null)
            stream = stream.filter(dataSeriesConfigElements[index].predicate);

        List<DataSeries> list = stream.collect(Collectors.toList());

        DataSeriesSelectionDialog dialog = new DataSeriesSelectionDialog(Display.getDefault().getActiveShell());
        dialog.setElements(list);
        dialog.setMultiSelection(false);

        if (dialog.open() != DataSeriesSelectionDialog.OK) // NOSONAR
            return;

        List<DataSeries> result = dialog.getResult();
        if (result.isEmpty())
            return;

        dataSeries[index] = result.get(0);
        delegate.getWidget().getConfiguration().put(configurationKey.name() + indexToUUIDSuffix(index), dataSeries[index].getUUID());

        // construct label to indicate the data series (user can manually change
        // the label later)
        delegate.getWidget().setLabel(WidgetFactory.valueOf(delegate.getWidget().getType()).getLabel() + ", " //$NON-NLS-1$
                        + getAllDataSeriesLabels());

        delegate.update();
        delegate.getClient().touch();
    }

    @Override
    public String getLabel()
    {
        String dataSeriesLabel = getAllDataSeriesLabels();
        return label + ": " + (dataSeriesLabel.length() > 0 ? dataSeriesLabel : "-"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    private String getAllDataSeriesLabels() 
    {
        String dataSeriesLabel = ""; //$NON-NLS-1$
        boolean first = true;
        for(DataSeries series : dataSeries)
        {
            if(series != null)
            {
                if(!first)
                {
                    dataSeriesLabel += ", "; //$NON-NLS-1$
                }
                first = false;
                dataSeriesLabel += series.getLabel();
            }
        }
        return dataSeriesLabel;
    }
    
    private String indexToUUIDSuffix(int index)
    {
        // Converts "0" to the empty string, necessary for backwards compatibility. //$NON-NLS-1$
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
