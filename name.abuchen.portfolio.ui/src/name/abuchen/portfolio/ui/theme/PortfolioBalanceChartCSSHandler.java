package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.eclipse.swt.graphics.Color;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.ColorSourceTracker;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.PortfolioBalanceChart;

/**
 * The PortfolioChartCSSHandler class is responsible for customizing the visual
 * appearance of PortfolioChart elements using CSS properties.
 */
@SuppressWarnings("restriction")
public class PortfolioBalanceChartCSSHandler implements ICSSPropertyHandler
{
    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (!(element instanceof PortfolioBalanceChartElementAdapter adapter))
            return false;

        PortfolioBalanceChart chart = adapter.getPortfolioChart();

        switch (property)
        {
            case "totals-color": //$NON-NLS-1$
                chart.setTotalsColor(getColor(value));
                ColorSourceTracker.markCssApplied("PortfolioBalanceChart", "totals-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;

            case "invested-capital-color": //$NON-NLS-1$
                chart.setAbsoluteInvestedCapitalColor(getColor(value));
                ColorSourceTracker.markCssApplied("PortfolioBalanceChart", "invested-capital-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;

            case "absolute-delta-color": //$NON-NLS-1$
                chart.setAbsoluteDeltaColor(getColor(value));
                ColorSourceTracker.markCssApplied("PortfolioBalanceChart", "absolute-delta-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;

            case "taxes-accumulated-color": //$NON-NLS-1$
                chart.setTaxesAccumulatedColor(getColor(value));
                ColorSourceTracker.markCssApplied("PortfolioBalanceChart", "taxes-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;

            case "fees-accumulated-color": //$NON-NLS-1$
                chart.setFeesAccumulatedColor(getColor(value));
                ColorSourceTracker.markCssApplied("PortfolioBalanceChart", "fees-accumulated-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;

            case "delta-area-positive-color": //$NON-NLS-1$
                chart.setDeltaAreaPositive(getColor(value));
                ColorSourceTracker.markCssApplied("PortfolioBalanceChart", "delta-area-positive-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;

            case "delta-area-negative-color": //$NON-NLS-1$
                chart.setDeltaAreaNegative(getColor(value));
                ColorSourceTracker.markCssApplied("PortfolioBalanceChart", "delta-area-negative-color"); //$NON-NLS-1$ //$NON-NLS-2$
                return true;

            default:
                return false;
        }
    }

    /**
     * Converts a CSSValue to a Color.
     */
    private Color getColor(CSSValue value)
    {
        return Colors.getColor(CSSSWTColorHelper.getRGBA(value).rgb);
    }
}