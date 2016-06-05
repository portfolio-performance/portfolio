package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.util.Colors;
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
     * Data series available for the Client type.
     */
    public enum ClientDataSeries
    {
        TOTALS, INVESTED_CAPITAL, TRANSFERALS, TAXES, ABSOLUTE_DELTA, DIVIDENDS, DIVIDENDS_ACCUMULATED, INTEREST, INTEREST_ACCUMULATED, ACCUMULATED, DELTA_PERCENTAGE;
    }

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

    /**
     * Determines the selection of data series available.
     */
    public enum Mode
    {
        STATEMENT_OF_ASSETS, PERFORMANCE, RETURN_VOLATILITY
    }

    private final String identifier;
    private final Client client;
    private final Mode mode;

    private final List<DataSeriesConfigurator.Listener> listeners = new ArrayList<>();

    private final List<DataSeries> availableSeries = new ArrayList<>();
    private final List<DataSeries> selectedSeries = new ArrayList<>();

    private ConfigurationStore store;

    private Menu configContextMenu;

    public DataSeriesConfigurator(AbstractFinanceView view, Mode mode)
    {
        this.identifier = view.getClass().getSimpleName() + "-PICKER"; //$NON-NLS-1$
        this.client = view.getClient();
        this.mode = mode;

        this.store = new ConfigurationStore(identifier, client, view.getPreferenceStore(), this);

        buildAvailableDataSeries();
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

    private void buildAvailableDataSeries()
    {
        ColorWheel wheel = new ColorWheel(30);

        switch (mode)
        {
            case STATEMENT_OF_ASSETS:
                buildStatementOfAssetsDataSeries();
                break;
            case PERFORMANCE:
                buildPerformanceDataSeries(wheel);
                break;
            case RETURN_VOLATILITY:
                buildReturnVolatilitySeries(wheel);
                break;
            default:
                throw new IllegalArgumentException(mode.name());
        }

        buildCommonDataSeries(wheel);
    }

    private void buildStatementOfAssetsDataSeries()
    {
        availableSeries.add(new DataSeries(Client.class, ClientDataSeries.TOTALS, Messages.LabelTotalSum,
                        Colors.TOTALS.swt()));

        DataSeries series = new DataSeries(Client.class, ClientDataSeries.TRANSFERALS, Messages.LabelTransferals,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.INVESTED_CAPITAL, Messages.LabelInvestedCapital,
                        Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
        series.setShowArea(true);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.ABSOLUTE_DELTA, Messages.LabelAbsoluteDelta,
                        Display.getDefault().getSystemColor(SWT.COLOR_GRAY).getRGB());
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.TAXES, Messages.LabelAccumulatedTaxes,
                        Display.getDefault().getSystemColor(SWT.COLOR_RED).getRGB());
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.DIVIDENDS, Messages.LabelDividends,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA).getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.DIVIDENDS_ACCUMULATED,
                        Messages.LabelAccumulatedDividends,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA).getRGB());
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.INTEREST, Messages.LabelInterest,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN).getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.INTEREST_ACCUMULATED, Messages.LabelAccumulatedInterest,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN).getRGB());
        availableSeries.add(series);

    }

    private void buildPerformanceDataSeries(ColorWheel wheel)
    {
        // accumulated performance
        availableSeries.add(new DataSeries(Client.class, ClientDataSeries.ACCUMULATED,
                        Messages.PerformanceChartLabelAccumulatedIRR, Colors.TOTALS.swt()));

        DataSeries series = new DataSeries(Client.class, ClientDataSeries.DELTA_PERCENTAGE,
                        Messages.LabelAggregationDaily,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY).getRGB());
        series.setLineChart(false);
        availableSeries.add(series);

        // consumer price index
        series = new DataSeries(ConsumerPriceIndex.class, ConsumerPriceIndex.class, Messages.LabelConsumerPriceIndex,
                        Colors.CPI.swt());
        series.setBenchmark(true);
        series.setLineStyle(LineStyle.DASHDOTDOT);
        availableSeries.add(series);

        // securities as benchmark
        int index = 0;
        for (Security security : client.getSecurities())
        {
            series = new DataSeries(Security.class, security, security.getName(), //
                            wheel.getRGB(index++));
            series.setBenchmark(true);
            availableSeries.add(series);
        }
    }

    private void buildReturnVolatilitySeries(ColorWheel wheel)
    {
        // accumulated performance
        availableSeries.add(new DataSeries(Client.class, ClientDataSeries.TOTALS,
                        Messages.PerformanceChartLabelAccumulatedIRR, Colors.TOTALS.swt()));

        // securities as benchmark
        int index = 0;
        for (Security security : client.getSecurities())
        {
            DataSeries series = new DataSeries(Security.class, security, security.getName(), //
                            wheel.getRGB(index++));

            series.setBenchmark(true);
            availableSeries.add(series);
        }
    }

    private void buildCommonDataSeries(ColorWheel wheel)
    {
        int index = client.getSecurities().size();

        for (Security security : client.getSecurities())
        {
            // securites w/o currency code (e.g. a stock index) cannot be added
            // as equity data series (only as benchmark)
            if (security.getCurrencyCode() == null)
                continue;

            availableSeries.add(new DataSeries(Security.class, security, security.getName(), //
                            wheel.getRGB(index++)));
        }

        for (Portfolio portfolio : client.getPortfolios())
            availableSeries.add(new DataSeries(Portfolio.class, portfolio, portfolio.getName(), //
                            wheel.getRGB(index++)));

        // portfolio + reference account
        for (Portfolio portfolio : client.getPortfolios())
        {
            DataSeries series = new DataSeries(Portfolio.class, portfolio,
                            portfolio.getName() + " + " + portfolio.getReferenceAccount().getName(), //$NON-NLS-1$
                            wheel.getRGB(index++));
            series.setPortfolioPlus(true);
            availableSeries.add(series);
        }

        for (Account account : client.getAccounts())
            availableSeries.add(new DataSeries(Account.class, account, account.getName(), wheel.getRGB(index++)));

        for (Taxonomy taxonomy : client.getTaxonomies())
        {
            taxonomy.foreach(new Taxonomy.Visitor()
            {
                @Override
                public void visit(Classification classification)
                {
                    if (classification.getParent() == null)
                        return;

                    availableSeries.add(new DataSeries(Classification.class, classification, classification.getName(),
                                    Colors.toRGB(classification.getColor())));
                }
            });
        }
    }

    private void addDefaultDataSeries()
    {
        EnumSet<ClientDataSeries> set = EnumSet.of(ClientDataSeries.TOTALS, ClientDataSeries.TRANSFERALS);

        for (DataSeries series : availableSeries)
        {
            if ((series.getType() == Client.class && set.contains(series.getInstance()))
                            || series.getType() == ConsumerPriceIndex.class)
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
        Map<String, DataSeries> uuid2series = new HashMap<>();
        for (DataSeries series : availableSeries)
            uuid2series.put(series.getUUID(), series);

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

        if (mode != Mode.STATEMENT_OF_ASSETS)
            manager.add(new SimpleAction(Messages.ChartSeriesPickerAddBenchmark, a -> doAddSeries(true)));

        manager.add(new SimpleAction(Messages.MenuResetChartSeries, a -> doResetSeries(null)));
    }

    private void doAddSeries(boolean showOnlyBenchmark)
    {
        List<DataSeries> list = new ArrayList<>(availableSeries);

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
        availableSeries.clear();
        buildAvailableDataSeries();

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
