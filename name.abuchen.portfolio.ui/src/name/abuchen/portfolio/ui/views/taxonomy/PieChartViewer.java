package name.abuchen.portfolio.ui.views.taxonomy;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.JavaFXBrowser;
import netscape.javascript.JSObject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/* package */class PieChartViewer extends AbstractChartPage
{
    private static final String FLARE_URL = "/META-INF/html/flare_chart.html"; //$NON-NLS-1$

    private JavaFXBrowser browser;

    public PieChartViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public Control createControl(Composite container)
    {
        browser = new JavaFXBrowser(container);
        browser.registerBrowserFunction("loadData", new LoadDataFunction()); //$NON-NLS-1$
        browser.load(FLARE_URL);
        return browser;
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
        browser.load(FLARE_URL);
    }

    @Override
    public void onConfigChanged()
    {
        browser.load(FLARE_URL);
    }

    public final class LoadDataFunction implements JavaFXBrowser.BrowserFunction
    {
        @Override
        public Object function(JSObject arguments)
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
