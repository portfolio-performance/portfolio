package name.abuchen.portfolio.ui.views.dataseries;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public final class DataSeriesLabelProvider extends LabelProvider
{
    @Override
    public Image getImage(Object element)
    {
        return ((DataSeries) element).getImage();
    }

    @Override
    public String getText(Object element)
    {
        return ((DataSeries) element).getSearchLabel();
    }
}