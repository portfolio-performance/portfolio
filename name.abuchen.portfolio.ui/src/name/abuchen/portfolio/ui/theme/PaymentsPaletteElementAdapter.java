package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.ElementAdapter;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import name.abuchen.portfolio.ui.views.payments.PaymentsPalette;

@SuppressWarnings("restriction")
public class PaymentsPaletteElementAdapter extends ElementAdapter
{
    public PaymentsPaletteElementAdapter(PaymentsPalette palette, CSSEngine engine)
    {
        super(palette, engine);
    }

    public PaymentsPalette getPalette()
    {
        return (PaymentsPalette) getNativeWidget();
    }

    @Override
    public String getCSSId()
    {
        return null;
    }

    @Override
    public String getCSSClass()
    {
        return "payments"; //$NON-NLS-1$
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
        return "ColorPalette"; //$NON-NLS-1$
    }

    @Override
    public String getAttribute(String name)
    {
        return ""; //$NON-NLS-1$
    }
}