package name.abuchen.portfolio.ui.views;

import java.util.Iterator;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.Values;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
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
import de.engehausen.treemap.ISelectionChangeListener;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.IWeightedTreeModel;
import de.engehausen.treemap.impl.SquarifiedLayout;
import de.engehausen.treemap.swt.TreeMap;

class TreeMapViewer
{
    private SashForm sash;

    private TreeMapLegend legend;
    private TreeMap<TreeMapItem> treeMap;

    public TreeMapViewer(Composite parent, int style, Client client)
    {
        sash = new SashForm(parent, SWT.HORIZONTAL);
        sash.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Composite container = new Composite(sash, style);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        treeMap = new TreeMap<TreeMapItem>(container);
        treeMap.setTreeMapLayout(new SquarifiedLayout<TreeMapItem>(10));
        treeMap.setLabelProvider(new ILabelProvider<TreeMapItem>()
        {
            @Override
            public String getLabel(ITreeModel<IRectangle<TreeMapItem>> model, IRectangle<TreeMapItem> rectangle)
            {
                return rectangle.getNode().getLabel();
            }
        });

        legend = new TreeMapLegend(container, treeMap);

        final SecurityDetailsViewer details = new SecurityDetailsViewer(sash, SWT.NONE, client,
                        SecurityDetailsViewer.Facet.values());
        treeMap.addSelectionChangeListener(new ISelectionChangeListener<TreeMapItem>()
        {
            @Override
            public void selectionChanged(ITreeModel<IRectangle<TreeMapItem>> model, IRectangle<TreeMapItem> rectangle,
                            String label)
            {
                TreeMapItem node = rectangle.getNode();
                details.setInput(node.isSecurity() ? node.getSecurity() : null);
            }
        });

        // layout tree map + legend
        GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 10).applyTo(container);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(treeMap);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(legend);

        // layout sash
        details.getControl().pack();
        int width = details.getControl().getBounds().width;
        sash.setWeights(new int[] { parent.getParent().getParent().getBounds().width - width, width });
    }

    public void setInput(TreeMapItem rootItem)
    {
        ColorWheel colorWheel = new ColorWheel(treeMap, Math.max(10, rootItem.getChildren().size()));
        setInput(rootItem, colorWheel);
    }

    public void setInput(TreeMapItem rootItem, ColorWheel colorWheel)
    {
        TreeMapColorProvider colorProvider = new TreeMapColorProvider(colorWheel);
        treeMap.setRectangleRenderer(new ClassificationRectangleRenderer(colorProvider));
        treeMap.setTreeModel(new Model(rootItem));
        legend.setColorProvider(colorProvider);
        legend.setRootItem(rootItem);
    }

    public Control getControl()
    {
        return sash;
    }

    private static class Model implements IWeightedTreeModel<TreeMapItem>
    {
        private TreeMapItem root;

        public Model(TreeMapItem root)
        {
            this.root = root;
        }

        @Override
        public Iterator<TreeMapItem> getChildren(TreeMapItem item)
        {
            return item.getChildren().iterator();
        }

        @Override
        public TreeMapItem getParent(TreeMapItem item)
        {
            return item.getParent();
        }

        @Override
        public TreeMapItem getRoot()
        {
            return root;
        }

        @Override
        public boolean hasChildren(TreeMapItem item)
        {
            return !item.getChildren().isEmpty();
        }

        @Override
        public long getWeight(TreeMapItem item)
        {
            return item.getValuation();
        }
    }

    private class ClassificationRectangleRenderer implements IRectangleRenderer<TreeMapItem, PaintEvent, Color>
    {
        private TreeMapColorProvider colorProvider;

        public ClassificationRectangleRenderer(TreeMapColorProvider colorProvider)
        {
            this.colorProvider = colorProvider;
        }

        @Override
        public void render(final PaintEvent event, final ITreeModel<IRectangle<TreeMapItem>> model,
                        final IRectangle<TreeMapItem> rectangle,
                        final IColorProvider<TreeMapItem, Color> colorProvider,
                        final ILabelProvider<TreeMapItem> labelProvider)
        {
            TreeMapItem item = rectangle.getNode();

            if (item.isCategory())
                return;

            Color oldForeground = event.gc.getForeground();
            Color oldBackground = event.gc.getBackground();

            Rectangle r = new Rectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
            this.colorProvider.drawRectangle(item, event.gc, r);

            String label = item.toString();
            String info = String.format("%s (%s%%)", Values.Amount.format(item.getValuation()), //$NON-NLS-1$
                            Values.Percent.format(item.getPercentage()));

            event.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

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
        public void highlight(final PaintEvent event, final ITreeModel<IRectangle<TreeMapItem>> model,
                        final IRectangle<TreeMapItem> rectangle,
                        final IColorProvider<TreeMapItem, Color> colorProvider,
                        final ILabelProvider<TreeMapItem> labelProvider)
        {
            event.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            event.gc.drawRectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
        }

    }

}
