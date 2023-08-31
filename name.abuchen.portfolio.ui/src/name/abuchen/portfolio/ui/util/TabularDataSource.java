package name.abuchen.portfolio.ui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Named;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.TransactionPair;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;
import name.abuchen.portfolio.ui.util.swt.TabularLayout;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.CopyPasteSupport;
import name.abuchen.portfolio.ui.views.AccountContextMenu;
import name.abuchen.portfolio.ui.views.SecurityContextMenu;
import name.abuchen.portfolio.ui.views.TransactionContextMenu;
import name.abuchen.portfolio.util.TextUtil;

public class TabularDataSource implements Named
{
    public static class Builder
    {
        private List<Column> columns = new ArrayList<>();
        private List<Object[]> rows = new ArrayList<>();
        private FooterRow footer;

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

        public void setFooter(Object... cells)
        {
            if (cells.length != columns.size())
                throw new UnsupportedOperationException();

            footer = new FooterRow(cells);
        }
    }

    public static class Column
    {
        private String label;
        private Function<Object, String> formatter;
        private Color backgroundColor;
        private int width = 60;
        private int align = SWT.RIGHT;
        private boolean hasLogo = false;

        public Column(String label)
        {
            this(label, SWT.RIGHT, 60);
        }

        public Column(String label, int align)
        {
            this(label, align, 60);
        }

        public Column(String label, int align, int width)
        {
            this.label = label;
            this.align = align;
            this.width = width;
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

            if (source.footer == null)
            {
                return source.rows.toArray();
            }
            else
            {
                Object[] elements = new Object[source.rows.size() + 1];
                source.rows.toArray(elements);
                elements[elements.length - 1] = source.footer;
                return elements;
            }
        }
    }

    private static final class TabularLabelProvider extends ColumnLabelProvider
    {
        private final Column column;
        private final Client client;
        private final int colIndex;

        private TabularLabelProvider(Client client, Column column, int colIndex)
        {
            this.column = column;
            this.client = client;
            this.colIndex = colIndex;
        }

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
                return column.formatter != null ? column.formatter.apply(row[colIndex]) : String.valueOf(row[colIndex]);
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
    }

    private final class ComparatorImplementation implements Comparator<Object>
    {
        private final Column column;
        private final int colIndex;

        private ComparatorImplementation(Column column, int colIndex)
        {
            this.column = column;
            this.colIndex = colIndex;
        }

        @Override
        public int compare(Object row1, Object row2)
        {
            if (row1 instanceof FooterRow)
            {
                int direction = ColumnViewerSorter.SortingContext.getSortDirection();
                return direction == SWT.UP ? 1 : -1;
            }
            else if (row2 instanceof FooterRow)
            {
                int direction = ColumnViewerSorter.SortingContext.getSortDirection();
                return direction == SWT.UP ? -1 : 1;
            }

            Object cell1 = ((Object[]) row1)[colIndex];
            Object cell2 = ((Object[]) row2)[colIndex];

            if (cell1 instanceof Long l1 && cell2 instanceof Long l2)
                return l1.compareTo(l2);
            else if (cell1 instanceof Money m1 && cell2 instanceof Money m2)
                return m1.compareTo(m2);

            String s1 = cell1 instanceof String || column.formatter == null ? String.valueOf(cell1)
                            : column.formatter.apply(cell1);
            String s2 = cell2 instanceof String || column.formatter == null ? String.valueOf(cell2)
                            : column.formatter.apply(cell2);

            if (s1 == null && s2 == null)
                return 0;
            else if (s1 == null)
                return -1;
            else if (s2 == null)
                return 1;

            return TextUtil.compare(s1, s2);
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

    public void attachToolTipTo(Control control)
    {
        ToolTip toolTip = new ToolTip(control, ToolTip.NO_RECREATE, false)
        {
            @Override
            protected Composite createToolTipContentArea(Event event, Composite parent)
            {
                return createPlainComposite(parent);
            }
        };

        toolTip.setPopupDelay(0);
        toolTip.activate();
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
        container.setLayout(new TabularLayout(data.columns.size(), 1, data.footer != null ? 1 : 0));

        for (Column column : data.columns)
        {
            ColoredLabel l = new ColoredLabel(container, column.align);
            l.setText(TextUtil.tooltip(column.label));
            if (column.backgroundColor != null)
                l.setBackdropColor(column.backgroundColor);
        }

        for (Object[] row : data.rows)
        {
            for (int ii = 0; ii < row.length; ii++)
            {
                Column column = data.columns.get(ii);
                Label l = new Label(container, column.align);

                String text = column.formatter != null ? column.formatter.apply(row[ii]) : String.valueOf(row[ii]);
                if (text == null)
                    text = ""; //$NON-NLS-1$

                l.setText(TextUtil.tooltip(text));
            }
        }

        if (data.footer != null)
        {
            for (int ii = 0; ii < data.footer.row.length; ii++)
            {
                Column column = data.columns.get(ii);
                ColoredLabel l = new ColoredLabel(container, column.align);
                if (column.backgroundColor != null)
                    l.setBackdropColor(column.backgroundColor);
                l.setText(TextUtil.tooltip(column.formatter != null ? column.formatter.apply(data.footer.row[ii])
                                : String.valueOf(data.footer.row[ii])));
            }
        }

        return container;
    }

    public Composite createTableViewer(Client client, AbstractFinanceView owner, Composite parent)
    {
        if (data == null)
        {
            this.data = new Builder();
            this.builder.accept(data);
        }

        int[] previousWidths = restoreWidthsFromPreferences(owner, data);

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

            TableViewerColumn tableColumn = new TableViewerColumn(tableViewer, column.align);
            tableColumn.getColumn().setText(column.label);
            tableColumn.setLabelProvider(new TabularLabelProvider(client, column, index));

            if (index < previousWidths.length && previousWidths[index] > 0)
                tableLayout.setColumnData(tableColumn.getColumn(), new ColumnPixelData(previousWidths[index]));
            else
                tableLayout.setColumnData(tableColumn.getColumn(), new ColumnPixelData(column.width));

            ColumnViewerSorter.create(new ComparatorImplementation(column, index)).attachTo(tableViewer, tableColumn);
        }

        tableViewer.setContentProvider(new TabularDataProvider());
        tableViewer.setInput(data);

        hookContextMenu(owner, tableViewer);

        // make sure widths are stored against exactly the source that was used
        // to create the table -> cannot use instance variable
        final Builder source = data;
        tableViewer.getTable().addDisposeListener(e -> storeWidthsToPreferences(owner, tableViewer, source));

        return container;
    }

