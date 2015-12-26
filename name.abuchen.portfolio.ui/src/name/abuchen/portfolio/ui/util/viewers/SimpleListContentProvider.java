package name.abuchen.portfolio.ui.util.viewers;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class SimpleListContentProvider implements IStructuredContentProvider
{
    private boolean reverse = false;
    private Object[] elements;

    public SimpleListContentProvider()
    {}

    public SimpleListContentProvider(boolean reverse)
    {
        this.reverse = reverse;
    }

    public void dispose()
    {}

    public Object[] getElements(Object inputElement)
    {
        return this.elements;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
    {
        if (newInput != null)
        {
            if (newInput instanceof Object[])
                elements = (Object[]) newInput;
            else
                elements = ((List<?>) newInput).toArray();

            if (reverse)
            {
                for (int left = 0, right = elements.length - 1; left < right; left++, right--)
                {
                    Object temp = elements[left];
                    elements[left] = elements[right];
                    elements[right] = temp;
                }
            }
        }
        else
        {
            elements = null;
        }
    }
}
