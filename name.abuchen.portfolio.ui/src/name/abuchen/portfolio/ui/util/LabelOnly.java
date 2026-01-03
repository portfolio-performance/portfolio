package name.abuchen.portfolio.ui.util;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.util.action.MenuContribution;

public class LabelOnly extends MenuContribution
{
    public LabelOnly(String text)
    {
        super(text);
    }

    public LabelOnly(String text, Images image)
    {
        super(text, image);
    }
}
