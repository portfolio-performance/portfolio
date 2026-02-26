package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.function.ToLongFunction;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerQuarterMatrixTab extends PaymentsMatrixTab
{
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$
    /*
     * The number of month in a quarter. While most people will know this, I
     * prefer named variables over the occurrence of magic numbers in the code.
     */
    private static final int MONTHS_IN_QUARTER = 3;

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsByQuarterAndVehicle;
    }

    @Override
    public void addConfigActions(IMenuManager manager)
    {
        addReverseColumnAction(manager);
        addAverageColumnAction(manager);
        addSumColumnAction(manager);
    }

    @Override
    protected void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createVehicleColumn(records, layout, true);

        createQuarterColumns(records, layout);

        if (showAverageColumn)
        {
            createAveragePerQuarterColumn(records, layout);
        }

        createSumColumn(records, layout, false);

        updateColumnOrder();
    }

    @Override
    protected void updateColumnOrder()
    {
        // Keep first column in same position
        setColumnOrder(1, 0);
    }

    private void createQuarterColumns(TableViewer records, TableColumnLayout layout)
    {
        var date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);

        var nMonths = model.getNoOfMonths();
        var nQuarters = getNoOfQuarters();

        var quarterBeginIndex = 0;
        var quarterEndIndex = Math.min(MONTHS_IN_QUARTER, nMonths);

        for (var quarter = 0; quarter < nQuarters; quarter++)
        {
            // the fifth total quarter is the first quarter in the corresponding
            // year
            var quarterWithinYear = (quarter % 4) + 1;

            // The caption looks like "Q<quarter within the year> <year>"
            var columnCaption = String.format("Q%d %s", quarterWithinYear, formatter.format(date)); //$NON-NLS-1$

            createQuarterColumn(records, layout, quarterBeginIndex, quarterEndIndex, columnCaption);

            // Starting from here, we make sure to step into the next quarter
            quarterBeginIndex = Math.min(quarterBeginIndex + MONTHS_IN_QUARTER, nMonths);
            quarterEndIndex = Math.min(quarterEndIndex + MONTHS_IN_QUARTER, nMonths);

            // every four quarters we need to switch to the next year
            if (quarterWithinYear == 4)
            {
                date = date.plusYears(1);
            }
        }

    }

    /**
     * @brief Creates a column collecting quarter-wise dividends. The quarter is
     *        specified by the start and end position within the values array of
     *        the Line within the DividendsViewModel.
     * @param quarterBeginIndex
     *            The start index of the quarter
     * @param quarterEndIndex
     *            The end index of the quarter
     * @param columnCaption
     *            The caption used for the column
     */
    private void createQuarterColumn(TableViewer records, TableColumnLayout layout, int quarterBeginIndex,
                    int quarterEndIndex, String columnCaption)
    {
        ToLongFunction<PaymentsViewModel.Line> valueFunction = line -> {
            var value = 0L;
            for (var i = quarterBeginIndex; i < quarterEndIndex; i++)
                value += line.getValue(i);
            return value;
        };

        var column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(columnCaption);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var line = (PaymentsViewModel.Line) element;
                var value = valueFunction.applyAsLong(line);
                return line.getVehicle() != null ? Values.Amount.formatNonZero(value) : Values.Amount.format(value);
            }

            @Override
            public String getToolTipText(Object element)
            {
                var vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return TextUtil.tooltip(vehicle != null ? vehicle.getName() : null);
            }

            @Override
            public Font getFont(Object element)
            {
                var vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return vehicle != null || ((PaymentsViewModel.Line) element).getConsolidatedRetired() ? null : boldFont;
            }
        });

        createSorter((l1, l2) -> Long.compare(valueFunction.applyAsLong(l1), valueFunction.applyAsLong(l2)))
                        .attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

    private int getNoOfQuarters()
    {
        var nMonths = model.getNoOfMonths();

        // How many quarters we are about to display. We show every started
        // quarter, hence the Math.ceil
        return (int) Math.ceil((double) nMonths / (double) MONTHS_IN_QUARTER);
    }

    protected void createAveragePerQuarterColumn(TableViewer records, TableColumnLayout layout)
    {
        var column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAverage);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var line = (Line) element;
                var average = PaymentsAverageCalculator.calculateAveragePerQuarter(line.getSum(), line.getNoOfMonths());
                return Values.Amount.formatNonZero(average);
            }

            @Override
            public Font getFont(Object element)
            {
                var line = (Line) element;
                return line.getConsolidatedRetired() ? null : boldFont;
            }
        });

        createSorter((l1, l2) -> {
            var avg1 = PaymentsAverageCalculator.calculateAveragePerQuarter(l1.getSum(), l1.getNoOfMonths());
            var avg2 = PaymentsAverageCalculator.calculateAveragePerQuarter(l2.getSum(), l2.getNoOfMonths());
            return Long.compare(avg1, avg2);
        }).attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(200));
    }

}
