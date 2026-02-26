package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.Iterator;

import jakarta.inject.Inject;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import de.engehausen.treemap.ICancelable;
import de.engehausen.treemap.IColorProvider;
import de.engehausen.treemap.ILabelProvider;
import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.IRectangleRenderer;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.IWeightedTreeModel;
import de.engehausen.treemap.impl.RectangleImpl;
import de.engehausen.treemap.impl.SquarifiedLayout;
import de.engehausen.treemap.swt.TreeMap;
import name.abuchen.portfolio.snapshot.ReportingPeriod;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.LabelOnly;
import name.abuchen.portfolio.ui.util.SWTHelper;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.util.swt.ColorSchema;
import name.abuchen.portfolio.ui.util.swt.SashLayout;
import name.abuchen.portfolio.ui.util.swt.SashLayoutData;
import name.abuchen.portfolio.ui.views.SecurityDetailsViewer;
import name.abuchen.portfolio.ui.views.taxonomy.TaxonomyNodeRenderer.PerformanceNodeRenderer;

/* package */class TreeMapViewer extends AbstractChartPage
{
    /**
     * Extend TreeMap to allow for update the image when the coloring changes.
     * The alternative would be to recalculate the tree map, however, that
     * resets the selection to the root node.
     */
    static class XTreeMap extends TreeMap<TaxonomyNode>
    {
        public XTreeMap(Composite composite)
        {
            super(composite);
        }

        protected void recolor()
        {
            var bounds = getBounds();
            this.rebuildImage(bounds.width, bounds.height, rectangles);
            redraw();
            update();
        }

        @Override
        protected boolean selectRectangle(final int x, final int y)
        {
            // We must override the selectRectangle method because if we show
            // headlines, it is possible to select the classification. However,
            // if the classification is selected (say "Asset Allocation") all
            // future mouse movements are within that classification. Then the
            // UI seems to freeze.

            // solution: only allow to select leaf notes (=instruments)

            if (selected == null || !selected.contains(x, y))
            {
                var rectangle = findRectangle(x, y);
                selected = rectangle == null || rectangle.getNode().isClassification() ? null : rectangle;

                redraw();
                if (selected != null && listeners != null)
                {
                    for (int i = listeners.size() - 1; i >= 0; i--)
                    {
                        listeners.get(i).selectionChanged(rectangles, selected, null);
                    }
                }
                return false;
            }
            else
            {
                return true;
            }
        }

        @Override
        protected void rebuildImage(final int width, final int height, final ITreeModel<IRectangle<TaxonomyNode>> rects)
        {
            // We must override the rebuildImage method because must propagate
            // the styled font into the graphics context created for drawing the
            // image

            if (width * height > 0)
            {
                final Display d = getDisplay();
                final Image result = new Image(d, width, height);
                final GC gc = new GC(result);
                gc.setFont(this.getFont()); // inject the font
                final Event synth = new Event();
                synth.widget = this;
                synth.display = d;
                synth.gc = gc;
                synth.x = 0;
                synth.y = 0;
                synth.width = width;
                synth.height = height;
                final PaintEvent event = new PaintEvent(synth);
                try
                {
                    render(event, rects);
                }
                finally
                {
                    gc.dispose();
                }
                setImage(result);
            }
        }
    }

    static class BorderSquarifiedLayout extends SquarifiedLayout<TaxonomyNode>
    {
        private static final long serialVersionUID = 1L;
        private static final ThreadLocal<TaxonomyNode> startNodeThreadLocal = new ThreadLocal<>();

        private int headingHeight;

        public BorderSquarifiedLayout(int headingHeight)
        {
            super(10);

            this.headingHeight = headingHeight;
        }

        @Override
        public ITreeModel<IRectangle<TaxonomyNode>> layout(IWeightedTreeModel<TaxonomyNode> model,
                        TaxonomyNode startNode, int width, int height, ICancelable cancelable)
        {
            try
            {
                startNodeThreadLocal.set(startNode);
                return super.layout(model, startNode, width, height, cancelable);
            }
            finally
            {
                startNodeThreadLocal.remove();
            }
        }

        @Override
        protected RectangleImpl<TaxonomyNode> createRectangle(final TaxonomyNode n, final int x, final int y,
                        final int w, final int h)
        {
            var startNode = startNodeThreadLocal.get();

            // no border around instruments
            if (n.getClassification() == null)
                return new RectangleImpl<>(n, x, y, w, h);

            if (startNode != n.getParent())
                return new RectangleImpl<>(n, x, y, w, h);

            var shrink = 1;

            final int nw = w - 2 * shrink;
            final int nh = h - shrink - headingHeight;
            if (nw > 0 && nh > 0)
            {
                return new RectangleImpl<>(n, x + shrink, y + headingHeight, nw, nh);
            }
            else
            {
                return null;
            }
        }
    }

    private static final String COLORING_STRATEGY_TTWROR = "ttwror:"; //$NON-NLS-1$

    private final IStylingEngine stylingEngine;
    private final AbstractFinanceView view;
    private XTreeMap treeMap;
    private TreeMapLegend legend;

    private PerformanceNodeRenderer selectedRenderer;
    private ColorSchema colorSchema = ColorSchema.BLUE_GRAY_ORANGE;

    private TaxonomyNode selectedNode;

    @Inject
    public TreeMapViewer(IStylingEngine stylingEngine, AbstractFinanceView view, TaxonomyModel model,
                    TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
        this.stylingEngine = stylingEngine;
        this.view = view;

        // first configure the color schema so that it is available when setting
        // up the coloring strategy
        String schema = model.getColorSchemaInTreeMap();
        if (schema != null && !schema.isBlank())
        {
            try
            {
                this.colorSchema = ColorSchema.valueOf(schema);
            }
            catch (IllegalArgumentException ignore)
            {
                // ignore unknown color schema
            }
        }

        // setup coloring strategy

        // as we support only "by classification" (the default) and "by ttwror",
        // the preference must be of value "ttwror:<reporting period code>"

        var coloringStrategy = model.getColoringStrategy();
        if (coloringStrategy != null && coloringStrategy.startsWith(COLORING_STRATEGY_TTWROR))
        {
            ReportingPeriod.tryFrom(coloringStrategy.substring(COLORING_STRATEGY_TTWROR.length())).ifPresent(period -> {
                selectedRenderer = new PerformanceNodeRenderer(model, renderer.resources, period);
                selectedRenderer.setColorSchema(colorSchema);
            });
        }
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
        legend.setNode(getModel().getVirtualRootNode());
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        manager.add(new LabelOnly(Messages.LabelColorBy));

        Action action = new SimpleAction(Messages.ColumnTaxonomy, a -> {
            getModel().setColoringStrategy(""); //$NON-NLS-1$
            selectedRenderer = null;
            treeMap.recolor();
        });
        action.setChecked(selectedRenderer == null);
        manager.add(action);

        var twrSubMenuManager = new MenuManager(Messages.LabelTTWROR);
        manager.add(twrSubMenuManager);

        // display the first 10 elements in the menu directly
        var limit = 25;
        var reportingPeriods = view.getPart().getClientInput().getReportingPeriods();
        reportingPeriods.stream().limit(limit).forEach(period -> {
            Action byPeriod = new SimpleAction(period.toString(), a -> {
                getModel().setColoringStrategy(COLORING_STRATEGY_TTWROR + period.getCode());
                selectedRenderer = new PerformanceNodeRenderer(getModel(), getRenderer().resources, period);
                selectedRenderer.setColorSchema(colorSchema);
                treeMap.recolor();
            });
            byPeriod.setChecked(selectedRenderer != null && selectedRenderer.getReportingPeriod().equals(period));
            twrSubMenuManager.add(byPeriod);
        });

        if (reportingPeriods.size() > limit)
        {
            var subMenu = new MenuManager(Messages.LabelMore);
            twrSubMenuManager.add(subMenu);
            reportingPeriods.stream().skip(limit).forEach(period -> {
                Action byPeriod = new SimpleAction(period.toString(), a -> {
                    getModel().setColoringStrategy(COLORING_STRATEGY_TTWROR + period.getCode());
                    selectedRenderer = new PerformanceNodeRenderer(getModel(), getRenderer().resources, period);
                    selectedRenderer.setColorSchema(colorSchema);
                    treeMap.recolor();
                });
                byPeriod.setChecked(selectedRenderer != null && selectedRenderer.getReportingPeriod().equals(period));
                subMenu.add(byPeriod);
            });
        }

        manager.add(new Separator());

        Action headlineSchema = new SimpleAction(Messages.LabelShowHeadline, a -> {
            getModel().setShowGroupHeadingInTreeMap(!getModel().doShowGroupHeadingInTreeMap());
            treeMap.setTreeMapLayout(getModel().doShowGroupHeadingInTreeMap()
                            ? new BorderSquarifiedLayout(treeMap.getFont().getFontData()[0].getHeight())
                            : new SquarifiedLayout<>(10));
            treeMap.setTreeModel(new Model(getModel()));
        });
        headlineSchema.setChecked(getModel().doShowGroupHeadingInTreeMap());
        manager.add(headlineSchema);

        var subMenu = new MenuManager(Messages.LabelColorSchema);
        manager.add(subMenu);
        for (var schema : ColorSchema.values())
        {
            Action actionSchema = new SimpleAction(schema.toString(), a -> {
                colorSchema = schema;
                getModel().setColorSchemaInTreeMap(colorSchema.name());
                if (selectedRenderer != null)
                    selectedRenderer.setColorSchema(colorSchema);
                treeMap.recolor();
            });
            actionSchema.setChecked(colorSchema == schema);
            subMenu.add(actionSchema);
        }

        super.configMenuAboutToShow(manager);
    }

    @Override
    public Control createControl(Composite parent)
    {
        Composite sash = new Composite(parent, SWT.NONE);
        sash.setLayout(new SashLayout(sash, SWT.HORIZONTAL | SWT.END));

        Composite container = new Composite(sash, SWT.NONE);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        treeMap = new XTreeMap(container);

        // make sure the tree map is styled with the correct font size used by
        // the tree map layout
        stylingEngine.style(treeMap);
        treeMap.setTreeMapLayout(getModel().doShowGroupHeadingInTreeMap()
                        ? new BorderSquarifiedLayout(treeMap.getFont().getFontData()[0].getHeight())
                        : new SquarifiedLayout<>(10));

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

        treeMap.setRectangleRenderer(new ClassificationRectangleRenderer());
        treeMap.setTreeModel(new Model(getModel()));
        legend.setNode(getModel().getChartRenderingRootNode());

        return sash;
    }

    private static class Model implements IWeightedTreeModel<TaxonomyNode>
    {
        private TaxonomyModel taxonomyModel;

        public Model(TaxonomyModel model)
        {
            this.taxonomyModel = model;
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
            return taxonomyModel.getChartRenderingRootNode();
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

    private class ClassificationRectangleRenderer implements IRectangleRenderer<TaxonomyNode, PaintEvent, Color>
    {
        @Override
        public void render(final PaintEvent event, final ITreeModel<IRectangle<TaxonomyNode>> model,
                        final IRectangle<TaxonomyNode> rectangle,
                        final IColorProvider<TaxonomyNode, Color> colorProvider,
                        final ILabelProvider<TaxonomyNode> labelProvider)
        {
            TaxonomyNode item = rectangle.getNode();

            if (item.getClassification() != null)
            {
                // draw the classification name only for the first level (not
                // the root, not below)

                if (getModel().doShowGroupHeadingInTreeMap() && item.getParent() == model.getRoot().getNode())
                {
                    var textExtend = event.gc.textExtent(item.getName());
                    event.gc.drawText(item.getName(), rectangle.getX() + 2, rectangle.getY() - textExtend.y, true);
                }

                return;
            }

            Color oldForeground = event.gc.getForeground();
            Color oldBackground = event.gc.getBackground();

            Rectangle r = new Rectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
                            rectangle.getHeight());

            var coloring = selectedRenderer != null ? selectedRenderer : getRenderer();
            coloring.drawRectangle(model.getRoot().getNode(), item, event.gc, r);

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
