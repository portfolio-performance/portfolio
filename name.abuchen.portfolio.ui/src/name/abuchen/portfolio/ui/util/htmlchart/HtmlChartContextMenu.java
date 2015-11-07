package name.abuchen.portfolio.ui.util.htmlchart;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;

/* package */class HtmlChartContextMenu
{
    private static final String[] EXTENSIONS = new String[] { "*.jpeg", "*.jpg", "*.png" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private HtmlChart chart;
    private Menu contextMenu;

    public HtmlChartContextMenu(HtmlChart chart)
    {
        this.chart = chart;

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> configMenuAboutToShow(manager));

        contextMenu = menuMgr.createContextMenu(chart.getBrowserControl());
        chart.getBrowserControl().setMenu(contextMenu);
        chart.getBrowserControl().addDisposeListener(e -> dispose());
    }

    private void configMenuAboutToShow(IMenuManager manager)
    {
        Action actionAdjustRange = new Action(Messages.MenuChartAdjustRange)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.resetXY();
                    chart.refreshChart();
                }
            }
        };
        actionAdjustRange.setAccelerator('0');
        manager.add(actionAdjustRange);

        manager.add(new Separator());
        addZoomActions(manager);

        manager.add(new Separator());
        addMoveActions(manager);

        manager.add(new Separator());
        exportMenuAboutToShow(manager, chart.getChartConfig().getTitle());
    }

    private void addZoomActions(IMenuManager manager)
    {
        Action actionYZoomIn = new Action(Messages.MenuChartYZoomIn)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.zoomInY();
                    chart.refreshChart();
                }
            }
        };
        actionYZoomIn.setAccelerator(SWT.MOD1 | SWT.ARROW_UP);
        manager.add(actionYZoomIn);

        Action actionYZoomOut = new Action(Messages.MenuChartYZoomOut)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.zoomOutY();
                    chart.refreshChart();
                }
            }
        };
        actionYZoomOut.setAccelerator(SWT.MOD1 | SWT.ARROW_DOWN);
        manager.add(actionYZoomOut);

        Action actionXZoomOut = new Action(Messages.MenuChartXZoomOut)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.zoomOutX();
                    chart.refreshChart();
                }
            }
        };
        actionXZoomOut.setAccelerator(SWT.MOD1 | SWT.ARROW_LEFT);
        manager.add(actionXZoomOut);

        Action actionXZoomIn = new Action(Messages.MenuChartXZoomIn)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.zoomInX();
                    chart.refreshChart();
                }
            }
        };
        actionXZoomIn.setAccelerator(SWT.MOD1 | SWT.ARROW_RIGHT);
        manager.add(actionXZoomIn);

    }

    private void addMoveActions(IMenuManager manager)
    {
        Action actionMoveUp = new Action(Messages.MenuChartYScrollUp)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.scrollUp();
                    chart.refreshChart();
                }
            }
        };
        actionMoveUp.setAccelerator(SWT.ARROW_UP);
        manager.add(actionMoveUp);

        Action actionMoveDown = new Action(Messages.MenuChartYScrollDown)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.scrollDown();
                    chart.refreshChart();
                }
            }
        };
        actionMoveDown.setAccelerator(SWT.ARROW_DOWN);
        manager.add(actionMoveDown);

        Action actionMoveLeft = new Action(Messages.MenuChartXScrollDown)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.scrollLeft();
                    chart.refreshChart();
                }
            }
        };
        actionMoveLeft.setAccelerator(SWT.ARROW_LEFT);
        manager.add(actionMoveLeft);

        Action actionMoveRight = new Action(Messages.MenuChartXScrollUp)
        {
            @Override
            public void run()
            {
                if (chart.getChartConfig() instanceof HtmlChartConfigTimeline)
                {
                    HtmlChartConfigTimeline config = (HtmlChartConfigTimeline) chart.getChartConfig();
                    config.scrollRight();
                    chart.refreshChart();
                }
            }
        };
        actionMoveRight.setAccelerator(SWT.ARROW_RIGHT);
        manager.add(actionMoveRight);
    }

    public void exportMenuAboutToShow(IMenuManager manager, final String label)
    {
        manager.add(new Action(Messages.MenuExportDiagram)
        {
            @Override
            public void run()
            {
                FileDialog dialog = new FileDialog(chart.getBrowserControl().getShell(), SWT.SAVE);
                dialog.setFileName(label);
                dialog.setFilterExtensions(EXTENSIONS);

                String filename = dialog.open();
                if (filename == null) { return; }

                int format;
                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) //$NON-NLS-1$ //$NON-NLS-2$
                    format = SWT.IMAGE_JPEG;
                else if (filename.endsWith(".png")) //$NON-NLS-1$
                    format = SWT.IMAGE_PNG;
                else
                    format = SWT.IMAGE_UNDEFINED;

                if (format != SWT.IMAGE_UNDEFINED)
                    try
                    {
                        chart.save(filename, format);
                    }
                    catch (Exception e)
                    {
                        PortfolioPlugin.log(e);
                    }
            }
        });
    }

    private void dispose()
    {
        if (contextMenu != null && !contextMenu.isDisposed())
            contextMenu.dispose();
    }
}
