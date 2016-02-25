package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Taxonomy;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.ListSelectionDialog;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ConfigurationStore;
import name.abuchen.portfolio.ui.util.ConfigurationStore.ConfigurationStoreOwner;

/* package */class ChartConfigurator extends Composite implements ConfigurationStoreOwner
{
    /* package */static enum ClientDataSeries
    {
        TOTALS, INVESTED_CAPITAL, TRANSFERALS, TAXES, ABSOLUTE_DELTA, DIVIDENDS, DIVIDENDS_ACCUMULATED, INTEREST, INTEREST_ACCUMULATED;
    }

    /* package */static final class DataSeries
    {
        private Class<?> type;
        private Object instance;
        private String label;
        private boolean isLineChart = true;
        private boolean isBenchmark = false;
        private boolean isPortfolioPlus = false;

        private Color color;
        private RGB rgb;

        private boolean showArea;
        private LineStyle lineStyle = LineStyle.SOLID;

        private DataSeries(Class<?> type, Object instance, String label, Color color)
        {
            this.type = type;
            this.instance = instance;
            this.label = label;
            this.color = color;
            this.rgb = color.getRGB();
        }

        public Class<?> getType()
        {
            return type;
        }

        public Object getInstance()
        {
            return instance;
        }

        public String getLabel()
        {
            return isBenchmark() ? label + Messages.ChartSeriesBenchmarkSuffix : label;
        }

        public String getSearchLabel()
        {
            StringBuilder buf = new StringBuilder();

            buf.append(label);

            if (instance instanceof Classification)
            {
                Classification parent = ((Classification) instance).getParent();
                buf.append(" (").append(parent.getPathName(true)).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
            }

            if (isBenchmark())
                buf.append(Messages.ChartSeriesBenchmarkSuffix);

            return buf.toString();
        }

        public void setColor(Color color)
        {
            this.color = color;
            this.rgb = color.getRGB();
        }

        public Color getColor()
        {
            return color;
        }

        public RGB getRGB()
        {
            return rgb;
        }

        public boolean isLineChart()
        {
            return isLineChart;
        }

        public void setLineChart(boolean isLineChart)
        {
            this.isLineChart = isLineChart;
        }

        public boolean isBenchmark()
        {
            return isBenchmark;
        }

        public void setBenchmark(boolean isBenchmark)
        {
            this.isBenchmark = isBenchmark;
        }

        public boolean isPortfolioPlus()
        {
            return isPortfolioPlus;
        }

        public void setPortfolioPlus(boolean isPortfolioPlus)
        {
            this.isPortfolioPlus = isPortfolioPlus;
        }

        public boolean isShowArea()
        {
            return showArea;
        }

        public void setShowArea(boolean showArea)
        {
            this.showArea = showArea;
        }

        public LineStyle getLineStyle()
        {
            return lineStyle;
        }

        public void setLineStyle(LineStyle lineStyle)
        {
            this.lineStyle = lineStyle;
        }

        public Image getImage()
        {
            if (type == Security.class)
                return Images.SECURITY.image();
            else if (type == Account.class)
                return Images.ACCOUNT.image();
            else if (type == Portfolio.class)
                return Images.PORTFOLIO.image();
            else if (type == Classification.class)
                return Images.CATEGORY.image();
            else
                return null;
        }

        public String getUUID()
        {
            String prefix = isBenchmark() ? "[b]" : ""; //$NON-NLS-1$ //$NON-NLS-2$
            if (isPortfolioPlus())
                prefix += "[+]"; //$NON-NLS-1$

            if (type == Security.class)
                return prefix + Security.class.getSimpleName() + ((Security) instance).getUUID();
            else if (type == Client.class)
                return prefix + Client.class.getSimpleName() + "-" + ((ClientDataSeries) instance).name().toLowerCase(); //$NON-NLS-1$
            else if (type == Account.class)
                return prefix + Account.class.getSimpleName() + ((Account) instance).getUUID();
            else if (type == Portfolio.class)
                return prefix + Portfolio.class.getSimpleName() + ((Portfolio) instance).getUUID();
            else if (type == ConsumerPriceIndex.class)
                return prefix + ConsumerPriceIndex.class.getSimpleName();
            else if (type == Classification.class)
                return prefix + Classification.class.getSimpleName() + ((Classification) instance).getId();

            throw new UnsupportedOperationException(type.getName());
        }

        public void configure(ILineSeries series)
        {
            series.setLineColor(getColor());
            series.setSymbolColor(getColor());
            series.enableArea(showArea);
            series.setLineStyle(lineStyle);
        }

        public void configure(IBarSeries series)
        {
            series.setBarPadding(50);
            series.setBarColor(getColor());
        }
    }

    @FunctionalInterface
    public interface Listener
    {
        void onUpdate();
    }

    public enum Mode
    {
        STATEMENT_OF_ASSETS, PERFORMANCE, RETURN_VOLATILITY
    }

    private static final ResourceBundle LABELS = ResourceBundle.getBundle("name.abuchen.portfolio.ui.views.labels"); //$NON-NLS-1$

    private final String identifier;
    private final Client client;
    private final Mode mode;

    private ChartConfigurator.Listener listener;

    private final List<DataSeries> availableSeries = new ArrayList<DataSeries>();
    private final List<DataSeries> selectedSeries = new ArrayList<DataSeries>();

    private ConfigurationStore store;

    private LocalResourceManager resources;
    private Menu configContextMenu;

    public ChartConfigurator(Composite parent, AbstractFinanceView view, Mode mode)
    {
        super(parent, SWT.NONE);

        this.identifier = view.getClass().getSimpleName() + "-PICKER"; //$NON-NLS-1$
        this.client = view.getClient();
        this.mode = mode;
        this.resources = new LocalResourceManager(JFaceResources.getResources(), this);

        this.store = new ConfigurationStore(identifier, client, view.getPreferenceStore(), this);

        buildAvailableDataSeries();
        load();

        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        RowLayout layout = new RowLayout();
        layout.wrap = true;
        layout.pack = true;
        layout.fill = true;
        setLayout(layout);

        for (DataSeries series : selectedSeries)
            new PaintItem(this, series);

        parent.addDisposeListener(e -> ChartConfigurator.this.widgetDisposed());
    }

    public void setListener(ChartConfigurator.Listener listener)
    {
        this.listener = listener;
    }
    
    public String getConfigurationName()
    {
        return store.getActiveName();
    }

    public void showMenu(Shell shell)
    {
        if (configContextMenu == null)
        {
            configContextMenu = createMenu(shell, new IMenuListener()
            {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    configMenuAboutToShow(manager);
                }
            });
        }
        configContextMenu.setVisible(true);
    }

    public void showSaveMenu(Shell shell)
    {
        store.showSaveMenu(shell);
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

    private Color colorFor(RGB rgb)
    {
        return resources.createColor(ColorDescriptor.createFrom(rgb));
    }

    private Color colorFor(Colors color)
    {
        return resources.createColor(ColorDescriptor.createFrom(color.swt()));
    }

    private void buildAvailableDataSeries()
    {
        ColorWheel wheel = new ColorWheel(this, 30);

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
        }

        buildCommonDataSeries(wheel);
    }

    private void buildStatementOfAssetsDataSeries()
    {
        availableSeries.add(new DataSeries(Client.class, ClientDataSeries.TOTALS, Messages.LabelTotalSum,
                        colorFor(Colors.TOTALS)));

        DataSeries series = new DataSeries(Client.class, ClientDataSeries.TRANSFERALS, Messages.LabelTransferals,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.INVESTED_CAPITAL, Messages.LabelInvestedCapital, Display
                        .getDefault().getSystemColor(SWT.COLOR_GRAY));
        series.setShowArea(true);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.ABSOLUTE_DELTA, Messages.LabelAbsoluteDelta, Display
                        .getDefault().getSystemColor(SWT.COLOR_GRAY));
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.TAXES, Messages.LabelAccumulatedTaxes, Display
                        .getDefault().getSystemColor(SWT.COLOR_RED));
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.DIVIDENDS, Messages.LabelDividends, Display.getDefault()
                        .getSystemColor(SWT.COLOR_DARK_MAGENTA));
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.DIVIDENDS_ACCUMULATED,
                        Messages.LabelAccumulatedDividends, Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA));
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.INTEREST, Messages.LabelInterest, Display.getDefault()
                        .getSystemColor(SWT.COLOR_DARK_GREEN));
        series.setLineChart(false);
        availableSeries.add(series);

        series = new DataSeries(Client.class, ClientDataSeries.INTEREST_ACCUMULATED, Messages.LabelAccumulatedInterest,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN));
        availableSeries.add(series);

    }

    private void buildPerformanceDataSeries(ColorWheel wheel)
    {
        // accumulated performance
        availableSeries.add(new DataSeries(Client.class, ClientDataSeries.TOTALS,
                        Messages.PerformanceChartLabelAccumulatedIRR, colorFor(Colors.TOTALS)));

        // daily change - must be TRANSFERALS for historical reasons as
        // it was stored this way in the XML file
        DataSeries series = new DataSeries(Client.class, ClientDataSeries.TRANSFERALS, Messages.LabelAggregationDaily,
                        Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
        series.setLineChart(false);
        availableSeries.add(series);

        // consumer price index
        series = new DataSeries(ConsumerPriceIndex.class, null, Messages.LabelConsumerPriceIndex, colorFor(Colors.CPI));
        series.setBenchmark(true);
        series.setLineStyle(LineStyle.DASHDOTDOT);
        availableSeries.add(series);

        // securities as benchmark
        int index = 0;
        for (Security security : client.getSecurities())
        {
            series = new DataSeries(Security.class, security, security.getName(), //
                            wheel.getSegment(index++).getColor());
            series.setBenchmark(true);
            availableSeries.add(series);
        }
    }

    private void buildReturnVolatilitySeries(ColorWheel wheel)
    {
        // accumulated performance
        availableSeries.add(new DataSeries(Client.class, ClientDataSeries.TOTALS,
                        Messages.PerformanceChartLabelAccumulatedIRR, colorFor(Colors.TOTALS)));

        // securities as benchmark
        int index = 0;
        for (Security security : client.getSecurities())
        {
            DataSeries series = new DataSeries(Security.class, security, security.getName(), //
                            wheel.getSegment(index++).getColor());
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

            availableSeries.add(new DataSeries(Security.class, security, security.getName(), wheel.getSegment(index++)
                            .getColor()));
        }

        for (Portfolio portfolio : client.getPortfolios())
            availableSeries.add(new DataSeries(Portfolio.class, portfolio, portfolio.getName(), wheel.getSegment(
                            index++).getColor()));

        // portfolio + reference account
        for (Portfolio portfolio : client.getPortfolios())
        {
            DataSeries series = new DataSeries(Portfolio.class, portfolio, portfolio.getName() + " + " //$NON-NLS-1$
                            + portfolio.getReferenceAccount().getName(), wheel.getSegment(index++).getColor());
            series.setPortfolioPlus(true);
            availableSeries.add(series);
        }

        for (Account account : client.getAccounts())
            availableSeries.add(new DataSeries(Account.class, account, account.getName(), wheel.getSegment(index++)
                            .getColor()));

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
                                    colorFor(Colors.toRGB(classification.getColor()))));
                }
            });
        }
    }

    private void addDefaultDataSeries()
    {
        EnumSet<ClientDataSeries> set = EnumSet.of(ClientDataSeries.TOTALS, ClientDataSeries.TRANSFERALS);

        for (DataSeries series : availableSeries)
        {
            if (series.getType() == Client.class && set.contains(series.getInstance()))
            {
                selectedSeries.add(series);
            }
            else if (series.getType() == ConsumerPriceIndex.class)
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
        Map<String, DataSeries> uuid2series = new HashMap<String, DataSeries>();
        for (DataSeries series : availableSeries)
            uuid2series.put(series.getUUID(), series);

        String[] items = config.split(","); //$NON-NLS-1$
        for (String item : items)
        {
            String[] store = item.split(";"); //$NON-NLS-1$

            String uuid = store[0];
            DataSeries s = uuid2series.get(uuid);
            if (s != null)
            {
                selectedSeries.add(s);

                if (store.length == 4)
                {
                    s.setColor(colorFor(Colors.toRGB(store[1])));
                    s.setLineStyle(LineStyle.valueOf(store[2]));
                    s.setShowArea(Boolean.parseBoolean(store[3]));
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
            buf.append(Colors.toHex(s.getRGB())).append(';');
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

    public void configMenuAboutToShow(IMenuManager manager)
    {
        for (final DataSeries series : selectedSeries)
        {
            Action action = new Action(series.getLabel())
            {
                @Override
                public void run()
                {
                    doDeleteSeries(series);
                }
            };
            action.setChecked(true);
            manager.add(action);
        }

        manager.add(new Separator());

        manager.add(new Action(Messages.ChartSeriesPickerAddItem)
        {
            @Override
            public void run()
            {
                doAddSeries(false);
            }
        });

        if (mode != Mode.STATEMENT_OF_ASSETS)
        {
            manager.add(new Action(Messages.ChartSeriesPickerAddBenchmark)
            {
                @Override
                public void run()
                {
                    doAddSeries(true);
                }
            });
        }

        manager.add(new Action(Messages.MenuResetChartSeries)
        {
            @Override
            public void run()
            {
                doResetSeries(null);
            }
        });
    }

    private void seriesMenuAboutToShow(IMenuManager manager, final PaintItem paintItem)
    {
        manager.add(new Action(Messages.ChartSeriesPickerColor)
        {
            @Override
            public void run()
            {
                ColorDialog colorDialog = new ColorDialog(getShell());
                colorDialog.setRGB(paintItem.series.getColor().getRGB());
                RGB newColor = colorDialog.open();
                if (newColor != null)
                {
                    paintItem.series.setColor(resources.createColor(newColor));
                    paintItem.redraw();
                    listener.onUpdate();
                    store.updateActive(serialize());
                }
            }
        });

        if (paintItem.series.isLineChart() && mode != Mode.RETURN_VOLATILITY)
        {
            MenuManager lineStyle = new MenuManager(Messages.ChartSeriesPickerLineStyle);
            for (final LineStyle style : LineStyle.values())
            {
                if (style == LineStyle.NONE)
                    continue;

                Action action = new Action(LABELS.getString("lineStyle." + style.name())) //$NON-NLS-1$
                {
                    @Override
                    public void run()
                    {
                        paintItem.series.setLineStyle(style);
                        listener.onUpdate();
                        store.updateActive(serialize());
                    }
                };
                action.setChecked(style == paintItem.series.getLineStyle());
                lineStyle.add(action);
            }
            manager.add(lineStyle);

            Action actionShowArea = new Action(Messages.ChartSeriesPickerShowArea)
            {
                @Override
                public void run()
                {
                    paintItem.series.setShowArea(!paintItem.series.isShowArea());
                    listener.onUpdate();
                    store.updateActive(serialize());
                }
            };
            actionShowArea.setChecked(paintItem.series.isShowArea());
            manager.add(actionShowArea);
        }

        manager.add(new Separator());
        manager.add(new Action(Messages.ChartSeriesPickerRemove)
        {
            @Override
            public void run()
            {
                doDeleteSeries(paintItem.series);
            }
        });
    }

    private void doAddSeries(boolean showOnlyBenchmark)
    {
        List<DataSeries> list = new ArrayList<DataSeries>(availableSeries);

        // remove items if (not) showing benchmarks only
        Iterator<DataSeries> iter = list.iterator();
        while (iter.hasNext())
            if (iter.next().isBenchmark() != showOnlyBenchmark)
                iter.remove();

        // remove already selected items
        for (DataSeries s : selectedSeries)
            list.remove(s);

        ListSelectionDialog dialog = new ListSelectionDialog(getShell(), new DataSeriesLabelProvider());
        dialog.setTitle(Messages.ChartSeriesPickerTitle);
        dialog.setMessage(Messages.ChartSeriesPickerTitle);
        dialog.setElements(list);

        if (dialog.open() != ListSelectionDialog.OK)
            return;

        Object[] result = dialog.getResult();
        if (result == null || result.length == 0)
            return;

        for (Object object : result)
        {
            selectedSeries.add((DataSeries) object);
            new PaintItem(this, (DataSeries) object);
        }

        layout();
        getParent().layout();

        listener.onUpdate();
        store.updateActive(serialize());
    }

    private void doResetSeries(String config)
    {
        availableSeries.clear();
        buildAvailableDataSeries();

        selectedSeries.clear();

        for (Control child : getChildren())
            child.dispose();

        if (config == null)
            addDefaultDataSeries();
        else
            load(config);

        for (DataSeries series : selectedSeries)
            new PaintItem(this, series);

        layout();
        getParent().layout();
        listener.onUpdate();
        store.updateActive(serialize());
    }

    private void doDeleteSeries(DataSeries series)
    {
        for (Control child : getChildren())
        {
            if (((PaintItem) child).series == series)
            {
                selectedSeries.remove(series);
                child.dispose();
                layout();
                getParent().layout();
                listener.onUpdate();
                store.updateActive(serialize());
                break;
            }
        }
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

    private static final class PaintItem extends Canvas implements org.eclipse.swt.widgets.Listener
    {
        private final DataSeries series;

        public PaintItem(Composite parent, DataSeries series)
        {
            super(parent, SWT.NONE);

            this.series = series;

            setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

            addListener(SWT.Paint, this);
            addListener(SWT.Resize, this);

            MenuManager menuManager = new MenuManager();
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(new IMenuListener()
            {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    ((ChartConfigurator) getParent()).seriesMenuAboutToShow(manager, PaintItem.this);
                }
            });
            this.setMenu(menuManager.createContextMenu(this));
        }

        public void handleEvent(Event event)
        {
            switch (event.type)
            {
                case SWT.Paint:
                    paintControl(event);
                    break;
                case SWT.Resize:
                    redraw();
                    break;
                default:
                    break;
            }
        }

        private void paintControl(Event e)
        {
            Color oldForeground = e.gc.getForeground();
            Color oldBackground = e.gc.getBackground();

            Point size = getSize();
            Rectangle r = new Rectangle(0, 0, size.y, size.y);
            GC gc = e.gc;

            gc.setBackground(series.getColor());
            gc.fillRectangle(r.x, r.y, r.width, r.height);

            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            gc.drawRectangle(r.x, r.y, r.width - 1, r.height - 1);

            String text = series.getLabel();

            e.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            e.gc.drawString(text, size.y + 2, 1, true);

            e.gc.setForeground(oldForeground);
            e.gc.setBackground(oldBackground);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            String text = series.getLabel();

            GC gc = new GC(this);
            Point extentText = gc.stringExtent(text);
            gc.dispose();

            return new Point(extentText.x + extentText.y + 12, extentText.y + 2);
        }
    }
}
