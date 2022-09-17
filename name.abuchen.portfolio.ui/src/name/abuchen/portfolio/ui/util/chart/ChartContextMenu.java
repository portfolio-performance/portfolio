package name.abuchen.portfolio.ui.util.chart;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.swtchart.Chart;
import org.swtchart.IAxis;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;
import name.abuchen.portfolio.util.TextUtil;

/* package */class ChartContextMenu
{
    private static final int DEFAULT_FILE_EXTENSION = 2;

    private static final String[] EXTENSIONS = new String[] { "*.jpeg", "*.jpg", "*.png" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    private Chart chart;
    private Menu contextMenu;

    private static int lastUsedFileExtension = DEFAULT_FILE_EXTENSION;

    public ChartContextMenu(Chart chart)
    {
        this.chart = chart;

        MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> configMenuAboutToShow(manager));

        contextMenu = menuMgr.createContextMenu(chart);
        chart.getPlotArea().setMenu(contextMenu);

        chart.getPlotArea().addDisposeListener(e -> dispose());
    }

    private void configMenuAboutToShow(IMenuManager manager)
    {
        Action actionAdjustRange = new Action(Messages.MenuChartAdjustRange)
        {
            @Override
            public void run()
            {
                if (chart instanceof ScatterChart)
                    ((ScatterChart) chart).adjustRange();
                else if (chart instanceof TimelineChart)
                    ((TimelineChart) chart).adjustRange();
                else
                    chart.getAxisSet().adjustRange();

                chart.redraw();
            }
        };
        actionAdjustRange.setAccelerator('0');
        manager.add(actionAdjustRange);

        manager.add(new Separator());
        addZoomActions(manager);

        manager.add(new Separator());
        addMoveActions(manager);

        manager.add(new Separator());
        exportMenuAboutToShow(manager, chart.getTitle().getText());
    }

    private void addZoomActions(IMenuManager manager)
    {
        Action actionZoomIn = new Action(Messages.MenuChartYZoomIn)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.zoomIn();
                chart.redraw();
            }
        };
        actionZoomIn.setAccelerator(SWT.MOD1 | SWT.ARROW_UP);
        manager.add(actionZoomIn);

        Action actionZoomOut = new Action(Messages.MenuChartYZoomOut)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.zoomOut();
                chart.redraw();
            }
        };
        actionZoomOut.setAccelerator(SWT.MOD1 | SWT.ARROW_DOWN);
        manager.add(actionZoomOut);

        Action actionYZoomIn = new Action(Messages.MenuChartXZoomOut)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.zoomIn();
                chart.redraw();
            }
        };
        actionYZoomIn.setAccelerator(SWT.MOD1 | SWT.ARROW_LEFT);
        manager.add(actionYZoomIn);

        Action actionXZoomOut = new Action(Messages.MenuChartXZoomIn)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.zoomOut();
                chart.redraw();
            }
        };
        actionXZoomOut.setAccelerator(SWT.MOD1 | SWT.ARROW_RIGHT);
        manager.add(actionXZoomOut);

    }

    private void addMoveActions(IMenuManager manager)
    {
        Action actionMoveUp = new Action(Messages.MenuChartYScrollUp)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.scrollUp();
                chart.redraw();
            }
        };
        actionMoveUp.setAccelerator(SWT.ARROW_UP);
        manager.add(actionMoveUp);

        Action actionMoveDown = new Action(Messages.MenuChartYScrollDown)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getYAxes())
                    axis.scrollDown();
                chart.redraw();
            }
        };
        actionMoveDown.setAccelerator(SWT.ARROW_DOWN);
        manager.add(actionMoveDown);

        Action actionMoveLeft = new Action(Messages.MenuChartXScrollDown)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.scrollDown();
                chart.redraw();
            }
        };
        actionMoveLeft.setAccelerator(SWT.ARROW_LEFT);
        manager.add(actionMoveLeft);

        Action actionMoveRight = new Action(Messages.MenuChartXScrollUp)
        {
            @Override
            public void run()
            {
                for (IAxis axis : chart.getAxisSet().getXAxes())
                    axis.scrollUp();
                chart.redraw();
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
                IRunnableWithProgress saveOperation = new IRunnableWithProgress()
                {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        FileDialog dialog = new FileDialog(chart.getShell(), SWT.SAVE);
                        dialog.setFileName(TextUtil.sanitizeFilename(label));
                        dialog.setFilterExtensions(EXTENSIONS);
                        dialog.setFilterIndex(lastUsedFileExtension);
                        dialog.setOverwrite(true);

                        String filename = dialog.open();
                        if (filename == null)
                            return;

                        lastUsedFileExtension = dialog.getFilterIndex();
                        if (lastUsedFileExtension == -1)
                            lastUsedFileExtension = DEFAULT_FILE_EXTENSION;

                        int format;
                        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) //$NON-NLS-1$ //$NON-NLS-2$
                            format = SWT.IMAGE_JPEG;
                        else if (filename.endsWith(".png")) //$NON-NLS-1$
                            format = SWT.IMAGE_PNG;
                        else
                        {
                            if (lastUsedFileExtension == 0 || lastUsedFileExtension == 1)
                            {
                                filename = filename + ".jpg"; //$NON-NLS-1$
                                format = SWT.IMAGE_JPEG;
                            }
                            else
                            {
                                filename = filename + ".png"; //$NON-NLS-1$
                                format = SWT.IMAGE_PNG;
                            }
                        }

                        boolean isChartTitleVisible = chart.getTitle().isVisible();
                        boolean isChartLegendVisible = chart.getLegend().isVisible();
                        try
                        {
                            chart.suspendUpdate(true);
                            chart.getTitle().setVisible(true);
                            chart.getLegend().setVisible(true);
                            chart.getLegend().setPosition(SWT.BOTTOM);
                            chart.suspendUpdate(false);
                            chart.save(filename, format);
                        }
                        finally
                        {
                            chart.suspendUpdate(true);
                            chart.getTitle().setVisible(isChartTitleVisible);
                            chart.getLegend().setVisible(isChartLegendVisible);
                            chart.suspendUpdate(false);
                        }
                    }

                };
                try
                {
                    new ProgressMonitorDialog(chart.getShell()).run(false, false, saveOperation);
                }
                catch (InvocationTargetException | InterruptedException e)
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
