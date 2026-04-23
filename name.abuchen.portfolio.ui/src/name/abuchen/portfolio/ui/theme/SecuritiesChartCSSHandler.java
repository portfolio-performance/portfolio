package name.abuchen.portfolio.ui.theme;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.eclipse.swt.graphics.Color;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.SecuritiesChart;

/**
 * The SecuritiesChartCSSHandler class is responsible for customizing the visual
 * appearance of SecuritiesChart elements using CSS properties. It uses a
 * mapping of CSS properties to color setters to apply the specified colors to
 * corresponding elements within the chart.
 */
@SuppressWarnings("restriction")
public class SecuritiesChartCSSHandler implements ICSSPropertyHandler
{
    private interface ColorSetter
    {
        /**
         * Sets the color on the SecuritiesChart element.
         */
        void setColor(SecuritiesChart chart, CSSValue value);
    }

    private final Map<String, ColorSetter> propertyMap = new HashMap<>();

    public SecuritiesChartCSSHandler()
    {
        initializePropertyMap();
    }

    private void initializePropertyMap()
    {
        propertyMap.put("quote-color", (chart, value) -> chart.setQuoteColor(getColor(value))); //$NON-NLS-1$
        propertyMap.put("quote-area-positive-color", (chart, value) -> chart.setQuoteAreaPositive(getColor(value))); //$NON-NLS-1$
        propertyMap.put("quote-area-negative-color", (chart, value) -> chart.setQuoteAreaNegative(getColor(value))); //$NON-NLS-1$
        propertyMap.put("purchase-event-color", (chart, value) -> chart.setPurchaseColor(getColor(value))); //$NON-NLS-1$
        propertyMap.put("sale-event-color", (chart, value) -> chart.setSaleColor(getColor(value))); //$NON-NLS-1$
        propertyMap.put("dividend-event-color", (chart, value) -> chart.setDividendColor(getColor(value))); //$NON-NLS-1$
        propertyMap.put("extreme-marker-high-color", //$NON-NLS-1$
                        (chart, value) -> chart.setExtremeMarkerHighColor(getColor(value)));
        propertyMap.put("extreme-marker-low-color", (chart, value) -> chart.setExtremeMarkerLowColor(getColor(value))); //$NON-NLS-1$
        propertyMap.put("non-trading-color", (chart, value) -> chart.setNonTradingColor(getColor(value))); //$NON-NLS-1$
        propertyMap.put("shares-held-color", (chart, value) -> chart.setSharesHeldColor(getColor(value))); //$NON-NLS-1$

        propertyMap.put("fifo-purchase-price-color", //$NON-NLS-1$
                        (chart, value) -> chart.setFifoPurchasePriceColor(getColor(value)));
        propertyMap.put("moving-average-purchase-price-color", //$NON-NLS-1$
                        (chart, value) -> chart.setMovingAveragePurchasePriceColor(getColor(value)));
        propertyMap.put("bollinger-bands-color", //$NON-NLS-1$
                        (chart, value) -> chart.setBollingerBandsColor(getColor(value)));
        propertyMap.put("macd-color", (chart, value) -> chart.setMacdColor(getColor(value))); //$NON-NLS-1$

        propertyMap.put("sma-1-color", (chart, value) -> chart.setSma1Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("sma-2-color", (chart, value) -> chart.setSma2Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("sma-3-color", (chart, value) -> chart.setSma3Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("sma-4-color", (chart, value) -> chart.setSma4Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("sma-5-color", (chart, value) -> chart.setSma5Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("sma-6-color", (chart, value) -> chart.setSma6Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("sma-7-color", (chart, value) -> chart.setSma7Color(getColor(value))); //$NON-NLS-1$

        propertyMap.put("ema-1-color", (chart, value) -> chart.setEma1Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("ema-2-color", (chart, value) -> chart.setEma2Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("ema-3-color", (chart, value) -> chart.setEma3Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("ema-4-color", (chart, value) -> chart.setEma4Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("ema-5-color", (chart, value) -> chart.setEma5Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("ema-6-color", (chart, value) -> chart.setEma6Color(getColor(value))); //$NON-NLS-1$
        propertyMap.put("ema-7-color", (chart, value) -> chart.setEma7Color(getColor(value))); //$NON-NLS-1$
    }

    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof SecuritiesChartElementAdapter adapter)
        {
            SecuritiesChart chart = adapter.getSecuritiesChart();
            ColorSetter colorSetter = propertyMap.get(property);

            if (colorSetter != null)
            {
                colorSetter.setColor(chart, value);
            }
        }

        return false;
    }

    /**
     * Converts a CSSValue to a Color.
     */
    private Color getColor(CSSValue value)
    {
        return Colors.getColor(CSSSWTColorHelper.getRGBA(value).rgb);
    }

}
