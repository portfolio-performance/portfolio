package name.abuchen.portfolio.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class ColorThemeManager
{
    private static boolean darkModeActive =  PortfolioPlugin.getDefault().getPreferenceStore()
    .getBoolean(UIConstants.Preferences.LIGHTS_OUT_THEME);
    
    public static Color getBackgroundColor() {
        if(darkModeActive) {
            return new Color(Display.getCurrent(),56,56,56);
        }else {
            return Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
        }
    }
}
