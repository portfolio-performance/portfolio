package name.abuchen.portfolio.ui.views.taxonomy;

import jakarta.inject.Inject;

import org.eclipse.e4.core.di.extensions.Preference;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.editor.AbstractFinanceView;
import name.abuchen.portfolio.ui.util.EmbeddedBrowser;
import name.abuchen.portfolio.ui.util.SimpleAction;
import name.abuchen.portfolio.ui.views.IPieChart;

/* package */class PieChartViewer extends AbstractChartPage
{
    private AbstractFinanceView view;

    private IPieChart chart;

    @Inject
    @Preference(UIConstants.Preferences.ENABLE_SWTCHART_PIECHARTS)
    boolean useSWTCharts;

    @Inject
    public PieChartViewer(AbstractFinanceView view, TaxonomyModel model, TaxonomyNodeRenderer renderer)
    {
        super(model, renderer);
        this.view = view;
    }

    @Override
    public void configMenuAboutToShow(IMenuManager manager)
    {
        super.configMenuAboutToShow(manager);

        Action action = new SimpleAction(Messages.LabelIncludeSecuritiesInPieChart, a -> {
            getModel().setExcludeSecuritiesInPieChart(!getModel().isSecuritiesInPieChartExcluded());
            chart.refresh(null);
        });
        action.setChecked(!getModel().isSecuritiesInPieChartExcluded());
        manager.add(action);
    }

    @Override
    public Control createControl(Composite container)
    {
        if (this.useSWTCharts)
        {
            chart = new TaxonomyPieChartSWT(this, view);
        }
        else
        {
            chart = new TaxonomyPieChartBrowser(make(EmbeddedBrowser.class), view, this);
        }
        return chart.createControl(container);
    }

    @Override
    public void beforePage()
    {
        // nothing to do
    }

    @Override
    public void afterPage()
    {
        // nothing to do
    }

    @Override
    public void nodeChange(TaxonomyNode node)
    {
        chart.refresh(null);
    }
}
