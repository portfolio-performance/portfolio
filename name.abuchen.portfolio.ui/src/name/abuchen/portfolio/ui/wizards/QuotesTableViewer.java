package name.abuchen.portfolio.ui.wizards;

import java.util.List;

import name.abuchen.portfolio.model.LatestSecurityPrice;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.views.SimpleListContentProvider;

import org.eclipse.jface.layout.TableColumnLayout;
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
        tableViewer.setContentProvider(new SimpleListContentProvider());
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

        public Image getColumnImage(Object element, int columnIndex)
        {
            return null;
        }

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
                        return String.format("%tF", p.getTime()); //$NON-NLS-1$
                    case 1:
                        return String.format("%,10.2f", p.getHigh() / 100d); //$NON-NLS-1$
                    case 2:
                        return String.format("%,10.2f", p.getLow() / 100d); //$NON-NLS-1$
                    case 3:
                        return String.format("%,10.2f", p.getValue() / 100d); //$NON-NLS-1$
                    case 4:
                        return String.format("%,d", p.getVolume()); //$NON-NLS-1$
                }
            }
            return null;
        }

    }
}
