package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.DoubleFunction;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.dataseries.DataSeries;
import name.abuchen.portfolio.util.Interval;

public class PerformanceHeatmapWidget extends WidgetDelegate
{
    private interface CellDataProvider
    {
        default Color getBackground()
        {
            return Colors.WHITE;
        }

        default Font getFont()
        {
            return null;
        }

        String getText();
    }

    private class Cell extends Canvas // NOSONAR
    {
        private static final int MARGIN = 2;

        private CellDataProvider dataProvider;

        public Cell(Composite parent, CellDataProvider dataProvider)
        {
            super(parent, SWT.NO_BACKGROUND | SWT.NO_FOCUS);
            this.dataProvider = dataProvider;

            addPaintListener(this::paintControl);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            GC gc = new GC(this);
            Point extent = gc.stringExtent(dataProvider.getText());
            gc.dispose();

            return new Point(10, extent.y + 2 * MARGIN);
        }

        private void paintControl(PaintEvent e)
        {
            GC gc = e.gc;

            Color oldBackground = gc.getBackground();
            Color oldForeground = gc.getForeground();
            Font oldFont = gc.getFont();

            Rectangle bounds = getClientArea();

            gc.setBackground(dataProvider.getBackground());
            gc.setForeground(Colors.BLACK);
            gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

            Font newFont = dataProvider.getFont();
            if (newFont != null)
                gc.setFont(newFont);

            String text = dataProvider.getText();
            Point extend = gc.stringExtent(text);

            gc.drawText(text, bounds.x + (bounds.width - extend.x) / 2, bounds.y + (bounds.height - extend.y) / 2);

            gc.setBackground(oldBackground);
            gc.setForeground(oldForeground);
            gc.setFont(oldFont);
        }

    }

    private class ColorSchemaConfig implements WidgetConfig
    {
        private final WidgetDelegate delegate;

        private ColorSchema colorSchema = ColorSchema.GREEN_YELLOW_RED;

        public ColorSchemaConfig(WidgetDelegate delegate)
        {
            this.delegate = delegate;

            String code = delegate.getWidget().getConfiguration().get(Dashboard.Config.COLOR_SCHEMA.name());

            if (code != null)
            {
                try
                {
                    colorSchema = ColorSchema.valueOf(code);
                }
                catch (IllegalArgumentException ignore)
                {
                    PortfolioPlugin.log(ignore);
                }
            }
        }

        @Override
        public void menuAboutToShow(IMenuManager manager)
        {
            MenuManager subMenu = new MenuManager(getLabel());
            manager.add(subMenu);

            subMenu.add(buildAction(Messages.LabelGreenYellowRed, ColorSchema.GREEN_YELLOW_RED));
            subMenu.add(buildAction(Messages.LabelGreenWhiteRed, ColorSchema.GREEN_WHITE_RED));
        }

        private Action buildAction(String label, ColorSchema schema)
        {
            Action action = new SimpleAction(label, a -> {
                this.colorSchema = schema;
                delegate.getWidget().getConfiguration().put(Dashboard.Config.COLOR_SCHEMA.name(), schema.name());
                delegate.getClient().markDirty();
            });
            action.setChecked(this.colorSchema == schema);
            return action;
        }

        @Override
        public String getLabel()
        {
            return Messages.LabelColorSchema;
        }

        public ColorSchema getColorSchema()
        {
            return colorSchema;
        }
    }

    private enum ColorSchema
    {
        GREEN_YELLOW_RED, GREEN_WHITE_RED
    }

    private Composite table;
    private Label title;
    private DashboardResources resources;

