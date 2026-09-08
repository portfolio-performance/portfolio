package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;

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
                return true;
            case "default-background": //$NON-NLS-1$
                theme.setDefaultBackground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "chip-background": //$NON-NLS-1$
                theme.setChipBackground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "warning-foreground": //$NON-NLS-1$
                theme.setWarningForeground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "warning-background": //$NON-NLS-1$
                theme.setWarningBackground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "red-background": //$NON-NLS-1$
                theme.setRedBackground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "green-background": //$NON-NLS-1$
                theme.setGreenBackground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "red-foreground": //$NON-NLS-1$
                theme.setRedForeground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "green-foreground": //$NON-NLS-1$
                theme.setGreenForeground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "gray-foreground": //$NON-NLS-1$
                theme.setGrayForeground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "hyperlink": //$NON-NLS-1$
                theme.setHyperlink(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "arrow-down": //$NON-NLS-1$
                var arrowDown = getText(value);
                if (arrowDown != null)
                    theme.setArrowDown(arrowDown);
                return true;
            case "arrow-left": //$NON-NLS-1$
                var arrowLeft = getText(value);
                if (arrowLeft != null)
                    theme.setArrowLeft(arrowLeft);
                return true;
            case "arrow-right": //$NON-NLS-1$
                var arrowRight = getText(value);
                if (arrowRight != null)
                    theme.setArrowRight(arrowRight);
                return true;
            case "arrow-up": //$NON-NLS-1$
                var arrowUp = getText(value);
                if (arrowUp != null)
                    theme.setArrowUp(arrowUp);
                return true;
            default:
                return false;
        }
    }

    private String getText(CSSValue value)
    {
        if (value.getCssValueType() != CSSValue.CSS_PRIMITIVE_VALUE)
            return null;
        var primitiveValue = (CSSPrimitiveValue) value;
        if (primitiveValue.getPrimitiveType() == CSSPrimitiveValue.CSS_STRING)
            return primitiveValue.getStringValue();
        else
            return null;
    }
}
