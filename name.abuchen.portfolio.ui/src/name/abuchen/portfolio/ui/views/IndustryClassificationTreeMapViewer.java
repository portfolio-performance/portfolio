package name.abuchen.portfolio.ui.views;

import java.util.Iterator;

import name.abuchen.portfolio.model.Values;
import name.abuchen.portfolio.ui.views.IndustryClassificationView.Item;

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

class IndustryClassificationTreeMapViewer
{
    private SashForm sash;

    private TreeMapLegend legend;
    private TreeMap<Item> treeMap;

    public IndustryClassificationTreeMapViewer(Composite parent, int style)
    {
        sash = new SashForm(parent, SWT.HORIZONTAL);
        sash.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        Composite container = new Composite(sash, style);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        treeMap = new TreeMap<Item>(container);
        treeMap.setTreeMapLayout(new SquarifiedLayout<Item>(10));
        treeMap.setLabelProvider(new ILabelProvider<Item>()
        {
            @Override
            public String getLabel(ITreeModel<IRectangle<Item>> model, IRectangle<Item> rectangle)
            {
                Item node = rectangle.getNode();

                if (node.isCategory())
                    return node.getCategory().getPathLabel();
                else if (node.isSecurity())
                    return node.getSecurity().getName();
                else if (node.isAccount())
                    return node.getAccount().getName();
                else
                    return ""; //$NON-NLS-1$
            }
        });

        legend = new TreeMapLegend(container, treeMap);

        final SecurityDetailsViewer details = new SecurityDetailsViewer(sash, SWT.NONE,
                        SecurityDetailsViewer.Facet.values());
        treeMap.addSelectionChangeListener(new ISelectionChangeListener<Item>()
        {
            @Override
            public void selectionChanged(ITreeModel<IRectangle<Item>> model, IRectangle<Item> rectangle, String label)
            {
                Item node = rectangle.getNode();
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

    public void setInput(Item rootItem)
    {
        TreeMapColorProvider colorProvider = new TreeMapColorProvider(treeMap, Math.max(10, rootItem.getChildren()
                        .size()));
        treeMap.setRectangleRenderer(new ClassificationRectangleRenderer(colorProvider));
        treeMap.setTreeModel(new Model(rootItem));
        legend.setColorProvider(colorProvider);
        legend.setRootItem(rootItem);
    }

    public Control getControl()
    {
        return sash;
    }

    private static class Model implements IWeightedTreeModel<Item>
    {
        private Item root;

        public Model(Item root)
        {
            this.root = root;
        }

        @Override
        public Iterator<Item> getChildren(Item item)
        {
            return item.getChildren().iterator();
        }

        @Override
        public Item getParent(Item item)
        {
            return item.getParent();
        }

        @Override
        public Item getRoot()
        {
            return root;
        }

        @Override
        public boolean hasChildren(Item item)
        {
            return !item.getChildren().isEmpty();
        }

        @Override
        public long getWeight(Item item)
        {
            return item.getValuation();
        }
    }

    private class ClassificationRectangleRenderer implements IRectangleRenderer<Item, PaintEvent, Color>
    {
        private TreeMapColorProvider colorProvider;

        public ClassificationRectangleRenderer(TreeMapColorProvider colorProvider)
        {
            this.colorProvider = colorProvider;
        }

        @Override
        public void render(final PaintEvent event, final ITreeModel<IRectangle<Item>> model,
                        final IRectangle<Item> rectangle, final IColorProvider<Item, Color> colorProvider,
                        final ILabelProvider<Item> labelProvider)
        {
            Item item = rectangle.getNode();

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
        public void highlight(final PaintEvent event, final ITreeModel<IRectangle<Item>> model,
                        final IRectangle<Item> rectangle, final IColorProvider<Item, Color> colorProvider,
                        final ILabelProvider<Item> labelProvider)
        {
            event.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            event.gc.drawRectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
        }

    }

}
