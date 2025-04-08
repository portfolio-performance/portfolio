package name.abuchen.portfolio.ui.util.chart;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.function.ToIntFunction;

import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;

import name.abuchen.portfolio.util.Dates;
import name.abuchen.portfolio.util.Triple;

/* package */ class TimeGridHelper
{
    /**
     * pixel threshold below which sparse label mode is used. The value is a bit
     * arbitrary, but should be fine.
     */
    private static final int SPARSE_LABEL_MODE_PIXEL_THRESHOLD = 320;

    /**
     * Minimum label width in pixels. Used to suppress last x-axis labels that
     * are too short to be useful.
     */
    private static final int MIN_LABEL_WIDTH = 50;

    private TimeGridHelper()
    {
    }

    /* package */ static void paintTimeGrid(Chart chart, PaintEvent e, LocalDate start, LocalDate end,
                    ToIntFunction<LocalDate> getPixelCoordinate)
    {
        IAxis xAxis = chart.getAxisSet().getXAxis(0);

        int days = Dates.daysBetween(start, end) + 1;

        Triple<Period, DateTimeFormatter, LocalDate> data = getPeriodFormatAndCursor(days, start, e.width);

        Period period = data.getFirst();
        DateTimeFormatter format = data.getSecond();
        LocalDate cursor = data.getThird();

        e.gc.setForeground(chart.getTitle().getForeground());

        int previousLabelExtend = -1;
        int xMax = xAxis.getPixelCoordinate(xAxis.getRange().upper);
        while (cursor.isBefore(end))
        {
            int x = getPixelCoordinate.applyAsInt(cursor);
            e.gc.drawLine(x, 0, x, e.height);

            if (isLabelable(x, xMax, previousLabelExtend))
            {
                String labelText = format.format(cursor);
                int currentLabelX = x + 5;
                e.gc.drawText(labelText, currentLabelX, 5, true);

                int textExtend = e.gc.textExtent(labelText).x;
                // remember the total label extend
                previousLabelExtend = currentLabelX + textExtend + 5;
            }

            cursor = cursor.plus(period);
        }
    }

    private static Triple<Period, DateTimeFormatter, LocalDate> getPeriodFormatAndCursor(long days, LocalDate start,
                    int width)
    {
        LocalDate cursor = start.getDayOfMonth() == 1 ? start : start.plusMonths(1).withDayOfMonth(1);
        Period period;
        DateTimeFormatter format;

        if (width > SPARSE_LABEL_MODE_PIXEL_THRESHOLD)
        {
            if (days < 250)
            {
                period = Period.ofMonths(1);
                format = DateTimeFormatter.ofPattern("MMMM yyyy"); //$NON-NLS-1$
            }
            else if (days < 800)
            {
                period = Period.ofMonths(3);
                format = DateTimeFormatter.ofPattern("QQQ yyyy"); //$NON-NLS-1$
                cursor = cursor.plusMonths((12 - cursor.getMonthValue() + 1) % 3);
            }
            else if (days < 1200)
            {
                period = Period.ofMonths(6);
                format = DateTimeFormatter.ofPattern("QQQ yyyy"); //$NON-NLS-1$
                cursor = cursor.plusMonths((12 - cursor.getMonthValue() + 1) % 6);
            }
            else
            {
                period = Period.ofYears(days > 5000 ? 2 : 1);
                format = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$

                if (cursor.getMonthValue() > 1)
                    cursor = cursor.plusYears(1).withDayOfYear(1);
            }
        }
        else
        {
            // Sparse labeling mode used in low width conditions.
            // Its purpose is to ensure enough space between the labels to avoid
            // overlays and ensure readability.
            // It is mainly relevant to charts embedded into dashboards with
            // narrow columns.
            if (days < 100)
            {
                period = Period.ofMonths(1);
                format = DateTimeFormatter.ofPattern("MMMM yyyy"); //$NON-NLS-1$
            }
            else if (days < 400)
            {
                period = Period.ofMonths(3);
                format = DateTimeFormatter.ofPattern("QQQ yyyy"); //$NON-NLS-1$
                cursor = cursor.plusMonths((12 - cursor.getMonthValue() + 1) % 3);
            }
            else
            {
                period = Period.ofYears(1);
                format = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$

                if (cursor.getMonthValue() > 1)
                    cursor = cursor.plusYears(1).withDayOfYear(1);
            }
        }

        return new Triple<>(period, format, cursor);

    }

    private static boolean isLabelable(int currentX, int xMax, int previousLabelExtend)
    {
        // allow adding a text label to the vertical line, if
        // a) it is the very first line at the beginning of the chart, or
        // b1) the new label fits to the right of the line, not exceeding the
        // x-axis, and
        // b2) there is sufficient space between the label of the previous line
        // and this new line

        if (previousLabelExtend == -1)
            return true;

        return currentX + MIN_LABEL_WIDTH <= xMax && currentX - previousLabelExtend >= 0;
    }
}
