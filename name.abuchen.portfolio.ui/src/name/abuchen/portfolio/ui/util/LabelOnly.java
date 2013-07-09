package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.action.Action;

public class LabelOnly extends Action
{
    public LabelOnly(String text)
    {
        super(text);
        setEnabled(false);
    }
}