    public PerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, true));
        addConfig(new ColorSchemaConfig(this));
    }

    @Override
    Composite createControl(Composite parent, DashboardResources resources)
    {
        this.resources = resources;

        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        table = new Composite(container, SWT.NONE);
        // 13 columns, one for the legend and 12 for the months
        GridDataFactory.fillDefaults().grab(true, false).applyTo(table);
        GridLayoutFactory.fillDefaults().numColumns(13).equalWidth(true).spacing(1, 1).applyTo(table);
        table.setBackground(container.getBackground());

        fillTable();

        return container;
    }

    private DoubleFunction<Color> buildColorFunction()
    {
        ColorSchema schema = get(ColorSchemaConfig.class).getColorSchema();

        switch (schema)
        {
            case GREEN_YELLOW_RED:
                return performance -> {
                    // convert to 0 = -0.07 -> 1 = +0.07
                    final double max = 0.07f;

                    double p = performance;
                    p = Math.max(-max, p);
                    p = Math.min(max, p);
                    p = (p + max) * (1 / (2 * max));

                    // 0 = red, 60 = yellow, 120 = red
                    float hue = (float) p * 120f;
                    return resources.getResourceManager().createColor(new RGB(hue, 0.9f, 1f));
                };
            case GREEN_WHITE_RED:
                return performance -> {
                    double max = 0.07;
                    double p = Math.min(max, Math.abs(performance));
                    int colorValue = (int) (255 * (1 - p / max));
                    RGB color = performance > 0d ? new RGB(colorValue, 255, colorValue)
                                    : new RGB(255, colorValue, colorValue);
                    return resources.getResourceManager().createColor(color);
                };
            default:
                throw new IllegalArgumentException();
        }
    }

    private void fillTable()
    {
        // fill the table lines according to the supplied period
        // calculate the performance with a temporary reporting period
        // calculate the color interpolated between red and green with yellow as
        // the median
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval();

        DoubleFunction<Color> coloring = buildColorFunction();

        addHeaderRow();

        DataSeries dataSeries = get(DataSeriesConfig.class).getDataSeries();

        // adapt interval to include the first and last month fully

        Interval calcInterval = Interval.of(
                        interval.getStart().getDayOfMonth() == interval.getStart().lengthOfMonth() ? interval.getStart()
                                        : interval.getStart().withDayOfMonth(1).minusDays(1),
                        interval.getEnd().withDayOfMonth(interval.getEnd().lengthOfMonth()));

        PerformanceIndex performanceIndex = getDashboardData().calculate(dataSeries,
                        new ReportingPeriod.FromXtoY(calcInterval));

        Interval actualInterval = performanceIndex.getActualInterval();
        
        for (Integer year : actualInterval.iterYears())
        {
            // year
            Cell cell = new Cell(table, () -> {
                int numColumns = getDashboardData().getDashboard().getColumns().size();
                return numColumns > 2 ? String.valueOf(year % 100) : String.valueOf(year);
            });
            GridDataFactory.fillDefaults().grab(true, false).applyTo(cell);

            // monthly data
            for (LocalDate month = LocalDate.of(year, 1, 1); month.getYear() == year; month = month.plusMonths(1))
            {
                if (actualInterval.contains(month))
                {
                    cell = createCell(performanceIndex, month, coloring);
                    InfoToolTip.attach(cell, Messages.PerformanceHeatmapToolTip);
                }
                else
                {
                    cell = new Cell(table, () -> ""); //$NON-NLS-1$
                }
                GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(cell);
            }
        }
        table.layout(true);
    }

    private Cell createCell(PerformanceIndex index, LocalDate month, DoubleFunction<Color> coloring)
    {
        int start = Arrays.binarySearch(index.getDates(), month.minusDays(1));
        // should not happen, but let's be defensive this time
        if (start < 0)
            return new Cell(table, () -> ""); //$NON-NLS-1$
        
        int end = Arrays.binarySearch(index.getDates(), month.withDayOfMonth(month.lengthOfMonth()));
        // make sure there is an end index if the binary search returns a
        // negative value (i.e. if the current month is not finished)
        if (end < 0)
        {
            // take the last available date
            end = index.getDates().length - 1;
        }

        double performance = ((index.getAccumulatedPercentage()[end] + 1)
                        / (index.getAccumulatedPercentage()[start] + 1)) - 1;

        return new Cell(table, new CellDataProvider()
        {
            @Override
            public Color getBackground()
            {
                return coloring.apply(performance);
            }

            @Override
            public Font getFont()
            {
                return resources.getSmallFont();
            }

            @Override
            public String getText()
            {
                return Values.PercentShort.format(performance);
            }
        });
    }

    private void addHeaderRow()
    {
        // Top Left is empty
        new Cell(table, () -> ""); //$NON-NLS-1$

        // then the legend of the months
        // no harm in hardcoding the year as each year has the same months
        for (LocalDate m = LocalDate.of(2016, 1, 1); m.getYear() == 2016; m = m.plusMonths(1))
        {
            Month month = m.getMonth();
            Cell cell = new Cell(table, () -> {
                int numColumns = getDashboardData().getDashboard().getColumns().size();

                TextStyle textStyle;
                if (numColumns == 1)
                    textStyle = TextStyle.FULL;
                else if (numColumns == 2)
                    textStyle = TextStyle.SHORT;
                else
                    textStyle = TextStyle.NARROW;

                return month.getDisplayName(textStyle, Locale.getDefault());
            });
            GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.FILL).applyTo(cell);
        }
    }

    @Override
    void update()
    {
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$

        for (Control child : table.getChildren())
            child.dispose();

        fillTable();

        table.getParent().layout(true);
        table.getParent().getParent().layout(true);
    }

    @Override
    Control getTitleControl()
    {
        return title;
    }

}
