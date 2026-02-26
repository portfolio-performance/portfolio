package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.engehausen.treemap.swt.TreeMap;

/* package */class TreeMapLegend extends Composite
{
    private TaxonomyModel model;
    private TaxonomyNodeRenderer renderer;

    public TreeMapLegend(Composite parent, TreeMap<TaxonomyNode> treeMap, TaxonomyModel model,
                    TaxonomyNodeRenderer renderer)
    {
        super(parent, SWT.NONE);

        this.model = model;
        this.renderer = renderer;

        setBackground(parent.getBackground());

        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.wrap = false;
        layout.pack = true;
        layout.justify = false;
        setLayout(layout);

        treeMap.addSelectionChangeListener((treeModel, rectangle, label) -> setNode(rectangle.getNode()));
    }

    public void setNode(TaxonomyNode node)
    {
        for (Control control : this.getChildren())
            control.dispose();

        TaxonomyNode root = model.getChartRenderingRootNode();

        boolean hasParent = false;
        List<TaxonomyNode> path = node.getPath();
        for (int ii = 0; ii < path.size(); ii++)
        {
            TaxonomyNode item = path.get(ii);

            if (!hasParent && !item.equals(root))
                continue;

            hasParent = true;
            new LegendItem(this, item);
        }

        pack();
        getParent().layout();
    }

    public class LegendItem extends Canvas implements Listener
    {
        private final TaxonomyNode item;

        public LegendItem(Composite parent, TaxonomyNode item)
        {
            super(parent, SWT.NO_BACKGROUND);

            this.item = item;

            addListener(SWT.Paint, this);
            addListener(SWT.Resize, this);
        }

        @Override
        public void handleEvent(Event event)
        {
            switch (event.type)
            {
                case SWT.Paint:
                    paintControl(event);
                    break;
                case SWT.Resize:
                    redraw();
                    break;
                default:
                    break;
            }
        }

        private void paintControl(Event e)
        {
            Color oldForeground = e.gc.getForeground();
            Color oldBackground = e.gc.getBackground();

            Point size = getSize();
            Rectangle r = new Rectangle(0, 0, size.x, size.y);

            renderer.drawRectangle(null, item, e.gc, r);

            e.gc.setForeground(oldForeground);
            e.gc.setBackground(oldBackground);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            var label = renderer.getLabel(item);

            GC gc = new GC(this);
            var width = 0;
            var height = 0;
            for (int ii = 0; ii < label.length; ii++)
            {
                Point extent = gc.textExtent(label[ii]);
                if (extent.x > width)
                    width = extent.x;
                height += extent.y;
            }
            gc.dispose();

            return new Point(width + 4, height + 4);
        }
    }
}
