package name.abuchen.portfolio.ui.views.dataseries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.swtchart.LineStyle;

import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;

/**
 * A legend for charts to configure data series, e.g. color, area fill, and line
 * type.
 */
public class DataSeriesChartLegend extends Composite
{
    private final DataSeriesConfigurator configurator;
    private final LocalResourceManager resources;

    /**
     * Constructor.
     * 
     * @param parent
     *            the parent composite
     * @param configurator
     *            the chart configurator
     */
    public DataSeriesChartLegend(Composite parent, DataSeriesConfigurator configurator)
    {
        super(parent, SWT.NONE);

        this.configurator = configurator;
        this.resources = new LocalResourceManager(JFaceResources.getResources(), parent);

        setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

        setLayout(new RowPlusChevronLayout(this));

        for (DataSeries series : configurator.getSelectedDataSeries())
            new PaintItem(this, series);

        this.configurator.addListener(this::onUpdate);
    }

    private void onUpdate()
    {
        for (Control child : getChildren())
            if (child instanceof PaintItem)
                child.dispose();

        for (DataSeries series : configurator.getSelectedDataSeries())
            new PaintItem(this, series);

        layout();
        getParent().layout();
    }

    private static final class PaintItem extends Canvas implements Listener // NOSONAR
    {
        private static final ResourceBundle LABELS = ResourceBundle.getBundle("name.abuchen.portfolio.ui.views.labels"); //$NON-NLS-1$

        private final DataSeries series;

        private PaintItem(Composite parent, DataSeries series)
        {
            super(parent, SWT.NONE);

            this.series = series;

            setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

            addListener(SWT.Paint, this);
            addListener(SWT.Resize, this);

            MenuManager menuManager = new MenuManager();
            menuManager.setRemoveAllWhenShown(true);
            menuManager.addMenuListener(this::seriesMenuAboutToShow);
            setMenu(menuManager.createContextMenu(this));
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

        private Color getColor()
        {
            DataSeriesChartLegend legend = (DataSeriesChartLegend) getParent();
            return legend.resources.createColor(series.getColor());
        }

        private void paintControl(Event e)
        {
            Color oldForeground = e.gc.getForeground();
            Color oldBackground = e.gc.getBackground();

            Point size = getSize();
            Rectangle r = new Rectangle(0, 0, size.y, size.y);
            GC gc = e.gc;

            gc.setBackground(getColor());
            gc.fillRectangle(r.x, r.y, r.width, r.height);

            gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            gc.drawRectangle(r.x, r.y, r.width - 1, r.height - 1);

            String text = series.getLabel();

            e.gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            e.gc.drawString(text, size.y + 2, 1, true);

            e.gc.setForeground(oldForeground);
            e.gc.setBackground(oldBackground);
        }

        @Override
        public Point computeSize(int wHint, int hHint, boolean changed)
        {
            String text = series.getLabel();

            GC gc = new GC(this);
            Point extentText = gc.stringExtent(text);
            gc.dispose();

            return new Point(extentText.x + extentText.y + 12, extentText.y + 2);
        }

        private void seriesMenuAboutToShow(IMenuManager manager) // NOSONAR
        {
            DataSeriesConfigurator configurator = ((DataSeriesChartLegend) getParent()).configurator;

            manager.add(new Action(Messages.ChartSeriesPickerColor)
            {
                @Override
                public void run()
                {
                    ColorDialog colorDialog = new ColorDialog(Display.getDefault().getActiveShell());
                    colorDialog.setRGB(series.getColor());
                    RGB newColor = colorDialog.open();
                    if (newColor != null)
                    {
                        series.setColor(newColor);
                        configurator.fireUpdate();
                    }
                }
            });

            if (series.isLineChart())
            {
                MenuManager lineStyle = new MenuManager(Messages.ChartSeriesPickerLineStyle);
                for (final LineStyle style : LineStyle.values())
                {
                    if (style == LineStyle.NONE)
                        continue;

                    Action action = new SimpleAction(LABELS.getString("lineStyle." + style.name()), a -> { //$NON-NLS-1$
                        series.setLineStyle(style);
                        configurator.fireUpdate();
                    });
                    action.setChecked(style == series.getLineStyle());
                    lineStyle.add(action);
                }
                manager.add(lineStyle);

                Action actionShowArea = new SimpleAction(Messages.ChartSeriesPickerShowArea, a -> {
                    series.setShowArea(!series.isShowArea());
                    configurator.fireUpdate();
                });
                actionShowArea.setChecked(series.isShowArea());
                manager.add(actionShowArea);
            }

            if (configurator.getSelectedDataSeries().size() > 1)
            {
                manager.add(new Separator());

                MenuManager position = new MenuManager(Messages.ChartMenuPosition);
                manager.add(position);

                int index = configurator.getSelectedDataSeries().indexOf(series);

                if (index > 0)
                {
                    position.add(new SimpleAction(Messages.ChartBringForward, a -> {
                        Collections.swap(configurator.getSelectedDataSeries(), index, index - 1);
                        configurator.fireUpdate();
                    }));

                    position.add(new SimpleAction(Messages.ChartBringToFront, a -> {
                        DataSeries s = configurator.getSelectedDataSeries().remove(index);
                        configurator.getSelectedDataSeries().add(0, s);
                        configurator.fireUpdate();
                    }));

                }

                if (index < configurator.getSelectedDataSeries().size() - 1)
                {
                    position.add(new SimpleAction(Messages.ChartSendBackwards, a -> {
                        Collections.swap(configurator.getSelectedDataSeries(), index, index + 1);
                        configurator.fireUpdate();
                    }));

                    position.add(new SimpleAction(Messages.ChartSendToBack, a -> {
                        DataSeries s = configurator.getSelectedDataSeries().remove(index);
                        configurator.getSelectedDataSeries().add(s);
                        configurator.fireUpdate();
                    }));

                }

                MenuManager sorting = new MenuManager(Messages.ChartMenuSorting);
                manager.add(sorting);

                sorting.add(new SimpleAction(Messages.ChartSortAZ, a -> {
                    Collections.sort(configurator.getSelectedDataSeries(),
                                    (r, l) -> r.getLabel().compareTo(l.getLabel()));
                    configurator.fireUpdate();
                }));
                sorting.add(new SimpleAction(Messages.ChartSortZA, a -> {
                    Collections.sort(configurator.getSelectedDataSeries(),
                                    (r, l) -> l.getLabel().compareTo(r.getLabel()));
                    configurator.fireUpdate();
                }));
            }

            manager.add(new Separator());
            manager.add(new SimpleAction(Messages.ChartSeriesPickerRemove, a -> configurator.doDeleteSeries(series)));
        }
    }

