package name.abuchen.portfolio.ui.theme;

import org.eclipse.e4.ui.css.core.dom.ElementAdapter;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import name.abuchen.portfolio.ui.views.PortfolioBalanceChart;

@SuppressWarnings("restriction")
public class PortfolioBalanceChartElementAdapter extends ElementAdapter
{

    public PortfolioBalanceChartElementAdapter(PortfolioBalanceChart chart, CSSEngine engine)
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
        return null;
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