package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.views.payments.PaymentsPalette;

public final class PaymentsPaletteCSS
{
    private PaymentsPaletteCSS()
    {
    }

    @SuppressWarnings("restriction")
    public static final class ElementAdapter extends org.eclipse.e4.ui.css.core.dom.ElementAdapter
    {
        public ElementAdapter(PaymentsPalette palette, CSSEngine engine)
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
            return ValueColorScheme.current().getIdentifier();
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

    @SuppressWarnings("restriction")
    public static final class Handler implements org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler
    {
        @Override
        public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo,
                        CSSEngine engine) throws Exception
        {
            if (!(element instanceof PaymentsPaletteCSS.ElementAdapter adapter))
                return false;

            PaymentsPalette palette = adapter.getPalette();

            switch (property)
            {
                case "color-0": //$NON-NLS-1$
                    palette.setColor(0, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-1": //$NON-NLS-1$
                    palette.setColor(1, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-2": //$NON-NLS-1$
                    palette.setColor(2, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-3": //$NON-NLS-1$
                    palette.setColor(3, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-4": //$NON-NLS-1$
                    palette.setColor(4, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-5": //$NON-NLS-1$
                    palette.setColor(5, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-6": //$NON-NLS-1$
                    palette.setColor(6, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-7": //$NON-NLS-1$
                    palette.setColor(7, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-8": //$NON-NLS-1$
                    palette.setColor(8, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-9": //$NON-NLS-1$
                    palette.setColor(9, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-10": //$NON-NLS-1$
                    palette.setColor(10, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-11": //$NON-NLS-1$
                    palette.setColor(11, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-12": //$NON-NLS-1$
                    palette.setColor(12, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-13": //$NON-NLS-1$
                    palette.setColor(13, CSSSWTColorHelper.getRGBA(value));
                    return true;

                case "color-14": //$NON-NLS-1$
                    palette.setColor(14, CSSSWTColorHelper.getRGBA(value));
                    return true;

                default:
                    return false;
            }
        }
    }
}