    /**
     * Displays at maximum LINES lines of legend items and displays a chevron
     * menu with the additional hidden items.
     */
    private class RowPlusChevronLayout extends Layout
    {
        private static final int LINES = 2;
        private static final int PADDING = 5;
        private static final int MARGIN = 5;

        private ImageHyperlink chevron;
        private Menu chevronMenu;

        private List<PaintItem> invisible = new ArrayList<>();
        private Map<Color, Image> colorRectangles = new HashMap<>();

        private RowPlusChevronLayout(Composite host)
        {
            this.chevron = new ImageHyperlink(host, SWT.PUSH);
            this.chevron.setImage(Images.CHEVRON.image());
            this.chevron.addHyperlinkListener(new HyperlinkAdapter()
            {
                @Override
                public void linkActivated(HyperlinkEvent e)
                {
                    ImageHyperlink item = (ImageHyperlink) e.widget;

                    if (chevronMenu == null)
                    {
                        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
                        menuMgr.setRemoveAllWhenShown(true);
                        menuMgr.addMenuListener(mgr -> overflowMenuAboutToShow(mgr));

                        chevronMenu = menuMgr.createContextMenu(item.getParent());
                    }

                    Rectangle rect = item.getBounds();
                    Point pt = item.getParent().toDisplay(new Point(rect.x, rect.y));

                    chevronMenu.setLocation(pt.x, pt.y + rect.height);
                    chevronMenu.setVisible(true);

                    item.addDisposeListener(event -> chevronMenu.dispose());
                }
            });

            this.chevron.addDisposeListener(e -> colorRectangles.values().stream().forEach(image -> image.dispose()));
        }

        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache)
        {
            return layout(composite, wHint);
        }

