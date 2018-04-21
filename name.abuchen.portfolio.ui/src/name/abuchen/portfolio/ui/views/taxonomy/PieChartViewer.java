package name.abuchen.portfolio.ui.views.taxonomy;

import javax.inject.Inject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
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
import name.abuchen.portfolio.ui.util.SimpleAction;

/* package */class PieChartViewer extends AbstractChartPage
{
    private EmbeddedBrowser browser;

    @Inject
    public PieChartViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        super.configMenuAboutToShow(manager);

        Action action = new SimpleAction(Messages.LabelIncludeSecuritiesInPieChart, a -> {
            getModel().setExcludeSecuritiesInPieChart(!getModel().isSecuritiesInPieChartExcluded());
            onConfigChanged();
        });
        action.setChecked(!getModel().isSecuritiesInPieChartExcluded());
        manager.add(action);
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

        @Override
        public Object function(Object[] arguments)
        {
            try
            {
                TaxonomyNode root = getModel().getChartRenderingRootNode();

                StringBuilder buffer = new StringBuilder();
                printNode(buffer, root, root.getActual());
                return buffer.toString();
            }
            catch (Throwable e) // NOSONAR
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

                if (child.isAssignment() && getModel().isSecuritiesInPieChartExcluded())
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
