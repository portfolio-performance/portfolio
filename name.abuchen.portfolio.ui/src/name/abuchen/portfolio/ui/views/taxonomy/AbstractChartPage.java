package name.abuchen.portfolio.ui.views.taxonomy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Menu;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;

public abstract class AbstractChartPage extends Page
{
    private Menu configMenu;

    public AbstractChartPage(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        Action action = new SimpleAction(Messages.LabelIncludeUnassignedCategoryInCharts, a -> {
            getModel().setExcludeUnassignedCategoryInCharts(!getModel().isUnassignedCategoryInChartsExcluded());
            onConfigChanged();
        });
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
