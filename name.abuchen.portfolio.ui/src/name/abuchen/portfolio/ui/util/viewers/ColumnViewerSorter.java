package name.abuchen.portfolio.ui.util.viewers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

import name.abuchen.portfolio.model.Adaptor;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Quote;
import name.abuchen.portfolio.ui.PortfolioPlugin;

public final class ColumnViewerSorter
{
    /**
     * The SortingContext provides comparators access to the original sort
     * direction and the currently selected column option. The sort direction is
     * typically used to keep an element at a stable position regardless of the
     * sort direction (for example a summary line shows up always at the end).
     */
    public static final class SortingContext
    {
        private static final String OPTION = "option"; //$NON-NLS-1$
        private static final String DIRECTION = "direction"; //$NON-NLS-1$

        private static final ThreadLocal<Map<String, Object>> MAP = ThreadLocal.withInitial(HashMap::new);

        private SortingContext()
        {
        }

        /* protected */ static void setSortDirection(int direction)
        {
            MAP.get().put(DIRECTION, direction);
        }

        /**
         * Returns the original sort direction.
         */
        public static int getSortDirection()
        {
            Object direction = MAP.get().get(DIRECTION);
            return direction == null ? SWT.DOWN : (int) direction;
        }

        /* protected */ static void setOption(Object option)
        {
            MAP.get().put(OPTION, option);
        }

        /**
         * Returns the currently selected column option.
         */
        public static Object getColumnOption()
        {
            return MAP.get().get(OPTION);
        }

        /* protected */ static void clear()
        {
            MAP.get().clear();
        }
    }

    private static final class ChainedComparator implements Comparator<Object>
    {
        private final List<Comparator<Object>> comparators;

        private ChainedComparator(List<Comparator<Object>> comparators)
        {
            this.comparators = comparators;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            for (Comparator<Object> c : comparators)
            {
                int result = c.compare(o1, o2);
                if (result != 0)
                    return result;
            }

            return 0;
        }
    }

    private static final class BeanComparator implements Comparator<Object>
    {
        private final Class<?> clazz;
        private final Method method;
        private final int type;

        private BeanComparator(Class<?> clazz, String attribute)
        {
            this.clazz = clazz;
            this.method = determineReadMethod(clazz, attribute);

            Class<?> returnType = method.getReturnType();

            if (returnType.equals(Object.class))
                type = 0;
            else if (returnType.isAssignableFrom(String.class))
                type = 1;
            else if (returnType.isAssignableFrom(Enum.class))
                type = 2;
            else if (returnType.isAssignableFrom(Integer.class) || returnType.isAssignableFrom(int.class))
                type = 3;
            else if (returnType.isAssignableFrom(Double.class) || returnType.isAssignableFrom(double.class))
                type = 4;
            else if (returnType.isAssignableFrom(Date.class))
                type = 5;
            else if (returnType.isAssignableFrom(Long.class) || returnType.isAssignableFrom(long.class))
                type = 6;
            else if (returnType.isAssignableFrom(Boolean.class) || returnType.isAssignableFrom(boolean.class))
                type = 7;
            else if (returnType.isAssignableFrom(Money.class))
                type = 8;
            else if (returnType.isAssignableFrom(Quote.class))
                type = 9;
            else
                type = 0;
        }

        private Method determineReadMethod(Class<?> clazz, String attribute)
        {
            String camelCaseAttribute = Character.toUpperCase(attribute.charAt(0)) + attribute.substring(1);
            try
            {
                return clazz.getMethod("get" + camelCaseAttribute); //$NON-NLS-1$
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    return clazz.getMethod("is" + camelCaseAttribute); //$NON-NLS-1$
                }
                catch (NoSuchMethodException e1)
                {
                    PortfolioPlugin.log(Arrays.asList(e, e1));
                    throw new IllegalArgumentException();
                }
            }
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            Object object1 = Adaptor.adapt(clazz, o1);
            Object object2 = Adaptor.adapt(clazz, o2);

            if (object1 == null && object2 == null)
                return 0;
            else if (object1 == null)
                return -1;
            else if (object2 == null)
                return 1;

