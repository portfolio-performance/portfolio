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

import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;

public class EventsTableViewer
{
    private TableViewer tableViewer;

    public EventsTableViewer(Composite container)
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
        column.setText(Messages.ColumnEventType);
        column.setAlignment(SWT.LEFT);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        column = new TableColumn(tableViewer.getTable(), SWT.None);
        column.setText(Messages.ColumnEventDetails);
        column.setAlignment(SWT.RIGHT);
        layout.setColumnData(column, new ColumnPixelData(80, true));

        tableViewer.setLabelProvider(new EventLabelProvider());
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
    }

    public void setInput(List<SecurityEvent> events)
    {
        tableViewer.setInput(events);
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

    static class EventLabelProvider extends LabelProvider implements ITableLabelProvider
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
                SecurityEvent e = (SecurityEvent) element;
                switch (columnIndex)
                {
                    case 0:
                        return Values.Date.format(e.getDate());
                    case 1:
                        return e.getType() == SecurityEvent.Type.NONE ? null : e.getType().toString();
                    case 2:
                        return e.getType() == SecurityEvent.Type.NONE ? null : e.getExplaination();
                    default:
                        throw new IllegalArgumentException(String.valueOf(columnIndex));
                }
            }
        }

    }
}
