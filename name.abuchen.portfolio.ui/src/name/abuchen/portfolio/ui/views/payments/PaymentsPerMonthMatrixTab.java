package name.abuchen.portfolio.ui.views.payments;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import jakarta.annotation.PostConstruct;

import org.eclipse.jface.action.Action;
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
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.payments.PaymentsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class PaymentsPerMonthMatrixTab extends PaymentsMatrixTab
{
    // Keys in PreferenceStore
    private static final String KEY_SHOW_ONE_YEAR = PaymentsPerMonthMatrixTab.class.getSimpleName()
                    + "-showOnlyOneYear"; //$NON-NLS-1$

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yy"); //$NON-NLS-1$

    private boolean showOnlyOneYear = false;

    @PostConstruct
    private void setup()
    {
        showOnlyOneYear = preferences.getBoolean(KEY_SHOW_ONE_YEAR);
    }

    @Override
    public String getLabel()
    {
        return Messages.LabelPaymentsByMonthAndVehicle;
    }

    @Override
    public void addConfigActions(IMenuManager manager)
    {
        Action action = new SimpleAction(Messages.LabelShowOnlyOneYear, a -> {
            showOnlyOneYear = !showOnlyOneYear;
            updateColumns(tableViewer, tableLayout);
            preferences.setValue(KEY_SHOW_ONE_YEAR, showOnlyOneYear);
        });
        action.setChecked(showOnlyOneYear);
        manager.add(action);

        addReverseColumnAction(manager);
        addAverageColumnAction(manager);
        addSumColumnAction(manager);
    }

    @Override
    protected void updateColumnOrder()
    {
        // Keep first and last column in same position
        setColumnOrder(1, 1);
    }

    @Override
    protected void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createVehicleColumn(records, layout, true);

        // create monthly columns
        var date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);

        var noOfMonths = showOnlyOneYear ? Math.min(12, model.getNoOfMonths()) : model.getNoOfMonths();

        for (var index = 0; index < noOfMonths; index++)
        {
            createMonthColumn(records, layout, date, index);
            date = date.plusMonths(1);
        }

        if (showAverageColumn)
        {
            createAveragePerMonthColumn(records, layout, showOnlyOneYear);
        }

        createSumColumn(records, layout, showOnlyOneYear);

        // add security name at the end of the matrix table again because the
        // first column is most likely not visible anymore
        createVehicleColumn(records, layout, false);
    }

    private void createMonthColumn(TableViewer records, TableColumnLayout layout, LocalDate start, int index)
    {
        var column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(formatter.format(start));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var line = (PaymentsViewModel.Line) element;
                return line.getVehicle() != null ? Values.Amount.formatNonZero(line.getValue(index))
                                : Values.Amount.format(line.getValue(index));
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

        createSorter((l1, l2) -> Long.compare(l1.getValue(index), l2.getValue(index))).attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

    protected void createAveragePerMonthColumn(TableViewer records, TableColumnLayout layout, boolean showOnlyFirstYear)
    {
        var column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(Messages.ColumnAverage);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                var line = (Line) element;

                if (showOnlyFirstYear)
                {
                    var noOfMonths = Math.min(12, line.getNoOfMonths());
                    var sum = 0L;
                    for (var ii = 0; ii < noOfMonths; ii++)
                        sum += line.getValue(ii);

                    var average = PaymentsAverageCalculator.calculateAveragePerMonth(sum, noOfMonths);
                    return Values.Amount.formatNonZero(average);
                }
                else
                {
                    var average = PaymentsAverageCalculator.calculateAveragePerMonth(line.getSum(),
                                    line.getNoOfMonths());
                    return Values.Amount.formatNonZero(average);
                }
            }

            @Override
            public Font getFont(Object element)
            {
                var line = (Line) element;
                return line.getConsolidatedRetired() ? null : boldFont;
            }
        });

        createSorter((l1, l2) -> {
            long avg1, avg2;

            if (showOnlyFirstYear)
            {
                var m1 = Math.min(12, l1.getNoOfMonths());
                var m2 = Math.min(12, l2.getNoOfMonths());

                var sum1 = 0L;
                for (var ii = 0; ii < m1; ii++)
                    sum1 += l1.getValue(ii);
                avg1 = PaymentsAverageCalculator.calculateAveragePerMonth(sum1, m1);

                var sum2 = 0L;
                for (var ii = 0; ii < m2; ii++)
                    sum2 += l2.getValue(ii);
                avg2 = PaymentsAverageCalculator.calculateAveragePerMonth(sum2, m2);
            }
            else
            {
                avg1 = PaymentsAverageCalculator.calculateAveragePerMonth(l1.getSum(), l1.getNoOfMonths());
                avg2 = PaymentsAverageCalculator.calculateAveragePerMonth(l2.getSum(), l2.getNoOfMonths());
            }

            return Long.compare(avg1, avg2);
        }).attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(200));
    }

}
