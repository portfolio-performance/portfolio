package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.views.payments.PaymentsPalette;

@SuppressWarnings("restriction")
public class PaymentsPaletteCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof PaymentsPaletteElementAdapter adapter)
        {
            var palette = adapter.getPalette();
            return applyTo(palette, property, value);
        }

        return false;
    }

    private boolean applyTo(PaymentsPalette palette, String property, CSSValue value)
    {
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
