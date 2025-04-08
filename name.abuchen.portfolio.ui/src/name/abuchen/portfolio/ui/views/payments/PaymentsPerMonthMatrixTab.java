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

import name.abuchen.portfolio.model.InvestmentVehicle;
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
        LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);

        int noOfMonths = showOnlyOneYear ? Math.min(12, model.getNoOfMonths()) : model.getNoOfMonths();

        for (int index = 0; index < noOfMonths; index++)
        {
            createMonthColumn(records, layout, date, index);
            date = date.plusMonths(1);
        }

        createSumColumn(records, layout, showOnlyOneYear);

        // add security name at the end of the matrix table again because the
        // first column is most likely not visible anymore
        createVehicleColumn(records, layout, false);
    }

    private void createMonthColumn(TableViewer records, TableColumnLayout layout, LocalDate start, int index)
    {
        TableViewerColumn column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(formatter.format(start));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Line line = (PaymentsViewModel.Line) element;
                return line.getVehicle() != null ? Values.Amount.formatNonZero(line.getValue(index))
                                : Values.Amount.format(line.getValue(index));
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

        createSorter((l1, l2) -> Long.compare(l1.getValue(index), l2.getValue(index))).attachTo(records, column);

        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

}
