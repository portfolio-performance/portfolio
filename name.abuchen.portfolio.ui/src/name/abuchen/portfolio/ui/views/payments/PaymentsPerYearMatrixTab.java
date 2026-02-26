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

public class PaymentsPerYearMatrixTab extends PaymentsMatrixTab
{
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsByYearAndVehicle;
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

        var date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);
        for (var index = 0; index < model.getNoOfMonths(); index += 12)
        {
            createYearColumn(records, layout, date, index);
            date = date.plusYears(1);
        }

        if (showAverageColumn)
        {
            createAveragePerYearColumn(records, layout);
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

    private void createYearColumn(TableViewer records, TableColumnLayout layout, LocalDate start, int index)
    {
        ToLongFunction<PaymentsViewModel.Line> valueFunction = line -> {
            var value = 0L;
            for (var ii = index; ii < index + 12 && ii < line.getNoOfMonths(); ii++)
                value += line.getValue(ii);
            return value;
        };

        var column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(formatter.format(start));
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

    protected void createAveragePerYearColumn(TableViewer records, TableColumnLayout layout)
    {
        var column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAverage);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var line = (Line) element;
                var average = PaymentsAverageCalculator.calculateAveragePerYear(line.getSum(), line.getNoOfMonths());
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
            var avg1 = PaymentsAverageCalculator.calculateAveragePerYear(l1.getSum(), l1.getNoOfMonths());
            var avg2 = PaymentsAverageCalculator.calculateAveragePerYear(l2.getSum(), l2.getNoOfMonths());
            return Long.compare(avg1, avg2);
        }).attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(200));
    }

}
