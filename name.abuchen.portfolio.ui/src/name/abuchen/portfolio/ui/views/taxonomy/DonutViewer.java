package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.inject.Inject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;

/* package */class DonutViewer extends AbstractChartPage
{
    private EmbeddedBrowser browser;

    @Inject
    public DonutViewer(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public Control createControl(Composite container)
    {
        browser = new EmbeddedBrowser("/META-INF/html/pie.html"); //$NON-NLS-1$
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
        private static final String ENTRY = "{\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"%s  %s  (%s)\"," //$NON-NLS-1$
                        + "\"valueLabel\":\"%s\"" //$NON-NLS-1$
                        + "}"; //$NON-NLS-1$

        private LoadDataFunction(Browser browser, String name)
        {
            super(browser, name);
        }

        @Override
        public Object function(Object[] arguments)
        {
            try
            {
                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                long total = getModel().getChartRenderingRootNode().getActual().getAmount();

                // classified nodes
                TaxonomyNode node = getModel().getClassificationRootNode();
                addChildren(joiner, node, total);
                
                // add unclassified if included
                if (!getModel().isUnassignedCategoryInChartsExcluded())
                {
                    TaxonomyNode unassigned = getModel().getUnassignedNode();
                    addChildren(joiner, unassigned, total);
                }
                
                return joiner.toString();
            }
            catch (Throwable e) // NOSONAR
            {
                PortfolioPlugin.log(e);
                return "{}"; //$NON-NLS-1$
            }
        }

        private void addChildren(StringJoiner joiner, TaxonomyNode node, long total)
        {
            for (TaxonomyNode child : node.getChildren())
            {
                if (child.getActual().isZero())
                    continue;

                if (child.isAssignment())
                {
                    addAssignment(joiner, child, node.getColor(), total);
                }
                else if (child.isClassification())
                {
                    List<TaxonomyNode> grandchildren = new ArrayList<>();
                    child.accept(n -> {
                        if (n.isAssignment())
                            grandchildren.add(n);
                    });
                    grandchildren.stream() //
                                    .filter(n -> !n.getActual().isZero()) //
                                    .sorted((l, r) -> Long.compare(r.getActual().getAmount(),
                                                    l.getActual().getAmount())) //
                                    .forEach(n -> addAssignment(joiner, n, child.getColor(), total));
                }
            }
        }

        private void addAssignment(StringJoiner joiner, TaxonomyNode node, String color, long total)
        {
            String name = StringEscapeUtils.escapeJson(node.getName());
            String percentage = Values.Percent2.format(node.getActual().getAmount() / (double) total);
            joiner.add(String.format(ENTRY, name, //
                            node.getActual().getAmount(), //
                            color, //
                            name, Values.Money.format(node.getActual()), percentage, //
                            percentage));

        }
    }
}
