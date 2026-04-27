package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.helpers.CSSSWTColorHelper;
import org.eclipse.swt.graphics.Color;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSValue;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.ValueColorScheme;
import name.abuchen.portfolio.ui.views.PortfolioBalanceChart;

public final class PortfolioBalanceChartCSS
{
    private PortfolioBalanceChartCSS()
    {
    }

    @SuppressWarnings("restriction")
    public static final class ElementAdapter extends org.eclipse.e4.ui.css.core.dom.ElementAdapter
    {
        public ElementAdapter(PortfolioBalanceChart chart, CSSEngine engine)
        {
            super(chart, engine);
        }
        public PortfolioBalanceChart getPortfolioChart()
        {
            return (PortfolioBalanceChart) getNativeWidget();
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
            return "PortfolioBalanceChart"; //$NON-NLS-1$
        }

        @Override
        public String getAttribute(String arg0)
        {
            return ""; //$NON-NLS-1$
        }
    }

    @SuppressWarnings("restriction")
    public static final class Handler implements org.eclipse.e4.ui.css.core.dom.properties.ICSSPropertyHandler
    {
        @Override
        public boolean applyCSSProperty(Object element, String property, CSSValue value, String pseudo, CSSEngine engine)
                        throws Exception
        {
            if (!(element instanceof PortfolioBalanceChartCSS.ElementAdapter adapter))
                return false;

            PortfolioBalanceChart chart = adapter.getPortfolioChart();

            switch (property)
            {
                case "totals-color": //$NON-NLS-1$
                    chart.setTotalsColor(getColor(value));
                    return true;

                case "invested-capital-color": //$NON-NLS-1$
                    chart.setAbsoluteInvestedCapitalColor(getColor(value));
                    return true;

                case "absolute-delta-color": //$NON-NLS-1$
                    chart.setAbsoluteDeltaColor(getColor(value));
                    return true;

                case "taxes-accumulated-color": //$NON-NLS-1$
                    chart.setTaxesAccumulatedColor(getColor(value));
                    return true;

                case "fees-accumulated-color": //$NON-NLS-1$
                    chart.setFeesAccumulatedColor(getColor(value));
                    return true;

                case "delta-area-positive-color": //$NON-NLS-1$
                    chart.setDeltaAreaPositive(getColor(value));
                    return true;

                case "delta-area-negative-color": //$NON-NLS-1$
                    chart.setDeltaAreaNegative(getColor(value));
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
}