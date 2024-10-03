package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.Iterator;

import jakarta.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import de.engehausen.treemap.IColorProvider;
import de.engehausen.treemap.ILabelProvider;
import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.IRectangleRenderer;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.IWeightedTreeModel;
import de.engehausen.treemap.impl.SquarifiedLayout;
import de.engehausen.treemap.swt.TreeMap;
import name.abuchen.portfolio.model.ClientProperties;
import name.abuchen.portfolio.model.SecurityNameConfig;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.SecurityDetailsViewer;

/* package */class TreeMapViewer extends AbstractChartPage
{
    private final AbstractFinanceView view;
    private TreeMap<TaxonomyNode> treeMap;
    private TreeMapLegend legend;

    private TaxonomyNode selectedNode;

    @Inject
    public TreeMapViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
        this.view = view;
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
        treeMap.setTreeModel(new Model(getModel()));
        legend.setRootItem(getModel().getVirtualRootNode());
    }

    @Override
    public Control createControl(Composite parent)
    {
        Composite sash = new Composite(parent, SWT.NONE);
        sash.setLayout(new SashLayout(sash, SWT.HORIZONTAL | SWT.END));

        Composite container = new Composite(sash, SWT.NONE);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        treeMap = new TreeMap<>(container);
        treeMap.setTreeMapLayout(new SquarifiedLayout<TaxonomyNode>(10));
        treeMap.setLabelProvider((model, rectangle) -> rectangle.getNode().getName());

        legend = new TreeMapLegend(container, treeMap, getModel(), getRenderer());

        final SecurityDetailsViewer details = new SecurityDetailsViewer(sash, SWT.NONE, getModel().getClient(), true);
        treeMap.addSelectionChangeListener((model, rectangle, label) -> {
            selectedNode = rectangle.getNode();
            details.setInput(selectedNode.getBackingSecurity());
        });

        treeMap.addMouseListener(MouseListener.mouseUpAdapter(e -> view.setInformationPaneInput(selectedNode)));

        // layout tree map + legend
        GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).applyTo(container);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(treeMap);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(legend);

        // layout sash
        details.getControl().setLayoutData(new SashLayoutData(SWTHelper.getPackedWidth(details.getControl())));

        treeMap.setRectangleRenderer(new ClassificationRectangleRenderer(getModel(), getRenderer()));
        treeMap.setTreeModel(new Model(getModel()));
        legend.setRootItem(getModel().getChartRenderingRootNode());

        return sash;
    }

    private static class Model implements IWeightedTreeModel<TaxonomyNode>
    {
        private TaxonomyModel model;

        public Model(TaxonomyModel model)
        {
            this.model = model;
        }

        @Override
        public Iterator<TaxonomyNode> getChildren(TaxonomyNode item)
        {
            return item.getChildren().iterator();
        }

        @Override
        public TaxonomyNode getParent(TaxonomyNode item)
        {
            if (getRoot().equals(item))
                return null;
            else
                return item.getParent();
        }

        @Override
        public TaxonomyNode getRoot()
        {
            return model.getChartRenderingRootNode();
        }

        @Override
        public boolean hasChildren(TaxonomyNode item)
        {
            return !item.getChildren().isEmpty();
        }

        @Override
        public long getWeight(TaxonomyNode item)
        {
            return item.getActual().getAmount();
        }
    }

    private static class ClassificationRectangleRenderer implements IRectangleRenderer<TaxonomyNode, PaintEvent, Color>
    {
        private TaxonomyModel model;
        private TaxonomyNodeRenderer colorProvider;
        private SecurityNameConfig nameConfig;

        public ClassificationRectangleRenderer(TaxonomyModel model, TaxonomyNodeRenderer colorProvider)
        {
            this.model = model;
            this.colorProvider = colorProvider;
            this.nameConfig = new ClientProperties(model.getClient()).getSecurityNameConfig();
        }

        @Override
        public void render(final PaintEvent event, final ITreeModel<IRectangle<TaxonomyNode>> model,
                        final IRectangle<TaxonomyNode> rectangle,
                        final IColorProvider<TaxonomyNode, Color> colorProvider,
                        final ILabelProvider<TaxonomyNode> labelProvider)
        {
            TaxonomyNode item = rectangle.getNode();

            if (item.getClassification() != null)
                return;

            Color oldForeground = event.gc.getForeground();
            Color oldBackground = event.gc.getBackground();

            Rectangle r = new Rectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                            rectangle.getHeight());
            this.colorProvider.drawRectangle(model.getRoot().getNode(), item, event.gc, r);

            String label = item.getBackingSecurity() != null ? item.getBackingSecurity().getName(nameConfig)
                            : item.getName();

            double total = this.model.getChartRenderingRootNode().getActual().getAmount();

            String info = String.format("%s (%s%%)", Values.Money.format(item.getActual()), //$NON-NLS-1$
                            Values.Percent.format(item.getActual().getAmount() / total));

            event.gc.setForeground(Colors.getTextColor(event.gc.getBackground()));

            Point labelExtend = event.gc.textExtent(label);
            Point infoExtend = event.gc.textExtent(info);

            int width = Math.max(labelExtend.x, infoExtend.x);
            if (width <= rectangle.getWidth() || rectangle.getWidth() > rectangle.getHeight())
            {
                event.gc.drawText(label, r.x + 2, r.y + 2, true);
                event.gc.drawText(info, r.x + 2, r.y + 2 + labelExtend.y, true);
            }
            else
            {
                final Transform transform = new Transform(event.display);
                try
                {
                    transform.translate(r.x, r.y);
                    transform.rotate(-90);
                    event.gc.setTransform(transform);
                    event.gc.drawString(label, -labelExtend.x - 2, 2, true);
                    event.gc.drawString(info, -infoExtend.x - 2, 2 + labelExtend.y, true);
                }
                finally
                {
                    transform.dispose();
                }
            }

            event.gc.setForeground(oldForeground);
            event.gc.setBackground(oldBackground);
        }

        @Override
        public void highlight(final PaintEvent event, final ITreeModel<IRectangle<TaxonomyNode>> model,
                        final IRectangle<TaxonomyNode> rectangle,
                        final IColorProvider<TaxonomyNode, Color> colorProvider,
                        final ILabelProvider<TaxonomyNode> labelProvider)
        {
            event.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            event.gc.drawRectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
        }

    }
}