        @Override
        protected void layout(Composite composite, boolean flushCache)
        {
            Rectangle clientArea = composite.getClientArea();
            layout(composite, clientArea.width);
        }

        private Point layout(Composite composite, int wHint)
        {
            invisible.clear();

            Point chevronSize = this.chevron.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            chevron.setVisible(false);

            PaintItem[] children = getChildren(composite);
            Point[] sizes = new Point[children.length];

            for (int ii = 0; ii < children.length; ii++)
                sizes[ii] = children[ii].computeSize(SWT.DEFAULT, SWT.DEFAULT);

            int x = MARGIN - PADDING;
            int y = MARGIN - PADDING;

            int line = 0;
            int lineHeight = 0;

            int width = 0;
            int height = 0;

            for (int ii = 0; ii < sizes.length; ii++)
            {
                // add the legend item to the line if
                // a) we anyway only have one line (SWT.DEFAULT), or
                // b) it is the last item and it fits, or
                // c) it is not the last item but the chevron would still fit

                if (wHint == SWT.DEFAULT || //
                                (line < LINES - 1 && x + PADDING + sizes[ii].x < wHint - MARGIN) || //
                                (line == LINES - 1 && x + 1 == children.length
                                                && x + PADDING + sizes[ii].x < wHint - MARGIN)
                                || //
                                (line == LINES - 1 && x + PADDING + sizes[ii].x + PADDING + chevronSize.x < wHint
                                                - MARGIN))
                {
                    children[ii].setBounds(x + PADDING, y + PADDING, sizes[ii].x, sizes[ii].y);
                    children[ii].setVisible(true);

                    x += PADDING + sizes[ii].x;
                    lineHeight = Math.max(lineHeight, sizes[ii].y);

                    width = Math.max(x, width);
                    height = Math.max(y + PADDING + sizes[ii].y, height);
                }
                else
                {
                    if (line < LINES - 1) // new line
                    {
                        x = MARGIN - PADDING;
                        y += PADDING + lineHeight;

                        line++;
                        lineHeight = 0;

                        children[ii].setBounds(x + PADDING, y + PADDING, sizes[ii].x, sizes[ii].y);
                        children[ii].setVisible(true);

                        x += PADDING + sizes[ii].x;
                        lineHeight = Math.max(lineHeight, sizes[ii].y);

                        width = Math.max(x, width);
                        height = Math.max(y + PADDING + sizes[ii].y, height);
                    }
                    else // chevron
                    {
                        invisible.add((PaintItem) children[ii]);
                        children[ii].setVisible(false);

                        chevron.setBounds(x + PADDING, y + PADDING, chevronSize.x, chevronSize.y);
                        chevron.setVisible(true);

                        x += PADDING + chevronSize.x;
                        lineHeight = Math.max(lineHeight, sizes[ii].y);

                        width = Math.max(x, width);
                        height = Math.max(y + PADDING + sizes[ii].y, height);

                        for (int jj = ii + 1; jj < children.length; jj++)
                        {
                            invisible.add((PaintItem) children[jj]);
                            children[jj].setVisible(false);
                        }

                        break;
                    }
                }
            }

            return new Point(width + MARGIN, height + MARGIN);
        }

        private PaintItem[] getChildren(Composite composite)
        {
            Control[] children = composite.getChildren();

            PaintItem[] answer = new PaintItem[children.length - 1];

            int index = 0;
            for (int ii = 0; ii < children.length; ii++)
            {
                if (children[ii] instanceof PaintItem)
                    answer[index++] = (PaintItem) children[ii];
            }

            return answer;
        }

        private void overflowMenuAboutToShow(IMenuManager manager)
        {
            for (PaintItem item : invisible)
            {
                Image image = colorRectangles.computeIfAbsent(item.getColor(), color -> {
                    Image i = new Image(null, 16, 16);
                    GC gc = new GC(i);
                    gc.setBackground(color);
                    gc.fillRectangle(0, 0, 16, 16);
                    gc.dispose();
                    return i;
                });

                MenuManager mgr = new MenuManager(item.series.getLabel(), ImageDescriptor.createFromImage(image), null);
                manager.add(mgr);

                item.seriesMenuAboutToShow(mgr);
            }
        }
    }
}
