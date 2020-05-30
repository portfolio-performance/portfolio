package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

public class LabelOnly extends Action
{
    public LabelOnly(String text)
    {
        super(text);
        setEnabled(false);
    }

    public LabelOnly(String text, ImageDescriptor image)
    {
        super(text, image);
        setEnabled(false);
    }
}
