package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.Objects;
import java.util.StringJoiner;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.json.simple.JSONObject;

import name.abuchen.portfolio.model.Classification;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.snapshot.ClientSnapshot;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser.ItemSelectedFunction;
import name.abuchen.portfolio.ui.views.IPieChart;

public class TaxonomyDonutBrowser implements IPieChart
{
    private EmbeddedBrowser browser;
    private DonutChartBuilder builder = new DonutChartBuilder();
    private DonutViewer chartPage;
    private AbstractFinanceView view;

    public TaxonomyDonutBrowser(EmbeddedBrowser browser, DonutViewer page, AbstractFinanceView view)
    {
        this.browser = browser;
        this.chartPage = page;
        this.view = view;
    }

    @Override
    public Control createControl(Composite parent)
    {
        browser.setHtmlpage("/META-INF/html/pie.html"); //$NON-NLS-1$
        return browser.createControl(parent, LoadDataFunction::new, b -> new ItemSelectedFunction(b, uuid -> getModel()
                        .getVirtualRootNode().getNodeById(uuid).ifPresent(n -> view.setInformationPaneInput(n))));

    }

    @Override
    public void refresh(ClientSnapshot snapshot)
    {
        browser.refresh();
    }

    private final class LoadDataFunction extends BrowserFunction
    {
        private static final String CAPTION_CLASSIFICATION = "%s  (%s)"; //$NON-NLS-1$
        private static final String CAPTION_CLASSIFIED = "<b>%s  %s  (%s)</b><br>%s"; //$NON-NLS-1$
        private static final String CAPTION_CLASSIFIED2 = "<b>%s  %s  (%s)</b><br>%s<br>%s"; //$NON-NLS-1$
        private static final String ENTRY = "{\"uuid\":\"%s\"," //$NON-NLS-1$
                        + "\"label\":\"%s\"," //$NON-NLS-1$
                        + "\"value\":%s," //$NON-NLS-1$
                        + "\"color\":\"%s\"," //$NON-NLS-1$
                        + "\"caption\":\"%s\"," //$NON-NLS-1$
                        + "\"valueLabel\":\"%s\"" //$NON-NLS-1$
                        + "}"; //$NON-NLS-1$

        private LoadDataFunction(Browser browser)
        {
            super(browser, "loadData"); //$NON-NLS-1$
        }

        @Override
        public Object function(Object[] arguments)
        {
            try
            {
                StringJoiner joiner = new StringJoiner(",", "[", "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                long total = getModel().getChartRenderingRootNode().getActual().getAmount();

                builder.computeNodeList(getModel()).forEach(
                                pair -> addAssignment(joiner, pair.getRight(), pair.getLeft().getColor(), total));

                return joiner.toString();
            }
            catch (Throwable e) // NOSONAR
            {
                PortfolioPlugin.log(e);
                return "{}"; //$NON-NLS-1$
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
            joiner.add(String.format(ENTRY, node.getId(), name, //
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

    private TaxonomyModel getModel()
    {
        return chartPage.getModel();
    }
}
