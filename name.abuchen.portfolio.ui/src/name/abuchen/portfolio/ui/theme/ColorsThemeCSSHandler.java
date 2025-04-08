package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.Colors;

@SuppressWarnings("restriction")
public class ColorsThemeCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof ColorsThemeElementAdapter colorsThemeAdapter)
        {
            Colors.Theme theme = colorsThemeAdapter.getColorsTheme();

            switch (property)
            {
                case "default-foreground": //$NON-NLS-1$
                    theme.setDefaultForeground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "default-background": //$NON-NLS-1$
                    theme.setDefaultBackground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "warning-background": //$NON-NLS-1$
                    theme.setWarningBackground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "red-background": //$NON-NLS-1$
                    theme.setRedBackground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "green-background": //$NON-NLS-1$
                    theme.setGreenBackground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "red-foreground": //$NON-NLS-1$
                    theme.setRedForeground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "green-foreground": //$NON-NLS-1$
                    theme.setGreenForeground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "gray-foreground": //$NON-NLS-1$
                    theme.setGrayForeground(CSSSWTColorHelper.getRGBA(value));
                    break;
                case "hyperlink": //$NON-NLS-1$
                    theme.setHyperlink(CSSSWTColorHelper.getRGBA(value));
                    break;
                default:
            }
        }

        return false;
    }

}
