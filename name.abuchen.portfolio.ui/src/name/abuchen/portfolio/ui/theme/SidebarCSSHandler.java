package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.dom.properties.converters.ICSSValueConverter;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.properties.AbstractCSSPropertySWTHandler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.editor.Sidebar;

@SuppressWarnings("restriction")
public class SidebarCSSHandler extends AbstractCSSPropertySWTHandler implements ICSSPropertyHandler
{
    private static final String SELECTION_COLOR = "selection-color"; //$NON-NLS-1$
    private static final String SELECTION_BACKGROUND_COLOR = "selection-background-color"; //$NON-NLS-1$

    @Override
    protected void applyCSSProperty(Control control, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {

        if (control instanceof Sidebar)
        {
            Sidebar<?> sidebar = (Sidebar<?>) control;
            if (SELECTION_COLOR.equalsIgnoreCase(property) && (value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE))
            {
                Color newColor = (Color) engine.convert(value, Color.class, control.getDisplay());
                sidebar.setSelectionForeground(newColor);
            }
            else if (SELECTION_BACKGROUND_COLOR.equalsIgnoreCase(property)
                            && (value.getCssValueType() == CSSValue.CSS_PRIMITIVE_VALUE))
            {
                Color newColor = (Color) engine.convert(value, Color.class, control.getDisplay());
                sidebar.setSelectionBackground(newColor);
            }
        }
    }

    @Override
    protected String retrieveCSSProperty(Control control, String property, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (control instanceof Sidebar)
        {
            Sidebar<?> sidebar = (Sidebar<?>) control;
            if (SELECTION_COLOR.equalsIgnoreCase(property))
            {
                ICSSValueConverter cssValueConverter = engine.getCSSValueConverter(String.class);
                return cssValueConverter.convert(sidebar.getSelectionForeground(), engine, null);
            }
            else if (SELECTION_BACKGROUND_COLOR.equalsIgnoreCase(property))
            {
                ICSSValueConverter cssValueConverter = engine.getCSSValueConverter(String.class);
                return cssValueConverter.convert(sidebar.getSelectionBackground(), engine, null);
            }

        }
        return null;
    }

}
