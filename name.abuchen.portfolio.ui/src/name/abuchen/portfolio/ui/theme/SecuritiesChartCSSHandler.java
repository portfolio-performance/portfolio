package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.eclipse.swt.graphics.Color;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.SecuritiesChart;

/**
 * The SecuritiesChartCSSHandler class is responsible for customizing the visual
 * appearance of SecuritiesChart elements using CSS properties.
 */
@SuppressWarnings("restriction")
public class SecuritiesChartCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (!(element instanceof SecuritiesChartElementAdapter adapter))
            return false;

        SecuritiesChart chart = adapter.getSecuritiesChart();

        switch (property)
        {
            case "quote-color": //$NON-NLS-1$
                chart.setQuoteColor(getColor(value));
                return true;

            case "quote-area-positive-color": //$NON-NLS-1$
                chart.setQuoteAreaPositive(getColor(value));
                return true;

            case "quote-area-negative-color": //$NON-NLS-1$
                chart.setQuoteAreaNegative(getColor(value));
                return true;

            case "purchase-event-color": //$NON-NLS-1$
                chart.setPurchaseColor(getColor(value));
                return true;

            case "sale-event-color": //$NON-NLS-1$
                chart.setSaleColor(getColor(value));
                return true;

            case "dividend-event-color": //$NON-NLS-1$
                chart.setDividendColor(getColor(value));
                return true;

            case "extreme-marker-high-color": //$NON-NLS-1$
                chart.setExtremeMarkerHighColor(getColor(value));
                return true;

            case "extreme-marker-low-color": //$NON-NLS-1$
                chart.setExtremeMarkerLowColor(getColor(value));
                return true;

            case "non-trading-color": //$NON-NLS-1$
                chart.setNonTradingColor(getColor(value));
                return true;

            case "shares-held-color": //$NON-NLS-1$
                chart.setSharesHeldColor(getColor(value));
                return true;

            case "fifo-purchase-price-color": //$NON-NLS-1$
                chart.setFifoPurchasePriceColor(getColor(value));
                return true;

            case "moving-average-purchase-price-color": //$NON-NLS-1$
                chart.setMovingAveragePurchasePriceColor(getColor(value));
                return true;

            case "bollinger-bands-color": //$NON-NLS-1$
                chart.setBollingerBandsColor(getColor(value));
                return true;

            case "macd-color": //$NON-NLS-1$
                chart.setMacdColor(getColor(value));
                return true;

            case "sma-1-color": //$NON-NLS-1$
                chart.setSma1Color(getColor(value));
                return true;

            case "sma-2-color": //$NON-NLS-1$
                chart.setSma2Color(getColor(value));
                return true;

            case "sma-3-color": //$NON-NLS-1$
                chart.setSma3Color(getColor(value));
                return true;

            case "sma-4-color": //$NON-NLS-1$
                chart.setSma4Color(getColor(value));
                return true;

            case "sma-5-color": //$NON-NLS-1$
                chart.setSma5Color(getColor(value));
                return true;

            case "sma-6-color": //$NON-NLS-1$
                chart.setSma6Color(getColor(value));
                return true;

            case "sma-7-color": //$NON-NLS-1$
                chart.setSma7Color(getColor(value));
                return true;

            case "ema-1-color": //$NON-NLS-1$
                chart.setEma1Color(getColor(value));
                return true;

            case "ema-2-color": //$NON-NLS-1$
                chart.setEma2Color(getColor(value));
                return true;

            case "ema-3-color": //$NON-NLS-1$
                chart.setEma3Color(getColor(value));
                return true;

            case "ema-4-color": //$NON-NLS-1$
                chart.setEma4Color(getColor(value));
                return true;

            case "ema-5-color": //$NON-NLS-1$
                chart.setEma5Color(getColor(value));
                return true;

            case "ema-6-color": //$NON-NLS-1$
                chart.setEma6Color(getColor(value));
                return true;

            case "ema-7-color": //$NON-NLS-1$
                chart.setEma7Color(getColor(value));
                return true;

            default:
                return false;
        }
    }

    private Color getColor(CSSValue value)
    {
        return Colors.getColor(CSSSWTColorHelper.getRGBA(value).rgb);
    }
}
