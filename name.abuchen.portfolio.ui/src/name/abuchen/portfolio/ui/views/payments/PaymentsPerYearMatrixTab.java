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

public class PaymentsPerYearMatrixTab extends PaymentsPerMonthMatrixTab
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
    }

    @Override
    protected void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createVehicleColumn(records, layout, true);

        LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            createYearColumn(records, layout, date, index);
            date = date.plusYears(1);
        }

        createSumColumn(records, layout);

        sortColumnOrder();
    }
    
    @Override
    protected void sortColumnOrder()
    {
        // Keep first column in same position
        sortColumnOrder(1, 0);
    }

    private void createYearColumn(TableViewer records, TableColumnLayout layout, LocalDate start, int index)
    {
        ToLongFunction<PaymentsViewModel.Line> valueFunction = line -> {
            long value = 0;
            for (int ii = index; ii < index + 12 && ii < line.getNoOfMonths(); ii++)
                value += line.getValue(ii);
            return value;
        };

        TableViewerColumn column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(formatter.format(start));
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
