package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */class PieChartViewer extends AbstractChartPage
{
    private EmbeddedBrowser browser;

    public PieChartViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public Control createControl(Composite container)
    {
        browser = new EmbeddedBrowser("/META-INF/html/flare_chart.html"); //$NON-NLS-1$
        return browser.createControl(container, b -> new LoadDataFunction(b, "loadData")); //$NON-NLS-1$
    }

    @Override
    public void beforePage()
    {}

    @Override
    public void afterPage()
    {}

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        browser.refresh();
    }

    @Override
    public void onConfigChanged()
    {
        browser.refresh();
    }

    private final class LoadDataFunction extends BrowserFunction
    {
        private LoadDataFunction(Browser browser, String name)
        {
            super(browser, name);
        }

        public Object function(Object[] arguments)
        {
            try
            {
                long total = getModel().getRootNode().getActual();
                if (getModel().isUnassignedCategoryInChartsExcluded())
                    total -= getModel().getUnassignedNode().getActual();

                StringBuilder buffer = new StringBuilder();
                printNode(buffer, getModel().getRootNode(), total);
                return buffer.toString();
            }
            catch (Throwable e)
            {
                PortfolioPlugin.log(e);
                return "{}"; //$NON-NLS-1$
            }
        }

        @SuppressWarnings("nls")
        private void printNode(StringBuilder buffer, TaxonomyNode node, long total)
        {
            String name = StringEscapeUtils.escapeJson(node.getName());
            long actual = node.isRoot() ? total : node.getActual();

            buffer.append("{\"name\":\"").append(name);
            buffer.append("\",\"caption\":\"");
            buffer.append(name).append(" ").append(Values.Amount.format(actual)).append(" (")
                            .append(Values.Percent2.format(actual / (double) total)).append(")\",");
            buffer.append("\"value\":").append(node.getActual());
            buffer.append(",\"color\":\"").append(node.getColor()).append("\"");

            boolean isFirst = true;
            for (TaxonomyNode child : node.getChildren())
            {
                if (child.getActual() == 0L)
                    continue;

                if (getModel().isUnassignedCategoryInChartsExcluded() && child.isUnassignedCategory())
                    continue;

                if (isFirst)
                    buffer.append(",\"children\": [");
                else
                    buffer.append(",");

                printNode(buffer, child, total);
                isFirst = false;
            }

            if (!isFirst)
                buffer.append("]");

            buffer.append("}");
        }

    }
}
