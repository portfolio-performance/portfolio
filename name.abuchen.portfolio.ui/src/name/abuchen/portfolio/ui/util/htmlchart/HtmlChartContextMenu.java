package name.abuchen.portfolio.ui.util.htmlchart;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.PortfolioPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
        exportMenuAboutToShow(manager, chart.getChartConfig().getTitle());
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
