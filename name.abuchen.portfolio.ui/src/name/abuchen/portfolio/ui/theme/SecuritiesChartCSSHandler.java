package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.SecuritiesChart;

@SuppressWarnings("restriction")
public class SecuritiesChartCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof SecuritiesChartElementAdapter)
        {
            SecuritiesChart chart = ((SecuritiesChartElementAdapter) element).getSecuritiesChart();

            switch (property)
            {
                case "quote-color": //$NON-NLS-1$
                    chart.setQuoteColor(Colors.getColor(CSSSWTColorHelper.getRGBA(value).rgb));
                    break;
                default:
            }
        }

        return false;
    }

}
