package name.abuchen.portfolio.ui.views.dataseries;

import java.util.Collections;
import java.util.ResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.swtchart.LineStyle;

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

        RowLayout layout = new RowLayout();
        layout.wrap = true;
        layout.pack = true;
        layout.fill = true;
        setLayout(layout);

        for (DataSeries series : configurator.getSelectedDataSeries())
            new PaintItem(this, series);

        this.configurator.addListener(this::onUpdate);
    }

    private void onUpdate()
    {
        for (Control child : getChildren())
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

        private Color colorFor(RGB color)
        {
            DataSeriesChartLegend legend = (DataSeriesChartLegend) getParent();
            return legend.resources.createColor(color);
        }

        private void paintControl(Event e)
        {
            Color oldForeground = e.gc.getForeground();
            Color oldBackground = e.gc.getBackground();

            Point size = getSize();
            Rectangle r = new Rectangle(0, 0, size.y, size.y);
            GC gc = e.gc;

            gc.setBackground(colorFor(series.getColor()));
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
                    position.add(new SimpleAction(Messages.ChartSendBackwards, a -> {
                        Collections.swap(configurator.getSelectedDataSeries(), index, index - 1);
                        configurator.fireUpdate();
                    }));

                    position.add(new SimpleAction(Messages.ChartSendToBack, a -> {
                        DataSeries s = configurator.getSelectedDataSeries().remove(index);
                        configurator.getSelectedDataSeries().add(0, s);
                        configurator.fireUpdate();
                    }));

                }

                if (index < configurator.getSelectedDataSeries().size() - 1)
                {
                    position.add(new SimpleAction(Messages.ChartBringForward, a -> {
                        Collections.swap(configurator.getSelectedDataSeries(), index, index + 1);
                        configurator.fireUpdate();
                    }));

                    position.add(new SimpleAction(Messages.ChartBringToFront, a -> {
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
}
