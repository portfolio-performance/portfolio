package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.ColorSourceTracker;
import name.abuchen.portfolio.ui.util.Colors;

@SuppressWarnings("restriction")
public class ColorsThemeCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (!(element instanceof ColorsThemeElementAdapter colorsThemeAdapter))
            return false;

        Colors.Theme theme = colorsThemeAdapter.getColorsTheme();

        switch (property)
        {
            case "default-foreground": //$NON-NLS-1$
                theme.setDefaultForeground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "default-foreground"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "default-background": //$NON-NLS-1$
                theme.setDefaultBackground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "default-background"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "chip-background": //$NON-NLS-1$
                theme.setChipBackground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "chip-background"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "warning-background": //$NON-NLS-1$
                theme.setWarningBackground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "warning-background"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "red-background": //$NON-NLS-1$
                theme.setRedBackground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "red-background"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "green-background": //$NON-NLS-1$
                theme.setGreenBackground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "green-background"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "red-foreground": //$NON-NLS-1$
                theme.setRedForeground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "red-foreground"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "green-foreground": //$NON-NLS-1$
                theme.setGreenForeground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "green-foreground"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "gray-foreground": //$NON-NLS-1$
                theme.setGrayForeground(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "gray-foreground"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            case "hyperlink": //$NON-NLS-1$
                theme.setHyperlink(CSSSWTColorHelper.getRGBA(value));
                ColorSourceTracker.markCssApplied("CustomColors", "hyperlink"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            default:
                return false;
        }
    }
}