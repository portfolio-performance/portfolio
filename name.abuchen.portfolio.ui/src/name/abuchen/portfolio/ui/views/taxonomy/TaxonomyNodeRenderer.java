package name.abuchen.portfolio.ui.views.taxonomy;

import java.util.List;

import name.abuchen.portfolio.util.ColorConversion;

import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

/* package */class TaxonomyNodeRenderer
{
    private final class Segment
    {
        private Color primary;
        private Color brighter;
        private Color darker;

        public Segment(String color)
        {
            RGB rgb = ColorConversion.hex2RGB(color);
            createColors(rgb, rgb.getHSB());
        }

        private void createColors(RGB rgb, float[] hsb)
        {
            primary = resources.createColor(rgb);
            brighter = resources.createColor(new RGB(hsb[0], hsb[1], Math.min(1.0f, hsb[2] + 0.05f)));
            darker = resources.createColor(new RGB(hsb[0], hsb[1], Math.max(0f, hsb[2] - 0.05f)));
        }
    }

    private LocalResourceManager resources;

    public TaxonomyNodeRenderer(LocalResourceManager resources)
    {
        this.resources = resources;
    }

    public void drawRectangle(TaxonomyNode rootItem, TaxonomyNode item, GC gc, Rectangle r)
    {
        Segment segment = getSegment(rootItem, item);

        gc.setBackground(segment.primary);
        gc.fillRectangle(r.x, r.y, r.width, r.height);

        gc.setForeground(segment.darker);
        gc.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1);
        gc.drawLine(r.x + r.width - 1, r.y, r.x + r.width - 1, r.y + r.height - 1);

        gc.setForeground(segment.brighter);
        gc.drawLine(r.x, r.y, r.x + r.width, r.y);
        gc.drawLine(r.x, r.y, r.x, r.y + r.height);
    }

    public Color getColorFor(TaxonomyNode node)
    {
        return getSegment(null, node).primary;
    }

    private Segment getSegment(TaxonomyNode rootItem, TaxonomyNode item)
    {
        if (rootItem == null || item.isRoot() || rootItem.equals(item))
            return new Segment(item.getColor());

        List<TaxonomyNode> path = item.getPath();
        int index = path.indexOf(rootItem);

        if (index < 0) // root not found!
            return new Segment(item.getColor());

        if (path.size() <= index + 1)
            return new Segment(item.getColor());

        TaxonomyNode reference = path.get(index + 1);
        return new Segment(reference.getColor());
    }

}
