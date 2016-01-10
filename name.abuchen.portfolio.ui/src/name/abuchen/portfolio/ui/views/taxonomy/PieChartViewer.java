package name.abuchen.portfolio.ui.views.taxonomy;

import javax.inject.Inject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

/* package */class PieChartViewer extends AbstractChartPage
{
    private EmbeddedBrowser browser;

    @Inject
    public PieChartViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public Control createControl(Composite container)
    {
        browser = new EmbeddedBrowser("/META-INF/html/flare.html"); //$NON-NLS-1$
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
                Money total = getModel().getRootNode().getActual();
                if (getModel().isUnassignedCategoryInChartsExcluded())
                    total = total.subtract(getModel().getUnassignedNode().getActual());

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
        private void printNode(StringBuilder buffer, TaxonomyNode node, Money total)
        {
            String name = StringEscapeUtils.escapeJson(node.getName());
            long actual = node.isRoot() ? total.getAmount() : node.getActual().getAmount();
            long base = node.isRoot() ? total.getAmount() : node.getParent().getActual().getAmount();

            String totalPercentage = "";
            if (node.getParent() != null && !node.getParent().isRoot())
                totalPercentage = "; " + MessageFormat.format(Messages.LabelTotalValuePercent,
                                Values.Percent2.format(actual / (double) total.getAmount()));

            buffer.append("{\"name\":\"").append(name);
            buffer.append("\",\"caption\":\"");
            buffer.append(name).append(" ").append(Values.Amount.format(actual)).append(" (")
                            .append(Values.Percent2.format(actual / (double) base)).append(totalPercentage)
                            .append(")\",");
            buffer.append("\"value\":").append(node.getActual().getAmount());
            buffer.append(",\"color\":\"").append(node.getColor()).append("\"");

            boolean isFirst = true;
            for (TaxonomyNode child : node.getChildren())
            {
                if (child.getActual().isZero())
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
