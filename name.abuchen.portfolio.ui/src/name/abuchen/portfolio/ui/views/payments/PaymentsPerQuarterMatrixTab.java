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

import name.abuchen.portfolio.model.InvestmentVehicle;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerQuarterMatrixTab extends PaymentsMatrixTab
{
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsByQuarterAndVehicle;
    }

    @Override
    public void addConfigActions(IMenuManager manager)
    {
        addReverseColumnAction(manager);
        addSumColumnAction(manager);
    }

    @Override
    protected void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createVehicleColumn(records, layout, true);

        createQuarterColumns(records, layout);

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
        LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);

        int nMonths = model.getNoOfMonths();

        /*
         * The number of month in a quarter. While most people will know this, I
         * prefer named variables over the occurrence of magic numbers in the
         * code.
         */
        int monthInQuarter = 3;

        // How many quarters we are about to display. We show every started
        // quarter, hence the Math.ceil
        int nQuarters = (int) Math.ceil((double) nMonths / (double) monthInQuarter);

        int quarterBeginIndex = 0;
        int quarterEndIndex = Math.min(monthInQuarter, nMonths);

        for (int quarter = 0; quarter < nQuarters; quarter++)
        {
            // the fifth total quarter is the first quarter in the corresponding
            // year
            int quarterWithinYear = (quarter % 4) + 1;

            // The caption looks like "Q<quarter within the year> <year>"
            String columnCaption = String.format("Q%d %s", quarterWithinYear, formatter.format(date)); //$NON-NLS-1$

            createQuarterColumn(records, layout, quarterBeginIndex, quarterEndIndex, columnCaption);

            // Starting from here, we make sure to step into the next quarter
            quarterBeginIndex = Math.min(quarterBeginIndex + monthInQuarter, nMonths);
            quarterEndIndex = Math.min(quarterEndIndex + monthInQuarter, nMonths);

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
            long value = 0;
            for (int i = quarterBeginIndex; i < quarterEndIndex; i++)
                value += line.getValue(i);
            return value;
        };

        TableViewerColumn column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(columnCaption);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Line line = (PaymentsViewModel.Line) element;
                long value = valueFunction.applyAsLong(line);
                return line.getVehicle() != null ? Values.Amount.formatNonZero(value) : Values.Amount.format(value);
            }

            @Override
            public String getToolTipText(Object element)
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return TextUtil.tooltip(vehicle != null ? vehicle.getName() : null);
            }

            @Override
            public Font getFont(Object element)
            {
                InvestmentVehicle vehicle = ((PaymentsViewModel.Line) element).getVehicle();
                return vehicle != null || ((PaymentsViewModel.Line) element).getConsolidatedRetired() ? null : boldFont;
            }
        });

        createSorter((l1, l2) -> Long.compare(valueFunction.applyAsLong(l1), valueFunction.applyAsLong(l2)))
                        .attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

}
