package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.Colors;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.ISelectionChangeListener;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.swt.TreeMap;

/* package */class TreeMapLegend extends Composite
{
    private TaxonomyModel model;
    private TaxonomyNodeRenderer renderer;

    private TaxonomyNode rootItem;

    public TreeMapLegend(Composite parent, TreeMap<TaxonomyNode> treeMap, TaxonomyModel model,
                    TaxonomyNodeRenderer renderer)
    {
        super(parent, SWT.NONE);

        this.model = model;
        this.renderer = renderer;

        setBackground(parent.getBackground());

        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.wrap = true;
        layout.pack = true;
        layout.justify = false;
        setLayout(layout);

        treeMap.addSelectionChangeListener(new ISelectionChangeListener<TaxonomyNode>()
        {
            @Override
            public void selectionChanged(ITreeModel<IRectangle<TaxonomyNode>> model,
                            IRectangle<TaxonomyNode> rectangle, String label)
            {
                TreeMapLegend.this.selectionChanged(model);
            }
        });
    }

    public void setRootItem(TaxonomyNode rootItem)
    {
        this.rootItem = rootItem;

        for (Control control : this.getChildren())
            control.dispose();

        boolean hasParent = false;

        List<TaxonomyNode> path = rootItem.getPath();
        for (int ii = 1; ii < path.size(); ii++)
        {
            hasParent = true;
            TaxonomyNode item = path.get(ii);
            new LegendItem(this, item);
        }

        if (hasParent && !rootItem.getChildren().isEmpty())
        {
            Label l = new Label(this, SWT.NONE);
            l.setText(">>"); //$NON-NLS-1$
            l.setBackground(this.getBackground());
        }

        List<TaxonomyNode> children = new ArrayList<TaxonomyNode>(rootItem.getChildren());
        Collections.sort(children, new Comparator<TaxonomyNode>()
        {
            @Override
            public int compare(TaxonomyNode o1, TaxonomyNode o2)
            {
                long v1 = o1.getActual().getAmount();
                long v2 = o2.getActual().getAmount();
                return v1 > v2 ? -1 : v1 == v2 ? 0 : 1;
            }
        });
        for (TaxonomyNode child : children)
        {
            if (model.isUnassignedCategoryInChartsExcluded() && child.isUnassignedCategory())
                continue;
            new LegendItem(this, child);
        }

        pack();
        getParent().layout();
    }

    private void selectionChanged(ITreeModel<IRectangle<TaxonomyNode>> model)
    {
        // find out if root changed (drill-down)
        TaxonomyNode newRoot = model.getRoot().getNode();
        if (!newRoot.equals(rootItem))
            setRootItem(newRoot);
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

            renderer.drawRectangle(rootItem, item, e.gc, r);

            String text = item.getName();
            String info = getInfo();

            GC gc = e.gc;
            gc.setForeground(Colors.getTextColor(gc.getBackground()));
            gc.drawString(text, 2, 2, true);
            Point extent = gc.stringExtent(text);
            gc.drawString(info, 2, extent.y + 1, true);

            e.gc.setForeground(oldForeground);
            e.gc.setBackground(oldBackground);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            String text = item.getName();
            String info = getInfo();

            GC gc = new GC(this);
            Point extentText = gc.stringExtent(text);
            Point extentInfo = gc.stringExtent(info);
            gc.dispose();

            return new Point(Math.max(extentText.x, extentInfo.x) + 4, extentText.y + extentInfo.y + 4);
        }

        private String getInfo()
        {
            return String.format("%s (%s%%)", Values.Money.format(item.getActual()), //$NON-NLS-1$
                            Values.Percent.format((double) item.getActual().getAmount()
                                            / (double) model.getRootNode().getActual().getAmount()));
        }

    }
}
