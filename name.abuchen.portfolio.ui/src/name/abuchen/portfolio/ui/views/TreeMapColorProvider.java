package name.abuchen.portfolio.ui.views;

import java.util.List;

import name.abuchen.portfolio.model.IndustryClassification.Category;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.views.IndustryClassificationView.Item;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/* package */class TreeMapColorProvider
{
    private ColorWheel colorWheel;

    public TreeMapColorProvider(Control owner, int size)
    {
        colorWheel = new ColorWheel(owner, size);
    }

    public ColorWheel.Segment getSegment(Item item)
    {
        List<Item> path = item.getPath();

        if (path.size() == 1)
            return colorWheel.new Segment(Colors.HEADINGS.swt());

        ColorWheel.Segment segment = null;
        Item rootLevel = path.get(0);
        Item firstLevel = path.get(1);

        if (firstLevel.isCategory() && Category.OTHER_ID.equals(firstLevel.getCategory().getId()))
            segment = colorWheel.new Segment(Colors.OTHER_CATEGORY.swt());
        else
            segment = colorWheel.getSegment(rootLevel.getChildren().indexOf(firstLevel));

        if (path.size() == 2)
            return segment;
        else
            return segment.getShade(path.get(1).getChildren().indexOf(path.get(2)));
    }

    public void drawRectangle(Item item, GC gc, Rectangle r)
    {
        ColorWheel.Segment segment = getSegment(item);

        gc.setBackground(segment.getColor());
        gc.fillRectangle(r.x, r.y, r.width, r.height);

        gc.setForeground(segment.getDarkerColor());
        gc.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1);
        gc.drawLine(r.x + r.width - 1, r.y, r.x + r.width - 1, r.y + r.height - 1);

        gc.setForeground(segment.getBrigherColor());
        gc.drawLine(r.x, r.y, r.x + r.width, r.y);
        gc.drawLine(r.x, r.y, r.x, r.y + r.height);
    }

}
