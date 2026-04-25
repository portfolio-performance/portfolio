package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.ColorSourceTracker;

@SuppressWarnings("restriction")
public class ColorGradientCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof ColorGradientElementAdapter adapter)
        {
            int index = Integer.parseInt(property.substring("color-".length())); //$NON-NLS-1$
            adapter.getDefinition().setColor(index, CSSSWTColorHelper.getRGBA(value));
            ColorSourceTracker.markCssApplied("ColorGradientDefinitions." + adapter.getDefinition().getCssClass(), //$NON-NLS-1$
                            property);
            return true;
        }

        return false;
    }
}
