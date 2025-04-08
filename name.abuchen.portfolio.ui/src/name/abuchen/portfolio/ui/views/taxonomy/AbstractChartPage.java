package name.abuchen.portfolio.ui.views.taxonomy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.SimpleAction;

public abstract class AbstractChartPage extends Page
{
    protected AbstractChartPage(TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        Action action = new SimpleAction(Messages.LabelIncludeUnassignedCategoryInCharts, a -> {
            getModel().setExcludeUnassignedCategoryInCharts(!getModel().isUnassignedCategoryInChartsExcluded());
            getModel().fireTaxonomyModelChange(getModel().getVirtualRootNode());
        });
        action.setChecked(!getModel().isUnassignedCategoryInChartsExcluded());
        manager.add(action);
    }
}
