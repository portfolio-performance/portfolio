package name.abuchen.portfolio.ui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Category;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.ConsumerPriceIndex;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.Security.AssetClass;
import name.abuchen.portfolio.ui.AbstractFinanceView;
import name.abuchen.portfolio.ui.ClientEditor;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.swtchart.IBarSeries;
import org.swtchart.ILineSeries;
import org.swtchart.LineStyle;

/* package */class ChartConfigurator extends Composite
{
    public static final class DataSeries
    {
        private Class<?> type;
        private Object instance;
        private String label;
        private boolean isLineChart = true;

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
            return label;
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
                return PortfolioPlugin.image(PortfolioPlugin.IMG_SECURITY);
            else if (type == Account.class)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_ACCOUNT);
            else if (type == Portfolio.class)
                return PortfolioPlugin.image(PortfolioPlugin.IMG_PORTFOLIO);
            else if (type == Category.class)
                return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
            else
                return null;
        }

        public String getUUID()
        {
            if (type == Security.class)
                return Security.class.getSimpleName() + ((Security) instance).getUUID();
            else if (type == AssetClass.class)
                return AssetClass.class.getSimpleName() + ((AssetClass) instance).name();
            else if (type == Client.class)
                return Client.class.getSimpleName() + (instance != null ? "-totals" : "-transferals"); //$NON-NLS-1$ //$NON-NLS-2$
            else if (type == Account.class)
                return Account.class.getSimpleName() + ((Account) instance).getUUID();
            else if (type == Portfolio.class)
                return Portfolio.class.getSimpleName() + ((Portfolio) instance).getUUID();
            else if (type == Category.class)
                return Category.class.getSimpleName() + ((Category) instance).getUUID();
            else if (type == ConsumerPriceIndex.class)
                return ConsumerPriceIndex.class.getSimpleName();

            throw new UnsupportedOperationException(type.getName());
        }

        public void configure(ILineSeries series)
        {
            series.setLineColor(getColor());
            series.enableArea(showArea);
            series.setLineStyle(lineStyle);
        }

        public void configure(IBarSeries series)
        {
            series.setBarPadding(50);
            series.setBarColor(getColor());
        }
    }

    public interface Listener
    {
        void onUpdate();
    }

    public enum Mode
    {
        STATEMENT_OF_ASSETS, PERFORMANCE
    }

    private static final class Configuration
    {
        private String name;
        private String config;

        public Configuration(String name, String config)
        {
            this.name = name;
            this.config = config;
        }

        public String getName()
        {
            return name;
        }

        public String getConfig()
        {
            return config;
        }

        public void setConfig(String config)
        {
            this.config = config;
        }
    }

    private static final ResourceBundle LABELS = ResourceBundle.getBundle("name.abuchen.portfolio.ui.views.labels"); //$NON-NLS-1$

    private final String identifier;
    private final ClientEditor clientEditor;
    private final Client client;
    private final Mode mode;

    private ChartConfigurator.Listener listener;

    private final List<DataSeries> availableSeries = new ArrayList<DataSeries>();
    private final List<DataSeries> selectedSeries = new ArrayList<DataSeries>();

    private String currentConfiguration;
    private List<Configuration> storedConfigurations = new ArrayList<Configuration>();

    private LocalResourceManager resources;
    private Menu configContextMenu;
    private Menu saveContextMenu;

    public ChartConfigurator(Composite parent, AbstractFinanceView view, Mode mode)
    {
        super(parent, SWT.NONE);

        this.identifier = view.getClass().getSimpleName() + "-PICKER"; //$NON-NLS-1$
        this.clientEditor = view.getClientEditor();
        this.client = clientEditor.getClient();
        this.mode = mode;
        this.resources = new LocalResourceManager(JFaceResources.getResources(), this);

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

        parent.addDisposeListener(new DisposeListener()
        {
            @Override
            public void widgetDisposed(DisposeEvent e)
            {
                ChartConfigurator.this.widgetDisposed();
            }
        });
    }

    public void setListener(ChartConfigurator.Listener listener)
    {
        this.listener = listener;
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
        if (saveContextMenu == null)
        {
            saveContextMenu = createMenu(shell, new IMenuListener()
            {
                @Override
                public void menuAboutToShow(IMenuManager manager)
                {
                    saveMenuAboutToShow(manager);
                }
            });
        }
        saveContextMenu.setVisible(true);
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
        return (Color) resources.createColor(ColorDescriptor.createFrom(rgb));
    }

    private Color colorFor(Colors color)
    {
        return (Color) resources.createColor(ColorDescriptor.createFrom(color.swt()));
    }

    private void buildAvailableDataSeries()
    {
        switch (mode)
        {
            case STATEMENT_OF_ASSETS:
            {
                availableSeries.add(new DataSeries(Client.class, client, Messages.LabelTotalSum,
                                colorFor(Colors.TOTALS)));

                DataSeries series = new DataSeries(Client.class, null, Messages.LabelTransferals, //
                                Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
                series.setLineChart(false);
                availableSeries.add(series);
                break;
            }
            case PERFORMANCE:
            {
                availableSeries.add(new DataSeries(Client.class, client, Messages.PerformanceChartLabelAccumulatedIRR,
                                colorFor(Colors.TOTALS)));

                DataSeries series = new DataSeries(Client.class, null, Messages.LabelAggregationDaily, Display
                                .getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
                series.setLineChart(false);
                availableSeries.add(series);

                series = new DataSeries(ConsumerPriceIndex.class, null, Messages.LabelConsumerPriceIndex,
                                colorFor(Colors.CPI));
                series.setLineStyle(LineStyle.DASHDOTDOT);
                availableSeries.add(series);
                break;
            }
        }

        ColorWheel wheel = new ColorWheel(this, 30);
        int index = 0;

        for (Security security : client.getSecurities())
            availableSeries.add(new DataSeries(Security.class, security, security.getName(), wheel.getSegment(index++)
                            .getColor()));

        for (Portfolio portfolio : client.getPortfolios())
            availableSeries.add(new DataSeries(Portfolio.class, portfolio, portfolio.getName(), wheel.getSegment(
                            index++).getColor()));

        for (Account account : client.getAccounts())
            availableSeries.add(new DataSeries(Account.class, account, account.getName(), wheel.getSegment(index++)
                            .getColor()));

        for (AssetClass assetClass : AssetClass.values())
            availableSeries.add(new DataSeries(AssetClass.class, assetClass, assetClass.toString(), //
                            colorFor(Colors.valueOf(assetClass.name()).swt())));

        LinkedList<Category> stack = new LinkedList<Category>();
        stack.add(client.getRootCategory());

        while (!stack.isEmpty())
        {
            Category category = stack.removeFirst();
            for (Category child : category.getChildren())
            {
                availableSeries.add(new DataSeries(Category.class, child, child.getName(), wheel.getSegment(index++)
                                .getColor()));
                stack.add(child);
            }
        }
    }

    private void addDefaultDataSeries()
    {
        for (DataSeries series : availableSeries)
        {
            if (series.getType() == Client.class || series.getType() == ConsumerPriceIndex.class)
            {
                selectedSeries.add(series);
            }
            else if (mode == Mode.STATEMENT_OF_ASSETS && series.getType() == AssetClass.class)
            {
                selectedSeries.add(series);
            }
        }
    }

    private void load()
    {
        String config = client.getProperty(identifier);

        if (config == null || config.trim().length() == 0)
            config = clientEditor.getPreferenceStore().getString(identifier);

        if (config != null && config.trim().length() > 0)
            load(config);

        if (selectedSeries.isEmpty())
        {
            addDefaultDataSeries();
            persist();
        }

        loadStoredConfigurations();
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

        currentConfiguration = config;
    }

    private void persist()
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
        currentConfiguration = buf.toString();
        client.setProperty(identifier, currentConfiguration);
    }

    private void loadStoredConfigurations()
    {
        int index = 0;

        String config = client.getProperty(identifier + '$' + index);
        while (config != null)
        {
            String[] split = config.split(":="); //$NON-NLS-1$
            storedConfigurations.add(new Configuration(split[0], split[1]));

            index++;
            config = client.getProperty(identifier + '$' + index);
        }
    }

    private void persistStoredConfigurations()
    {
        for (int index = 0; index < storedConfigurations.size(); index++)
        {
            Configuration config = storedConfigurations.get(index);
            client.setProperty(identifier + '$' + index, config.getName() + ":=" + config.getConfig()); //$NON-NLS-1$
        }

        client.removeProperity(identifier + '$' + storedConfigurations.size());
    }

    private void widgetDisposed()
    {
        if (configContextMenu != null && !configContextMenu.isDisposed())
            configContextMenu.dispose();
        if (saveContextMenu != null && !saveContextMenu.isDisposed())
            saveContextMenu.dispose();
    }

    private void configMenuAboutToShow(IMenuManager manager)
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
                doAddSeries();
            }
        });

        manager.add(new Action(Messages.MenuResetChartSeries)
        {
            @Override
            public void run()
            {
                doResetSeries(null);
            }
        });
    }

    private void saveMenuAboutToShow(IMenuManager manager)
    {
        for (final Configuration config : storedConfigurations)
        {
            if (config.getConfig().equals(currentConfiguration))
            {
                Action action = new Action(config.getName())
                {
                    @Override
                    public void run()
                    {
                        doResetSeries(null);
                    }
                };
                action.setChecked(true);
                manager.add(action);
            }
            else
            {
                Action action = new Action(config.getName())
                {
                    @Override
                    public void run()
                    {
                        doResetSeries(config.getConfig());
                    }
                };
                manager.add(action);
            }
        }

        manager.add(new Separator());

        manager.add(new Action(Messages.ChartSeriesPickerSave)
        {
            @Override
            public void run()
            {
                doSaveConfiguration();
            }
        });

        if (!storedConfigurations.isEmpty())
        {
            MenuManager configMenu = new MenuManager(Messages.ChartSeriesPickerDelete);
            for (final Configuration config : storedConfigurations)
            {
                configMenu.add(new Action(config.getName())
                {
                    @Override
                    public void run()
                    {
                        storedConfigurations.remove(config);
                        persistStoredConfigurations();
                    }
                });
            }
            manager.add(configMenu);
        }
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
                    paintItem.layout();
                    listener.onUpdate();
                    persist();
                }
            }
        });

        if (paintItem.series.isLineChart())
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
                        persist();
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
                    persist();
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

    private void doAddSeries()
    {
        List<DataSeries> list = new ArrayList<DataSeries>(availableSeries);
        for (DataSeries s : selectedSeries)
            list.remove(s);

        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new SecurityLabelProvider());
        dialog.setElements(list.toArray());
        dialog.setTitle(Messages.ChartSeriesPickerTitle);
        dialog.setMessage(Messages.ChartSeriesPickerTitle);
        dialog.setMultipleSelection(true);

        if (dialog.open() != Window.OK)
            return;

        Object[] result = dialog.getResult();
        if (result.length == 0)
            return;

        for (Object object : result)
        {
            selectedSeries.add((DataSeries) object);
            new PaintItem(this, (DataSeries) object);
        }

        getParent().layout();
        listener.onUpdate();
        persist();
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
        persist();
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
                persist();
                break;
            }
        }
    }

    private void doSaveConfiguration()
    {
        InputDialog dlg = new InputDialog(clientEditor.getSite().getShell(), Messages.ChartSeriesPickerDialogTitle,
                        Messages.ChartSeriesPickerDialogMsg, null, null);
        if (dlg.open() != InputDialog.OK)
            return;

        String name = dlg.getValue();

        boolean replace = false;
        for (Configuration config : storedConfigurations)
        {
            if (name.equals(config.getName()))
            {
                config.setConfig(currentConfiguration);
                replace = true;
                break;
            }
        }

        if (!replace)
            storedConfigurations.add(new Configuration(name, currentConfiguration));
        persistStoredConfigurations();
    }

    private static final class SecurityLabelProvider extends LabelProvider
    {
        @Override
        public Image getImage(Object element)
        {
            return ((DataSeries) element).getImage();
        }

        @Override
        public String getText(Object element)
        {
            return ((DataSeries) element).getLabel();
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
