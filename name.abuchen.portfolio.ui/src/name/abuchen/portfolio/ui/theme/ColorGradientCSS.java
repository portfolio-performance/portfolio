package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.ColorGradientDefinitions;

public final class ColorGradientCSS
{
    private ColorGradientCSS()
    {
    }

    @SuppressWarnings("restriction")
    public static final class ElementAdapter extends org.eclipse.e4.ui.css.core.dom.ElementAdapter
    {
        public ElementAdapter(ColorGradientDefinitions.Definition definition, CSSEngine engine)
        {
            super(definition, engine);
        }

        public ColorGradientDefinitions.Definition getDefinition()
        {
            return (ColorGradientDefinitions.Definition) getNativeWidget();
        }

        @Override
        public String getCSSId()
        {
            return null;
        }

        @Override
        public String getCSSClass()
        {
            return getDefinition().getCssClass();
        }

        @Override
        public String getCSSStyle()
        {
            return null;
        }

        @Override
        public Node getParentNode()
        {
            return null;
        }

        @Override
        public NodeList getChildNodes()
        {
            return null;
        }

        @Override
        public String getNamespaceURI()
        {
            return null;
        }

        @Override
        public String getLocalName()
        {
            return "ColorGradient"; //$NON-NLS-1$
        }

        @Override
        public String getAttribute(String name)
        {
            return ""; //$NON-NLS-1$
        }
    }

    @SuppressWarnings("restriction")
    public static final class Handler implements org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler
    {
        @Override
        public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo,
                        CSSEngine engine) throws Exception
        {
            if (element instanceof ColorGradientCSS.ElementAdapter adapter)
            {
                int index = Integer.parseInt(property.substring("color-".length())); //$NON-NLS-1$
                adapter.getDefinition().setColor(index, CSSSWTColorHelper.getRGBA(value));
                return true;
            }

            return false;
        }
    }
}
