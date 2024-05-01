package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.UseCase;

public class BasicDataSeriesConfigurator
{
    /**
     * Listener for updates on data series.
     */
    @FunctionalInterface
    public interface Listener
    {
        /**
         * Handles the update of data series.
         */
        void onUpdate();
    }

    public static final String IDENTIFIER_POSTFIX = "-PICKER"; //$NON-NLS-1$

    private final Client client;
    private final List<DataSeriesConfigurator.Listener> listeners = new ArrayList<>();

    private DataSeriesSet dataSeriesSet;
    private List<DataSeries> selectedSeries = new ArrayList<>();

    public BasicDataSeriesConfigurator(Client client, DataSeriesSet dataSeriesSet)
    {
        this.client = client;

        this.dataSeriesSet = dataSeriesSet;
        this.selectedSeries = new DataSeriesSerializer().fromString(dataSeriesSet, null);
    }

    protected Client getClient()
    {
        return client;
    }

    public UseCase getUseCase()
    {
        return dataSeriesSet.getUseCase();
    }

    public void addListener(DataSeriesConfigurator.Listener listener)
    {
        this.listeners.add(listener);
    }

    protected void fireUpdate()
    {
        listeners.forEach(Listener::onUpdate);
    }

    public List<DataSeries> getSelectedDataSeries()
    {
        return selectedSeries;
    }

    public void setSelectedData(String config)
    {
        selectedSeries = new DataSeriesSerializer().fromString(dataSeriesSet, config);
        fireUpdate();
    }

    protected void setSeriesDataSet(DataSeriesSet newSet, String newConfig)
    {
        this.dataSeriesSet = newSet;
        selectedSeries = new DataSeriesSerializer().fromString(dataSeriesSet, newConfig);
        fireUpdate();
    }

    public void configMenuAboutToShow(IMenuManager manager)
    {
        for (final DataSeries series : selectedSeries)
        {
            Action action = new SimpleAction(series.getLabel(), a -> doDeleteSeries(series));
            action.setChecked(true);
            manager.add(action);
        }

        manager.add(new Separator());

        manager.add(new SimpleAction(Messages.ChartSeriesPickerAddItem, a -> doAddSeries(false)));

        if (dataSeriesSet.getUseCase() != DataSeries.UseCase.STATEMENT_OF_ASSETS)
            manager.add(new SimpleAction(Messages.ChartSeriesPickerAddBenchmark, a -> doAddSeries(true)));

        addCopyFromOtherChartsMenu(manager);
    }

    private void doAddSeries(boolean showOnlyBenchmark)
    {
        List<DataSeries> list = new ArrayList<>(dataSeriesSet.getAvailableSeries());

        // remove items if that do not match the benchmark flag
        Iterator<DataSeries> iter = list.iterator();
        while (iter.hasNext())
            if (iter.next().isBenchmark() != showOnlyBenchmark)
                iter.remove();

        // remove already selected items
        for (DataSeries s : selectedSeries)
            list.remove(s);

        DataSeriesSelectionDialog dialog = new DataSeriesSelectionDialog(Display.getDefault().getActiveShell(), client);
        dialog.setElements(list);

        if (dialog.open() != DataSeriesSelectionDialog.OK)
            return;

        List<DataSeries> result = dialog.getResult();
        if (result.isEmpty())
            return;

        result.forEach(series -> {
            series.setVisible(true);
            selectedSeries.add(series);
        });

        fireUpdate();
    }

    private void addCopyFromOtherChartsMenu(IMenuManager manager)
    {
        String[] charts = new String[] { //
                        "StatementOfAssetsHistoryView", Messages.LabelStatementOfAssetsHistory, //$NON-NLS-1$
                        "PerformanceChartView", Messages.LabelPerformanceChart, //$NON-NLS-1$
                        "ReturnsVolatilityChartView", Messages.LabelHistoricalReturnsAndVolatiltity }; //$NON-NLS-1$

        MenuManager copyFromOthers = new MenuManager(Messages.ChartSeriesCopySeriesFromOtherChart);
        manager.add(copyFromOthers);
        MenuManager replaceByOthers = new MenuManager(Messages.ChartSeriesReplaceSeriesByOtherChart);
        manager.add(replaceByOthers);

        String currentConfigUUID = getActiveUUID();

        for (int ii = 0; ii < charts.length; ii += 2)
        {
            ConfigurationSet set = client.getSettings().getConfigurationSet(charts[ii] + IDENTIFIER_POSTFIX);

            MenuManager menuCopy = new MenuManager(charts[ii + 1]);
            copyFromOthers.add(menuCopy);
            MenuManager menuReplace = new MenuManager(charts[ii + 1]);
            replaceByOthers.add(menuReplace);

            set.getConfigurations().forEach(config -> {

                if (Objects.equals(currentConfigUUID, config.getUUID()))
                    return;

                menuCopy.add(new SimpleAction(config.getName(), a -> {
                    List<DataSeries> list = new DataSeriesSerializer().fromString(dataSeriesSet, config.getData());
                    list.stream().filter(s -> !selectedSeries.contains(s)).forEach(s -> selectedSeries.add(s));
                    fireUpdate();
                }));

                menuReplace.add(new SimpleAction(config.getName(), a -> {
                    List<DataSeries> list = new DataSeriesSerializer().fromString(dataSeriesSet, config.getData());
                    selectedSeries.clear();
                    list.stream().forEach(s -> selectedSeries.add(s));
                    fireUpdate();
                }));
            });
        }
    }

    protected String getActiveUUID()
    {
        return null;
    }

    protected void doDeleteSeries(DataSeries series)
    {
        selectedSeries.remove(series);
        fireUpdate();
    }

}