    private void storeWidthsToPreferences(AbstractFinanceView owner, TableViewer tableViewer, Builder source)
    {
        StringBuilder prefValue = new StringBuilder();
        for (int index = 0; index < tableViewer.getTable().getColumnCount(); index++)
        {
            if (index > 0)
                prefValue.append(',');
            prefValue.append(tableViewer.getTable().getColumn(index).getWidth());
        }

        String key = TabularDataSource.class.getSimpleName()
                        + source.columns.stream().map(c -> c.label).collect(Collectors.joining());

        owner.getPreferenceStore().putValue(key, prefValue.toString());
    }

    private static int[] restoreWidthsFromPreferences(AbstractFinanceView owner, Builder builder)
    {
        try
        {
            var preferences = owner.getPreferenceStore();

            String key = TabularDataSource.class.getSimpleName()
                            + builder.columns.stream().map(c -> c.label).collect(Collectors.joining());

            String value = preferences.getString(key);
            if (value == null)
                return new int[0];

            String[] values = value.split(","); //$NON-NLS-1$
            if (values.length != builder.columns.size())
                return new int[0];

            int[] widths = new int[values.length];
            for (int ii = 0; ii < values.length; ii++)
            {
                widths[ii] = Integer.parseInt(values[ii]);
            }

            return widths;
        }
        catch (NumberFormatException e)
        {
            return new int[0];
        }
    }

    private void hookContextMenu(AbstractFinanceView owner, TableViewer tableViewer)
    {
        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(menuManager -> {
            IStructuredSelection selection = tableViewer.getStructuredSelection();
            if (!selection.isEmpty() && selection.getFirstElement() instanceof Object[] row)
            {
                if (row[0] instanceof Security security)
                    new SecurityContextMenu(owner).menuAboutToShow(menuManager, security);
                else if (row[0] instanceof Account account)
                    new AccountContextMenu(owner).menuAboutToShow(menuManager, account, null);
                else if (row[0] instanceof TransactionPair<?>)
                    new TransactionContextMenu(owner).menuAboutToShow(menuManager, true,
                                    new StructuredSelection(row[0]));
            }

        });

        Menu contextMenu = menuMgr.createContextMenu(tableViewer.getTable().getShell());
        tableViewer.getTable().setMenu(contextMenu);
        tableViewer.getTable().setData(ContextMenu.DEFAULT_MENU, contextMenu);

        tableViewer.getTable().addDisposeListener(e -> contextMenu.dispose());
    }
}
