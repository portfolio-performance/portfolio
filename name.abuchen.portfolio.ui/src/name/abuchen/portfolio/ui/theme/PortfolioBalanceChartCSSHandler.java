package name.abuchen.portfolio.ui.theme;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.eclipse.swt.graphics.Color;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.PortfolioBalanceChart;

/**
 * The PortfolioChartCSSHandler class is responsible for customizing the visual
 * appearance of PortfolioChart elements using CSS properties. It uses a mapping
 * of CSS properties to color setters to apply the specified colors to
 * corresponding elements within the chart.
 */
@SuppressWarnings("restriction")
public class PortfolioBalanceChartCSSHandler implements ICSSPropertyHandler
{
    private interface ColorSetter
    {
        /**
         * Sets the color on the PortfolioChart element.
         */
        void setColor(PortfolioBalanceChart chart, CSSValue value);
    }

    private final Map<String, ColorSetter> propertyMap = new HashMap<>();

    public PortfolioBalanceChartCSSHandler()
    {
        initializePropertyMap();
    }

    private void initializePropertyMap()
    {
        propertyMap.put("invested-capital-color", //$NON-NLS-1$
                        (chart, value) -> chart.setAbsoluteInvestedCapitalColor(getColor(value)));
        propertyMap.put("absolute-delta-color", (chart, value) -> chart.setAbsoluteDeltaColor(getColor(value))); //$NON-NLS-1$
        propertyMap.put("delta-area-positive-color", (chart, value) -> chart.setDeltaAreaPositive(getColor(value))); //$NON-NLS-1$
        propertyMap.put("delta-area-negative-color", (chart, value) -> chart.setDeltaAreaNegative(getColor(value))); //$NON-NLS-1$
    }

    @Override
    public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                    throws Exception
    {
        if (element instanceof PortfolioBalanceChartElementAdapter adapter)
        {
            PortfolioBalanceChart chart = adapter.getPortfolioChart();
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