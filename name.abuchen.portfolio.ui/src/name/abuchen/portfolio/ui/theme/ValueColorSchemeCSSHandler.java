package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSPrimitiveValue;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.ValueColorScheme;

@SuppressWarnings("restriction")
public class ValueColorSchemeCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof ValueColorSchemeElementAdapter adapter)
        {
            var scheme = adapter.getScheme();
            return applyTo(scheme, property, value);
        }

        return false;
    }

    private boolean applyTo(ValueColorScheme scheme, String property, CSSValue value)
    {
        switch (property)
        {
            case "positive-foreground": //$NON-NLS-1$
                scheme.setPositiveForeground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "negative-foreground": //$NON-NLS-1$
                scheme.setNegativeForeground(CSSSWTColorHelper.getRGBA(value));
                return true;
            case "up-arrow": //$NON-NLS-1$
                var upArrow = getText(value);
                if (upArrow != null)
                    scheme.setUpArrow(upArrow);
                return true;
            case "down-arrow": //$NON-NLS-1$
                var downArrow = getText(value);
                if (downArrow != null)
                    scheme.setDownArrow(downArrow);
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
