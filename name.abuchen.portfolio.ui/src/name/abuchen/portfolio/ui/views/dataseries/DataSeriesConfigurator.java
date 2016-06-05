package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ConfigurationStore;
import name.abuchen.portfolio.ui.util.ConfigurationStore.ConfigurationStoreOwner;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries.ClientDataSeries;

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

    private final String identifier;
    private final Client client;
    private final ConfigurationStore store;

    private final List<DataSeriesConfigurator.Listener> listeners = new ArrayList<>();

    private DataSeriesSet dataSeriesSet;
    private final List<DataSeries> selectedSeries = new ArrayList<>();

    private Menu configContextMenu;

    public DataSeriesConfigurator(AbstractFinanceView view, DataSeries.UseCase useCase)
    {
        this.identifier = view.getClass().getSimpleName() + "-PICKER"; //$NON-NLS-1$
        this.client = view.getClient();
        this.store = new ConfigurationStore(identifier, client, view.getPreferenceStore(), this);

        this.dataSeriesSet = new DataSeriesSet(client, useCase);
        load();

        view.getControl().addDisposeListener(e -> DataSeriesConfigurator.this.widgetDisposed());
    }

    public void addListener(DataSeriesConfigurator.Listener listener)
    {
        this.listeners.add(listener);
    }

    /* protected */ void fireUpdate()
    {
        listeners.forEach(l -> l.onUpdate());
        store.updateActive(serialize());
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

    private void addDefaultDataSeries()
    {
        EnumSet<ClientDataSeries> set = EnumSet.of(ClientDataSeries.TOTALS, ClientDataSeries.TRANSFERALS);

        for (DataSeries series : dataSeriesSet.getAvailableSeries())
        {
            if ((series.getType() == DataSeries.Type.CLIENT && set.contains(series.getInstance()))
                            || series.getType() == DataSeries.Type.CONSUMER_PRICE_INDEX)
            {
                selectedSeries.add(series);
            }
        }
    }

    private void load()
    {
        String config = store.getActive();

        if (config != null && config.trim().length() > 0)
            load(config);

        if (selectedSeries.isEmpty())
        {
            addDefaultDataSeries();
            store.updateActive(serialize());
        }
    }

    private void load(String config)
    {
        Map<String, DataSeries> uuid2series = dataSeriesSet.getAvailableSeries().stream()
                        .collect(Collectors.toMap(s -> s.getUUID(), s -> s));

        String[] items = config.split(","); //$NON-NLS-1$
        for (String item : items)
        {
            String[] data = item.split(";"); //$NON-NLS-1$

            String uuid = data[0];
            DataSeries s = uuid2series.get(uuid);
            if (s != null)
            {
                selectedSeries.add(s);

                if (data.length == 4)
                {
                    s.setColor(Colors.toRGB(data[1]));
                    s.setLineStyle(LineStyle.valueOf(data[2]));
                    s.setShowArea(Boolean.parseBoolean(data[3]));
                }
            }
        }
    }

    private String serialize()
    {
        StringBuilder buf = new StringBuilder();
        for (DataSeries s : selectedSeries)
        {
            if (buf.length() > 0)
                buf.append(',');
            buf.append(s.getUUID()).append(';');
            buf.append(Colors.toHex(s.getColor())).append(';');
            buf.append(s.getLineStyle().name()).append(';');
            buf.append(s.isShowArea());
        }
        return buf.toString();
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
        dataSeriesSet = new DataSeriesSet(client, dataSeriesSet.getUseCase());

        selectedSeries.clear();

        if (config == null)
            addDefaultDataSeries();
        else
            load(config);

        fireUpdate();
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

    private static final class DataSeriesLabelProvider extends LabelProvider
    {
        @Override
        public Image getImage(Object element)
        {
            return ((DataSeries) element).getImage();
        }

        @Override
        public String getText(Object element)
        {
            return ((DataSeries) element).getSearchLabel();
        }
    }
}
