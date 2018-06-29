package name.abuchen.portfolio.ui.util;

import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class QuotesTableViewer
{
    private TableViewer tableViewer;

    public QuotesTableViewer(Composite container)
    {
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        tableViewer = new TableViewer(container, SWT.BORDER);
        Table table = tableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        TableColumn column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnDate);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnDaysHigh);
        column.setAlignment(SWT.RIGHT);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnDaysLow);
        column.setAlignment(SWT.RIGHT);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnQuote);
        column.setAlignment(SWT.RIGHT);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnVolume);
        column.setAlignment(SWT.RIGHT);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        tableViewer.setLabelProvider(new PriceLabelProvider());
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
    }

    public void setInput(List<LatestSecurityPrice> quotes)
    {
        tableViewer.setInput(quotes);
    }

    public void setMessage(String message)
    {
        tableViewer.setInput(new String[] { message });
    }

    public void refresh()
    {
        tableViewer.refresh();
    }

    public void refresh(boolean updateLabels)
    {
        tableViewer.refresh(updateLabels);
    }

    public Table getTable()
    {
        return tableViewer.getTable();
    }

    public Widget getControl()
    {
        return tableViewer.getControl();
    }

    static class PriceLabelProvider extends LabelProvider implements ITableLabelProvider
    {

        @Override
        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex)
        {
            if (element instanceof String)
            {
                return columnIndex == 0 ? element.toString() : null;
            }
            else
            {
                LatestSecurityPrice p = (LatestSecurityPrice) element;
                switch (columnIndex)
                {
                    case 0:
                        return Values.Date.format(p.getDate());
                    case 1:
                        return p.getHigh() == LatestSecurityPrice.NOT_AVAILABLE ? null : Values.Quote.format(p.getHigh());
                    case 2:
                        return p.getLow() == LatestSecurityPrice.NOT_AVAILABLE ? null : Values.Quote.format(p.getLow());
                    case 3:
                        return Values.Quote.format(p.getValue());
                    case 4:
                        return String.format("%,d", p.getVolume()); //$NON-NLS-1$
                    default:
                        throw new IllegalArgumentException(String.valueOf(columnIndex));
                }
            }
        }

    }
}
