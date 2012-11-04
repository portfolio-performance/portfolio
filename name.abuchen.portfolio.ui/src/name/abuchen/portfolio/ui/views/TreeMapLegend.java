package name.abuchen.portfolio.ui.views;

import java.util.List;

import name.abuchen.portfolio.model.Values;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import de.engehausen.treemap.IRectangle;
import de.engehausen.treemap.ISelectionChangeListener;
import de.engehausen.treemap.ITreeModel;
import de.engehausen.treemap.swt.TreeMap;

/* package */class TreeMapLegend extends Composite
{
    private TreeMapColorProvider colorProvider;

    private TreeMapItem rootItem;

    public TreeMapLegend(Composite parent, TreeMap<TreeMapItem> treeMap)
    {
        super(parent, SWT.NONE);

        setBackground(parent.getBackground());

        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.wrap = true;
        layout.pack = true;
        layout.justify = false;
        setLayout(layout);

        treeMap.addSelectionChangeListener(new ISelectionChangeListener<TreeMapItem>()
        {
            @Override
            public void selectionChanged(ITreeModel<IRectangle<TreeMapItem>> model, IRectangle<TreeMapItem> rectangle, String label)
            {
                TreeMapLegend.this.selectionChanged(model, rectangle, label);
            }
        });
    }

    public void setColorProvider(TreeMapColorProvider colorProvider)
    {
        this.colorProvider = colorProvider;
    }

    public void setRootItem(TreeMapItem rootItem)
    {
        this.rootItem = rootItem;

        for (Control control : this.getChildren())
            control.dispose();

        boolean hasParent = false;

        List<TreeMapItem> path = rootItem.getPath();
        for (int ii = 1; ii < path.size(); ii++)
        {
            hasParent = true;
            TreeMapItem item = path.get(ii);
            new LegendItem(this, item);
        }

        if (hasParent && !rootItem.getChildren().isEmpty())
        {
            Label l = new Label(this, SWT.NONE);
            l.setText(">>"); //$NON-NLS-1$
            l.setBackground(this.getBackground());
        }

        for (TreeMapItem child : rootItem.getChildren())
            new LegendItem(this, child);

        pack();
        getParent().layout();
    }

    private void selectionChanged(ITreeModel<IRectangle<TreeMapItem>> model, IRectangle<TreeMapItem> rectangle, String label)
    {
        // find out if root changed (drill-down)
        TreeMapItem newRoot = model.getRoot().getNode();
        if (newRoot != rootItem)
            setRootItem(newRoot);
    }

    public class LegendItem extends Canvas implements Listener
    {
        private final TreeMapItem item;

        public LegendItem(Composite parent, TreeMapItem item)
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

            colorProvider.drawRectangle(item, e.gc, r);

            String text = item.toString();
            String info = getInfo();

            GC gc = e.gc;
            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            gc.drawString(text, 2, 2, true);
            Point extent = gc.stringExtent(text);
            gc.drawString(info, 2, extent.y + 1, true);

            e.gc.setForeground(oldForeground);
            e.gc.setBackground(oldBackground);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            String text = item.toString();
            String info = getInfo();

            GC gc = new GC(this);
            Point extentText = gc.stringExtent(text);
            Point extentInfo = gc.stringExtent(info);
            gc.dispose();

            return new Point(Math.max(extentText.x, extentInfo.x) + 4, extentText.y + extentInfo.y + 4);
        }

        private String getInfo()
        {
            return String.format("%s (%s%%)", Values.Amount.format(item.getValuation()), //$NON-NLS-1$
                            Values.Percent.format(item.getPercentage()));
        }

    }
}