            try
            {
                Object attribute1 = method.invoke(object1);
                Object attribute2 = method.invoke(object2);

                if (attribute1 == null && attribute2 == null)
                    return 0;
                else if (attribute1 == null)
                    return -1;
                else if (attribute2 == null)
                    return 1;

                switch (type)
                {
                    case 1:
                        return ((String) attribute1).compareToIgnoreCase((String) attribute2);
                    case 2:
                        return ((Enum<?>) attribute2).name().compareTo(((Enum<?>) attribute2).name());
                    case 3:
                        return ((Integer) attribute1).compareTo((Integer) attribute2);
                    case 4:
                        return ((Double) attribute1).compareTo((Double) attribute2);
                    case 5:
                        return ((Date) attribute1).compareTo((Date) attribute2);
                    case 6:
                        return ((Long) attribute1).compareTo((Long) attribute2);
                    case 7:
                        return ((Boolean) attribute1).compareTo((Boolean) attribute2);
                    case 8:
                        return ((Money) attribute1).compareTo((Money) attribute2);
                    case 9:
                        return ((Quote) attribute1).compareTo((Quote) attribute2);
                    default:
                        return String.valueOf(attribute1).compareToIgnoreCase(String.valueOf(attribute2));
                }
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    private static final class ValueProviderComparator implements Comparator<Object>
    {
        private final Function<Object, Comparable<?>> valueProvider;

        public ValueProviderComparator(Function<Object, Comparable<?>> valueProvider)
        {
            this.valueProvider = valueProvider;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return -1;
            else if (o2 == null)
                return 1;

            @SuppressWarnings("unchecked")
            Comparable<Object> v1 = (Comparable<Object>) valueProvider.apply(o1);
            @SuppressWarnings("unchecked")
            Comparable<Object> v2 = (Comparable<Object>) valueProvider.apply(o2);

            if (v1 == null && v2 == null)
                return 0;
            else if (v1 == null)
                return -1;
            else if (v2 == null)
                return 1;

            return v1.compareTo(v2);
        }
    }

    private static final class ViewerSorter extends ViewerComparator
    {
        private ColumnViewer columnViewer;
        private ViewerColumn viewerColumn;
        private Comparator<Object> comparator;
        private int direction = SWT.DOWN;

        public ViewerSorter(ColumnViewer columnViewer, ViewerColumn viewerColumn, Comparator<Object> comparator)
        {
            this.columnViewer = columnViewer;
            this.viewerColumn = viewerColumn;
            this.comparator = comparator;

            Widget widget;

            if (viewerColumn instanceof TableViewerColumn)
                widget = ((TableViewerColumn) viewerColumn).getColumn();
            else if (viewerColumn instanceof TreeViewerColumn)
                widget = ((TreeViewerColumn) viewerColumn).getColumn();
            else
                throw new UnsupportedOperationException();

            widget.addListener(SWT.Selection, event -> handleSelectionEvent());
        }

        private void handleSelectionEvent()
        {
            // check if current column is already sorted -> switch direction

            boolean columnIsCurrentlySorted;

            if (viewerColumn instanceof TableViewerColumn)
            {
                columnIsCurrentlySorted = ((TableViewer) columnViewer).getTable()
                                .getSortColumn() == ((TableViewerColumn) viewerColumn).getColumn();
            }
            else if (viewerColumn instanceof TreeViewerColumn)
            {
                columnIsCurrentlySorted = ((TreeViewer) columnViewer).getTree()
                                .getSortColumn() == ((TreeViewerColumn) viewerColumn).getColumn();
            }
            else
            {
                throw new IllegalArgumentException();
            }

            if (columnIsCurrentlySorted)
                setSorter(direction == SWT.DOWN ? SWT.UP : SWT.DOWN);
            else
                setSorter(SWT.DOWN);
        }

        private void setSorter(int direction)
        {
            this.direction = direction;

            if (viewerColumn instanceof TableViewerColumn)
            {
                TableColumn c = ((TableViewerColumn) viewerColumn).getColumn();
                c.getParent().setSortColumn(c);
                c.getParent().setSortDirection(direction);
            }
            else if (viewerColumn instanceof TreeViewerColumn)
            {
                TreeColumn c = ((TreeViewerColumn) viewerColumn).getColumn();
                c.getParent().setSortColumn(c);
                c.getParent().setSortDirection(direction);
            }

            if (columnViewer.getComparator() == this)
                columnViewer.refresh();
            else
                columnViewer.setComparator(this);
        }

        @Override
        public int compare(Viewer viewer, Object element1, Object element2)
        {
            try
            {
                setupSortingContext();

                int dir = direction == SWT.DOWN ? 1 : -1;

                if (element1 == null && element2 == null)
                    return 0;
                else if (element1 == null)
                    return dir;
                else if (element2 == null)
                    return -dir;

                return dir * comparator.compare(element1, element2);
            }
            finally
            {
                SortingContext.clear();
            }
        }

        private void setupSortingContext()
        {
            SortingContext.setSortDirection(direction);

            Object option = null;
            if (viewerColumn instanceof TableViewerColumn)
                option = ((TableViewerColumn) viewerColumn).getColumn().getData(ShowHideColumnHelper.OPTIONS_KEY);
            else if (viewerColumn instanceof TreeViewerColumn)
                option = ((TreeViewerColumn) viewerColumn).getColumn().getData(ShowHideColumnHelper.OPTIONS_KEY);
            SortingContext.setOption(option);
        }
    }

    private Comparator<Object> comparator;

    private ColumnViewerSorter(Comparator<Object> comparator)
    {
        this.comparator = comparator;
    }

    public static ColumnViewerSorter create(Class<?> clazz, String... attributes)
    {
        List<Comparator<Object>> comparators = new ArrayList<>();

        for (String attribute : attributes)
            comparators.add(new BeanComparator(clazz, attribute));

        return new ColumnViewerSorter(
                        comparators.size() == 1 ? comparators.get(0) : new ChainedComparator(comparators));
    }

    public static ColumnViewerSorter create(Function<Object, Comparable<?>> valueProvider)
    {
        return create(new ValueProviderComparator(valueProvider));
    }

    @SuppressWarnings("unchecked")
    public static ColumnViewerSorter create(Comparator<? extends Object> comparator)
    {
        return new ColumnViewerSorter((Comparator<Object>) comparator);
    }

    public ColumnViewerSorter wrap(UnaryOperator<Comparator<Object>> provider)
    {
        this.comparator = provider.apply(this.comparator);
        return this;
    }

    public void attachTo(Column column)
    {
        column.setSorter(this);
    }

    public void attachTo(Column column, int direction)
    {
        column.setSorter(this, direction);
    }

    public void attachTo(ColumnViewer viewer, ViewerColumn column)
    {
        attachTo(viewer, column, SWT.NONE);
    }

    public void attachTo(ColumnViewer viewer, ViewerColumn column, boolean makeDefault)
    {
        attachTo(viewer, column, makeDefault ? SWT.DOWN : SWT.NONE);
    }

    public void attachTo(ColumnViewer viewer, ViewerColumn column, int direction)
    {
        ViewerSorter x = new ViewerSorter(viewer, column, comparator);

        if (direction != SWT.NONE)
            x.setSorter(direction);
    }
}
