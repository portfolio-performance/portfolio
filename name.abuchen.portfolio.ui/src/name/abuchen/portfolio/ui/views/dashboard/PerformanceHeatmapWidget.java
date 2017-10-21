package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

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

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.PerformanceIndex;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
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

    private Composite table;
    private Label title;
    private DashboardResources resources;

    public PerformanceHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new DataSeriesConfig(this, true));
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

    private Color getScaledColorForPerformance(double performance)
    {
        // convert to 0 = -0.07 -> 1 = +0.07

        final float max = 0.07f;

        float p = (float) performance;
        p = Math.max(-max, p);
        p = Math.min(max, p);
        p = (p + max) * (1 / (2 * max));

        float hue = p * 120f; // 0 = red, 60 = yellow, 120 = red
        return resources.getResourceManager().createColor(new RGB(hue, 0.9f, 1f));
    }

    private void fillTable()
    {
        // fill the table lines according to the supplied period
        // calculate the performance with a temporary reporting period
        // calculate the color interpolated between red and green with yellow as
        // the median
        Interval interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval();

        addHeaderRow();

        DataSeries dataSeries = get(DataSeriesConfig.class).getDataSeries();

        for (Integer year : interval.iterYears())
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
                if (interval.contains(month))
                {
                    cell = createCell(dataSeries, month);
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

    private Cell createCell(DataSeries dataSeries, LocalDate month)
    {
        ReportingPeriod period = new ReportingPeriod.FromXtoY(month.minusDays(1),
                        month.withDayOfMonth(month.lengthOfMonth()));
        PerformanceIndex performance = getDashboardData().calculate(dataSeries, period);

        return new Cell(table, new CellDataProvider()
        {
            @Override
            public Color getBackground()
            {
                return getScaledColorForPerformance(performance.getFinalAccumulatedPercentage());
            }

            @Override
            public Font getFont()
            {
                return resources.getSmallFont();
            }

            @Override
            public String getText()
            {
                return Values.PercentShort.format(performance.getFinalAccumulatedPercentage());
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
