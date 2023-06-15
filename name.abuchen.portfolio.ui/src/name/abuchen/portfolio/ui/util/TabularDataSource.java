package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.util.swt.TabularLayout;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;

public class TabularDataSource implements Named
{
    public static class Builder
    {
        private List<Column> columns = new ArrayList<>();
        private List<Object[]> rows = new ArrayList<>();
        private List<Object[]> footer = new ArrayList<>();

        public void addColumns(Column... columns)
        {
            this.columns.addAll(Arrays.asList(columns));
        }

        public void addRow(Object... cells)
        {
            if (cells.length != columns.size())
                throw new UnsupportedOperationException();

            rows.add(cells);
        }

        public void addFooter(Object... cells)
        {
            if (cells.length != columns.size())
                throw new UnsupportedOperationException();

            footer.add(cells);
        }
    }

    public static class Column
    {
        private String label;
        private Function<Object, String> formatter;
        private Color backgroundColor;
        private int align = SWT.RIGHT;
        private boolean hasLogo = false;

        public Column(String label)
        {
            this.label = label;
        }

        public Column(String label, int align)
        {
            this.label = label;
            this.align = align;
        }

        public Column withFormatter(Function<Object, String> formatter)
        {
            this.formatter = formatter;
            return this;
        }

        public Column withBackgroundColor(Color color)
        {
            this.backgroundColor = color;
            return this;
        }

        public Column withLogo()
        {
            this.hasLogo = true;
            return this;
        }
    }

    private static final class TabularDataProvider implements IStructuredContentProvider
    {
        @Override
        public Object[] getElements(Object inputElement)
        {
            Builder source = (Builder) inputElement;

            Object[] elements = new Object[source.rows.size() + source.footer.size()];
            source.rows.toArray(elements);

            for (int ii = source.rows.size(); ii < elements.length; ii++)
                elements[ii] = new FooterRow(source.footer.get(ii - source.rows.size()));

            return elements;
        }
    }

    private static final class FooterRow
    {
        public FooterRow(Object[] row)
        {
            this.row = row;
        }

        private Object[] row;
    }

    private String label;
    private Consumer<Builder> builder;
    private Builder data;

    public TabularDataSource(String label, Consumer<Builder> builder)
    {
        this.label = label;
        this.builder = builder;
    }

    public void invalidate()
    {
        this.data = null;
    }

    @Override
    public String getNote()
    {
        return null;
    }

    @Override
    public void setNote(String note)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return label;
    }

    @Override
    public void setName(String name)
    {
        this.label = name;
    }

    public Composite createPlainComposite(Composite parent)
    {
        if (data == null)
        {
            this.data = new Builder();
            this.builder.accept(data);
        }

        final Composite container = new Composite(parent, SWT.NONE);
        container.setBackgroundMode(SWT.INHERIT_FORCE);
        container.setLayout(new TabularLayout(data.columns.size(), 1, data.footer.size()));

        for (Column column : data.columns)
        {
            ColoredLabel l = new ColoredLabel(container, column.align);
            l.setText(column.label);
            if (column.backgroundColor != null)
                l.setBackdropColor(column.backgroundColor);
        }

        for (Object[] row : data.rows)
        {
            for (int ii = 0; ii < row.length; ii++)
            {
                Column column = data.columns.get(ii);
                Label l = new Label(container, column.align);
                l.setText(column.formatter != null ? column.formatter.apply(row[ii]) : String.valueOf(row[ii]));
            }
        }

        for (Object[] row : data.footer)
        {
            for (int ii = 0; ii < row.length; ii++)
            {
                Column column = data.columns.get(ii);
                ColoredLabel l = new ColoredLabel(container, column.align);
                if (column.backgroundColor != null)
                    l.setBackdropColor(column.backgroundColor);
                l.setText(column.formatter != null ? column.formatter.apply(row[ii]) : String.valueOf(row[ii]));
            }
        }

        return container;
    }

    public Composite createTableViewer(Client client, Composite parent)
    {
        if (data == null)
        {
            this.data = new Builder();
            this.builder.accept(data);
        }

        Composite container = new Composite(parent, SWT.NONE);

        TableColumnLayout tableLayout = new TableColumnLayout();
        container.setLayout(tableLayout);

        TableViewer tableViewer = new TableViewer(container, SWT.FULL_SELECTION);
        ColumnViewerToolTipSupport.enableFor(tableViewer, ToolTip.NO_RECREATE);
        CopyPasteSupport.enableFor(tableViewer);
        tableViewer.getTable().setHeaderVisible(true);
        tableViewer.getTable().setLinesVisible(true);

        for (int index = 0; index < data.columns.size(); index++)
        {
            Column column = data.columns.get(index);
            final int colIndex = index;

            TableViewerColumn col = new TableViewerColumn(tableViewer, column.align);
            col.getColumn().setText(column.label);
            col.setLabelProvider(new ColumnLabelProvider()
            {
                @Override
                public String getText(Object element)
                {
                    if (element instanceof FooterRow footerRow)
                    {
                        Object value = footerRow.row[colIndex];
                        if (value instanceof String s)
                            return s;
                        if (column.formatter != null)
                            return column.formatter.apply(value);
                        return String.valueOf(value);
                    }
                    else
                    {
                        Object[] row = (Object[]) element;
                        return column.formatter != null ? column.formatter.apply(row[colIndex])
                                        : String.valueOf(row[colIndex]);
                    }
                }

                @Override
                public Image getImage(Object element)
                {
                    if (column.hasLogo && element instanceof Object[] row)
                        return LogoManager.instance().getDefaultColumnImage(row[colIndex], client.getSettings());

                    return null;
                }

                @Override
                public Color getForeground(Object element)
                {
                    return element instanceof FooterRow && column.backgroundColor != null
                                    ? Colors.getTextColor(column.backgroundColor)
                                    : null;
                }

                @Override
                public Color getBackground(Object element)
                {
                    return element instanceof FooterRow ? column.backgroundColor : null;
                }

            });
            tableLayout.setColumnData(col.getColumn(), new ColumnPixelData(index == 0 ? 220 : 60));
        }

        tableViewer.setContentProvider(new TabularDataProvider());
        tableViewer.setInput(data);

        return container;
    }
}
