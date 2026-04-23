package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.ElementAdapter;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import name.abuchen.portfolio.ui.util.ColorGradientDefinitions;

@SuppressWarnings("restriction")
public class ColorGradientElementAdapter extends ElementAdapter
{
    public ColorGradientElementAdapter(ColorGradientDefinitions.Definition definition, CSSEngine engine)
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