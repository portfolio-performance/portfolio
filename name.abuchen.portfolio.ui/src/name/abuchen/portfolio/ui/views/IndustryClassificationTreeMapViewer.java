package name.abuchen.portfolio.ui.views;

import java.util.Iterator;
import java.util.List;

import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.IndustryClassificationView.Item;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
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

class IndustryClassificationTreeMapViewer
{
    private Composite container;
    private TreeMap<Item> treeMap;

    public IndustryClassificationTreeMapViewer(Composite parent, int style)
    {
        container = new Composite(parent, style);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        FillLayout layout = new FillLayout();
        layout.marginHeight = 10;
        layout.marginWidth = 10;
        container.setLayout(layout);

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
    }

    public void setInput(Item rootItem)
    {
        treeMap.setRectangleRenderer(new ClassificationRectangleRenderer(rootItem));
        treeMap.setTreeModel(new Model(rootItem));
    }

    public Control getControl()
    {
        return container;
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
        private ColorWheel colorWheel;

        public ClassificationRectangleRenderer(Item rootItem)
        {
            colorWheel = new ColorWheel(container, Math.max(10, rootItem.getChildren().size()));
        }

        private ColorWheel.Segment getSegment(ITreeModel<IRectangle<Item>> model, IRectangle<Item> rectangle)
        {
            Item item = rectangle.getNode();

            if (item.isCategory())
                return colorWheel.new Segment(Colors.HEADINGS.swt());
            else if (item.isAccount())
                return colorWheel.new Segment(Colors.CASH.swt());
            else if (model.getParent(rectangle) == null)
                return colorWheel.new Segment(Colors.valueOf(item.getSecurity().getType().name()).swt());

            List<Item> path = item.getPath();
            int index = path.get(0).getChildren().indexOf(path.get(1));
            return colorWheel.getSegment(index);
        }

        @Override
        public void render(final PaintEvent event, final ITreeModel<IRectangle<Item>> model,
                        final IRectangle<Item> rectangle, final IColorProvider<Item, Color> colorProvider,
                        final ILabelProvider<Item> labelProvider)
        {
            ColorWheel.Segment segment = getSegment(model, rectangle);

            Color oldForeground = event.gc.getForeground();
            Color oldBackground = event.gc.getBackground();

            event.gc.setBackground(segment.getColor());
            event.gc.fillRectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());

            event.gc.setForeground(segment.getDarkerColor());
            event.gc.drawLine(rectangle.getX(), rectangle.getY() + rectangle.getHeight() - 1, rectangle.getX()
                            + rectangle.getWidth() - 1, rectangle.getY() + rectangle.getHeight() - 1);
            event.gc.drawLine(rectangle.getX() + rectangle.getWidth() - 1, rectangle.getY(), rectangle.getX()
                            + rectangle.getWidth() - 1, rectangle.getY() + rectangle.getHeight() - 1);

            event.gc.setForeground(segment.getBrigherColor());
            event.gc.drawLine(rectangle.getX(), rectangle.getY(), rectangle.getX() + rectangle.getWidth(),
                            rectangle.getY());
            event.gc.drawLine(rectangle.getX(), rectangle.getY(), rectangle.getX(),
                            rectangle.getY() + rectangle.getHeight());

            String label = labelProvider.getLabel(model, rectangle);
            if (label != null)
            {
                event.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
                event.gc.drawText(label, rectangle.getX() + 1, rectangle.getY() + 1);
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
