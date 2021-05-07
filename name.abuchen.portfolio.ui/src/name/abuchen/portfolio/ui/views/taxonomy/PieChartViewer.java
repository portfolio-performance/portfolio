package name.abuchen.portfolio.ui.views.taxonomy;

import javax.inject.Inject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.json.simple.JSONObject;

import com.ibm.icu.text.MessageFormat;

import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser.ItemSelectedFunction;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.util.ColorConversion;

/* package */class PieChartViewer extends AbstractChartPage
{
    private AbstractFinanceView view;

    private EmbeddedBrowser browser;

    @Inject
    public PieChartViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
        this.view = view;
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
        browser = make(EmbeddedBrowser.class);
        browser.setHtmlpage("/META-INF/html/flare.html"); //$NON-NLS-1$
        return browser.createControl(container, LoadDataFunction::new,
                        b -> new ItemSelectedFunction(b, uuid -> getModel().getVirtualRootNode().getNodeById(uuid)
                                        .ifPresent(n -> view.setInformationPaneInput(n))));
    }

    @Override
    public void beforePage()
    {
        // nothing to do
    }

    @Override
    public void afterPage()
    {
        // nothing to do
    }

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
        private LoadDataFunction(Browser browser) // NOSONAR
        {
            super(browser, "loadData"); //$NON-NLS-1$
        }

        @Override
        public Object function(Object[] arguments)
        {
            try
            {
                TaxonomyNode root = getModel().getChartRenderingRootNode();

                StringBuilder buffer = new StringBuilder();
                printNode(buffer, root, "", root.getActual(), getModel().isSecuritiesInPieChartExcluded()); //$NON-NLS-1$
                return buffer.toString();
            }
            catch (Throwable e) // NOSONAR
            {
                PortfolioPlugin.log(e);
                return "{}"; //$NON-NLS-1$
            }
        }

        @SuppressWarnings("nls")
        private void printNode(StringBuilder buffer, TaxonomyNode node, String nodeColor, Money total,
                        boolean excludeSecurities)
        {
            String name = JSONObject.escape(node.getName());
            long actual = node.isRoot() ? total.getAmount() : node.getActual().getAmount();
            long base = node.isRoot() ? total.getAmount() : node.getParent().getActual().getAmount();

            String totalPercentage = "";
            if (node.getParent() != null && !node.getParent().isRoot())
                totalPercentage = "; " + MessageFormat.format(Messages.LabelTotalValuePercent,
                                Values.Percent2.format(actual / (double) total.getAmount()));

            if (excludeSecurities && node.isAssignment())
            {
                buffer.append("{\"uuid\":\"\",\"name\":\"\",\"caption\":\"\",");
                buffer.append("\"value\":").append(node.getActual().getAmount());
                buffer.append(",\"color\":\"#FFFFFF\"");
            }
            else
            {
                buffer.append("{\"uuid\":\"").append(node.getId());
                buffer.append("\",\"name\":\"").append(name);
                buffer.append("\",\"caption\":\"");
                buffer.append(name).append(" ").append(Values.Amount.format(actual)).append(" (")
                                .append(Values.Percent2.format(actual / (double) base)).append(totalPercentage)
                                .append(")\",");
                buffer.append("\"value\":").append(node.getActual().getAmount());
                buffer.append(",\"color\":\"")
                                .append(node.isAssignment() ? ColorConversion.brighter(nodeColor) : node.getColor())
                                .append("\"");
            }

            addChildren(buffer, node, node.getColor(), total, excludeSecurities);

            buffer.append("}");
        }

        private void addChildren(StringBuilder buffer, TaxonomyNode node, String nodeColor, Money total,
                        boolean excludeSecurities)
        {
            // iterate over children if
            // a) all are shown anyway or
            // b) if the children contain at least one classification (which
            // means if securities are not show we do not go over the children
            // as there are only securities)

            boolean iterateChildren = !excludeSecurities
                            || node.getChildren().stream().anyMatch(n -> !n.isAssignment());

            if (!iterateChildren)
                return;

            boolean isFirst = true;
            for (TaxonomyNode child : node.getChildren())
            {
                if (child.getActual().isZero())
                    continue;

                if (isFirst)
                    buffer.append(",\"children\": ["); //$NON-NLS-1$
                else
                    buffer.append(","); //$NON-NLS-1$

                printNode(buffer, child, nodeColor, total, excludeSecurities);
                isFirst = false;
            }

            if (!isFirst)
                buffer.append("]"); //$NON-NLS-1$
        }
    }
}
