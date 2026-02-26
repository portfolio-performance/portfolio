package name.abuchen.portfolio.ui.util.viewers;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import name.abuchen.portfolio.util.TextUtil;

/**
 * A locale-sensitive ViewerComparator that uses a LabelProvider to extract text
 * from elements and compares them using them TextUtil.
 */
public class LocaleSenstiveViewerComparator extends ViewerComparator
{
    private final ILabelProvider labelProvider;

    public LocaleSenstiveViewerComparator(ILabelProvider labelProvider)
    {
        this.labelProvider = labelProvider;
    }

    @Override
    public int compare(Viewer viewer, Object o1, Object o2)
    {
        if (o1 == null && o2 == null)
            return 0;
        else if (o1 == null)
            return -1;
        else if (o2 == null)
            return 1;

        var s1 = labelProvider.getText(o1);
        var s2 = labelProvider.getText(o2);

        if (s1 == null && s2 == null)
            return 0;
        else if (s1 == null)
            return -1;
        else if (s2 == null)
            return 1;

        return TextUtil.compare(s1, s2);
    }
}
