package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.css.CSSValue;

@SuppressWarnings("restriction")
public class ColumnViewerCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof ColumnViewerElement)
        {
            ColumnViewerElement viewer = (ColumnViewerElement) element;

            switch (property)
            {
                case "swt-lines-visible": //$NON-NLS-1$
                    Boolean isLinesVisible = (Boolean) engine.convert(value, Boolean.class, Display.getDefault());
                    viewer.setLinesVisible(isLinesVisible);
                    break;
                default:
            }
        }

        return false;
    }

    @Override
    public String retrieveCSSProperty(Object element, String property, String pseudo, CSSEngine engine) throws Exception
    {
        if (element instanceof ColumnViewerElement)
        {
            ColumnViewerElement viewer = (ColumnViewerElement) element;

            switch (property)
            {
                case "swt-lines-visible": //$NON-NLS-1$
                    return String.valueOf(viewer.isLinesVisible());
                default:
            }
        }

        return ICSSPropertyHandler.super.retrieveCSSProperty(element, property, pseudo, engine);
    }

}
