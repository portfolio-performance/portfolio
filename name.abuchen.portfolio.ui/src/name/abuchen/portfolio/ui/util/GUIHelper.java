package name.abuchen.portfolio.ui.util;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public final class GUIHelper
{
    private static final ColorRegistry COLOR_REGISTRY = new ColorRegistry();
    
    private GUIHelper()
    {}

    public static Color getColor(int red, int green, int blue)
    {
        String key = getColorKey(red, green, blue);
        if (COLOR_REGISTRY.hasValueFor(key))
        {
            return COLOR_REGISTRY.get(key);
        }
        else
        {
            COLOR_REGISTRY.put(key, new RGB(red, green, blue));
            return COLOR_REGISTRY.get(key);
        }
    }

    private static String getColorKey(int red, int green, int blue)
    {
        return "COLOR_" + red + "_" + green + "_" + blue; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
