package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import javax.inject.Inject;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
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
    {
    }

    @Override
    public void afterPage()
    {
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
        private static final String CAPTION_CLASSIFICATION = "%s  (%s)"; //$NON-NLS-1$
        private static final String CAPTION_CLASSIFIED = "<b>%s  %s  (%s)</b><br>%s"; //$NON-NLS-1$
        private static final String CAPTION_CLASSIFIED2 = "<b>%s  %s  (%s)</b><br>%s<br>%s"; //$NON-NLS-1$
        private static final String ENTRY = "{\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"%s\"," //$NON-NLS-1$
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
            String name = JSONObject.escape(node.getName());
            String percentage = Values.Percent2.format(node.getActual().getAmount() / (double) total);
            // get classification nodes
            TaxonomyNode taxn = getClassificationNode(node);
            TaxonomyNode taxRoot = getRootClassificationNode(node);
            // get classification caption for root node
            String captionClassificationRoot = formatClassification(taxRoot, total);
            // detect no classification
            if (captionClassificationRoot == null)
            {
                captionClassificationRoot = Messages.LabelWithoutClassification;
            }
            String captionClassification = null;
            // only use another entry if classification node is not the root of
            // the classification
            if (!Objects.equals(taxRoot, taxn))
            {
                captionClassification = formatClassification(taxn, total);
            }
            // build caption depending on available nodes
            String caption;
            if (captionClassification != null)
            {
                caption = String.format(CAPTION_CLASSIFIED2, name, Values.Money.format(node.getActual()), percentage, //
                                captionClassification, captionClassificationRoot);
            }
            else
            {
                caption = String.format(CAPTION_CLASSIFIED, name, Values.Money.format(node.getActual()), percentage, //
                                captionClassificationRoot);
            }
            joiner.add(String.format(ENTRY, name, //
                            node.getActual().getAmount(), //
                            color, //
                            caption, //
                            percentage));
        }

        /**
         * Format the {@link Classification} of the given {@link TaxonomyNode}
         * to a {@link String}.
         * 
         * @param taxn
         *            {@link TaxonomyNode} (can be null)
         * @param total
         *            total amount
         * @return {@link String} on success, else null
         */
        private String formatClassification(TaxonomyNode taxn, double total)
        {
            // check if node is valid
            if ((taxn != null) && (taxn.getParent() != null) && !taxn.getParent().isRoot())
            {
                // check if it actually has a classification
                Classification cl = taxn.getClassification();
                if (cl != null)
                {
                    return String.format(CAPTION_CLASSIFICATION, cl.getPathName(false),
                                    Values.Percent2.format(taxn.getActual().getAmount() / total));
                }
            }
            return null;
        }

        /**
         * Gets the {@link Classification} node for the given node (or from its
         * parents if the node has none).
         * 
         * @param node
         *            {@link TaxonomyNode}
         * @return {@link Classification} on success, else null
         */
        private TaxonomyNode getClassificationNode(TaxonomyNode node)
        {
            // first try to get classification from the current node
            Classification cl = node.getClassification();
            if (cl != null)
                return node;
            // then try the parent node
            TaxonomyNode parent = node.getParent();
            if (parent != null)
                return getClassificationNode(parent);
            return null;
        }

        /**
         * Gets the root {@link Classification} node for the given node.
         * 
         * @param node
         *            {@link TaxonomyNode}
         * @return {@link Classification} on success, else null
         */
        private TaxonomyNode getRootClassificationNode(TaxonomyNode node)
        {
            // check if node has a parent
            TaxonomyNode parent = node.getParent();
            boolean parentIsRoot = false;
            if ((parent != null))
            {
                if (!parent.isRoot())
                {
                    // try to walk further up
                    TaxonomyNode tn = getRootClassificationNode(parent);
                    if (tn != null)
                        return tn;
                }
                else
                {
                    parentIsRoot = true;
                }
            }
            // then try current node
            if (!parentIsRoot && (node.getClassification() != null))
                return node;
            return null;
        }

    }
}
