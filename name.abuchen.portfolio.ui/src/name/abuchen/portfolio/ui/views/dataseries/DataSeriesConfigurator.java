package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConfigurationSet;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.util.ConfigurationStore;
import name.abuchen.portfolio.ui.util.ConfigurationStore.ConfigurationStoreOwner;
import name.abuchen.portfolio.ui.util.SimpleAction;

/**
 * The DataSeriesConfigurator manages the currently available set of data
 * series.
 */
public class DataSeriesConfigurator implements ConfigurationStoreOwner
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

    private final String identifier;
    private final Client client;
    private final IPreferenceStore preferences;
    private final ConfigurationStore store;

    private final List<DataSeriesConfigurator.Listener> listeners = new ArrayList<>();

    private DataSeriesSet dataSeriesSet;
    private List<DataSeries> selectedSeries = new ArrayList<>();

    private Menu configContextMenu;

    public DataSeriesConfigurator(AbstractFinanceView view, DataSeries.UseCase useCase)
    {
        this.identifier = view.getClass().getSimpleName() + IDENTIFIER_POSTFIX;
        this.client = view.getClient();
        this.preferences = view.getPreferenceStore();
        this.store = new ConfigurationStore(identifier, client, preferences, this);

        this.dataSeriesSet = new DataSeriesSet(client, preferences, useCase);
        this.selectedSeries = new DataSeriesSerializer().fromString(dataSeriesSet, store.getActive());

        view.getControl().addDisposeListener(e -> DataSeriesConfigurator.this.widgetDisposed());
    }

    public void addListener(DataSeriesConfigurator.Listener listener)
    {
        this.listeners.add(listener);
    }

    /* protected */ void fireUpdate()
    {
        listeners.forEach(l -> l.onUpdate());
        store.updateActive(new DataSeriesSerializer().toString(selectedSeries));
    }

    public String getConfigurationName()
    {
        return store.getActiveName();
    }

    /**
     * Shows the menu to add and remove data series from the current set of data
     * series.
     * 
     * @param shell
     */
    public void showMenu(Shell shell)
    {
        if (configContextMenu == null)
            configContextMenu = createMenu(shell, this::configMenuAboutToShow);
        configContextMenu.setVisible(true);
    }

    /**
     * Shows the menu to manage sets of data series, e.g. add, create, delete
     * sets.
     * 
     * @param shell
     */
    public void showSaveMenu(Shell shell)
    {
        store.showMenu(shell);
    }

    private Menu createMenu(Shell shell, IMenuListener listener)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(listener);
        return menuMgr.createContextMenu(shell);
    }

    public List<DataSeries> getSelectedDataSeries()
    {
        return selectedSeries;
    }

    private void widgetDisposed()
    {
        if (configContextMenu != null && !configContextMenu.isDisposed())
            configContextMenu.dispose();

        store.dispose();
    }

    private void configMenuAboutToShow(IMenuManager manager) // NOSONAR
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

        manager.add(new SimpleAction(Messages.MenuResetChartSeries, a -> doResetSeries(null)));
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

        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(),
                        new DataSeriesLabelProvider());
        dialog.setTitle(Messages.ChartSeriesPickerTitle);
        dialog.setMessage(Messages.ChartSeriesPickerTitle);
        dialog.setElements(list);

        if (dialog.open() != ListSelectionDialog.OK)
            return;

        Object[] result = dialog.getResult();
        if (result == null || result.length == 0)
            return;

        for (Object object : result)
            selectedSeries.add((DataSeries) object);

        fireUpdate();
    }

    private void doResetSeries(String config)
    {
        dataSeriesSet = new DataSeriesSet(client, preferences, dataSeriesSet.getUseCase());

        selectedSeries = new DataSeriesSerializer().fromString(dataSeriesSet, config);

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

        for (int ii = 0; ii < charts.length; ii += 2)
        {
            ConfigurationSet set = client.getSettings().getConfigurationSet(charts[ii] + IDENTIFIER_POSTFIX);

            MenuManager menu = new MenuManager(charts[ii + 1]);
            copyFromOthers.add(menu);

            set.getConfigurations().forEach(config -> menu.add(new SimpleAction(config.getName(), a -> {
                List<DataSeries> list = new DataSeriesSerializer().fromString(dataSeriesSet, config.getData());
                list.stream().filter(s -> !selectedSeries.contains(s)).forEach(s -> selectedSeries.add(s));
                fireUpdate();
            })));
        }
    }

    /* package */ void doDeleteSeries(DataSeries series)
    {
        selectedSeries.remove(series);
        fireUpdate();
    }

    @Override
    public void beforeConfigurationPicked()
    {
        // do nothing - all configuration changes are stored via #updateActive
    }

    @Override
    public void onConfigurationPicked(String data)
    {
        this.doResetSeries(data);
    }
}
