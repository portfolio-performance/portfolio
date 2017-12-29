package name.abuchen.portfolio.ui.views.dividends;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

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
import name.abuchen.portfolio.ui.views.dividends.DividendsViewModel.Line;
import name.abuchen.portfolio.util.TextUtil;

public class DividendsYearMatrixTab extends DividendsMatrixTab
{
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy"); //$NON-NLS-1$

    @Override
    public String getLabel()
    {
        return Messages.LabelDividendsByYearAndVehicle;
    }
    
    @Override
    public void addConfigActions(IMenuManager manager)
    {
        // do not add config option from divident / month tab
    }

    protected void createColumns(TableViewer records, TableColumnLayout layout)
    {
        createSecurityColumn(records, layout, true);

        LocalDate date = LocalDate.of(model.getStartYear(), Month.JANUARY, 1);
        for (int index = 0; index < model.getNoOfMonths(); index += 12)
        {
            createYearColumn(records, layout, date, index);
            date = date.plusYears(1);
        }

        createSumColumn(records, layout);
    }

    private void createYearColumn(TableViewer records, TableColumnLayout layout, LocalDate start, int index)
    {
        TableViewerColumn column = new TableViewerColumn(records, SWT.RIGHT);
        column.getColumn().setText(formatter.format(start));
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                Line line = (DividendsViewModel.Line) element;

                long value = 0;
                for (int ii = index; ii < index + 12 && ii < line.getNoOfMonths(); ii++)
                    value += line.getValue(ii);

                return line.getVehicle() != null ? Values.Amount.formatNonZero(value) : Values.Amount.format(value);
            }

            @Override
            public String getToolTipText(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return TextUtil.tooltip(vehicle != null ? vehicle.getName() : null);
            }

            @Override
            public Font getFont(Object element)
            {
                InvestmentVehicle vehicle = ((DividendsViewModel.Line) element).getVehicle();
                return vehicle != null ? null : boldFont;
            }
        });
        layout.setColumnData(column.getColumn(), new ColumnPixelData(50));
    }

}
