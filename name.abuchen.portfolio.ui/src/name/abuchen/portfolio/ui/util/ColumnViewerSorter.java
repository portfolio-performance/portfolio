package name.abuchen.portfolio.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Widget;

public final class ColumnViewerSorter extends ViewerComparator
{
    private static class ChainedComparator implements Comparator<Object>
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

    private static class BeanComparator implements Comparator<Object>
    {
        private final Class<?> clazz;
        private final Method method;
        private final int type;

        private BeanComparator(Class<?> clazz, String attribute)
        {
            Method readMethod = null;
            String camelCaseAttribute = Character.toUpperCase(attribute.charAt(0)) + attribute.substring(1);
            try
            {
                readMethod = clazz.getMethod("get" + camelCaseAttribute); //$NON-NLS-1$
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    readMethod = clazz.getMethod("is" + camelCaseAttribute); //$NON-NLS-1$
                }
                catch (NoSuchMethodException e1)
                {
                    throw new UnsupportedOperationException(e);
                }
            }

            this.clazz = clazz;
            this.method = readMethod;

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
            else
                type = 0;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            if (!clazz.isInstance(o1) || !clazz.isInstance(o2))
                return 0;

            try
            {
                Object attribute1 = method.invoke(o1);
                Object attribute2 = method.invoke(o2);

                if (attribute1 == null)
                    return attribute2 == null ? 0 : -1;
                if (attribute2 == null)
                    return 1;

                switch (type)
                {
                    case 1:
                        return ((String) attribute1).compareTo((String) attribute2);
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
                    default:
                        return String.valueOf(attribute1).compareTo(String.valueOf(attribute2));
                }
            }
            catch (IllegalAccessException e)
            {
                throw new UnsupportedOperationException(e);
            }
            catch (InvocationTargetException e)
            {
                throw new UnsupportedOperationException(e);
            }
        }
    }

    public static ColumnViewerSorter create(Class<?> clazz, String... attributes)
    {
        List<Comparator<Object>> comparators = new ArrayList<Comparator<Object>>();

        for (String attribute : attributes)
            comparators.add(new BeanComparator(clazz, attribute));

        return new ColumnViewerSorter(comparators.size() == 1 ? comparators.get(0) : new ChainedComparator(comparators));
    }

    public static ColumnViewerSorter create(Comparator<Object> comparator)
    {
        return new ColumnViewerSorter(comparator);
    }

    private final Comparator<Object> comparator;

    private ColumnViewer columnViewer;
    private ViewerColumn viewerColumn;
    private int direction = SWT.DOWN;

    private ColumnViewerSorter(Comparator<Object> comparator)
    {
        this.comparator = comparator;
    }

    public int compare(Viewer viewer, Object element1, Object element2)
    {
        int dir = direction == SWT.DOWN ? 1 : -1;

        if (element1 == null)
            return element2 == null ? 0 : dir;
        if (element2 == null)
            return dir * -1;

        return dir * comparator.compare(element1, element2);
    }

    public void attachTo(ColumnViewer viewer, ViewerColumn column)
    {
        attachTo(viewer, column, false);
    }

    public void attachTo(ColumnViewer viewer, ViewerColumn column, boolean makeDefault)
    {
        this.columnViewer = viewer;
        this.viewerColumn = column;

        Widget widget = null;

        if (column instanceof TableViewerColumn)
            widget = ((TableViewerColumn) column).getColumn();
        else if (column instanceof TreeViewerColumn)
            widget = ((TreeViewerColumn) column).getColumn();
        else
            throw new UnsupportedOperationException();

        widget.addListener(SWT.Selection, new Listener()
        {

            @Override
            public void handleEvent(Event event)
            {
                if (columnViewer.getComparator() != null)
                {
                    if (columnViewer.getComparator() == ColumnViewerSorter.this)
                    {
                        setSorter(direction == SWT.DOWN ? SWT.UP : SWT.DOWN);
                    }
                    else
                    {
                        setSorter(SWT.DOWN);
                    }
                }
                else
                {
                    setSorter(SWT.DOWN);
                }
            }
        });

        if (makeDefault)
            setSorter(direction);
    }

    /* package */void setSorter(int direction)
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

}
