package name.abuchen.portfolio.ui.views.taxonomy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import name.abuchen.portfolio.ui.Messages;

public abstract class AbstractChartPage extends Page
{
    private Menu configMenu;

    public AbstractChartPage(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public final void showConfigMenu(Shell shell)
    {
        if (configMenu == null)
        {
            MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
            menuMgr.setRemoveAllWhenShown(true);
            menuMgr.addMenuListener(manager -> initializeConfigMenu(manager));

            configMenu = menuMgr.createContextMenu(shell);
        }

        configMenu.setVisible(true);
    }

    protected void initializeConfigMenu(IMenuManager manager)
    {
        Action action = new Action(Messages.LabelIncludeUnassignedCategoryInCharts)
        {
            @Override
            public void run()
            {
                getModel().setExcludeUnassignedCategoryInCharts(!getModel().isUnassignedCategoryInChartsExcluded());
                onConfigChanged();
            }
        };
        action.setChecked(!getModel().isUnassignedCategoryInChartsExcluded());
        manager.add(action);
    }

    public abstract void onConfigChanged();

    @Override
    public void dispose()
    {
        if (configMenu != null && !configMenu.isDisposed())
            configMenu.dispose();

        super.dispose();
    }
}
